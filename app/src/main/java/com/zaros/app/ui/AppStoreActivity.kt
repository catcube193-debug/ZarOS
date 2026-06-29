package com.zaros.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zaros.app.R
import com.zaros.app.backend.SoundManager
import com.zaros.app.databinding.ActivityAppStoreBinding

data class StoreApp(
    val name: String,
    val developer: String,
    val description: String,
    val emoji: String,
    val rating: String,
    val category: String,
    val playStoreId: String,
    val isInstalled: Boolean = false
)

class AppStoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppStoreBinding
    private var currentCategory = "All"

    private val allApps = listOf(
        StoreApp("YouTube",        "Google",        "Watch videos, music & live streams",       "▶️",  "4.7", "Media",       "com.google.android.youtube",             true),
        StoreApp("YT Music",       "Google",        "Stream music and podcasts",                "🎵",  "4.5", "Media",       "com.google.android.apps.youtube.music",  true),
        StoreApp("Fynder",         "ZarOS",         "AI-powered browser and search engine",     "🔍",  "4.9", "ZarOS",       "app.fynder.browser",                     true),
        StoreApp("Spotify",        "Spotify AB",    "Music & podcasts streaming",               "🎧",  "4.6", "Media",       "com.spotify.music",                      false),
        StoreApp("Netflix",        "Netflix",       "Stream movies and TV shows",               "🎬",  "4.4", "Media",       "com.netflix.mediaclient",                false),
        StoreApp("Instagram",      "Meta",          "Share photos and videos",                  "📸",  "4.2", "Social",      "com.instagram.android",                  false),
        StoreApp("WhatsApp",       "Meta",          "Messaging and video calls",                "💬",  "4.5", "Social",      "com.whatsapp",                           false),
        StoreApp("Snapchat",       "Snap Inc",      "Snap, chat, and share",                    "👻",  "4.1", "Social",      "com.snapchat.android",                   false),
        StoreApp("TikTok",         "ByteDance",     "Short videos and live streams",            "🎶",  "4.3", "Social",      "com.zhiliaoapp.musically",               false),
        StoreApp("Google Maps",    "Google",        "Navigation and directions",                "🗺️",  "4.7", "Navigation",  "com.google.android.apps.maps",           false),
        StoreApp("Uber",           "Uber",          "Request a ride in minutes",                "🚗",  "4.4", "Navigation",  "com.ubercab",                            false),
        StoreApp("Discord",        "Discord Inc",   "Chat with friends and communities",        "🎮",  "4.5", "Social",      "com.discord",                            false),
        StoreApp("Twitch",         "Twitch",        "Live game streaming",                      "💜",  "4.4", "Media",       "tv.twitch.android.app",                  false),
        StoreApp("Gmail",          "Google",        "Email by Google",                          "📧",  "4.5", "Productivity","com.google.android.gm",                  false),
        StoreApp("Google Drive",   "Google",        "Cloud storage and file sharing",           "☁️",  "4.6", "Productivity","com.google.android.apps.docs",           false),
        StoreApp("Minecraft",      "Mojang",        "Build and explore infinite worlds",        "⛏️",  "4.6", "Games",       "com.mojang.minecraftpe",                 false),
        StoreApp("Roblox",         "Roblox Corp",   "Play millions of games",                   "🟥",  "4.3", "Games",       "com.roblox.client",                      false),
        StoreApp("Cash App",       "Block Inc",     "Send and receive money",                   "💰",  "4.6", "Finance",     "com.squareup.cash",                      false),
        StoreApp("Amazon",         "Amazon",        "Shop millions of products",                "📦",  "4.5", "Shopping",    "com.amazon.mShop.android.shopping",      false),
        StoreApp("Shazam",         "Apple",         "Identify songs instantly",                 "🎵",  "4.7", "Music",       "com.shazam.android",                     false),
    )

    private val categories = listOf("All", "ZarOS", "Media", "Social", "Games", "Navigation", "Productivity", "Finance", "Shopping", "Music")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        binding = ActivityAppStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SoundManager.init(this)
        binding.btnBack.setOnClickListener { finish() }
        setupCategories()
        setupAppList(allApps)
        setupSearch()
    }

    private fun setupCategories() {
        binding.rvCategories.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategories.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                val tv: TextView = v.findViewById(R.id.tvCategory)
            }
            override fun onCreateViewHolder(p: ViewGroup, t: Int) =
                VH(LayoutInflater.from(p.context).inflate(R.layout.item_category_chip, p, false))
            override fun getItemCount() = categories.size
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, i: Int) {
                h as VH
                h.tv.text = categories[i]
                h.tv.isSelected = categories[i] == currentCategory
                h.tv.alpha = if (categories[i] == currentCategory) 1f else 0.6f
                h.itemView.setOnClickListener {
                    currentCategory = categories[i]
                    notifyDataSetChanged()
                    val filtered = if (currentCategory == "All") allApps
                        else allApps.filter { it.category == currentCategory }
                    setupAppList(filtered)
                    SoundManager.playTap(this@AppStoreActivity)
                }
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s.toString().trim()
                val filtered = if (q.isEmpty()) allApps
                    else allApps.filter {
                        it.name.contains(q, true) || it.description.contains(q, true)
                    }
                setupAppList(filtered)
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    private fun setupAppList(apps: List<StoreApp>) {
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                val emoji:  TextView = v.findViewById(R.id.tvStoreEmoji)
                val name:   TextView = v.findViewById(R.id.tvStoreName)
                val dev:    TextView = v.findViewById(R.id.tvStoreDev)
                val desc:   TextView = v.findViewById(R.id.tvStoreDesc)
                val rating: TextView = v.findViewById(R.id.tvStoreRating)
                val btn:    TextView = v.findViewById(R.id.btnStoreAction)
            }
            override fun onCreateViewHolder(p: ViewGroup, t: Int) =
                VH(LayoutInflater.from(p.context).inflate(R.layout.item_store_app, p, false))
            override fun getItemCount() = apps.size
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, i: Int) {
                h as VH; val app = apps[i]
                h.emoji.text  = app.emoji
                h.name.text   = app.name
                h.dev.text    = app.developer
                h.desc.text   = app.description
                h.rating.text = "★ ${app.rating}"
                h.btn.text    = if (app.isInstalled) "Open" else "Get"
                h.btn.alpha   = if (app.isInstalled) 0.6f else 1f
                h.btn.setOnClickListener {
                    SoundManager.playTap(this@AppStoreActivity)
                    if (app.isInstalled) {
                        // Launch the app
                        val launch = packageManager.getLaunchIntentForPackage(app.playStoreId)
                        if (launch != null) startActivity(launch)
                        else Toast.makeText(this@AppStoreActivity, "Opening ${app.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        // Open Play Store listing
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=${app.playStoreId}")))
                        } catch (e: Exception) {
                            startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=${app.playStoreId}")))
                        }
                    }
                }
                h.itemView.setOnClickListener { h.btn.performClick() }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
}
