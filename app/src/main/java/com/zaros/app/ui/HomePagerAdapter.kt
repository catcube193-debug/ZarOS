package com.zaros.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zaros.app.backend.AppInfo
import com.zaros.app.databinding.PageAppGridBinding

/**
 * ZarOS HomePagerAdapter (4.1.0)
 *
 * Drives the ViewPager2 on the home screen. Page 0 is always the
 * Spotlight/search page (its content is just an inflated static
 * layout — no special view-holder logic needed there since it has
 * no per-item binding). Every page after that is a 4-column app
 * grid showing one "chunk" of AppRegistry.getHomeApps(), so the
 * apps the user has automatically spread across as many pages as
 * needed instead of always cramming onto a single page.
 *
 * This mirrors how a real phone's home screen pages work: add more
 * apps to the registry and ZarOS automatically grows another page
 * for them rather than needing the layout hand-edited.
 *
 * onSpotlightBound is invoked every time the spotlight page (position 0)
 * is bound, so the caller can wire up its search bar / quick-app icons /
 * recent chips reliably — ViewPager2 can recycle and rebind that page
 * (e.g. after scrolling far away and back, or on memory-constrained
 * devices), so listeners attached only once in onCreate() would silently
 * stop working after a rebind without this callback.
 */
class HomePagerAdapter(
    private val appPages: List<List<AppInfo>>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onSpotlightBound: (View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SPOTLIGHT = 0
        private const val TYPE_APP_GRID  = 1
    }

    /** Holds the inflated spotlight page. No bind-time work needed — it's static content. */
    inner class SpotlightVH(view: View) : RecyclerView.ViewHolder(view)

    inner class AppGridVH(val binding: PageAppGridBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int = 1 + appPages.size

    override fun getItemViewType(position: Int): Int =
        if (position == 0) TYPE_SPOTLIGHT else TYPE_APP_GRID

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SPOTLIGHT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(com.zaros.app.R.layout.page_spotlight, parent, false)
            SpotlightVH(view)
        } else {
            val binding = PageAppGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            AppGridVH(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AppGridVH -> {
                // position 0 is the spotlight page, so grid pages start at index 1
                val apps = appPages[position - 1]
                val adapter = AppGridAdapter(apps, onAppClick)
                holder.binding.rvAppGrid.layoutManager =
                    GridLayoutManager(holder.binding.root.context, 4)
                holder.binding.rvAppGrid.adapter = adapter
            }
            is SpotlightVH -> {
                // Fired every time the spotlight page is (re)bound — e.g. after
                // ViewPager2 recycles it on a slow/low-end device — so click
                // listeners on the search bar / quick icons / chips always get
                // re-attached rather than only working the very first time.
                onSpotlightBound(holder.itemView)
            }
        }
    }
}
