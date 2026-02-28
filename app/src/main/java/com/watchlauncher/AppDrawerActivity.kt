package com.watchlauncher

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class AppDrawerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var adapter: AppAdapter
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_drawer)
        recyclerView = findViewById(R.id.appRecyclerView)
        searchBar    = findViewById(R.id.searchBar)
        applyTheme()
        setupRecyclerView()
        setupSearch()
        loadApps()

        // Swipe down on root to go back home
        findViewById<View>(R.id.drawerRoot).setOnTouchListener(SwipeListener(
            onSwipeDown = { finish(); overridePendingTransition(R.anim.fade_in, R.anim.slide_down) }
        ))
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }

    private fun applyTheme() {
        val colors = ThemeManager.getColors(ThemeManager.getTheme(this))
        findViewById<View>(R.id.drawerRoot).setBackgroundColor(colors.background)
        val bg = GradientDrawable().apply { setColor(colors.surface()); cornerRadius = 50f }
        searchBar.background = bg
        searchBar.setTextColor(colors.textPrimary)
        searchBar.setHintTextColor(colors.textSecondary)
    }

    private fun setupRecyclerView() {
        val theme = ThemeManager.getTheme(this)
        val colors = ThemeManager.getColors(theme)
        adapter = AppAdapter(
            apps = emptyList(), colors = colors,
            showLabels = ThemeManager.getShowLabels(this),
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> Toast.makeText(this, app.label, Toast.LENGTH_SHORT).show() }
        )
        recyclerView.layoutManager = GridLayoutManager(this, ThemeManager.getGridCols(this))
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadApps() {
        scope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = packageManager
                val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                pm.queryIntentActivities(intent, 0)
                    .map { ri -> AppInfo(label = ri.loadLabel(pm).toString(),
                        packageName = ri.activityInfo.packageName,
                        activityName = ri.activityInfo.name, icon = ri.loadIcon(pm)) }
                    .sortedBy { it.label.lowercase() }
                    .filter { it.packageName != packageName }
            }
            adapter.updateApps(apps)
        }
    }

    private fun launchApp(app: AppInfo) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) startActivity(intent)
        else Toast.makeText(this, "Cannot open", Toast.LENGTH_SHORT).show()
    }
}
