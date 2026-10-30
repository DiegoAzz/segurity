package com.familyguard.child.child_flutter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.familyguard.child.child_flutter.engine.PolicyEngine
import com.familyguard.child.child_flutter.ui.EnforcementOverlayActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            val policy = PolicyEngine.getInstance(context).getActivePolicy()

            if (policy.instantLockActive || policy.bedtimeActive) {
                val lockIntent = Intent(context, EnforcementOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("block_reason", if (policy.instantLockActive) "INSTANT_LOCK_ACTIVE" else "BEDTIME_SCHEDULE_ACTIVE")
                    putExtra("policy_version", policy.version)
                }
                context.startActivity(lockIntent)
            }
        }
    }
}
