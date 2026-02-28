package com.watchlauncher

import android.Manifest
import android.app.WallpaperManager
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { setWallpaperFromUri(it) }
    }
    private val requestPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pickImage.launch("image/*")
        else Toast.makeText(this, "Permission needed", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val theme = ThemeManager.getTheme(this)
        val colors = ThemeManager.getColors(theme)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(colors.background)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 50, 20, 40)
        }

        fun sectionLabel(text: String) = TextView(this).apply {
            this.text = text
            textSize = 11f
            setTextColor(colors.textSecondary)
            setPadding(4, 20, 0, 8)
            letterSpacing = 0.2f
        }

        fun rowCard(content: LinearLayout.() -> Unit): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(16, 12, 16, 12)
                background = GradientDrawable().apply { setColor(colors.surface()); cornerRadius = 16f }
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, 0, 6)
                layoutParams = lp
                content()
            }
        }

        // ── Grid Columns ─────────────────────────────────────────────────────
        container.addView(sectionLabel("APP GRID"))
        container.addView(rowCard {
            addView(TextView(this@SettingsActivity).apply {
                text = "Grid Columns"
                textSize = 13f
                setTextColor(colors.textPrimary)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            val colVal = TextView(this@SettingsActivity).apply {
                text = ThemeManager.getGridCols(this@SettingsActivity).toString()
                textSize = 13f
                setTextColor(colors.accent)
                setPadding(8, 0, 8, 0)
            }
            val minus = Button(this@SettingsActivity).apply {
                text = "−"; textSize = 16f; setPadding(8, 0, 8, 0)
                setOnClickListener {
                    val c = (ThemeManager.getGridCols(this@SettingsActivity) - 1).coerceAtLeast(2)
                    ThemeManager.setGridCols(this@SettingsActivity, c)
                    colVal.text = c.toString()
                }
            }
            val plus = Button(this@SettingsActivity).apply {
                text = "+"; textSize = 16f; setPadding(8, 0, 8, 0)
                setOnClickListener {
                    val c = (ThemeManager.getGridCols(this@SettingsActivity) + 1).coerceAtMost(4)
                    ThemeManager.setGridCols(this@SettingsActivity, c)
                    colVal.text = c.toString()
                }
            }
            addView(minus); addView(colVal); addView(plus)
        })

        // ── Show Labels ──────────────────────────────────────────────────────
        container.addView(rowCard {
            addView(TextView(this@SettingsActivity).apply {
                text = "Show App Labels"
                textSize = 13f
                setTextColor(colors.textPrimary)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(Switch(this@SettingsActivity).apply {
                isChecked = ThemeManager.getShowLabels(this@SettingsActivity)
                setOnCheckedChangeListener { _, checked ->
                    ThemeManager.setShowLabels(this@SettingsActivity, checked)
                }
            })
        })

        // ── Wallpaper ────────────────────────────────────────────────────────
        container.addView(sectionLabel("WALLPAPER"))
        container.addView(rowCard {
            addView(Button(this@SettingsActivity).apply {
                text = "Pick from Gallery"
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setOnClickListener { checkPermAndPick() }
            })
        })

        // ── Presets ──────────────────────────────────────────────────────────
        val presets = listOf(
            "#0A0A0A" to "#3D5AFE" to "Deep Space",
            "#001f3f" to "#0074D9" to "Ocean",
            "#0a2e0a" to "#1a5c1a" to "Forest",
            "#FF3B00" to "#FFDD00" to "Fire",
            "#050010" to "#FF00FF" to "Neon"
        )
        container.addView(sectionLabel("PRESET BACKGROUNDS"))
        presets.forEach { (colPair, name) ->
            val (c1, c2) = colPair
            container.addView(rowCard {
                val swatch = android.view.View(this@SettingsActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(32, 32).also { it.marginEnd = 12 }
                    background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        intArrayOf(android.graphics.Color.parseColor(c1), android.graphics.Color.parseColor(c2))).apply {
                        cornerRadius = 8f
                    }
                }
                addView(swatch)
                addView(TextView(this@SettingsActivity).apply {
                    text = name; textSize = 13f; setTextColor(colors.textPrimary)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(Button(this@SettingsActivity).apply {
                    text = "Set"; textSize = 11f; setPadding(12, 0, 12, 0)
                    setOnClickListener { applyGradientWallpaper(c1, c2) }
                })
            })
        }

        // ── About ────────────────────────────────────────────────────────────
        container.addView(sectionLabel("GESTURES"))
        container.addView(TextView(this).apply {
            text = "↑ Swipe Up → App Drawer\n← Swipe Left → Watch Faces\n→ Swipe Right → Themes\n↕ Double Tap → Cycle Face\n⊙ Long Press → Settings"
            textSize = 11f
            setTextColor(colors.textSecondary)
            setPadding(8, 4, 8, 4)
            setLineSpacing(0f, 1.6f)
        })

        scroll.addView(container)
        setContentView(scroll)
    }

    private fun checkPermAndPick() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED)
            pickImage.launch("image/*")
        else requestPerm.launch(perm)
    }

    private fun setWallpaperFromUri(uri: Uri) {
        try {
            val bmp = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
            WallpaperManager.getInstance(this).setBitmap(bmp)
            Toast.makeText(this, "Wallpaper set!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyGradientWallpaper(c1: String, c2: String) {
        try {
            val bmp = android.graphics.Bitmap.createBitmap(400, 400, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            val paint = android.graphics.Paint()
            paint.shader = android.graphics.LinearGradient(
                0f, 0f, 400f, 400f,
                android.graphics.Color.parseColor(c1),
                android.graphics.Color.parseColor(c2),
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, 400f, 400f, paint)
            WallpaperManager.getInstance(this).setBitmap(bmp)
            Toast.makeText(this, "Wallpaper applied!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
