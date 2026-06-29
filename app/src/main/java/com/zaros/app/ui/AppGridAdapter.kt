package com.zaros.app.ui

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zaros.app.R
import com.zaros.app.backend.AppInfo
import com.zaros.app.databinding.ItemAppIconBinding

/**
 * ZarOS AppGridAdapter (4.0.0 — Glossy iOS-style icons)
 * Each cell: gradient squircle bg + symbol + gloss shine overlay.
 */
class AppGridAdapter(
    private val apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.AppVH>() {

    inner class AppVH(val binding: ItemAppIconBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppVH {
        val binding = ItemAppIconBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AppVH(binding)
    }

    override fun onBindViewHolder(holder: AppVH, position: Int) {
        val app = apps[position]
        val ctx = holder.binding.root.context

        holder.binding.tvAppLabel.text = app.label

        val isInternal = isInternalApp(app.packageName)

        if (isInternal) {
            holder.binding.ivAppIcon.setImageDrawable(ctx.getDrawable(app.iconResId))
            val glossRes = if (app.glossBgRes != 0) app.glossBgRes else R.drawable.bg_glossy_blue
            holder.binding.ivIconBg.setBackgroundResource(glossRes)
        } else {
            val pm = ctx.packageManager
            val realIcon = try { pm.getApplicationIcon(app.packageName) } catch (e: PackageManager.NameNotFoundException) { null }
            if (realIcon != null) {
                holder.binding.ivAppIcon.setImageDrawable(realIcon)
                holder.binding.ivAppIcon.setPadding(0, 0, 0, 0)
                holder.binding.ivIconBg.background = null
            } else {
                holder.binding.ivAppIcon.setImageDrawable(ctx.getDrawable(app.iconResId))
                val glossRes = if (app.glossBgRes != 0) app.glossBgRes else R.drawable.bg_glossy_blue
                holder.binding.ivIconBg.setBackgroundResource(glossRes)
            }
        }

        holder.binding.root.setOnClickListener {
            it.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    onAppClick(app)
                }.start()
        }
    }

    private fun isInternalApp(packageName: String): Boolean {
        return packageName.startsWith("com.zaros.app.") ||
               packageName == "app.fynder.browser" ||
               packageName == "android.media.action.IMAGE_CAPTURE"
    }

    override fun getItemCount() = apps.size
}
