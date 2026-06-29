package com.zaros.app.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zaros.app.BuildConfig
import com.zaros.app.R
import com.zaros.app.backend.FynderSearchEngine
import com.zaros.app.backend.ZarOSPrefs
import com.zaros.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var selectedWallpaper = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCurrentValues()
        setupDisplay()
        setupWallpaper()
        setupSound()
        setupConnectivity()
        setupBrowser()
        setupClock()
        setupSystem()

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun loadCurrentValues() {
        // Always reflects the real app version from build.gradle.
        binding.tvSettingsVersion.text = "Version ${BuildConfig.VERSION_NAME} · Built with ♥"

        selectedWallpaper                     = ZarOSPrefs.getWallpaper(this)
        binding.toggleDarkMode.isChecked      = ZarOSPrefs.isDarkMode(this)
        binding.sliderBrightness.value        = ZarOSPrefs.getBrightness(this).toFloat()
        binding.tvBrightnessVal.text          = "${ZarOSPrefs.getBrightness(this)}%"
        binding.sliderVolume.value            = ZarOSPrefs.getVolume(this).toFloat()
        binding.tvVolumeVal.text              = "${ZarOSPrefs.getVolume(this)}%"
        binding.toggleVibration.isChecked     = ZarOSPrefs.isVibration(this)
        binding.toggleNotifications.isChecked = ZarOSPrefs.isNotifications(this)
        binding.toggle24h.isChecked           = ZarOSPrefs.is24HClock(this)
        binding.toggleAnimatedWallpaper.isChecked = ZarOSPrefs.isAnimatedWallpaper(this)

        val wifiEnabled = try {
            val wm = applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager
            wm?.isWifiEnabled ?: true
        } catch (e: Exception) {
            true
        }
        binding.toggleWifi.isChecked      = wifiEnabled
        binding.toggleBluetooth.isChecked = ZarOSPrefs.isBluetooth(this)
    }

    private fun setupDisplay() {
        binding.sliderBrightness.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val pct = value.toInt()
                binding.tvBrightnessVal.text = "$pct%"
                ZarOSPrefs.setBrightness(this, pct)
                val lp = window.attributes
                lp.screenBrightness = pct / 100f
                window.attributes = lp
            }
        }
        binding.toggleDarkMode.setOnCheckedChangeListener { _, checked ->
            ZarOSPrefs.setDarkMode(this, checked)
        }
    }

    private fun setupWallpaper() {
        val wallButtons = listOf(
            binding.wall0, binding.wall1, binding.wall2, binding.wall3,
            binding.wall4, binding.wall5, binding.wall6, binding.wall7
        )
        wallButtons.forEachIndexed { i, v ->
            v.alpha = if (i == selectedWallpaper) 1f else 0.6f
            if (i == selectedWallpaper) { v.scaleX = 1.1f; v.scaleY = 1.1f }
        }
        wallButtons.forEachIndexed { index, view ->
            view.setOnClickListener {
                wallButtons.forEach { btn ->
                    btn.alpha = 0.6f
                    btn.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }
                view.alpha = 1f
                view.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150).start()
                selectedWallpaper = index
                ZarOSPrefs.setWallpaper(this, index)
                Toast.makeText(this, "Wallpaper saved", Toast.LENGTH_SHORT).show()
            }
        }
        binding.toggleAnimatedWallpaper.setOnCheckedChangeListener { _, checked ->
            ZarOSPrefs.setAnimatedWallpaper(this, checked)
        }
    }

    private fun setupSound() {
        binding.sliderVolume.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val pct = value.toInt()
                binding.tvVolumeVal.text = "$pct%"
                ZarOSPrefs.setVolume(this, pct)
            }
        }
        binding.toggleVibration.setOnCheckedChangeListener { _, checked ->
            ZarOSPrefs.setVibration(this, checked)
        }
        binding.toggleNotifications.setOnCheckedChangeListener { _, checked ->
            ZarOSPrefs.setNotifications(this, checked)
        }
    }

    private fun setupConnectivity() {
        binding.toggleWifi.setOnCheckedChangeListener { _, _ ->
            val wifiEnabled = try {
                val wm = applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager
                wm?.isWifiEnabled ?: true
            } catch (e: Exception) {
                true
            }
            binding.toggleWifi.isChecked = wifiEnabled
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            Toast.makeText(this, "Opening Wi-Fi settings", Toast.LENGTH_SHORT).show()
        }
        binding.toggleBluetooth.setOnCheckedChangeListener { _, _ ->
            binding.toggleBluetooth.isChecked = ZarOSPrefs.isBluetooth(this)
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            Toast.makeText(this, "Opening Bluetooth settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBrowser() {
        binding.rowClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Search History")
                .setMessage("Delete all Fynder search history?")
                .setPositiveButton("Clear") { _, _ ->
                    FynderSearchEngine.clearHistory(this)
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        binding.rowOpenFynder.setOnClickListener {
            startActivity(Intent(this, FynderActivity::class.java))
        }
    }

    private fun setupClock() {
        binding.toggle24h.setOnCheckedChangeListener { _, checked ->
            ZarOSPrefs.set24HClock(this, checked)
        }
    }

    private fun setupSystem() {
        // Switch launcher — opens Android's home app picker
        binding.rowSwitchLauncher.setOnClickListener {
            startActivity(Intent(this, HomeSwitcherActivity::class.java))
        }

        binding.rowUpdate.setOnClickListener {
            Toast.makeText(this, "ZarOS ${BuildConfig.VERSION_NAME} — Up to date ✓", Toast.LENGTH_SHORT).show()
        }

        binding.rowReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Factory Reset")
                .setMessage("This will erase all ZarOS settings. Are you sure?")
                .setPositiveButton("Reset") { _, _ ->
                    getSharedPreferences("zaros_settings", MODE_PRIVATE).edit().clear().apply()
                    getSharedPreferences("fynder_prefs",   MODE_PRIVATE).edit().clear().apply()
                    Toast.makeText(this, "ZarOS reset complete", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.rowAbout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("About ZarOS")
                .setMessage("ZarOS Version ${BuildConfig.VERSION_NAME}\n\nBuilt with Kotlin + Java\nDefault browser: Fynder\n\n© 2025 ZarOS Project")
                .setPositiveButton("OK", null)
                .show()
        }
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    companion object {
        private const val REQUEST_ROLE_HOME = 1002
    }
}
