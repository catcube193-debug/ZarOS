package com.zaros.app.ui

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zaros.app.R
import com.zaros.app.backend.AppInfo
import com.zaros.app.databinding.ItemDockIconBinding

class DockAdapter(
    private val apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<DockAdapter.DockVH>() {

    inner class DockVH(val binding: ItemDockIconBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DockVH {
        val binding = ItemDockIconBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DockVH(binding)
    }

    override fun onBindViewHolder(holder: DockVH, position: Int) {
        val app = apps[position]
        val ctx = holder.binding.root.context

        holder.binding.tvDockLabel.text = app.label

        val isInternal = isInternalApp(app.packageName)

        if (isInternal) {
            holder.binding.ivDockIcon.setImageDrawable(ctx.getDrawable(app.iconResId))
            val glossRes = if (app.glossBgRes != 0) app.glossBgRes else R.drawable.bg_glossy_blue
            holder.binding.ivDockIconBg.setBackgroundResource(glossRes)
        } else {
            val pm = ctx.packageManager
            val realIcon = try { pm.getApplicationIcon(app.packageName) } catch (e: PackageManager.NameNotFoundException) { null }
            if (realIcon != null) {
                holder.binding.ivDockIcon.setImageDrawable(realIcon)
                holder.binding.ivDockIcon.setPadding(0, 0, 0, 0)
                holder.binding.ivDockIconBg.background = null
            } else {
                holder.binding.ivDockIcon.setImageDrawable(ctx.getDrawable(app.iconResId))
                val glossRes = if (app.glossBgRes != 0) app.glossBgRes else R.drawable.bg_glossy_blue
                holder.binding.ivDockIconBg.setBackgroundResource(glossRes)
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
