package com.watchlauncher

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WatchFacePickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = ThemeManager.getTheme(this)
        val colors = ThemeManager.getColors(theme)
        val currentFace = ThemeManager.getWatchFace(this)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(colors.background)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 40, 16, 40)
        }
        container.addView(TextView(this).apply {
            text = "WATCH FACES"; textSize = 14f
            setTextColor(colors.textSecondary); setPadding(8, 0, 0, 20); letterSpacing = 0.2f
        })

        WatchFaceStyle.values().forEach { face ->
            val isSelected = face == currentFace
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 8) }
                background = GradientDrawable().apply {
                    setColor(if (isSelected) colors.accent else colors.surface()); cornerRadius = 20f
                }
                setOnClickListener {
                    ThemeManager.setWatchFace(this@WatchFacePickerActivity, face)
                    Toast.makeText(this@WatchFacePickerActivity, "${face.displayName} selected", Toast.LENGTH_SHORT).show()
                    finish(); overridePendingTransition(R.anim.fade_in, R.anim.slide_right)
                }
            }
            card.addView(ProClockView(this).apply {
                watchFaceStyle = face; this.theme = theme
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 160)
            })
            card.addView(TextView(this).apply {
                text = face.displayName; textSize = 11f; setPadding(8, 8, 0, 0)
                setTextColor(if (isSelected) colors.background else colors.textPrimary)
            })
            container.addView(card)
        }

        scroll.addView(container)
        setContentView(scroll)

        // Swipe right to dismiss
        scroll.setOnTouchListener(SwipeListener(
            onSwipeRight = { finish(); overridePendingTransition(R.anim.fade_in, R.anim.slide_right) }
        ))
    }
}
