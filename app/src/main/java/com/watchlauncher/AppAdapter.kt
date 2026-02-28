package com.watchlauncher

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private var apps: List<AppInfo>,
    private var colors: ThemeColors,
    private var showLabels: Boolean,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    private var filtered: List<AppInfo> = apps.toList()

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val label: TextView = view.findViewById(R.id.appLabel)
        val iconBg: View    = view.findViewById(R.id.iconBg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = filtered[position]
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label
        holder.label.setTextColor(colors.textSecondary)
        holder.label.visibility = if (showLabels) View.VISIBLE else View.GONE

        // Icon background: blend primary color at low alpha over the background
        val r = Color.red(colors.primary)
        val g = Color.green(colors.primary)
        val b = Color.blue(colors.primary)
        val iconBgColor = Color.argb(40, r, g, b)
        val bg = GradientDrawable().apply {
            setColor(iconBgColor)
            cornerRadius = 24f
        }
        holder.iconBg.background = bg

        holder.itemView.setOnClickListener { onAppClick(app) }
        holder.itemView.setOnLongClickListener { onAppLongClick(app); true }
    }

    override fun getItemCount() = filtered.size

    fun filter(q: String) {
        filtered = if (q.isBlank()) apps.toList()
        else apps.filter { it.label.contains(q, ignoreCase = true) }
        notifyDataSetChanged()
    }

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps; filtered = newApps.toList(); notifyDataSetChanged()
    }

    fun updateTheme(newColors: ThemeColors, newShowLabels: Boolean) {
        colors = newColors; showLabels = newShowLabels; notifyDataSetChanged()
    }
}
