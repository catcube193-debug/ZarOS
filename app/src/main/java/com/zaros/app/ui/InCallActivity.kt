package com.zaros.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.zaros.app.R
import com.zaros.app.backend.ContactsManager
import com.zaros.app.backend.SoundManager
import com.zaros.app.databinding.ActivityInCallBinding
import kotlinx.coroutines.*

/**
 * ZarOS InCallActivity
 * Custom call screen shown during active calls.
 * Shows contact name, initials avatar, call timer, mute/speaker/hang up.
 */
class InCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInCallBinding
    private val scope   = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    private var callSeconds = 0
    private var muted       = false
    private var speaker     = false
    private var ringtone: MediaPlayer? = null

    // Incoming call info passed via intent
    private var phoneNumber = ""
    private var contactName = ""
    private var isIncoming  = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        phoneNumber = intent.getStringExtra(EXTRA_NUMBER) ?: ""
        isIncoming  = intent.getBooleanExtra(EXTRA_INCOMING, false)

        // Look up contact name
        scope.launch {
            contactName = withContext(Dispatchers.IO) {
                ContactsManager.getNameForNumber(this@InCallActivity, phoneNumber)
            }
            setupUI()
        }

        SoundManager.init(this)
    }

    private fun setupUI() {
        binding.tvCallerName.text   = contactName.ifEmpty { phoneNumber }
        binding.tvCallerNumber.text = phoneNumber

        // Initials avatar with gradient
        val initials = if (contactName.isNotEmpty()) {
            val parts = contactName.trim().split(" ")
            if (parts.size >= 2) "${parts[0][0]}${parts[1][0]}"
            else "${parts[0][0]}"
        } else "?"
        binding.tvAvatar.text = initials.uppercase()

        val grad = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF0EA5E9.toInt(), 0xFF0369A1.toInt())
        )
        grad.cornerRadius = 999f
        binding.tvAvatar.background = grad

        if (isIncoming) {
            // NOTE: This branch is currently unreachable in the shipped app.
            // EXTRA_INCOMING is only ever set to false by DialerActivity
            // (the sole caller of this Activity right now), because
            // detecting and responding to real incoming calls requires
            // ZarOS to hold the default Dialer role (RoleManager.ROLE_DIALER)
            // and implement an InCallService — neither of which exists yet.
            // This UI is left in place, ready to be wired up if/when that
            // bigger integration is built, rather than deleted outright.
            binding.layoutIncoming.visibility = View.VISIBLE
            binding.layoutActive.visibility   = View.GONE
            binding.tvCallStatus.text         = "Incoming Call"
            playRingtone()

            binding.btnAnswer.setOnClickListener {
                answerCall()
                SoundManager.playTap(this)
            }
            binding.btnDecline.setOnClickListener {
                declineCall()
                SoundManager.playTap(this)
            }
        } else {
            // Outgoing / active call
            binding.layoutIncoming.visibility = View.GONE
            binding.layoutActive.visibility   = View.VISIBLE
            binding.tvCallStatus.text         = "Calling..."
            startCallTimer()
        }

        // Active call controls
        binding.btnMute.setOnClickListener {
            muted = !muted
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.isMicrophoneMute = muted
            binding.btnMute.text = if (muted) "🎙️\nUnmute" else "🎙️\nMute"
            binding.btnMute.alpha = if (muted) 0.5f else 1f
            SoundManager.playTap(this)
        }

        binding.btnSpeaker.setOnClickListener {
            speaker = !speaker
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.isSpeakerphoneOn = speaker
            binding.btnSpeaker.text = if (speaker) "🔊\nSpeaker" else "🔈\nSpeaker"
            binding.btnSpeaker.alpha = if (speaker) 1f else 0.5f
            SoundManager.playTap(this)
        }

        binding.btnHangup.setOnClickListener {
            hangUp()
            SoundManager.playTap(this)
        }
    }

    private fun startCallTimer() {
        binding.tvCallStatus.text = "00:00"
        handler.postDelayed(object : Runnable {
            override fun run() {
                callSeconds++
                val m = callSeconds / 60; val s = callSeconds % 60
                binding.tvCallStatus.text = String.format("%02d:%02d", m, s)
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun answerCall() {
        stopRingtone()
        binding.layoutIncoming.visibility = View.GONE
        binding.layoutActive.visibility   = View.VISIBLE
        startCallTimer()
        // Answer via TelecomManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS)
            == PackageManager.PERMISSION_GRANTED) {
            val tm = getSystemService(TelecomManager::class.java)
            tm?.acceptRingingCall()
        }
    }

    private fun declineCall() {
        stopRingtone()
        endCallSafely()
        finish()
    }

    private fun hangUp() {
        handler.removeCallbacksAndMessages(null)
        endCallSafely()
        finish()
    }

    /**
     * Attempts to end the call we initiated via TelecomManager. The exact
     * permission requirements for a non-default-dialer app to end its own
     * self-placed call have varied across Android versions and OEM skins,
     * so this can't be guaranteed to work on every device — if it doesn't,
     * we still close our screen and tell the user plainly rather than
     * leaving the button looking broken with no explanation. The system
     * call (and its own controls, if the OS shows any) continues either way.
     */
    private fun endCallSafely() {
        try {
            val tm = getSystemService(TelecomManager::class.java)
            val ended = tm?.endCall() ?: false
            if (!ended) {
                android.widget.Toast.makeText(this,
                    "Couldn't end the call from here — use your phone's " +
                    "normal call screen to hang up.", android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: SecurityException) {
            android.widget.Toast.makeText(this,
                "ZarOS doesn't have permission to end this call on this device — " +
                "use your phone's normal call screen to hang up.",
                android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Unexpected — still close our screen rather than getting stuck.
        }
    }

    private fun playRingtone() {
        try {
            ringtone = MediaPlayer.create(this, R.raw.ringtone_default)
            ringtone?.isLooping = true
            ringtone?.start()
        } catch (e: Exception) { /* no ringtone */ }
    }

    private fun stopRingtone() {
        ringtone?.stop()
        ringtone?.release()
        ringtone = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    companion object {
        const val EXTRA_NUMBER   = "extra_number"
        const val EXTRA_INCOMING = "extra_incoming"
    }
}
