package com.fomokiller

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fomokiller.databinding.ActivityAppPickerBinding

class AppPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppPickerBinding
    private lateinit var mode: String
    private val selectedApps = mutableSetOf<String>()
    private val allApps = mutableListOf<AppInfo>()

    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: android.graphics.drawable.Drawable?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.init(applicationContext)

        binding = ActivityAppPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mode = intent.getStringExtra("mode") ?: "blocked"

        // Pre-select currently saved apps
        selectedApps.addAll(
            if (mode == "blocked") AppState.blockedApps else AppState.vipApps
        )

        binding.titleText.text = if (mode == "blocked") "Apps à bloquer" else "Apps VIP"
        binding.subtitleText.text = if (mode == "blocked")
            "Bloquées en mode Tout Bloquer"
        else
            "Autorisées en mode VIP seulement"

        loadApps()
        setupRecyclerView()

        binding.btnDone.setOnClickListener {
            saveAndExit()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadApps() {
        binding.loadingIndicator.visibility = View.VISIBLE

        Thread {
            val pm = packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            val userApps = installedApps
                .filter { app ->
                    // Only show user-installed apps (not system) unless they have launcher activity
                    val isUserApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                    val hasLauncher = pm.getLaunchIntentForPackage(app.packageName) != null
                    // Exclude ourselves and always-allowed packages in VIP mode
                    val isNotUs = app.packageName != packageName
                    (isUserApp || hasLauncher) && isNotUs
                }
                .map { app ->
                    AppInfo(
                        packageName = app.packageName,
                        label = pm.getApplicationLabel(app).toString(),
                        icon = try { pm.getApplicationIcon(app.packageName) } catch (_: Exception) { null }
                    )
                }
                .sortedBy { it.label.lowercase() }

            allApps.clear()
            allApps.addAll(userApps)

            runOnUiThread {
                binding.loadingIndicator.visibility = View.GONE
                binding.recyclerView.adapter?.notifyDataSetChanged()
                updateDoneButton()
            }
        }.start()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = AppAdapter()
    }

    private fun updateDoneButton() {
        val count = selectedApps.size
        binding.btnDone.text = if (count > 0) "Confirmer ($count)" else "Confirmer"
    }

    private fun saveAndExit() {
        if (mode == "blocked") {
            AppState.blockedApps = selectedApps.toSet()
        } else {
            AppState.vipApps = selectedApps.toSet()
        }
        finish()
    }

    inner class AppAdapter : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.appIcon)
            val label: TextView = view.findViewById(R.id.appLabel)
            val pkg: TextView = view.findViewById(R.id.appPackage)
            val checkbox: CheckBox = view.findViewById(R.id.appCheckbox)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = allApps[position]
            holder.label.text = app.label
            holder.pkg.text = app.packageName
            holder.icon.setImageDrawable(app.icon)
            holder.checkbox.isChecked = selectedApps.contains(app.packageName)

            val clickListener = View.OnClickListener {
                if (selectedApps.contains(app.packageName)) {
                    selectedApps.remove(app.packageName)
                } else {
                    selectedApps.add(app.packageName)
                }
                holder.checkbox.isChecked = selectedApps.contains(app.packageName)
                updateDoneButton()
            }

            holder.itemView.setOnClickListener(clickListener)
            holder.checkbox.setOnClickListener(clickListener)
        }

        override fun getItemCount() = allApps.size
    }
}
