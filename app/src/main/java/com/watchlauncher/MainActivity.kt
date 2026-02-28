package com.watchlauncher

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import kotlinx.coroutines.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var wallpaperView: ImageView
    private lateinit var clockView: ProClockView
    private lateinit var dockContainer: LinearLayout
    private lateinit var rootLayout: View
    private lateinit var gestureDetector: GestureDetectorCompat

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { loadDock() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)
        wallpaperView  = findViewById(R.id.wallpaperView)
        clockView      = findViewById(R.id.clockView)
        dockContainer  = findViewById(R.id.dockContainer)
        rootLayout     = findViewById(R.id.rootLayout)

        setupGestures()
        registerPackageReceiver()
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
        loadDock()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        try { unregisterReceiver(packageReceiver) } catch (_: Exception) {}
    }

    // ════════════════════════════════════════════════════════════════════════
    // THEME
    // ════════════════════════════════════════════════════════════════════════
    private fun applyTheme() {
        val theme = ThemeManager.getTheme(this)
        val face  = ThemeManager.getWatchFace(this)
        val colors = ThemeManager.getColors(theme)

        // Apply background
        rootLayout.setBackgroundColor(colors.background)

        // Apply watch face
        clockView.theme = theme
        clockView.watchFaceStyle = face

        // Wallpaper
        try {
            val wm = WallpaperManager.getInstance(this)
            val d  = wm.drawable
            if (d != null) {
                wallpaperView.setImageDrawable(d)
                wallpaperView.alpha = 0.25f   // subtle under the clock
            } else {
                wallpaperView.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
            }
        } catch (_: Exception) {
            wallpaperView.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        // Dock background
        val dockBg = GradientDrawable().apply {
            setColor(colors.dockBackground())
            cornerRadius = 60f
        }
        dockContainer.background = dockBg
    }

    // ════════════════════════════════════════════════════════════════════════
    // GESTURES
    // ════════════════════════════════════════════════════════════════════════
    private fun setupGestures() {
        val listener = object : GestureDetector.SimpleOnGestureListener() {

            override fun onFling(e1: MotionEvent?, e2: MotionEvent?,
                                 velocityX: Float, velocityY: Float): Boolean {
                val dY = (e1?.y ?: 0f) - (e2?.y ?: 0f)
                val dX = abs((e1?.x ?: 0f) - (e2?.x ?: 0f))
                return when {
                    // Swipe UP → app drawer
                    dY > 80 && dY > dX -> {
                        vibrate()
                        openAppDrawer(); true
                    }
                    // Swipe LEFT → watch face picker
                    -dX > 80 && -dX > dY -> {
                        openWatchFacePicker(); true
                    }
                    // Swipe RIGHT → theme picker
                    dX > 80 && dX > abs(dY) -> {
                        openThemePicker(); true
                    }
                    else -> false
                }
            }

            override fun onLongPress(e: MotionEvent) {
                vibrate()
                openSettings()
            }

            override fun onDown(e: MotionEvent) = true

            // Double-tap cycles watch face
            override fun onDoubleTap(e: MotionEvent): Boolean {
                cycleWatchFace(); return true
            }
        }

        gestureDetector = GestureDetectorCompat(this, listener)
        rootLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event); true
        }
    }

    private fun cycleWatchFace() {
        val faces = WatchFaceStyle.values()
        val cur = ThemeManager.getWatchFace(this)
        val next = faces[(faces.indexOf(cur) + 1) % faces.size]
        ThemeManager.setWatchFace(this, next)
        clockView.watchFaceStyle = next
        Toast.makeText(this, next.displayName, Toast.LENGTH_SHORT).show()
    }

    // ════════════════════════════════════════════════════════════════════════
    // DOCK
    // ════════════════════════════════════════════════════════════════════════
    private fun loadDock() {
        scope.launch {
            val apps = withContext(Dispatchers.IO) { queryPinnedApps() }
            buildDock(apps)
        }
    }

    private fun queryPinnedApps(): List<AppInfo> {
        val pm = packageManager
        val defaultPins = listOf(
            "com.google.android.apps.maps",
            "com.google.android.wearable.app",
            "com.android.settings",
            "com.google.android.gms"
        )
        return defaultPins.mapNotNull { pkg ->
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                AppInfo(
                    label = pm.getApplicationLabel(info).toString(),
                    packageName = pkg,
                    activityName = "",
                    icon = pm.getApplicationIcon(pkg),
                    isPinned = true
                )
            } catch (_: PackageManager.NameNotFoundException) { null }
        }
    }

    private fun buildDock(apps: List<AppInfo>) {
        dockContainer.removeAllViews()
        val theme  = ThemeManager.getTheme(this)
        val colors = ThemeManager.getColors(theme)
        val size   = dpToPx(44)
        val margin = dpToPx(5)
        val padding = dpToPx(6)

        apps.take(5).forEach { app ->
            val container = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(size + padding * 2, size + padding * 2).also {
                    it.marginStart = margin; it.marginEnd = margin
                }
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding(padding, padding, padding, padding)
                background = GradientDrawable().apply {
                    setColor(colors.iconBackground())
                    cornerRadius = (size / 2f)
                }
            }
            val iv = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size)
                setImageDrawable(app.icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setOnClickListener { launchApp(app.packageName) }
                setOnLongClickListener {
                    Toast.makeText(context, app.label, Toast.LENGTH_SHORT).show(); true
                }
            }
            container.addView(iv)
            dockContainer.addView(container)
        }

        // Settings gear button
        val settingsBtn = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).also {
                it.marginStart = margin; it.marginEnd = margin
            }
            setImageDrawable(getDrawable(android.R.drawable.ic_menu_manage))
            setColorFilter(colors.textSecondary)
            setOnClickListener { openSettings() }
        }
        dockContainer.addView(settingsBtn)
    }

    // ════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ════════════════════════════════════════════════════════════════════════
    private fun openAppDrawer() {
        startActivity(Intent(this, AppDrawerActivity::class.java))
        overridePendingTransition(R.anim.slide_up, R.anim.fade_out)
    }
    private fun openWatchFacePicker() {
        startActivity(Intent(this, WatchFacePickerActivity::class.java))
        overridePendingTransition(R.anim.slide_left, R.anim.fade_out)
    }
    private fun openThemePicker() {
        startActivity(Intent(this, ThemePickerActivity::class.java))
        overridePendingTransition(R.anim.slide_right, R.anim.fade_out)
    }
    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) startActivity(intent)
        else Toast.makeText(this, "App not available", Toast.LENGTH_SHORT).show()
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
    }

    private fun vibrate() {
        try {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            v?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}
