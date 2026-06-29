package com.zaros.app.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zaros.app.R
import com.zaros.app.backend.AppDrawerManager
import com.zaros.app.backend.AppLauncher
import com.zaros.app.backend.SoundManager
import com.zaros.app.databinding.ActivityAppDrawerBinding
import kotlinx.coroutines.*

/**
 * ZarOS AppDrawerActivity (Kotlin UI)
 *
 * Full-screen app drawer:
 *  - Loads all installed apps via AppDrawerManager
 *  - Live search filter
 *  - 4-column grid with real app icons
 *  - Tap to launch + sound/haptic
 *  - Slide-up animation entry
 */
class AppDrawerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDrawerBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var allApps: List<AppDrawerManager.DrawerApp> = emptyList()
    private lateinit var adapter: DrawerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        binding = ActivityAppDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        loadApps()

        binding.btnClose.setOnClickListener {
            SoundManager.playTap(this)
            finish()
            overridePendingTransition(0, R.anim.slide_down_exit)
        }
    }

    private fun setupRecyclerView() {
        adapter = DrawerAdapter { app ->
            SoundManager.playAppOpen(this)
            AppLauncher.launch(this, app.packageName)
        }
        binding.rvDrawer.layoutManager = GridLayoutManager(this, 4)
        binding.rvDrawer.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                val filtered = if (query.isEmpty()) allApps
                else allApps.filter {
                    it.label.contains(query, ignoreCase = true)
                }
                adapter.submitList(filtered)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadApps() {
        binding.progressLoading.visibility = View.VISIBLE
        scope.launch {
            val apps = withContext(Dispatchers.IO) {
                AppDrawerManager.getAllApps(applicationContext)
            }
            allApps = apps
            adapter.submitList(apps)
            binding.progressLoading.visibility = View.GONE
            binding.tvAppCount.text = "${apps.size} apps"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    // ── Drawer RecyclerView Adapter ───────────────────────────────────────

    inner class DrawerAdapter(
        private val onAppClick: (AppDrawerManager.DrawerApp) -> Unit
    ) : RecyclerView.Adapter<DrawerAdapter.VH>() {

        private var apps: List<AppDrawerManager.DrawerApp> = emptyList()

        fun submitList(list: List<AppDrawerManager.DrawerApp>) {
            apps = list
            notifyDataSetChanged()
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView  = view.findViewById(R.id.ivAppIcon)
            val label: TextView  = view.findViewById(R.id.tvAppLabel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_icon, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val app = apps[position]
            holder.icon.setImageDrawable(app.icon)
            holder.icon.background = null
            holder.label.text = app.label
            holder.itemView.setOnClickListener {
                it.animate().scaleX(0.85f).scaleY(0.85f).setDuration(90)
                    .withEndAction {
                        it.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                        onAppClick(app)
                    }.start()
            }
        }

        override fun getItemCount() = apps.size
    }
}
