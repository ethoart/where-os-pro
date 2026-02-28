package com.watchlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.*

/**
 * ProClockView — renders 7 distinct watch face styles on a Canvas.
 * Style and theme are hot-swappable at runtime.
 */
class ProClockView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var watchFaceStyle: WatchFaceStyle = WatchFaceStyle.DIGITAL_NOTHING
        set(v) { field = v; invalidate() }

    var theme: WatchTheme = WatchTheme.NOTHING
        set(v) { field = v; applyThemePaints(); invalidate() }

    private var colors = ThemeManager.getColors(WatchTheme.NOTHING)

    // ── Paints ───────────────────────────────────────────────────────────────
    private val p1 = Paint(Paint.ANTI_ALIAS_FLAG) // primary text
    private val p2 = Paint(Paint.ANTI_ALIAS_FLAG) // secondary text
    private val p3 = Paint(Paint.ANTI_ALIAS_FLAG) // accent
    private val pBg = Paint(Paint.ANTI_ALIAS_FLAG) // background fill
    private val pGeo = Paint(Paint.ANTI_ALIAS_FLAG) // geometric shapes

    // ── Tick ─────────────────────────────────────────────────────────────────
    private val ticker = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { invalidate() }
    }

    init { applyThemePaints() }

    private fun applyThemePaints() {
        colors = ThemeManager.getColors(theme)
        listOf(p1, p2, p3, pBg, pGeo).forEach {
            it.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        p1.color = colors.textPrimary
        p2.color = colors.textSecondary
        p3.color = colors.accent
        pBg.color = colors.background
        pGeo.color = colors.primary
        pGeo.style = Paint.Style.STROKE
        pGeo.strokeWidth = 3f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(ticker, f)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try { context.unregisterReceiver(ticker) } catch (_: Exception) {}
    }

    // ════════════════════════════════════════════════════════════════════════
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cal = Calendar.getInstance()
        val hr  = cal.get(Calendar.HOUR_OF_DAY)
        val min = cal.get(Calendar.MINUTE)
        val sec = cal.get(Calendar.SECOND)
        val cx  = w / 2f
        val cy  = h / 2f

        // Fill background
        canvas.drawRect(0f, 0f, w, h, pBg)

        when (watchFaceStyle) {
            WatchFaceStyle.DIGITAL_NOTHING  -> drawNothingDigital(canvas, w, h, cx, cy, hr, min, cal)
            WatchFaceStyle.RETRO_DOT        -> drawRetroDot(canvas, w, h, cx, cy, hr, min)
            WatchFaceStyle.HALF_PAST        -> drawHalfPast(canvas, w, h, cx, cy, hr, min)
            WatchFaceStyle.BOLD_BLOCK       -> drawBoldBlock(canvas, w, h, cx, cy, hr, min, cal)
            WatchFaceStyle.MINIMAL_HANDS    -> drawMinimalHands(canvas, w, h, cx, cy, hr, min, sec)
            WatchFaceStyle.NEON_GLOW        -> drawNeonGlow(canvas, w, h, cx, cy, hr, min, sec)
            WatchFaceStyle.TYPE_CLOCK       -> drawTypeClock(canvas, w, h, cx, cy, hr, min, cal)
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. NOTHING DIGITAL — clean dot-matrix digits on dark background
    // ════════════════════════════════════════════════════════════════════════
    private fun drawNothingDigital(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float,
                                    hr: Int, min: Int, cal: Calendar) {
        p1.textSize = h * 0.35f
        p1.textAlign = Paint.Align.CENTER
        p1.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText(String.format("%02d:%02d", hr, min), cx, cy + p1.textSize * 0.35f, p1)

        p2.textSize = h * 0.12f
        p2.textAlign = Paint.Align.CENTER
        val days = arrayOf("SUN","MON","TUE","WED","THU","FRI","SAT")
        val months = arrayOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")
        canvas.drawText(
            "${days[cal.get(Calendar.DAY_OF_WEEK)-1]} ${cal.get(Calendar.DAY_OF_MONTH)} ${months[cal.get(Calendar.MONTH)]}",
            cx, cy + p1.textSize * 0.35f + p2.textSize * 1.6f, p2
        )

        // Dot separator blink
        p3.textSize = h * 0.10f
        p3.textAlign = Paint.Align.CENTER
        val dotY = cy - h * 0.30f
        canvas.drawCircle(cx - h * 0.05f, dotY, 3f, p3)
        canvas.drawCircle(cx + h * 0.05f, dotY, 3f, p3)
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. RETRO DOT — phosphor green dot-matrix style
    // ════════════════════════════════════════════════════════════════════════
    private fun drawRetroDot(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float,
                              hr: Int, min: Int) {
        // Scanline effect
        val scanPaint = Paint().apply { color = Color.argb(15, 0, 255, 0) }
        var y = 0f
        while (y < h) { canvas.drawRect(0f, y, w, y + 1f, scanPaint); y += 4f }

        p1.textSize = h * 0.38f
        p1.textAlign = Paint.Align.CENTER
        p1.color = colors.primary
        p1.setShadowLayer(12f, 0f, 0f, colors.primary)
        canvas.drawText(String.format("%02d", hr), cx, cy, p1)

        // Divider line
        p3.color = colors.accent
        p3.strokeWidth = 2f
        p3.style = Paint.Style.STROKE
        canvas.drawLine(cx - w * 0.25f, cy + h * 0.05f, cx + w * 0.25f, cy + h * 0.05f, p3)

        p1.textSize = h * 0.28f
        canvas.drawText(String.format("%02d", min), cx, cy + h * 0.38f, p1)
        p1.clearShadowLayer()
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. HALF PAST — typographic text clock (HALF PAST 12 / QUARTER TO 3)
    // ════════════════════════════════════════════════════════════════════════
    private fun drawHalfPast(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float,
                              hr: Int, min: Int) {
        val (line1, line2, line3) = getTextTime(hr, min)
        p1.textSize = h * 0.20f
        p1.textAlign = Paint.Align.LEFT
        p1.color = colors.textPrimary

        val x = w * 0.08f
        canvas.drawText(line1, x, h * 0.30f, p1)
        p1.textSize = h * 0.20f
        canvas.drawText(line2, x, h * 0.52f, p1)
        p3.textSize = h * 0.20f
        p3.textAlign = Paint.Align.LEFT
        p3.color = colors.accent
        canvas.drawText(line3, x, h * 0.74f, p3)
    }

    private fun getTextTime(hr: Int, min: Int): Triple<String, String, String> {
        val hours = arrayOf("TWELVE","ONE","TWO","THREE","FOUR","FIVE","SIX",
                            "SEVEN","EIGHT","NINE","TEN","ELEVEN","TWELVE")
        val h12 = hr % 12
        return when {
            min < 5  -> Triple("IT IS", hours[h12], "O'CLOCK")
            min < 10 -> Triple("FIVE", "PAST", hours[h12])
            min < 15 -> Triple("TEN", "PAST", hours[h12])
            min < 20 -> Triple("QUARTER", "PAST", hours[h12])
            min < 25 -> Triple("TWENTY", "PAST", hours[h12])
            min < 30 -> Triple("TWENTY", "FIVE PAST", hours[h12])
            min < 35 -> Triple("HALF", "PAST", hours[h12])
            min < 40 -> Triple("TWENTY", "FIVE TO", hours[(h12 + 1) % 13])
            min < 45 -> Triple("TWENTY", "TO", hours[(h12 + 1) % 13])
            min < 50 -> Triple("QUARTER", "TO", hours[(h12 + 1) % 13])
            min < 55 -> Triple("TEN", "TO", hours[(h12 + 1) % 13])
            else     -> Triple("FIVE", "TO", hours[(h12 + 1) % 13])
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. BOLD BLOCK — giant hours + minutes in color blocks (Nothing OS3-style)
    // ════════════════════════════════════════════════════════════════════════
    private fun drawBoldBlock(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float,
                               hr: Int, min: Int, cal: Calendar) {
        // Top block - hours
        val blockPaint = Paint().apply {
            color = colors.accent
            style = Paint.Style.FILL
        }
        val r = 20f
        val topRect = RectF(w * 0.06f, h * 0.06f, w * 0.94f, h * 0.50f)
        canvas.drawRoundRect(topRect, r, r, blockPaint)

        p1.textSize = h * 0.32f
        p1.textAlign = Paint.Align.CENTER
        p1.color = colors.background
        canvas.drawText(String.format("%02d", hr), cx, h * 0.36f, p1)

        // Bottom block - minutes
        blockPaint.color = colors.surface()
        val botRect = RectF(w * 0.06f, h * 0.54f, w * 0.94f, h * 0.92f)
        canvas.drawRoundRect(botRect, r, r, blockPaint)

        p1.color = colors.textPrimary
        canvas.drawText(String.format("%02d", min), cx, h * 0.80f, p1)
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. MINIMAL HANDS — clean analog with a single accent dot
    // ════════════════════════════════════════════════════════════════════════
    private fun drawMinimalHands(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float,
                                  hr: Int, min: Int, sec: Int) {
        val r = minOf(w, h) * 0.40f

        // Thin circle
        pGeo.color = colors.surface()
        pGeo.strokeWidth = 2f
        pGeo.style = Paint.Style.STROKE
        canvas.drawCircle(cx, cy, r, pGeo)

        // Hour marks
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90)
            val inner = if (i % 3 == 0) r * 0.82f else r * 0.90f
            pGeo.strokeWidth = if (i % 3 == 0) 3f else 1.5f
            pGeo.color = if (i % 3 == 0) colors.textPrimary else colors.textSecondary
            canvas.drawLine(
                cx + (inner * cos(a)).toFloat(), cy + (inner * sin(a)).toFloat(),
                cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat(), pGeo
            )
        }

        // Hour hand
        val hAngle = Math.toRadians((hr % 12) * 30.0 + min * 0.5 - 90)
        val hPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textPrimary; strokeWidth = 5f; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
        }
        canvas.drawLine(cx, cy, cx + (r * 0.55f * cos(hAngle)).toFloat(), cy + (r * 0.55f * sin(hAngle)).toFloat(), hPaint)

        // Minute hand
        val mAngle = Math.toRadians(min * 6.0 - 90)
        hPaint.strokeWidth = 3f
        canvas.drawLine(cx, cy, cx + (r * 0.78f * cos(mAngle)).toFloat(), cy + (r * 0.78f * sin(mAngle)).toFloat(), hPaint)

        // Seconds dot (accent color)
        val sAngle = Math.toRadians(sec * 6.0 - 90)
        val sPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.accent }
        canvas.drawCircle(
            cx + (r * 0.72f * cos(sAngle)).toFloat(),
            cy + (r * 0.72f * sin(sAngle)).toFloat(),
            5f, sPaint
        )
        canvas.drawCircle(cx, cy, 6f, sPaint)
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. NEON GLOW — cyberpunk neon style with glow effects
    // ════════════════════════════════════════════════════════════════════════
    private fun drawNeonGlow(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float,
                              hr: Int, min: Int, sec: Int) {
        // Progress arc - seconds
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.accent
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(16f, 0f, 0f, colors.accent)
        }
        val oval = RectF(w * 0.05f, h * 0.05f, w * 0.95f, h * 0.95f)
        canvas.drawArc(oval, -90f, sec * 6f, false, arcPaint)

        // Time text with glow
        p1.textSize = h * 0.30f
        p1.textAlign = Paint.Align.CENTER
        p1.color = colors.primary
        p1.setShadowLayer(20f, 0f, 0f, colors.primary)
        canvas.drawText(String.format("%02d", hr), cx, cy - h * 0.04f, p1)

        p3.textSize = h * 0.30f
        p3.textAlign = Paint.Align.CENTER
        p3.color = colors.accent
        p3.setShadowLayer(20f, 0f, 0f, colors.accent)
        canvas.drawText(String.format("%02d", min), cx, cy + h * 0.30f, p3)

        p1.clearShadowLayer()
        p3.clearShadowLayer()
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7. TYPE CLOCK — magazine editorial style typography
    // ════════════════════════════════════════════════════════════════════════
    private fun drawTypeClock(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float,
                               hr: Int, min: Int, cal: Calendar) {
        val days = arrayOf("SUN","MON","TUE","WED","THU","FRI","SAT")
        val day = days[cal.get(Calendar.DAY_OF_WEEK) - 1]
        val date = cal.get(Calendar.DAY_OF_MONTH).toString()

        // Day label - small top left
        p2.textSize = h * 0.10f
        p2.textAlign = Paint.Align.LEFT
        canvas.drawText(day, w * 0.08f, h * 0.18f, p2)

        // Big hour
        p1.textSize = h * 0.42f
        p1.textAlign = Paint.Align.LEFT
        p1.color = colors.textPrimary
        canvas.drawText(String.format("%02d", hr), w * 0.05f, h * 0.58f, p1)

        // Minutes - accent color, right-aligned
        p3.textSize = h * 0.28f
        p3.textAlign = Paint.Align.RIGHT
        p3.color = colors.accent
        canvas.drawText(String.format("%02d", min), w * 0.95f, h * 0.90f, p3)

        // Date - large background number
        p2.textSize = h * 0.50f
        p2.textAlign = Paint.Align.RIGHT
        p2.alpha = 40
        canvas.drawText(date, w * 0.98f, h * 0.65f, p2)
        p2.alpha = 255
    }
}
