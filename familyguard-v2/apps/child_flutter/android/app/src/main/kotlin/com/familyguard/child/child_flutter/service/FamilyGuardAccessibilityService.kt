package com.familyguard.child.child_flutter.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.familyguard.child.child_flutter.engine.PolicyEngine
import com.familyguard.child.child_flutter.ui.EnforcementOverlayActivity

class FamilyGuardAccessibilityService : AccessibilityService() {

    private var lastEvaluatedPackage: String? = null
    private var lastBlockedState: Boolean = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // Ignore our own package to prevent self-locking
        if (packageName == this.packageName) {
            return
        }

        val policyEngine = PolicyEngine.getInstance(applicationContext)
        val decision = policyEngine.evaluateApp(packageName)

        // Deduplication: Only trigger transition if state changed or new blocked app opened
        if (decision.isBlocked) {
            if (packageName != lastEvaluatedPackage || !lastBlockedState) {
                lastEvaluatedPackage = packageName
                lastBlockedState = true

                val intent = Intent(this, EnforcementOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("blocked_package", packageName)
                    putExtra("block_reason", decision.reason.name)
                    putExtra("policy_version", decision.policyVersion)
                }
                startActivity(intent)
            }
        } else {
            lastEvaluatedPackage = packageName
            lastBlockedState = false
        }
    }

    override fun onInterrupt() {}
}
