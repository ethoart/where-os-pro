package com.watchlauncher

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat

class ThemePickerActivity : AppCompatActivity() {

    private lateinit var gestureDetector: GestureDetectorCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentTheme = ThemeManager.getTheme(this)
        val currentColors = ThemeManager.getColors(currentTheme)
        val face = ThemeManager.getWatchFace(this)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(currentColors.background)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 40, 16, 40)
        }

        container.addView(TextView(this).apply {
            text = "THEMES"
            textSize = 14f
            setTextColor(currentColors.textSecondary)
            setPadding(8, 0, 0, 20)
            letterSpacing = 0.2f
        })

        WatchTheme.values().forEach { theme ->
            val colors = ThemeManager.getColors(theme)
            val isSelected = theme == currentTheme

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 0, 10)
                layoutParams = lp
                background = GradientDrawable().apply {
                    setColor(colors.background)
                    cornerRadius = 20f
                    setStroke(if (isSelected) 3 else 1,
                              if (isSelected) currentColors.accent else colors.surface())
                }
                setOnClickListener {
                    ThemeManager.setTheme(this@ThemePickerActivity, theme)
                    Toast.makeText(this@ThemePickerActivity, "${theme.displayName} applied", Toast.LENGTH_SHORT).show()
                    finish()
                    overridePendingTransition(R.anim.fade_in, R.anim.slide_left)
                }
            }

            // Live clock preview in this theme
            val preview = ProClockView(this).apply {
                watchFaceStyle = face
                this.theme = theme
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 140
                )
            }

            // Color swatches row
            val swatchRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(8, 8, 8, 0)
            }
            listOf(colors.primary, colors.accent, colors.surface(), colors.textSecondary).forEach { c ->
                swatchRow.addView(android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(20, 20).also { it.marginEnd = 6 }
                    background = GradientDrawable().apply { setColor(c); cornerRadius = 10f }
                })
            }

            val label = TextView(this).apply {
                text = theme.displayName
                textSize = 12f
                setTextColor(colors.textPrimary)
                setPadding(8, 4, 0, 0)
            }

            card.addView(preview)
            card.addView(swatchRow)
            card.addView(label)
            container.addView(card)
        }

        scroll.addView(container)
        setContentView(scroll)

        val gl = object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, vX: Float, vY: Float): Boolean {
                val dX = (e1?.x ?: 0f) - (e2?.x ?: 0f)
                if (dX > 100) { finish(); overridePendingTransition(R.anim.fade_in, R.anim.slide_left); return true }
                return false
            }
            override fun onDown(e: MotionEvent) = true
        }
        gestureDetector = GestureDetectorCompat(this, gl)
        scroll.setOnTouchListener { _, e -> gestureDetector.onTouchEvent(e); false }
    }
}
