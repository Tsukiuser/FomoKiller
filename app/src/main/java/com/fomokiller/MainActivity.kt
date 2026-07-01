package com.fomokiller

import android.Manifest
import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButtonToggleGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fomokiller.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsBehavior: BottomSheetBehavior<View>
    private lateinit var keywordsBehavior: BottomSheetBehavior<View>
    private lateinit var gestureDetector: GestureDetector

    private var currentConfigMode = FomoMode.KILL_ALL
    private var isBlockAction = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, getString(R.string.toast_permission_denied), Toast.LENGTH_LONG).show()
            findViewById<MaterialSwitch>(R.id.switchRedisplay)?.isChecked = false
            AppState.reDisplayNotifications = false
        }
    }

    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.init(applicationContext)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupSettingsPanel()
        setupKeywordsPanel()
        setupGestureDetector()
        
        AppState.openCount++
        if (AppState.openCount % 6 == 0 && !isIgnoringBatteryOptimizations()) {
            showBatteryTipSheet()
        }
        
        setupButtons()
        updateUI()
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null && e2 != null) {
                    val diffY = e2.y - e1.y
                    // Swipe UP -> Settings (Bottom to Up)
                    if (diffY < -100 && Math.abs(velocityY) > 100) {
                        settingsBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        return true
                    }
                    // Swipe DOWN -> Keywords (Top to Down)
                    if (diffY > 100 && Math.abs(velocityY) > 100) {
                        keywordsBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun setupKeywordsPanel() {
        val keywordsPanel = findViewById<View>(R.id.keywordsPanel)
        keywordsBehavior = BottomSheetBehavior.from(keywordsPanel)
        
        // Inversion : La poignée est en bas, le panneau glisse du haut vers le bas.
        // On simule cela en cachant le panneau en haut par défaut.
        keywordsBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        val toggleMode = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroupKeywordMode)
        val toggleAction = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroupKeywordAction)
        val editKeyword = findViewById<TextInputEditText>(R.id.editKeyword)
        val chipGroup = findViewById<ChipGroup>(R.id.keywordChipGroup)
        val inputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.inputKeywordLayout)

        fun refreshChips() {
            chipGroup.removeAllViews()
            val keywords = AppState.getKeywords(currentConfigMode, isBlockAction)
            keywords.forEach { word ->
                val chip = Chip(this).apply {
                    text = word
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        val updated = AppState.getKeywords(currentConfigMode, isBlockAction).toMutableSet()
                        updated.remove(word)
                        AppState.setKeywords(currentConfigMode, isBlockAction, updated)
                        refreshChips()
                        FomoNotificationService.instance?.applyCurrentMode()
                    }
                }
                chipGroup.addView(chip)
            }
        }

        toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentConfigMode = if (checkedId == R.id.btnConfigKillAll) FomoMode.KILL_ALL else FomoMode.VIP_ONLY
                refreshChips()
            }
        }

        toggleAction.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isBlockAction = checkedId == R.id.btnActionBlock
                refreshChips()
            }
        }

        inputLayout.setEndIconOnClickListener {
            val word = editKeyword.text?.toString()?.trim()
            if (!word.isNullOrEmpty()) {
                val updated = AppState.getKeywords(currentConfigMode, isBlockAction).toMutableSet()
                updated.add(word)
                AppState.setKeywords(currentConfigMode, isBlockAction, updated)
                editKeyword.setText("")
                refreshChips()
                FomoNotificationService.instance?.applyCurrentMode()
            }
        }

        refreshChips()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Le geste ne fonctionne que si aucune autre modale n'est ouverte
        // (BottomSheetDialog n'expose pas facilement son état, mais on peut vérifier si settings est caché)
        if (settingsBehavior.state == BottomSheetBehavior.STATE_HIDDEN || settingsBehavior.state == BottomSheetBehavior.STATE_SETTLING) {
            gestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun setupSettingsPanel() {
        val bottomSheet = findViewById<View>(R.id.settingsBottomSheet)
        val scrim = findViewById<View>(R.id.settingsScrim)
        settingsBehavior = BottomSheetBehavior.from(bottomSheet)
        settingsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        
        settingsBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    scrim.visibility = View.GONE
                } else {
                    scrim.visibility = View.VISIBLE
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // slideOffset va de -1 (caché) à 1 (étendu). On veut 0 à 1.
                // Ici avec hideable=true et peek=0, 0 est "collapsé/caché" et 1 est étendu.
                val alpha = slideOffset.coerceIn(0f, 1f)
                scrim.alpha = alpha * 0.6f // Max 60% d'opacité
                scrim.visibility = if (alpha > 0) View.VISIBLE else View.GONE
            }
        })

        scrim.setOnClickListener {
            settingsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        val switchRedisplay = findViewById<MaterialSwitch>(R.id.switchRedisplay)
        switchRedisplay.isChecked = AppState.reDisplayNotifications
        switchRedisplay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            AppState.reDisplayNotifications = isChecked
        }

        // Configuration du mode de raccourci (Segmented Button)
        val prefs = getSharedPreferences("fomokiller_prefs", Context.MODE_PRIVATE)
        val toggleGroup = findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleGroupTileMode)
        val initialMode = prefs.getString("tile_target_mode", "KILL_ALL") ?: "KILL_ALL"
        
        if (initialMode == "KILL_ALL") {
            toggleGroup.check(R.id.btnTileKillAll)
        } else {
            toggleGroup.check(R.id.btnTileVipOnly)
        }

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val modeStr = if (checkedId == R.id.btnTileKillAll) "KILL_ALL" else "VIP_ONLY"
                prefs.edit().putString("tile_target_mode", modeStr).apply()
            }
        }

        loadAboutMarkdown()
    }

    private fun loadAboutMarkdown() {
        val textAbout = findViewById<TextView>(R.id.textAbout)
        try {
            // On choisit le fichier en fonction de la langue du système
            val language = java.util.Locale.getDefault().language
            val fileName = if (language == "fr") "about.md" else "about_en.md"
            
            val content = assets.open(fileName).bufferedReader().use { it.readText() }
            // Conversion simple Markdown vers HTML (on gère les liens [text](url))
            val htmlContent = content
                .replace(Regex("\\[(.*?)\\]\\((.*?)\\)"), "<a href=\"$2\">$1</a>")
                .replace("\n", "<br/>")
            
            textAbout.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(htmlContent)
            }
            textAbout.movementMethod = LinkMovementMethod.getInstance()
        } catch (e: Exception) {
            textAbout.text = "${getString(R.string.app_name)} v1.1"
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun showBatteryTipSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_battery_tip, null)
        dialog.setContentView(view)

        view.findViewById<View>(R.id.btnOpenBatterySettings).setOnClickListener {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:$packageName")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(fallbackIntent)
                } catch (e2: Exception) {
                    Toast.makeText(this, getString(R.string.error_msg, e2.message), Toast.LENGTH_LONG).show()
                }
            }
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btnIgnoreBattery).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(FomoNotificationService.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(serviceStateReceiver, filter)
        }
        
        // Si on revient dans l'app et que la permission n'est toujours pas là, 
        // on vérifie si on doit afficher l'aide pour les paramètres restreints
        if (!isNotificationListenerEnabled()) {
            checkRestrictedSettingsIfNeeded()
        }
        
        updateUI()
    }

    private fun checkRestrictedSettingsIfNeeded() {
        // Cette aide n'est pertinente que sur Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            try {
                // Check for OPSTR_BIND_NOTIFICATION_LISTENER_SERVICE equivalent
                // Or simply show the tip if the user just tried to enable it but failed.
                // We'll use a simple logic: if they are on Android 13+ and returning without permission.
                showRestrictedSettingsTip()
            } catch (e: Exception) {}
        }
    }

    private fun showRestrictedSettingsTip() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_restricted_settings, null)
        dialog.setContentView(view)

        // Support HTML tags for bold text
        val descView = view.findViewById<TextView>(R.id.restrictedSettingsDesc)
        descView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(getString(R.string.restricted_settings_desc), Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(getString(R.string.restricted_settings_desc))
        }

        view.findViewById<View>(R.id.btnOpenAppInfo).setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btnIgnoreRestricted).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(serviceStateReceiver) } catch (_: Exception) {}
    }

    private fun setupButtons() {
        binding.btnOff.setOnClickListener {
            updateMode(FomoMode.OFF)
        }
        binding.btnKillAll.setOnClickListener {
            updateMode(FomoMode.KILL_ALL)
        }
        binding.btnVipOnly.setOnClickListener {
            updateMode(FomoMode.VIP_ONLY)
        }
        binding.btnKillAll.setOnLongClickListener {
            showAppPickerSheet(mode = "blocked")
            true
        }
        binding.btnVipOnly.setOnLongClickListener {
            showAppPickerSheet(mode = "vip")
            true
        }
    }

    private fun updateMode(mode: FomoMode) {
        if (!isNotificationListenerEnabled()) {
            requestNotificationAccess()
            return
        }
        AppState.currentMode = mode
        
        val service = FomoNotificationService.instance
        if (service != null) {
            service.applyCurrentMode()
        } else {
            // Si le service n'est pas lié, on demande au système de le relancer
            val componentName = ComponentName(this, FomoNotificationService::class.java)
            NotificationListenerService.requestRebind(componentName)
            Toast.makeText(this, getString(R.string.toast_initializing), Toast.LENGTH_SHORT).show()
        }
        updateUI()
    }

    private fun showAppPickerSheet(mode: String) {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_app_picker, null)
        dialog.setContentView(sheetView)

        val title = sheetView.findViewById<TextView>(R.id.sheetTitle)
        val subtitle = sheetView.findViewById<TextView>(R.id.sheetSubtitle)
        val loading = sheetView.findViewById<ProgressBar>(R.id.sheetLoading)
        val recycler = sheetView.findViewById<RecyclerView>(R.id.sheetRecyclerView)
        val btnCancel = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.sheetBtnCancel)
        val btnConfirm = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.sheetBtnConfirm)

        title.text = if (mode == "blocked") getString(R.string.picker_title_blocked) else getString(R.string.picker_title_vip)
        subtitle.text = if (mode == "blocked") getString(R.string.picker_subtitle_blocked) else getString(R.string.picker_subtitle_vip)

        val selectedApps = mutableSetOf<String>().apply {
            addAll(if (mode == "blocked") AppState.blockedApps else AppState.vipApps)
        }

        val allApps = mutableListOf<AppInfo>()
        val adapter = AppSheetAdapter(allApps, selectedApps) { count ->
            btnConfirm.text = if (count > 0) getString(R.string.picker_btn_confirm_count, count) else getString(R.string.picker_btn_confirm)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        Thread {
            try {
                val pm = packageManager
                val installed = pm.getInstalledPackages(PackageManager.GET_META_DATA)
                val apps = installed
                    .filter { pkg ->
                        val isUser = (pkg.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
                        val hasLauncher = pm.getLaunchIntentForPackage(pkg.packageName) != null
                        (isUser || hasLauncher) && pkg.packageName != packageName
                    }
                    .map { pkg ->
                        AppInfo(
                            packageName = pkg.packageName,
                            label = pm.getApplicationLabel(pkg.applicationInfo).toString(),
                            icon = try { pm.getApplicationIcon(pkg.packageName) } catch (_: Exception) { null }
                        )
                    }
                    .sortedBy { it.label.lowercase() }

                runOnUiThread {
                    allApps.clear()
                    allApps.addAll(apps)
                    adapter.notifyDataSetChanged()
                    loading.visibility = View.GONE
                    recycler.visibility = View.VISIBLE
                    val count = selectedApps.size
                    btnConfirm.text = if (count > 0) getString(R.string.picker_btn_confirm_count, count) else getString(R.string.picker_btn_confirm)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loading.visibility = View.GONE
                    Toast.makeText(this, getString(R.string.error_msg, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }.start()

        btnCancel.text = getString(R.string.picker_btn_cancel)
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            if (mode == "blocked") AppState.blockedApps = selectedApps.toSet()
            else AppState.vipApps = selectedApps.toSet()
            
            // Réappliquer immédiatement
            val service = FomoNotificationService.instance
            if (service != null) {
                service.applyCurrentMode()
            } else {
                val componentName = ComponentName(this, FomoNotificationService::class.java)
                NotificationListenerService.requestRebind(componentName)
            }

            updateUI()
            dialog.dismiss()
        }

        dialog.show()
    }

    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: android.graphics.drawable.Drawable?
    )

    inner class AppSheetAdapter(
        private val apps: List<AppInfo>,
        private val selected: MutableSet<String>,
        private val onSelectionChanged: (Int) -> Unit
    ) : RecyclerView.Adapter<AppSheetAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.appIcon)
            val label: TextView = view.findViewById(R.id.appLabel)
            val pkg: TextView = view.findViewById(R.id.appPackage)
            val checkbox: CheckBox = view.findViewById(R.id.appCheckbox)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val app = apps[position]
            holder.label.text = app.label
            holder.pkg.text = app.packageName
            holder.icon.setImageDrawable(app.icon)
            holder.checkbox.isChecked = selected.contains(app.packageName)

            val toggle = View.OnClickListener {
                if (selected.contains(app.packageName)) selected.remove(app.packageName)
                else selected.add(app.packageName)
                holder.checkbox.isChecked = selected.contains(app.packageName)
                onSelectionChanged(selected.size)
            }
            holder.itemView.setOnClickListener(toggle)
            holder.checkbox.setOnClickListener(toggle)
        }

        override fun getItemCount() = apps.size
    }

    private fun updateUI() {
        val listenerEnabled = isNotificationListenerEnabled()
        val mode = AppState.currentMode
        binding.permissionBanner.visibility = if (!listenerEnabled) View.VISIBLE else View.GONE
        binding.btnOff.isSelected = (mode == FomoMode.OFF)
        binding.btnKillAll.isSelected = (mode == FomoMode.KILL_ALL)
        binding.btnVipOnly.isSelected = (mode == FomoMode.VIP_ONLY)

        val blockedCount = AppState.blockedApps.size
        val vipCount = AppState.vipApps.size
        
        // Mode ACTIVÉ (KILL_ALL) : Bloque uniquement la liste noire
        binding.labelKillAll.text = if (blockedCount > 0) {
            if (blockedCount == 1) getString(R.string.label_blocked_count_single)
            else getString(R.string.label_blocked_count_plural, blockedCount)
        } else {
            getString(R.string.label_kill_all_default)
        }
            
        // Mode PROTÉGÉ (VIP_ONLY) : Bloque tout sauf VIP
        binding.labelVipOnly.text = if (vipCount > 0) {
            if (vipCount == 1) getString(R.string.label_vip_count_single)
            else getString(R.string.label_vip_count_plural, vipCount)
        } else {
            getString(R.string.label_vip_only_default)
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun requestNotificationAccess() {
        Toast.makeText(this, getString(R.string.toast_request_access), Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
}