package com.familyguard.child.child_flutter.engine

import android.content.Context
import android.content.SharedPreferences

/**
 * FamilyGuard 2.0 - PolicyEngine
 * 
 * Single authoritative decision engine for device and application enforcement.
 * Deterministic, monotonic versioning, zero flickering, and offline persistence.
 */
class PolicyEngine private constructor(context: Context) {

    companion object {
        @Volatile
        private var instance: PolicyEngine? = null

        fun getInstance(context: Context): PolicyEngine {
            return instance ?: synchronized(this) {
                instance ?: PolicyEngine(context.applicationContext).also { instance = it }
            }
        }

        private val EMERGENCY_PACKAGES = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.phone",
            "com.android.emergency"
        )

        private val SYSTEM_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller"
        )
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("familyguard_v2_policy_cache", Context.MODE_PRIVATE)

    @Volatile
    private var activePolicy: PolicySnapshot = loadPersistedPolicy()

    @Volatile
    private var lastDecisions: MutableMap<String, EnforcementDecision> = HashMap()

    @Synchronized
    fun updatePolicy(newPolicy: PolicySnapshot): Boolean {
        // Monotonic guard: Ignore duplicate or stale versions
        if (newPolicy.version <= activePolicy.version && activePolicy.version > 0) {
            return false
        }

        activePolicy = newPolicy
        persistPolicy(newPolicy)
        lastDecisions.clear()
        return true
    }

    fun getActivePolicy(): PolicySnapshot = activePolicy

    fun evaluateApp(packageName: String): EnforcementDecision {
        // Check cache for identical decision to eliminate redundant checks
        val cached = lastDecisions[packageName]
        if (cached != null && cached.policyVersion == activePolicy.version) {
            return cached
        }

        val decision = computeDecision(packageName)
        lastDecisions[packageName] = decision
        return decision
    }

    private fun computeDecision(pkg: String): EnforcementDecision {
        val policy = activePolicy

        // 1. Emergency Dialers always allowed
        if (EMERGENCY_PACKAGES.contains(pkg)) {
            return EnforcementDecision(pkg, false, DecisionReason.EMERGENCY_ALLOWED, policy.version)
        }

        // 2. System UI and critical permission controllers allowed
        if (SYSTEM_PACKAGES.contains(pkg)) {
            return EnforcementDecision(pkg, false, DecisionReason.SYSTEM_ESSENTIAL_ALLOWED, policy.version)
        }

        // 3. Instant Lock
        if (policy.instantLockActive) {
            return EnforcementDecision(pkg, true, DecisionReason.INSTANT_LOCK_ACTIVE, policy.version)
        }

        // 4. Bedtime Schedule
        if (policy.bedtimeActive) {
            return EnforcementDecision(pkg, true, DecisionReason.BEDTIME_SCHEDULE_ACTIVE, policy.version)
        }

        // 5. School Schedule
        if (policy.schoolActive) {
            return EnforcementDecision(pkg, true, DecisionReason.SCHOOL_SCHEDULE_ACTIVE, policy.version)
        }

        // 6. Daily Usage Limit
        if (policy.dailyLimitMinutes in 1..policy.dailyUsageMinutes) {
            return EnforcementDecision(pkg, true, DecisionReason.DAILY_LIMIT_EXHAUSTED, policy.version)
        }

        // 7. Manual Blocklist
        if (policy.blockedPackages.contains(pkg)) {
            return EnforcementDecision(pkg, true, DecisionReason.APP_MANUALLY_BLOCKED, policy.version)
        }

        return EnforcementDecision(pkg, false, DecisionReason.ALLOWED, policy.version)
    }

    private fun persistPolicy(policy: PolicySnapshot) {
        prefs.edit()
            .putInt("version", policy.version)
            .putBoolean("instantLockActive", policy.instantLockActive)
            .putInt("dailyLimitMinutes", policy.dailyLimitMinutes)
            .putInt("dailyUsageMinutes", policy.dailyUsageMinutes)
            .putBoolean("bedtimeActive", policy.bedtimeActive)
            .putBoolean("schoolActive", policy.schoolActive)
            .putStringSet("blockedPackages", policy.blockedPackages)
            .apply()
    }

    private fun loadPersistedPolicy(): PolicySnapshot {
        val version = prefs.getInt("version", 0)
        return PolicySnapshot(
            version = version,
            instantLockActive = prefs.getBoolean("instantLockActive", false),
            dailyLimitMinutes = prefs.getInt("dailyLimitMinutes", 120),
            dailyUsageMinutes = prefs.getInt("dailyUsageMinutes", 0),
            bedtimeActive = prefs.getBoolean("bedtimeActive", false),
            schoolActive = prefs.getBoolean("schoolActive", false),
            blockedPackages = prefs.getStringSet("blockedPackages", emptySet()) ?: emptySet()
        )
    }
}
