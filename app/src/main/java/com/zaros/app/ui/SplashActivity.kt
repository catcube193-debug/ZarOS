package com.zaros.app.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.zaros.app.BuildConfig
import com.zaros.app.backend.SoundManager
import com.zaros.app.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on top of system lock screen — must be set before setContentView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Always reflects the real app version from build.gradle — never
        // goes stale again like the old hardcoded "v2.0.0" string did.
        binding.tvBootVersion.text = "v${BuildConfig.VERSION_NAME}"

        SoundManager.init(this)

        // Start invisible
        binding.splashContent.alpha  = 0f
        binding.splashContent.scaleX = 0.85f
        binding.splashContent.scaleY = 0.85f

        // Animate in
        handler.postDelayed({
            binding.splashContent.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(700).start()
        }, 150)

        // Boot sound
        handler.postDelayed({ SoundManager.playBoot(this) }, 500)

        // Go straight to ZarOS lock screen — no gap for system lock
        handler.postDelayed({
            binding.splashRoot.animate()
                .alpha(0f).setDuration(350)
                .withEndAction {
                    val intent = Intent(this, LockActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }.start()
        }, 2500)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
