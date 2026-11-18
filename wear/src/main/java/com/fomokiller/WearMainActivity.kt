package com.fomokiller

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fomokiller.databinding.ActivityMainBinding
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

enum class FomoMode {
    OFF,
    KILL_ALL,
    VIP_ONLY
}

class WearMainActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener, DataClient.OnDataChangedListener {

    private lateinit var binding: ActivityMainBinding
    private var activeMode: FomoMode = FomoMode.OFF
    private var focusedIndex: Int = 0 // 0 = OFF, 1 = KILL_ALL, 2 = VIP_ONLY
    private var rotaryAccumulator: Float = 0f

    private val prefs by lazy {
        getSharedPreferences("fomo_wear_prefs", Context.MODE_PRIVATE)
    }

    private val cardViews by lazy {
        listOf(binding.cardOff, binding.cardKillAll, binding.cardVipOnly)
    }

    private val cardTitles by lazy {
        listOf(binding.titleOff, binding.titleKillAll, binding.titleVipOnly)
    }

    private val modeDescriptions by lazy {
        listOf(
            getString(R.string.desc_off),
            getString(R.string.desc_kill_all),
            getString(R.string.desc_vip_only)
        )
    }

    private val modeUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val modeStr = intent?.getStringExtra("mode") ?: return
            try {
                val mode = FomoMode.valueOf(modeStr)
                updateModeFromRemote(mode)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Charger le dernier mode connu en cache local
        val savedModeStr = prefs.getString("active_mode", FomoMode.OFF.name) ?: FomoMode.OFF.name
        activeMode = try { FomoMode.valueOf(savedModeStr) } catch (_: Exception) { FomoMode.OFF }
        focusedIndex = when (activeMode) {
            FomoMode.OFF -> 0
            FomoMode.KILL_ALL -> 1
            FomoMode.VIP_ONLY -> 2
        }

        setupRotaryScroll()
        setupCardClickListeners()
        setupBackgroundResetHook()

        updateCardPositions(animate = false)
        updateCardStyles()
        updateSubtext()

        // Récupérer l'état actuel depuis DataClient et envoyer une requête
        queryExistingDataItems()
        requestModeFromPhone()
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
        Wearable.getDataClient(this).addListener(this)

        val filter = IntentFilter(WearMessageListenerService.ACTION_WEAR_MODE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(modeUpdateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(modeUpdateReceiver, filter)
        }

        queryExistingDataItems()
        requestModeFromPhone()
    }

    override fun onPause() {
        super.onPause()
        try { Wearable.getMessageClient(this).removeListener(this) } catch (_: Exception) {}
        try { Wearable.getDataClient(this).removeListener(this) } catch (_: Exception) {}
        try { unregisterReceiver(modeUpdateReceiver) } catch (_: Exception) {}
    }

    private fun setupRotaryScroll() {
        binding.rootContainer.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()

            setOnGenericMotionListener { _, event ->
                if (event.action == MotionEvent.ACTION_SCROLL) {
                    val rawDelta = -event.getAxisValue(MotionEvent.AXIS_SCROLL)
                    val delta = if (rawDelta != 0f) rawDelta else -event.getAxisValue(MotionEvent.AXIS_VSCROLL)

                    if (delta != 0f) {
                        // 2 crans pour déplacer d'un bouton
                        val step = if (delta > 0f) 1f else -1f
                        rotaryAccumulator += step

                        if (rotaryAccumulator >= 2f) {
                            rotaryAccumulator = 0f
                            if (focusedIndex < 2) {
                                focusedIndex++
                                performHapticTick()
                                onFocusedIndexChanged(animate = true)
                            }
                        } else if (rotaryAccumulator <= -2f) {
                            rotaryAccumulator = 0f
                            if (focusedIndex > 0) {
                                focusedIndex--
                                performHapticTick()
                                onFocusedIndexChanged(animate = true)
                            }
                        }
                        return@setOnGenericMotionListener true
                    }
                }
                false
            }
        }
    }

    private fun setupCardClickListeners() {
        binding.cardOff.setOnClickListener {
            handleCardClick(0, FomoMode.OFF, binding.cardOff)
        }
        binding.cardKillAll.setOnClickListener {
            handleCardClick(1, FomoMode.KILL_ALL, binding.cardKillAll)
        }
        binding.cardVipOnly.setOnClickListener {
            handleCardClick(2, FomoMode.VIP_ONLY, binding.cardVipOnly)
        }
    }

    private fun handleCardClick(index: Int, mode: FomoMode, targetView: View) {
        // Déplacer au centre si pas encore centré
        if (focusedIndex != index) {
            focusedIndex = index
            onFocusedIndexChanged(animate = true)
        }

        // Animation discrète d'appui (sans le gros rond gris)
        targetView.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(70)
            .withEndAction {
                targetView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90).start()
            }
            .start()

