package com.zaros.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.zaros.app.R
import com.zaros.app.backend.AppInfo
import com.zaros.app.backend.AppLauncher
import com.zaros.app.backend.AppRegistry
import com.zaros.app.backend.SoundManager
import com.zaros.app.backend.ZarOSPrefs
import com.zaros.app.databinding.ActivityHomeBinding

/**
 * ZarOS HomeActivity (4.1.0 — Springboard)
 *
 * The home screen is now a real ViewPager2-driven springboard:
 *   Page 0     → Spotlight (search bar, weather, quick icons, recent chips)
 *   Page 1..N  → App grid pages, auto-split from AppRegistry.getHomeApps()
 *
 * Adding more apps to the registry automatically creates more pages —
 * nothing here needs to change as the app list grows.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var touchStartY = 0f

    /** Apps per grid page. 8 (a 4x2 grid) keeps each page comfortably below the dock/fold. */
    private val appsPerPage = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SoundManager.init(this)
        applyWallpaper()
        requestLocationPermission()
        setupSpringboard()
        setupDock()
        setupGestures()
        setupAppDrawerButton()
    }

    override fun onResume() {
        super.onResume()
        applyWallpaper()
    }

    /**
     * Applies the selected wallpaper. If animated wallpaper is enabled,
     * drives AnimatedWallpaperView with the selected color pair and hides
     * the static gradient background; otherwise shows the static gradient.
     */
    private fun applyWallpaper() {
        val idx = ZarOSPrefs.getWallpaper(this)
        val startColor = android.graphics.Color.parseColor(ZarOSPrefs.WALLPAPER_STARTS[idx])
        val endColor   = android.graphics.Color.parseColor(ZarOSPrefs.WALLPAPER_ENDS[idx])

        if (ZarOSPrefs.isAnimatedWallpaper(this)) {
            binding.root.background = null
            binding.animatedWallpaper.visibility = View.VISIBLE
            binding.animatedWallpaper.setColors(startColor, endColor)
            binding.animatedWallpaper.startAnimating()
        } else {
            binding.animatedWallpaper.stopAnimating()
            binding.animatedWallpaper.visibility = View.GONE
            val grad = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(startColor, endColor)
            )
            binding.root.background = grad
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    // ── Springboard (ViewPager2 + page indicator dots) ──────────────────────

    /**
     * Splits [items] into pages of at most [maxPerPage], but balances the
     * final split evenly across however many pages are needed instead of
     * always filling earlier pages to the max — e.g. 11 apps with a max
     * of 8 per page becomes two pages of 6 and 5 (not a lopsided 8 + 3).
     * This keeps every springboard page looking intentional rather than
     * having a nearly-empty trailing page as more apps get added over time.
     */
    private fun <T> evenlyChunked(items: List<T>, maxPerPage: Int): List<List<T>> {
        if (items.isEmpty()) return emptyList()
        val pageCount = Math.ceil(items.size.toDouble() / maxPerPage).toInt()
        val perPage = Math.ceil(items.size.toDouble() / pageCount).toInt()
        return items.chunked(perPage)
    }

    private fun setupSpringboard() {
        val appPages: List<List<AppInfo>> = evenlyChunked(AppRegistry.getHomeApps(), appsPerPage)

        val pagerAdapter = HomePagerAdapter(
            appPages = appPages,
            onAppClick = { appInfo ->
                SoundManager.playAppOpen(this)
                AppLauncher.launch(this, appInfo.packageName)
            },
            onSpotlightBound = { spotlightView ->
                wireSpotlightPage(spotlightView)
            }
        )

        binding.homePager.adapter = pagerAdapter
        // Springboard pages don't nest a vertically-scrolling parent the way
        // the old layout did, so the default page transformer / offscreen
        // page limit are fine here — no special tuning needed.

        setupPageIndicator(totalPages = 1 + appPages.size)
    }

    /**
     * Wires the spotlight page's interactive views. Called every time
     * HomePagerAdapter (re)binds the spotlight page — see the adapter's
     * doc comment for why a one-time onCreate() wiring isn't sufficient
     * with ViewPager2's RecyclerView-backed recycling.
     */
    private fun wireSpotlightPage(spotlightView: View) {
        spotlightView.findViewById<View>(R.id.searchBar)?.setOnClickListener {
            SoundManager.playTap(this)
            startActivity(Intent(this, FynderActivity::class.java))
            overridePendingTransition(R.anim.slide_up_enter, 0)
        }
        spotlightView.findViewById<View>(R.id.quickPhone)?.setOnClickListener {
            SoundManager.playTap(this)
            AppLauncher.launch(this, "com.zaros.app.dialer")
        }
        spotlightView.findViewById<View>(R.id.quickFynder)?.setOnClickListener {
            SoundManager.playTap(this)
            AppLauncher.launch(this, "app.fynder.browser")
        }
        spotlightView.findViewById<View>(R.id.quickMessages)?.setOnClickListener {
            SoundManager.playTap(this)
            AppLauncher.launch(this, "com.zaros.app.messages")
        }
        spotlightView.findViewById<View>(R.id.chipFynder)?.setOnClickListener {
            SoundManager.playTap(this)
            AppLauncher.launch(this, "app.fynder.browser")
        }
        spotlightView.findViewById<View>(R.id.chipSettings)?.setOnClickListener {
            SoundManager.playTap(this)
            AppLauncher.launch(this, "com.zaros.app.settings")
        }
        spotlightView.findViewById<View>(R.id.chipStore)?.setOnClickListener {
            SoundManager.playTap(this)
            AppLauncher.launch(this, "com.zaros.app.store")
        }
    }

    private fun setupPageIndicator(totalPages: Int) {
        binding.pageIndicator.removeAllViews()

        // Single-page case (shouldn't normally happen with 11+ home apps,
        // but stay correct if the registry ever shrinks below appsPerPage):
        // don't bother showing a dot row for just one page.
        if (totalPages <= 1) {
            binding.pageIndicator.visibility = View.GONE
            return
        }
        binding.pageIndicator.visibility = View.VISIBLE

        val dots = mutableListOf<ImageView>()
        val dotSizePx = (8 * resources.displayMetrics.density).toInt()
        val dotMarginPx = (4 * resources.displayMetrics.density).toInt()

        for (i in 0 until totalPages) {
            val dot = ImageView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(dotSizePx, dotSizePx).apply {
                    marginStart = dotMarginPx
                    marginEnd   = dotMarginPx
                }
                setImageResource(if (i == 0) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            }
            dots.add(dot)
            binding.pageIndicator.addView(dot)
        }

        binding.homePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.forEachIndexed { index, dot ->
                    dot.setImageResource(
                        if (index == position) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
                    )
                }
            }
        })
    }

    // ── Dock ──────────────────────────────────────────────────────────────

    private fun setupDock() {
        val dockApps = AppRegistry.getDockApps()
        val dockAdapter = DockAdapter(dockApps) { appInfo ->
            SoundManager.playTap(this)
            AppLauncher.launch(this, appInfo.packageName)
        }
        binding.rvDock.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, dockApps.size)
            adapter = dockAdapter
        }
    }

    private fun setupAppDrawerButton() {
        binding.btnAppDrawer?.setOnClickListener {
            SoundManager.playTap(this)
            startActivity(Intent(this, AppDrawerActivity::class.java))
            overridePendingTransition(R.anim.slide_up_enter, 0)
        }
    }

    // ── Gestures ──────────────────────────────────────────────────────────

    @SuppressLint("ClicksNotAccessible")
    private fun setupGestures() {
        // Attached to homePager (not binding.root) because ViewPager2 now
        // covers nearly the whole screen and Android's touch dispatch is
        // child-first — a listener on the root FrameLayout would rarely
        // see anything since the pager claims touches before they'd ever
        // bubble up. Returning false here lets ViewPager2 still handle its
        // own horizontal paging/fling normally; we only inspect the
        // DOWN/UP coordinates to detect a top-of-screen swipe-down.
        binding.homePager.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { touchStartY = event.rawY; false }
                MotionEvent.ACTION_UP -> {
                    if (touchStartY < 120 && event.rawY - touchStartY > 80) {
                        openNotificationShade()
                        true
                    } else false
                }
                else -> false
            }
        }
    }

    private fun openNotificationShade() {
        SoundManager.playTap(this)
        startActivity(Intent(this, NotificationShadeActivity::class.java))
        overridePendingTransition(R.anim.slide_down_enter, 0)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(android.R.anim.fade_in, R.anim.slide_down_exit)
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
}
