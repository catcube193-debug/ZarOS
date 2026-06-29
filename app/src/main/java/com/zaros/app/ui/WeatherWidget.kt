package com.zaros.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.zaros.app.R
import com.zaros.app.backend.TimeManager
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * ZarOS WeatherWidget — home screen widget
 *
 * Uses Open-Meteo API (completely free, no key needed).
 * Falls back to a placeholder if location permission not granted.
 * Add to home layout with: <com.zaros.app.ui.WeatherWidget ... />
 */
class WeatherWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val tvTemp:      TextView
    private val tvCondition: TextView
    private val tvLocation:  TextView
    private val tvDate:      TextView

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val client = OkHttpClient()

    // WMO weather codes → emoji + description
    private val weatherCodes = mapOf(
        0 to Pair("☀️", "Clear"),
        1 to Pair("🌤️", "Mostly Clear"),
        2 to Pair("⛅", "Partly Cloudy"),
        3 to Pair("☁️", "Overcast"),
        45 to Pair("🌫️", "Foggy"),
        48 to Pair("🌫️", "Icy Fog"),
        51 to Pair("🌦️", "Light Drizzle"),
        61 to Pair("🌧️", "Rain"),
        71 to Pair("❄️", "Snow"),
        80 to Pair("🌧️", "Showers"),
        95 to Pair("⛈️", "Thunderstorm"),
        99 to Pair("⛈️", "Heavy Storm")
    )

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_weather_widget, this, true)
        tvTemp      = findViewById(R.id.tvWeatherTemp)
        tvCondition = findViewById(R.id.tvWeatherCondition)
        tvLocation  = findViewById(R.id.tvWeatherLocation)
        tvDate      = findViewById(R.id.tvWeatherDate)

        tvDate.text = TimeManager.getDate()
        loadWeather()
    }

    private fun loadWeather() {
        scope.launch {
            val (lat, lon) = getLocation() ?: run {
                // Default to Rupert, Idaho if no permission
                Pair(42.6138, -113.6766)
            }
            fetchWeather(lat, lon)
        }
    }

    private suspend fun getLocation(): Pair<Double, Double>? {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return withContext(Dispatchers.IO) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc: Location? = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                loc?.let { Pair(it.latitude, it.longitude) }
            } catch (_: Exception) { null }
        }
    }

    private suspend fun fetchWeather(lat: Double, lon: Double) {
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current_weather=true&temperature_unit=fahrenheit"
                val req = Request.Builder().url(url).build()
                val res = client.newCall(req).execute()
                val body = res.body?.string() ?: return@withContext
                val json = JSONObject(body)
                val cw   = json.getJSONObject("current_weather")
                val temp = cw.getDouble("temperature").toInt()
                val code = cw.getInt("weathercode")
                val (emoji, desc) = weatherCodes[code] ?: Pair("🌡️", "Unknown")

                withContext(Dispatchers.Main) {
                    tvTemp.text      = "$temp°F"
                    tvCondition.text = "$emoji $desc"
                    tvLocation.text  = "📍 Your Location"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvTemp.text      = "--°F"
                    tvCondition.text = "☁️ Unavailable"
                    tvLocation.text  = "Enable location for weather"
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
