package com.zaros.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * ZarOS AnimatedWallpaperView (4.0.3)
 *
 * A visibly animated gradient background. Two things make the motion
 * actually noticeable (the original version technically animated but
 * was nearly imperceptible since a 2-color gradient rotating slowly
 * around a rectangle's center barely changes what's on screen):
 *
 *  1. A THIRD color stop drifts back and forth between the two base
 *     colors at its own independent speed, so the gradient visibly
 *     flows and shifts in brightness/hue, not just "rotates."
 *  2. The rotation itself completes a full cycle in 8 seconds instead
 *     of 20, which is fast enough to read as "this is alive" at a
 *     glance without being distracting or draining the battery.
 */
class AnimatedWallpaperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var startColor = 0xFF38BDF8.toInt()
    private var endColor   = 0xFF0C4A6E.toInt()

    private val paint = Paint()
    private var animatedAngle = 0f   // 0–360, drives gradient rotation
    private var driftT        = 0f   // 0–1–0, drives the mid-color's drift

    private var rotationAnimator: ValueAnimator? = null
    private var driftAnimator: ValueAnimator? = null

    fun setColors(start: Int, end: Int) {
        startColor = start
        endColor   = end
        invalidate()
    }

    fun startAnimating() {
        rotationAnimator?.cancel()
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 8000L   // Fast enough to clearly read as motion
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                animatedAngle = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        driftAnimator?.cancel()
        driftAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 6000L   // Slightly offset from the rotation speed
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                driftT = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopAnimating() {
        rotationAnimator?.cancel(); rotationAnimator = null
        driftAnimator?.cancel();    driftAnimator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val rad = Math.toRadians(animatedAngle.toDouble())
        val cx = w / 2f; val cy = h / 2f
        val radius = Math.sqrt((w * w + h * h).toDouble()).toFloat() / 2f

        val x0 = cx + radius * Math.cos(rad).toFloat()
        val y0 = cy + radius * Math.sin(rad).toFloat()
        val x1 = cx - radius * Math.cos(rad).toFloat()
        val y1 = cy - radius * Math.sin(rad).toFloat()

        // The middle stop drifts between the two base colors over time,
        // and its position along the gradient also drifts slightly —
        // this is what makes the wallpaper visibly "breathe" rather
        // than just look like a static gradient that slowly spins.
        val driftingMid = blend(startColor, endColor, driftT)
        val midPosition = 0.35f + (driftT * 0.3f) // ranges ~0.35–0.65

        paint.shader = LinearGradient(
            x0, y0, x1, y1,
            intArrayOf(startColor, driftingMid, endColor),
            floatArrayOf(0f, midPosition, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    private fun blend(c1: Int, c2: Int, ratio: Float): Int {
        val r = Color.red(c1)   + ((Color.red(c2)   - Color.red(c1))   * ratio).toInt()
        val g = Color.green(c1) + ((Color.green(c2) - Color.green(c1)) * ratio).toInt()
        val b = Color.blue(c1)  + ((Color.blue(c2)  - Color.blue(c1))  * ratio).toInt()
        val a = Color.alpha(c1) + ((Color.alpha(c2) - Color.alpha(c1)) * ratio).toInt()
        return Color.argb(a, r, g, b)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimating()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimating()
    }
}
