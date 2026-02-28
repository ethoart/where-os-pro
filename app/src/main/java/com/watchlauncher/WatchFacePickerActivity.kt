package com.watchlauncher

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat

class WatchFacePickerActivity : AppCompatActivity() {

    private lateinit var gestureDetector: GestureDetectorCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val theme = ThemeManager.getTheme(this)
        val colors = ThemeManager.getColors(theme)
        val currentFace = ThemeManager.getWatchFace(this)

        // Build UI programmatically for maximum flexibility
        val scroll = ScrollView(this).apply {
            setBackgroundColor(colors.background)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 40, 16, 40)
        }

        // Title
        container.addView(TextView(this).apply {
            text = "WATCH FACES"
            textSize = 14f
            setTextColor(colors.textSecondary)
            setPadding(8, 0, 0, 20)
            letterSpacing = 0.2f
        })

        // Each watch face gets a live preview ProClockView
        WatchFaceStyle.values().forEach { face ->
            val isSelected = face == currentFace
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
                val margin = 8
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 0, margin)
                layoutParams = lp
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(if (isSelected) colors.accent else colors.surface())
                    cornerRadius = 20f
                }
                setOnClickListener {
                    ThemeManager.setWatchFace(this@WatchFacePickerActivity, face)
                    Toast.makeText(this@WatchFacePickerActivity, "${face.displayName} selected", Toast.LENGTH_SHORT).show()
                    finish()
                    overridePendingTransition(R.anim.fade_in, R.anim.slide_right)
                }
            }

            // Live preview clock
            val preview = ProClockView(this).apply {
                watchFaceStyle = face
                this.theme = theme
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 160
                )
            }

            // Label
            val label = TextView(this).apply {
                text = face.displayName
                textSize = 11f
                setTextColor(if (isSelected) colors.background else colors.textPrimary)
                setPadding(8, 8, 0, 0)
            }

            card.addView(preview)
            card.addView(label)
            container.addView(card)
        }

        scroll.addView(container)
        setContentView(scroll)

        // Swipe right to dismiss
        val gl = object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, vX: Float, vY: Float): Boolean {
                val dX = (e2?.x ?: 0f) - (e1?.x ?: 0f)
                if (dX > 100) { finish(); overridePendingTransition(R.anim.fade_in, R.anim.slide_right); return true }
                return false
            }
            override fun onDown(e: MotionEvent) = true
        }
        gestureDetector = GestureDetectorCompat(this, gl)
        scroll.setOnTouchListener { _, e -> gestureDetector.onTouchEvent(e); false }
    }
}
