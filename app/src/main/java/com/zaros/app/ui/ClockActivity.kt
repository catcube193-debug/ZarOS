package com.zaros.app.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zaros.app.backend.AlarmReceiver
import com.zaros.app.backend.ClockService
import com.zaros.app.backend.SoundManager
import com.zaros.app.backend.TimeManager
import com.zaros.app.databinding.ActivityClockBinding
import java.util.Calendar

class ClockActivity : AppCompatActivity(), ClockService.Callback {

    private lateinit var binding: ActivityClockBinding
    private var clockService: ClockService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ClockService.ClockBinder
            clockService = binder.getService()
            clockService?.setCallback(this@ClockActivity)
            bound = true
            // Restore UI state from service
            updateSwDisplay(clockService?.getSwElapsed() ?: 0)
            updateTimerDisplay(clockService?.getTimerLeft() ?: 0)
            if (clockService?.isSwRunning() == true)
                binding.btnSwStartStop.text = "Stop"
            if (clockService?.isTimerRunning() == true)
                binding.btnTimerStart.text = "Stop"
        }
        override fun onServiceDisconnected(name: ComponentName) { bound = false }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        binding = ActivityClockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SoundManager.init(this)

        // Start and bind to ClockService
        val serviceIntent = Intent(this, ClockService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        setupTabs()
        setupStopwatch()
        setupTimer()
        setupAlarm()
        showTab(TAB_CLOCK)
        startClockTick()

        binding.btnBack.setOnClickListener { finish() }
    }

    // ── ClockService.Callback ─────────────────────────────────────────────
    override fun onSwTick(elapsed: Long)  { runOnUiThread { updateSwDisplay(elapsed) } }
    override fun onTimerTick(left: Long)  { runOnUiThread { updateTimerDisplay(left) } }
    override fun onTimerDone() {
        runOnUiThread {
            updateTimerDisplay(0)
            binding.btnTimerStart.text = "Start"
            binding.btnTimerStart.setBackgroundColor(0xFF22C55E.toInt())
            Toast.makeText(this, "⏰ Timer done!", Toast.LENGTH_LONG).show()
        }
    }

    // ── Clock tick ────────────────────────────────────────────────────────
    private val clockHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            binding.tvClockTime.text = TimeManager.getTime()
            binding.tvClockAmPm.text = TimeManager.getAmPm()
            binding.tvClockDate.text = TimeManager.getDate()
            clockHandler.postDelayed(this, 1000)
        }
    }
    private fun startClockTick() { clockHandler.post(clockRunnable) }

    // ── Tabs ──────────────────────────────────────────────────────────────
    private fun setupTabs() {
        binding.tabClock.setOnClickListener     { showTab(TAB_CLOCK);     SoundManager.playTap(this) }
        binding.tabStopwatch.setOnClickListener { showTab(TAB_STOPWATCH); SoundManager.playTap(this) }
        binding.tabTimer.setOnClickListener     { showTab(TAB_TIMER);     SoundManager.playTap(this) }
        binding.tabAlarm.setOnClickListener     { showTab(TAB_ALARM);     SoundManager.playTap(this) }
    }

    private fun showTab(tab: Int) {
        binding.panelClock.visibility     = if (tab == TAB_CLOCK)     View.VISIBLE else View.GONE
        binding.panelStopwatch.visibility = if (tab == TAB_STOPWATCH) View.VISIBLE else View.GONE
        binding.panelTimer.visibility     = if (tab == TAB_TIMER)     View.VISIBLE else View.GONE
        binding.panelAlarm.visibility     = if (tab == TAB_ALARM)     View.VISIBLE else View.GONE
        val active = 0xFF38BDF8.toInt(); val inactive = 0x88FFFFFF.toInt()
        binding.tabClock.setTextColor(if (tab == TAB_CLOCK) active else inactive)
        binding.tabStopwatch.setTextColor(if (tab == TAB_STOPWATCH) active else inactive)
        binding.tabTimer.setTextColor(if (tab == TAB_TIMER) active else inactive)
        binding.tabAlarm.setTextColor(if (tab == TAB_ALARM) active else inactive)
    }

    // ── Stopwatch ─────────────────────────────────────────────────────────
    private var lapCount = 0
    private fun setupStopwatch() {
        binding.btnSwStartStop.setOnClickListener {
            val svc = clockService ?: return@setOnClickListener
            if (svc.isSwRunning()) {
                svc.stopStopwatch()
                binding.btnSwStartStop.text = "Start"
                binding.btnSwStartStop.setBackgroundColor(0xFF22C55E.toInt())
            } else {
                svc.startStopwatch()
                binding.btnSwStartStop.text = "Stop"
                binding.btnSwStartStop.setBackgroundColor(0xFFEF4444.toInt())
            }
            SoundManager.playTap(this)
        }
        binding.btnSwReset.setOnClickListener {
            clockService?.resetStopwatch(); lapCount = 0
            updateSwDisplay(0)
            binding.btnSwStartStop.text = "Start"
            binding.btnSwStartStop.setBackgroundColor(0xFF22C55E.toInt())
            binding.tvLaps.text = ""
            SoundManager.playTap(this)
        }
        binding.btnSwLap.setOnClickListener {
            val elapsed = clockService?.getSwElapsed() ?: 0
            if (clockService?.isSwRunning() == true) {
                lapCount++
                val s = elapsed / 1000; val ms = (elapsed % 1000) / 10
                val m = s / 60; val sec = s % 60
                val t = "Lap $lapCount: ${m.toString().padStart(2,'0')}:${sec.toString().padStart(2,'0')}.${ms.toString().padStart(2,'0')}\n"
                binding.tvLaps.text = t + binding.tvLaps.text
                SoundManager.playTap(this)
            }
        }
    }

    private fun updateSwDisplay(elapsed: Long) {
        val ms = (elapsed % 1000) / 10; val s = elapsed / 1000
        val m = s / 60; val sec = s % 60
        binding.tvStopwatch.text = "${m.toString().padStart(2,'0')}:${sec.toString().padStart(2,'0')}.${ms.toString().padStart(2,'0')}"
    }

    // ── Timer ─────────────────────────────────────────────────────────────
    private fun setupTimer() {
        binding.pickerTimerMin.minValue = 0; binding.pickerTimerMin.maxValue = 99; binding.pickerTimerMin.value = 0
        binding.pickerTimerSec.minValue = 0; binding.pickerTimerSec.maxValue = 59; binding.pickerTimerSec.value = 30

        binding.btnTimerStart.setOnClickListener {
            val svc = clockService ?: return@setOnClickListener
            if (!svc.isTimerRunning()) {
                val totalMs = (binding.pickerTimerMin.value * 60L + binding.pickerTimerSec.value) * 1000L
                if (totalMs <= 0) { Toast.makeText(this, "Set a time first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                svc.startTimer(totalMs)
                binding.btnTimerStart.text = "Stop"
                binding.btnTimerStart.setBackgroundColor(0xFFEF4444.toInt())
            } else {
                svc.stopTimer()
                binding.btnTimerStart.text = "Resume"
                binding.btnTimerStart.setBackgroundColor(0xFF0EA5E9.toInt())
            }
            SoundManager.playTap(this)
        }
        binding.btnTimerReset.setOnClickListener {
            clockService?.resetTimer(); updateTimerDisplay(0)
            binding.btnTimerStart.text = "Start"
            binding.btnTimerStart.setBackgroundColor(0xFF22C55E.toInt())
            SoundManager.playTap(this)
        }
    }

    private fun updateTimerDisplay(left: Long) {
        val s = left / 1000; val m = s / 60; val sec = s % 60
        binding.tvTimer.text = "${m.toString().padStart(2,'0')}:${sec.toString().padStart(2,'0')}"
    }

    // ── Alarm ─────────────────────────────────────────────────────────────
    private fun setupAlarm() {
        binding.pickerAlarmHour.minValue = 1; binding.pickerAlarmHour.maxValue = 12; binding.pickerAlarmHour.value = 7
        binding.pickerAlarmMin.minValue  = 0; binding.pickerAlarmMin.maxValue  = 59; binding.pickerAlarmMin.value  = 0

        binding.btnSetAlarm.setOnClickListener {
            val h = binding.pickerAlarmHour.value; val min = binding.pickerAlarmMin.value
            val isAm = binding.btnAlarmAmPm.text == "AM"
            val h24 = h % 12 + if (isAm) 0 else 12
            scheduleAlarm(h24, min, "ZarOS Alarm")
            binding.tvAlarmStatus.text = "⏰ Alarm set for $h:${min.toString().padStart(2,'0')} ${binding.btnAlarmAmPm.text}"
            SoundManager.playTap(this)
        }
        binding.btnAlarmAmPm.setOnClickListener {
            binding.btnAlarmAmPm.text = if (binding.btnAlarmAmPm.text == "AM") "PM" else "AM"
            SoundManager.playTap(this)
        }
        binding.btnCancelAlarm.setOnClickListener {
            cancelAlarm()
            binding.tvAlarmStatus.text = "No alarm set"
            SoundManager.playTap(this)
        }
    }

    private fun scheduleAlarm(hour: Int, minute: Int, label: String) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM; putExtra(AlarmReceiver.EXTRA_LABEL, label)
        }
        val pi = PendingIntent.getBroadcast(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        Toast.makeText(this, "Alarm scheduled", Toast.LENGTH_SHORT).show()
    }

    private fun cancelAlarm() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(this, 0, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        pi?.let { am.cancel(it) }
        Toast.makeText(this, "Alarm cancelled", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacksAndMessages(null)
        if (bound) { unbindService(connection); bound = false }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    companion object {
        private const val TAB_CLOCK = 0; private const val TAB_STOPWATCH = 1
        private const val TAB_TIMER = 2; private const val TAB_ALARM = 3
    }
}
