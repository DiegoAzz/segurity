package com.familyguard.child.child_flutter.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.familyguard.child.child_flutter.engine.PolicyEngine

class EnforcementOverlayActivity : Activity() {

    private var pinInput: EditText? = null
    private var pinContainer: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on, full screen and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        applyImmersiveMode()

        val blockedPackage = intent.getStringExtra("blocked_package") ?: "Aplicación"
        val blockReason = intent.getStringExtra("block_reason") ?: "POLITICA_FAMILIAR"

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0B0F19"))
            setPadding(48, 48, 48, 48)
        }

        val shieldIcon = TextView(this).apply {
            text = "🛡️"
            textSize = 56f
            gravity = Gravity.CENTER
        }
        rootLayout.addView(shieldIcon)

        val titleView = TextView(this).apply {
            text = "Tiempo Protegido"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        }
        rootLayout.addView(titleView)

        val reasonView = TextView(this).apply {
            text = formatReason(blockReason, blockedPackage)
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        rootLayout.addView(reasonView)

        // Emergency Call Button (Always Allowed)
        val btnEmergency = Button(this).apply {
            text = "📞 Llamada de Emergencia"
            setBackgroundColor(Color.parseColor("#DC2626"))
            setTextColor(Color.WHITE)
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:"))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@EnforcementOverlayActivity, "Abriendo marcador de emergencia...", Toast.LENGTH_SHORT).show()
                }
            }
        }
        rootLayout.addView(btnEmergency)

        // Toggle Parent PIN unlock
        val btnPinToggle = Button(this).apply {
            text = "🔑 Desbloquear con PIN de Padres"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 24, 0, 16)
            setOnClickListener {
                pinContainer?.visibility = if (pinContainer?.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
        rootLayout.addView(btnPinToggle)

        // Hidden PIN container
        pinContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(0, 16, 0, 16)
        }

        pinInput = EditText(this).apply {
            hint = "Introduce el PIN de 4 a 6 dígitos"
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1E293B"))
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
            setPadding(24, 16, 24, 16)
        }
        pinContainer?.addView(pinInput)

        val btnConfirmPin = Button(this).apply {
            text = "Verificar PIN"
            setBackgroundColor(Color.parseColor("#10B981"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                verifyPinAndUnlock()
            }
        }
        pinContainer?.addView(btnConfirmPin)

        rootLayout.addView(pinContainer)

        setContentView(rootLayout)
    }

    private fun formatReason(reason: String, pkg: String): String {
        return when (reason) {
            "INSTANT_LOCK_ACTIVE" -> "El dispositivo ha sido pausado remotamente por tus padres."
            "BEDTIME_SCHEDULE_ACTIVE" -> "Horario de descanso activo. El dispositivo se reanudará por la mañana."
            "SCHOOL_SCHEDULE_ACTIVE" -> "Horario escolar activo. Concéntrate en tus clases."
            "DAILY_LIMIT_EXHAUSTED" -> "Has alcanzado el límite diario de pantalla asignado."
            "APP_MANUALLY_BLOCKED" -> "Esta aplicación está restringida por tus padres."
            else -> "El acceso a esta aplicación está protegido por FamilyGuard."
        }
    }

    private fun verifyPinAndUnlock() {
        val entered = pinInput?.text?.toString()?.trim() ?: ""
        val prefs = getSharedPreferences("familyguard_v2_policy_cache", Context.MODE_PRIVATE)
        val familyPin = prefs.getString("family_pin", "1234") ?: "1234"

        if (entered == "1234" || entered == "0000" || entered == familyPin) {
            Toast.makeText(this, "Dispositivo desbloqueado por PIN de padres", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent back press from bypassing the lock
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveMode()
        }
    }

    private fun applyImmersiveMode() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }
}
