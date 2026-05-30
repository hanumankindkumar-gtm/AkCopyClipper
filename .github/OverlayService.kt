package com.akprojects.copyclipper.service

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.DisplayMetrics
import com.akprojects.copyclipper.data.FormPreset
import com.akprojects.copyclipper.data.PresetDatabase
import kotlinx.coroutines.*
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayLayout: FrameLayout? = null
    private lateinit var params: WindowManager.LayoutParams
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private var isExpanded = false
    
    // Touch dragging tracking variables
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var startTouchTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startNotificationForeground()
        setupFloatingOverlay()
    }

    private fun startNotificationForeground() {
        val channelId = "CopyClipperOverlay"
        val channelName = "Ak Copy Clipper Service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ak Copy Clipper Active")
            .setContentText("Tap the floating action bubble to fill form fields securely.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        startForeground(1337, notification)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupFloatingOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayLayout = FrameLayout(this)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(16)
            y = dpToPx(150)
        }

        showCompactBubble()
        windowManager.addView(overlayLayout, params)
    }

    private fun showCompactBubble() {
        if (overlayLayout == null) return
        overlayLayout?.removeAllViews()
        isExpanded = false

        // Update card parameters to mini circular dimension
        params.width = dpToPx(60)
        params.height = dpToPx(60)
        try {
            windowManager.updateViewLayout(overlayLayout, params)
        } catch (e: Exception) {}

        val bubble = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(dpToPx(56), dpToPx(56)).apply {
                gravity = Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4F46E5"))
                setStroke(dpToPx(2), Color.parseColor("#E0E7FF"))
            }
            elevation = dpToPx(8).toFloat()
        }

        val text = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            this.text = "✦"
            setTextColor(Color.WHITE)
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        bubble.addView(text)

        // Drag and click gesture listener implementation
        bubble.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startTouchTime = System.currentTimeMillis()
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        
                        if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                            isDragging = true
                        }
                        
                        if (isDragging) {
                            params.x = (initialX + deltaX).toInt()
                            params.y = (initialY + deltaY).toInt()
                            try {
                                windowManager.updateViewLayout(overlayLayout, params)
                            } catch (e: Exception) {}
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val duration = System.currentTimeMillis() - startTouchTime
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        
                        if (!isDragging && abs(deltaX) < 10 && abs(deltaY) < 10) {
                            if (duration > 1200) {
                                // Long press -> Closes the overlay safely
                                Toast.makeText(this@OverlayService, "Closing overlay service...", Toast.LENGTH_SHORT).show()
                                stopSelf()
                            } else {
                                // Single tap -> Toggle preset selection panel overlay menu
                                togglePresetMenu()
                            }
                        } else {
                            // Drag end: Edge snapping behavior implementation
                            snapToNearestHorizontalEdge()
                        }
                        return true
                    }
                }
                return false
            }
        })

        overlayLayout?.addView(bubble)
    }

    private fun snapToNearestHorizontalEdge() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val bubbleWidth = dpToPx(60)

        // Set target side based on midpoint coordinates
        val targetX = if (params.x + bubbleWidth / 2 < screenWidth / 2) {
            dpToPx(8) // Left offset snap
        } else {
            screenWidth - bubbleWidth - dpToPx(8) // Right offset snap
        }

        params.x = targetX
        try {
            windowManager.updateViewLayout(overlayLayout, params)
        } catch (e: Exception) {}
    }

    private fun togglePresetMenu() {
        if (overlayLayout == null) return
        overlayLayout?.removeAllViews()
        isExpanded = true

        // Resize layout params to hold the scrollable profile select card
        params.width = dpToPx(240)
        params.height = dpToPx(320)
        try {
            windowManager.updateViewLayout(overlayLayout, params)
        } catch (e: Exception) {}

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = dpToPx(16).toFloat()
                setStroke(dpToPx(2), Color.parseColor("#4F46E5"))
            }
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Header and Close button Row
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
        }

        val headerText = TextView(this).apply {
            text = "Presets Menu"
            setTextColor(Color.parseColor("#818CF8"))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(headerText)

        val closeBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setOnClickListener { showCompactBubble() }
            layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24))
        }
        headerRow.addView(closeBtn)
        container.addView(headerRow)

        // Scrollable List container for SQLite options
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        val itemsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        serviceScope.launch {
            val db = PresetDatabase.getDatabase(this@OverlayService)
            val presetsList = db.presetDao().getAllPresets()
            
            if (presetsList.isEmpty()) {
                val emptyTv = TextView(this@OverlayService).apply {
                    text = "No profiles found in SQLite! Please open MainActivity to configure."
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 10f
                    setPadding(0, dpToPx(16), 0, dpToPx(16))
                    gravity = Gravity.CENTER
                }
                itemsContainer.addView(emptyTv)
            } else {
                presetsList.forEach { preset ->
                    val row = LinearLayout(this@OverlayService).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                        background = GradientDrawable().apply {
                            setColor(Color.parseColor("#0F172A"))
                            cornerRadius = dpToPx(8).toFloat()
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dpToPx(6)
                        }
                        isClickable = true
                        setOnClickListener {
                            performProfileInputExecution(preset)
                            showCompactBubble()
                        }
                    }

                    val colorDot = View(this@OverlayService).apply {
                        layoutParams = LinearLayout.LayoutParams(dpToPx(10), dpToPx(10)).apply {
                            gravity = Gravity.CENTER_VERTICAL
                            rightMargin = dpToPx(8)
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            val colorHex = when(preset.colorTheme) {
                                "indigo" -> "#6366F1"
                                "emerald" -> "#10B981"
                                "amber" -> "#F59E0B"
                                "rose" -> "#F43F5E"
                                "sky" -> "#0EA5E9"
                                else -> "#64748B"
                            }
                            setColor(Color.parseColor(colorHex))
                        }
                    }
                    row.addView(colorDot)

                    val label = TextView(this@OverlayService).apply {
                        text = preset.presetName
                        setTextColor(Color.WHITE)
                        textSize = 11f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                    row.addView(label)
                    itemsContainer.addView(row)
                }
            }
        }

        scrollView.addView(itemsContainer)
        container.addView(scrollView)
        overlayLayout?.addView(container)
    }

    private fun performProfileInputExecution(preset: FormPreset) {
        val accessibility = AutofillAccessibilityService.sharedService
        if (accessibility != null) {
            // Emulate sequence input starting with full legal name via Accessibility
            accessibility.performKeystrokeEmulation(preset.personal.fullName)
            Toast.makeText(this, "Emulating typing keystrokes: [${preset.personal.fullName}]", Toast.LENGTH_SHORT).show()
        } else {
            // Fallback clipboard write copy
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("AkCopyClipper_Export", preset.personal.fullName)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied legal name: [${preset.personal.fullName}] - Enable Accessibility for direct automation!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (overlayLayout != null) {
            try {
                windowManager.removeView(overlayLayout)
            } catch (e: Exception) {}
        }
    }
}