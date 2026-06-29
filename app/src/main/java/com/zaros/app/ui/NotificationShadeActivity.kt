package com.zaros.app.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zaros.app.R
import com.zaros.app.backend.SoundManager
import com.zaros.app.backend.ZarOSPrefs
import com.zaros.app.databinding.ActivityNotificationShadeBinding

/**
 * ZarOS NotificationShadeActivity (Kotlin UI)
 *
 * Swipe-down notification shade with:
 *  - Quick toggles: Wi-Fi, Bluetooth, Vibration, Notifications, Dark Mode
 *  - Live brightness slider
 *  - Notification cards (static demo + expandable)
 *  - Swipe up or tap outside to dismiss
 *  - Frosted glass style background
 */
class NotificationShadeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationShadeBinding
    private var touchStartY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transparent background so home screen shows through
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        window.setBackgroundDrawableResource(android.R.color.transparent)

        binding = ActivityNotificationShadeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Slide down entry
        binding.shadePanel.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
        )

        setupQuickToggles()
        setupBrightness()
        setupDismiss()
        loadNotifications()
    }

    // ── Quick toggles ─────────────────────────────────────────────────────

    private fun setupQuickToggles() {
        // Wi-Fi
        updateToggleUI(binding.toggleWifi, ZarOSPrefs.isWifi(this))
        binding.toggleWifi.setOnClickListener {
            val newVal = !ZarOSPrefs.isWifi(this)
            ZarOSPrefs.setWifi(this, newVal)
            updateToggleUI(binding.toggleWifi, newVal)
            SoundManager.playTap(this)
        }

        // Bluetooth
        updateToggleUI(binding.toggleBt, ZarOSPrefs.isBluetooth(this))
        binding.toggleBt.setOnClickListener {
            val newVal = !ZarOSPrefs.isBluetooth(this)
            ZarOSPrefs.setBluetooth(this, newVal)
            updateToggleUI(binding.toggleBt, newVal)
            SoundManager.playTap(this)
        }

        // Vibration
        updateToggleUI(binding.toggleVibrate, ZarOSPrefs.isVibration(this))
        binding.toggleVibrate.setOnClickListener {
            val newVal = !ZarOSPrefs.isVibration(this)
            ZarOSPrefs.setVibration(this, newVal)
            updateToggleUI(binding.toggleVibrate, newVal)
            SoundManager.playTap(this)
        }

        // Notifications
        updateToggleUI(binding.toggleNotif, ZarOSPrefs.isNotifications(this))
        binding.toggleNotif.setOnClickListener {
            val newVal = !ZarOSPrefs.isNotifications(this)
            ZarOSPrefs.setNotifications(this, newVal)
            updateToggleUI(binding.toggleNotif, newVal)
            SoundManager.playTap(this)
        }

        // Dark mode
        updateToggleUI(binding.toggleDark, ZarOSPrefs.isDarkMode(this))
        binding.toggleDark.setOnClickListener {
            val newVal = !ZarOSPrefs.isDarkMode(this)
            ZarOSPrefs.setDarkMode(this, newVal)
            updateToggleUI(binding.toggleDark, newVal)
            SoundManager.playTap(this)
        }

        // Do Not Disturb (stored as inverted notification pref)
        binding.toggleDnd.setOnClickListener {
            Toast.makeText(this, "Do Not Disturb toggled", Toast.LENGTH_SHORT).show()
            SoundManager.playTap(this)
        }
    }

    private fun updateToggleUI(view: View, isOn: Boolean) {
        view.alpha = if (isOn) 1f else 0.4f
        view.isSelected = isOn
    }

    // ── Brightness ────────────────────────────────────────────────────────

    private fun setupBrightness() {
        binding.brightnessSlider.value = ZarOSPrefs.getBrightness(this).toFloat()
        binding.brightnessSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val pct = value.toInt()
                ZarOSPrefs.setBrightness(this, pct)
                val lp = window.attributes
                lp.screenBrightness = pct / 100f
                window.attributes = lp
            }
        }
    }

    // ── Dismiss (swipe up or tap scrim) ───────────────────────────────────

    @SuppressLint("ClicksNotAccessible")
    private fun setupDismiss() {
        // Tap on the dark scrim area
        binding.scrim.setOnClickListener { dismiss() }

        // Swipe up on the panel
        binding.shadePanel.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { touchStartY = event.rawY; true }
                MotionEvent.ACTION_UP   -> {
                    if (event.rawY - touchStartY < -80) dismiss()
                    true
                }
                else -> false
            }
        }
    }

    private fun dismiss() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.slide_up_exit)
        binding.shadePanel.startAnimation(anim)
        binding.shadePanel.postDelayed({ finish() }, 260)
    }

    // ── Notifications ─────────────────────────────────────────────────────

    private fun loadNotifications() {
        // Demo notifications — in a real build these would come from
        // a NotificationListenerService
        val notifs = listOf(
            Triple("🔍", "Fynder", "Your AI search is ready"),
            Triple("📶", "ZarOS", "Connected to Wi-Fi"),
            Triple("🔋", "System", "Battery at 84%")
        )

        val container = binding.notifContainer
        notifs.forEach { (icon, title, body) ->
            val view = layoutInflater.inflate(R.layout.item_notification, container, false)
            view.findViewById<android.widget.TextView>(R.id.tvNotifIcon).text = icon
            view.findViewById<android.widget.TextView>(R.id.tvNotifTitle).text = title
            view.findViewById<android.widget.TextView>(R.id.tvNotifBody).text = body
            view.setOnClickListener {
                SoundManager.playTap(this)
                container.removeView(view)
            }
            container.addView(view)
        }

        binding.btnClearAll.setOnClickListener {
            container.removeAllViews()
            SoundManager.playTap(this)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }
}
