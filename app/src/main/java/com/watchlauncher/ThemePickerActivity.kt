package com.watchlauncher

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ThemePickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentTheme = ThemeManager.getTheme(this)
        val currentColors = ThemeManager.getColors(currentTheme)
        val face = ThemeManager.getWatchFace(this)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(currentColors.background)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(16, 40, 16, 40)
        }
        container.addView(TextView(this).apply {
            text = "THEMES"; textSize = 14f
            setTextColor(currentColors.textSecondary); setPadding(8, 0, 0, 20); letterSpacing = 0.2f
        })

        WatchTheme.values().forEach { theme ->
            val colors = ThemeManager.getColors(theme)
            val isSelected = theme == currentTheme
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 10) }
                background = GradientDrawable().apply {
                    setColor(colors.background); cornerRadius = 20f
                    setStroke(if (isSelected) 3 else 1, if (isSelected) currentColors.accent else colors.surface())
                }
                setOnClickListener {
                    ThemeManager.setTheme(this@ThemePickerActivity, theme)
                    Toast.makeText(this@ThemePickerActivity, "${theme.displayName} applied", Toast.LENGTH_SHORT).show()
                    finish(); overridePendingTransition(R.anim.fade_in, R.anim.slide_left)
                }
            }
            card.addView(ProClockView(this).apply {
                watchFaceStyle = face; this.theme = theme
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 140)
            })
            val swatchRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; setPadding(8, 8, 8, 0)
            }
            listOf(colors.primary, colors.accent, colors.surface(), colors.textSecondary).forEach { c ->
                swatchRow.addView(android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(20, 20).also { it.marginEnd = 6 }
                    background = GradientDrawable().apply { setColor(c); cornerRadius = 10f }
                })
            }
            card.addView(swatchRow)
            card.addView(TextView(this).apply {
                text = theme.displayName; textSize = 12f; setTextColor(colors.textPrimary); setPadding(8, 4, 0, 0)
            })
            container.addView(card)
        }

        scroll.addView(container)
        setContentView(scroll)

        // Swipe left to dismiss
        scroll.setOnTouchListener(SwipeListener(
            onSwipeLeft = { finish(); overridePendingTransition(R.anim.fade_in, R.anim.slide_left) }
        ))
    }
}
