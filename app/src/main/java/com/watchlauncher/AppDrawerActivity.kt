package com.watchlauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kotlin.math.abs

class AppDrawerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var adapter: AppAdapter
    private lateinit var gestureDetector: GestureDetectorCompat
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_drawer)

        recyclerView = findViewById(R.id.appRecyclerView)
        searchBar    = findViewById(R.id.searchBar)

        applyTheme()
        setupRecyclerView()
        setupSearch()
        setupSwipeDown()
        loadApps()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun applyTheme() {
        val theme = ThemeManager.getTheme(this)
        val colors = ThemeManager.getColors(theme)
        val root = findViewById<View>(R.id.drawerRoot)
        root.setBackgroundColor(colors.background)

        // Style search bar
        val bg = GradientDrawable().apply {
            setColor(colors.surface())
            cornerRadius = 50f
            setStroke(1, colors.textSecondary)
        }
        searchBar.background = bg
        searchBar.setTextColor(colors.textPrimary)
        searchBar.setHintTextColor(colors.textSecondary)
    }

    private fun setupRecyclerView() {
        val theme = ThemeManager.getTheme(this)
        val colors = ThemeManager.getColors(theme)
        val cols = ThemeManager.getGridCols(this)
        val showLabels = ThemeManager.getShowLabels(this)

        adapter = AppAdapter(
            apps = emptyList(),
            colors = colors,
            showLabels = showLabels,
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app ->
                Toast.makeText(this, app.label, Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.layoutManager = GridLayoutManager(this, cols)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupSwipeDown() {
        val listener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                val dY = (e2?.y ?: 0f) - (e1?.y ?: 0f)
                val dX = abs((e1?.x ?: 0f) - (e2?.x ?: 0f))
                if (dY > 100 && dY > dX) {
                    finish()
                    overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
                    return true
                }
                return false
            }
            override fun onDown(e: MotionEvent) = true
        }
        gestureDetector = GestureDetectorCompat(this, listener)
        findViewById<View>(R.id.drawerRoot).setOnTouchListener { _, e ->
            gestureDetector.onTouchEvent(e); false
        }
    }

    private fun loadApps() {
        scope.launch {
            val apps = withContext(Dispatchers.IO) { queryAllApps() }
            adapter.updateApps(apps)
        }
    }

    private fun queryAllApps(): List<AppInfo> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return pm.queryIntentActivities(intent, 0)
            .map { ri -> AppInfo(
                label = ri.loadLabel(pm).toString(),
                packageName = ri.activityInfo.packageName,
                activityName = ri.activityInfo.name,
                icon = ri.loadIcon(pm)
            )}
            .sortedBy { it.label.lowercase() }
            .filter { it.packageName != packageName }
    }

    private fun launchApp(app: AppInfo) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) startActivity(intent)
        else Toast.makeText(this, "Cannot open", Toast.LENGTH_SHORT).show()
    }
}
