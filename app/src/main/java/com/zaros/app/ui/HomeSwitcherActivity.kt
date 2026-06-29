package com.zaros.app.ui

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zaros.app.backend.SoundManager
import com.zaros.app.databinding.ActivityHomeSwitcherBinding

/**
 * ZarOS HomeSwitcherActivity (4.0.0)
 *
 * Standalone "Home App" switcher — separated out from Settings
 * so it can be launched independently from the home grid / app drawer too.
 * Lets the user pick their default launcher (ZarOS or otherwise).
 */
class HomeSwitcherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeSwitcherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        binding = ActivityHomeSwitcherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SoundManager.init(this)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSwitchLauncher.setOnClickListener {
            SoundManager.playTap(this)
            switchLauncher()
        }
        binding.btnSetZarOS.setOnClickListener {
            SoundManager.playTap(this)
            setZarOSAsDefault()
        }
    }

    private fun switchLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                startActivityForResult(intent, REQ_ROLE_HOME)
                return
            }
        }
        openFallbackSettings()
    }

    private fun setZarOSAsDefault() {
        switchLauncher()
        Toast.makeText(this, "Select ZarOS from the list", Toast.LENGTH_LONG).show()
    }

    private fun openFallbackSettings() {
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            } catch (e2: Exception) {
                Toast.makeText(this,
                    "Go to Settings → Apps → Default Apps → Home App",
                    Toast.LENGTH_LONG).show()
            }
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
        private const val REQ_ROLE_HOME = 1002
    }
}
