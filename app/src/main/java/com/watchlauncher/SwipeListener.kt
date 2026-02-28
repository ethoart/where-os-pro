package com.watchlauncher

import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Simple swipe detector — no GestureDetector, no override issues.
 * Attach via view.setOnTouchListener(SwipeListener(...))
 */
class SwipeListener(
    private val onSwipeUp: (() -> Unit)? = null,
    private val onSwipeDown: (() -> Unit)? = null,
    private val onSwipeLeft: (() -> Unit)? = null,
    private val onSwipeRight: (() -> Unit)? = null,
    private val onLongPress: (() -> Unit)? = null,
    private val onDoubleTap: (() -> Unit)? = null
) : View.OnTouchListener {

    private var startX = 0f
    private var startY = 0f
    private var startTime = 0L
    private var lastTapTime = 0L
    private val longPressRunnable = Runnable { onLongPress?.invoke() }
    private var handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var moved = false

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                startTime = System.currentTimeMillis()
                moved = false
                if (onLongPress != null) {
                    handler.postDelayed(longPressRunnable, 600)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - startX)
                val dy = abs(event.y - startY)
                if (dx > 10 || dy > 10) {
                    moved = true
                    handler.removeCallbacks(longPressRunnable)
                }
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                val dx = event.x - startX
                val dy = event.y - startY
                val adx = abs(dx)
                val ady = abs(dy)
                val duration = System.currentTimeMillis() - startTime

                if (!moved && duration < 300) {
                    // Tap — check double tap
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < 350 && onDoubleTap != null) {
                        onDoubleTap.invoke()
                        lastTapTime = 0
                    } else {
                        lastTapTime = now
                    }
                } else if (ady > 80 && ady > adx) {
                    if (dy < 0) onSwipeUp?.invoke() else onSwipeDown?.invoke()
                } else if (adx > 80 && adx > ady) {
                    if (dx < 0) onSwipeLeft?.invoke() else onSwipeRight?.invoke()
                }
                v.performClick()
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
            }
        }
        return true
    }
}
