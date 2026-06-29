package com.zaros.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.zaros.app.R
import com.zaros.app.backend.TimeManager
import kotlinx.coroutines.*

class StatusBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private var tvTime:    TextView? = null
    private var tvBattery: TextView? = null
    private var tvSignal:  TextView? = null
    private var tvWifi:    TextView? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val level   = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return
            val scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val pct     = if (scale > 0) (level * 100 / scale) else -1
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val charging = plugged != 0
            tvBattery?.text = when {
                pct < 0  -> "🔋"
                charging -> "⚡$pct%"
                pct <= 15 -> "🪫$pct%"
                else     -> "🔋$pct%"
            }
        }
    }

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.view_status_bar, this, true)
        tvTime    = findViewById(R.id.tvStatusTime)
        tvBattery = findViewById(R.id.tvStatusBattery)
        tvSignal  = findViewById(R.id.tvStatusSignal)
        tvWifi    = findViewById(R.id.tvStatusWifi)
        updateTime()
        startTimeUpdater()
    }

    private fun updateTime() {
        tvTime?.text = TimeManager.getTime() + " " + TimeManager.getAmPm()
    }

    private fun startTimeUpdater() {
        scope.launch {
            while (isActive) {
                delay(10_000)
                updateTime()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try { context.unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        scope.cancel()
    }
}