        // Changement de mode
        activeMode = mode
        prefs.edit().putString("active_mode", mode.name).apply()
        updateCardStyles()
        performHapticConfirm()
        sendModeToPhone(mode)
    }

    private fun setupBackgroundResetHook() {
        val resetListener = View.OnLongClickListener {
            triggerAppReset()
            true
        }

        // Associer le listener sur tous les éléments de fond
        binding.rootContainer.setOnLongClickListener(resetListener)
        binding.wheelContainer.setOnLongClickListener(resetListener)
        binding.subtextScrim.setOnLongClickListener(resetListener)
    }

    private fun triggerAppReset() {
        performHapticReset()
        Toast.makeText(this, "Réinitialisation...", Toast.LENGTH_SHORT).show()

        rotaryAccumulator = 0f
        requestModeFromPhone()

        // Redémarrer l'activité proprement
        binding.rootContainer.postDelayed({
            val intent = Intent(this, WearMainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            finish()
            overridePendingTransition(0, 0)
            startActivity(intent)
        }, 300)
    }

    private fun onFocusedIndexChanged(animate: Boolean) {
        updateCardPositions(animate = animate)
        updateSubtext()
    }

    private fun updateSubtext() {
        binding.textSubtext.text = modeDescriptions[focusedIndex]
    }

    private fun updateCardPositions(animate: Boolean) {
        val slotDistance = dpToPx(82f)

        for (i in cardViews.indices) {
            val card = cardViews[i]
            val slotOffset = i - focusedIndex

            val targetY = slotOffset * slotDistance
            val targetScale = if (slotOffset == 0) 1.0f else 0.88f
            val targetAlpha = when (slotOffset) {
                0 -> 1.0f
                -1, 1 -> 0.70f
                else -> 0.0f
            }

            if (animate) {
                val animY = ObjectAnimator.ofFloat(card, View.TRANSLATION_Y, targetY)
                val animScaleX = ObjectAnimator.ofFloat(card, View.SCALE_X, targetScale)
                val animScaleY = ObjectAnimator.ofFloat(card, View.SCALE_Y, targetScale)
                val animAlpha = ObjectAnimator.ofFloat(card, View.ALPHA, targetAlpha)

                AnimatorSet().apply {
                    playTogether(animY, animScaleX, animScaleY, animAlpha)
                    duration = 220
                    interpolator = DecelerateInterpolator()
                    start()
                }
            } else {
                card.translationY = targetY
                card.scaleX = targetScale
                card.scaleY = targetScale
                card.alpha = targetAlpha
            }
        }
    }

    private fun updateCardStyles() {
        val modes = listOf(FomoMode.OFF, FomoMode.KILL_ALL, FomoMode.VIP_ONLY)

        for (i in cardViews.indices) {
            val card = cardViews[i]
            val title = cardTitles[i]
            val mode = modes[i]

            val isActive = (mode == activeMode)
            if (isActive) {
                card.background = ContextCompat.getDrawable(this, R.drawable.bg_card_active)
                title.setTextColor(ContextCompat.getColor(this, R.color.card_title_active))
            } else {
                card.background = ContextCompat.getDrawable(this, R.drawable.bg_card_inactive)
                title.setTextColor(ContextCompat.getColor(this, R.color.card_title_inactive))
            }
        }
    }

    private fun updateModeFromRemote(mode: FomoMode) {
        runOnUiThread {
            if (activeMode != mode) {
                activeMode = mode
                prefs.edit().putString("active_mode", mode.name).apply()
                updateCardStyles()
            }
        }
    }

    private fun performHapticTick() {
        binding.rootContainer.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun performHapticConfirm() {
        val vibrator = getVibrator()
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        }
    }

    private fun performHapticReset() {
        val vibrator = getVibrator()
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Double vibration marquée pour la réinitialisation
                val timings = longArrayOf(0, 90, 60, 130)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(220)
            }
        }
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun sendModeToPhone(mode: FomoMode) {
        val payload = mode.name.toByteArray(Charsets.UTF_8)

        // 1. Synchronisation via DataClient
        try {
            val putDataMapReq = PutDataMapRequest.create("/fomo_request_mode").apply {
                dataMap.putString("mode", mode.name)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }
            val request = putDataMapReq.asPutDataRequest().setUrgent()
            Wearable.getDataClient(this).putDataItem(request)
        } catch (e: Exception) {
            Log.e("WearMainActivity", "DataClient send error: ${e.message}")
        }

        // 2. Envoi direct via MessageClient
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                for (node in nodes) {
                    Wearable.getMessageClient(this)
                        .sendMessage(node.id, "/set_mode", payload)
                        .addOnFailureListener { e ->
                            Log.e("WearMainActivity", "Error sending mode to phone: ${e.message}")
                        }
                }
            }
    }

    private fun requestModeFromPhone() {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                for (node in nodes) {
                    Wearable.getMessageClient(this)
                        .sendMessage(node.id, "/request_mode", ByteArray(0))
                }
            }
    }

    private fun queryExistingDataItems() {
        try {
            Wearable.getDataClient(this).getDataItems(Uri.parse("wear://*/fomo_mode"))
                .addOnSuccessListener { buffer ->
                    for (item in buffer) {
                        try {
                            val dataMap = DataMapItem.fromDataItem(item).dataMap
                            val modeStr = dataMap.getString("mode")
                            if (modeStr != null) {
                                val mode = FomoMode.valueOf(modeStr)
                                updateModeFromRemote(mode)
                            }
                        } catch (_: Exception) {}
                    }
                    buffer.release()
                }
        } catch (_: Exception) {}
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/mode_updated") {
            val modeStr = String(messageEvent.data, Charsets.UTF_8)
            try {
                val mode = FomoMode.valueOf(modeStr)
                updateModeFromRemote(mode)
            } catch (e: Exception) {
                Log.e("WearMainActivity", "Error parsing mode: ${e.message}")
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/fomo_mode") {
                try {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val modeStr = dataMap.getString("mode")
                    if (modeStr != null) {
                        val mode = FomoMode.valueOf(modeStr)
                        updateModeFromRemote(mode)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }
}
