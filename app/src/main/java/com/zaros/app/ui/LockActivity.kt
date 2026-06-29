package com.zaros.app.ui

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.zaros.app.R
import com.zaros.app.backend.SoundManager
import com.zaros.app.backend.TimeManager
import com.zaros.app.backend.ZarOSPrefs
import com.zaros.app.databinding.ActivityLockBinding
import kotlinx.coroutines.*

class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private val clockJob = SupervisorJob()
    private val scope    = CoroutineScope(Dispatchers.Main + clockJob)

    private var touchStartY = 0f
    private var touchStartX = 0f
    private val SWIPE_THRESHOLD = 110f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dismiss system keyguard FIRST before anything else draws
        dismissSystemKeyguard()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // Draw our window before system UI can paint
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SoundManager.init(this)
        applyWallpaper()
        updateClock()
        startClockUpdater()
        setupGestures()
        animateHint()
    }

    /** Mirrors HomeActivity's wallpaper logic so the lock screen matches the home screen. */
    private fun applyWallpaper() {
        val idx = ZarOSPrefs.getWallpaper(this)
        val startColor = android.graphics.Color.parseColor(ZarOSPrefs.WALLPAPER_STARTS[idx])
        val endColor   = android.graphics.Color.parseColor(ZarOSPrefs.WALLPAPER_ENDS[idx])

        if (ZarOSPrefs.isAnimatedWallpaper(this)) {
            binding.root.background = null
            binding.animatedWallpaper.visibility = View.VISIBLE
            binding.animatedWallpaper.setColors(startColor, endColor)
            binding.animatedWallpaper.startAnimating()
        } else {
            binding.animatedWallpaper.stopAnimating()
            binding.animatedWallpaper.visibility = View.GONE
            val grad = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(startColor, endColor)
            )
            binding.root.background = grad
        }
    }

    private fun dismissSystemKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val km = getSystemService(KeyguardManager::class.java)
            km?.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    // System keyguard gone — ZarOS lock screen is now the only lock
                }
                override fun onDismissCancelled() {
                    // Device has a PIN/pattern — ZarOS overlays on top instead
                }
                override fun onDismissError() { }
            })
        }
        // For all API levels — add dismiss flag directly to window
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    private fun updateClock() {
        binding.tvTime.text = TimeManager.getTime()
        binding.tvAmPm.text = TimeManager.getAmPm()
        binding.tvDate.text = TimeManager.getDate()
    }

    private fun startClockUpdater() {
        scope.launch {
            while (isActive) { delay(10_000L); updateClock() }
        }
    }

    @SuppressLint("ClicksNotAccessible")
    private fun setupGestures() {
        binding.root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartY = event.rawY
                    touchStartX = event.rawX
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dy = touchStartY - event.rawY
                    when {
                        touchStartY < 200 && event.rawY - touchStartY > 80 ->
                            openNotificationShade()
                        dy > SWIPE_THRESHOLD -> unlock()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun unlock() {
        SoundManager.playUnlock(this)
        val anim = AnimationUtils.loadAnimation(this, R.anim.slide_up_exit)
        binding.root.startAnimation(anim)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, HomeActivity::class.java))
            overridePendingTransition(R.anim.slide_up_enter, android.R.anim.fade_out)
        }, 280L)
    }

    private fun openNotificationShade() {
        startActivity(Intent(this, NotificationShadeActivity::class.java))
        overridePendingTransition(R.anim.slide_down_enter, 0)
    }

    private fun animateHint() {
        binding.llSwipeHint.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.bounce_hint)
        )
    }

    // Block back button — can't bypass lock screen
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* intentionally empty */ }

    // Re-show if user tries to leave
    override fun onPause() {
        super.onPause()
        if (!isFinishing) {
            val km = getSystemService(KeyguardManager::class.java)
            // If system keyguard becomes active again, come back on top
            if (km != null && km.isKeyguardLocked) {
                startActivity(Intent(this, LockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                })
            }
        }
    }

    override fun onResume() { super.onResume(); updateClock(); applyWallpaper() }
    override fun onDestroy() { super.onDestroy(); clockJob.cancel() }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }
}
