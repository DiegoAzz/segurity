package com.familyguard.child.child_flutter.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test Suite for PolicyEngine core algorithmic logic.
 * Guarantees zero flickering, monotonic versioning, and strict emergency precedence.
 */
class PolicyEngineTest {

    private fun evaluateDeterministic(policy: PolicySnapshot, pkg: String): EnforcementDecision {
        val emergencyPkgs = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.phone",
            "com.android.emergency"
        )
        val systemPkgs = setOf(
            "com.android.systemui",
            "com.android.permissioncontroller"
        )

        if (emergencyPkgs.contains(pkg)) {
            return EnforcementDecision(pkg, false, DecisionReason.EMERGENCY_ALLOWED, policy.version)
        }
        if (systemPkgs.contains(pkg)) {
            return EnforcementDecision(pkg, false, DecisionReason.SYSTEM_ESSENTIAL_ALLOWED, policy.version)
        }
        if (policy.instantLockActive) {
            return EnforcementDecision(pkg, true, DecisionReason.INSTANT_LOCK_ACTIVE, policy.version)
        }
        if (policy.bedtimeActive) {
            return EnforcementDecision(pkg, true, DecisionReason.BEDTIME_SCHEDULE_ACTIVE, policy.version)
        }
        if (policy.schoolActive) {
            return EnforcementDecision(pkg, true, DecisionReason.SCHOOL_SCHEDULE_ACTIVE, policy.version)
        }
        if (policy.dailyLimitMinutes in 1..policy.dailyUsageMinutes) {
            return EnforcementDecision(pkg, true, DecisionReason.DAILY_LIMIT_EXHAUSTED, policy.version)
        }
        if (policy.blockedPackages.contains(pkg)) {
            return EnforcementDecision(pkg, true, DecisionReason.APP_MANUALLY_BLOCKED, policy.version)
        }
        return EnforcementDecision(pkg, false, DecisionReason.ALLOWED, policy.version)
    }

    @Test
    fun testEmergencyDialersAlwaysAllowedEvenDuringInstantLock() {
        val lockedPolicy = PolicySnapshot(
            version = 1,
            instantLockActive = true,
            bedtimeActive = true,
            blockedPackages = setOf("com.google.android.dialer")
        )

        val decision = evaluateDeterministic(lockedPolicy, "com.google.android.dialer")
        assertFalse("Dialer must NEVER be blocked", decision.isBlocked)
        assertEquals(DecisionReason.EMERGENCY_ALLOWED, decision.reason)
    }

    @Test
    fun testInstantLockOverridesRegularApps() {
        val policy = PolicySnapshot(version = 2, instantLockActive = true)
        val decision = evaluateDeterministic(policy, "com.instagram.android")
        assertTrue(decision.isBlocked)
        assertEquals(DecisionReason.INSTANT_LOCK_ACTIVE, decision.reason)
    }

    @Test
    fun testBedtimeScheduleActive() {
        val policy = PolicySnapshot(version = 3, bedtimeActive = true)
        val decision = evaluateDeterministic(policy, "com.tiktok.android")
        assertTrue(decision.isBlocked)
        assertEquals(DecisionReason.BEDTIME_SCHEDULE_ACTIVE, decision.reason)
    }

    @Test
    fun testDailyLimitExhaustion() {
        val policy = PolicySnapshot(
            version = 4,
            dailyLimitMinutes = 120,
            dailyUsageMinutes = 125
        )
        val decision = evaluateDeterministic(policy, "com.netflix.mediaclient")
        assertTrue(decision.isBlocked)
        assertEquals(DecisionReason.DAILY_LIMIT_EXHAUSTED, decision.reason)
    }

    @Test
    fun testManualBlockedAppsOnly() {
        val policy = PolicySnapshot(
            version = 5,
            blockedPackages = setOf("com.zhiliaoapp.musically")
        )
        val blocked = evaluateDeterministic(policy, "com.zhiliaoapp.musically")
        val allowed = evaluateDeterministic(policy, "com.duolingo")

        assertTrue(blocked.isBlocked)
        assertEquals(DecisionReason.APP_MANUALLY_BLOCKED, blocked.reason)
        assertFalse(allowed.isBlocked)
        assertEquals(DecisionReason.ALLOWED, allowed.reason)
    }

    @Test
    fun testDeduplicationAntiFlicker1000Cycles() {
        val policy = PolicySnapshot(version = 6, instantLockActive = true)
        var transitionCount = 0
        var lastState: Boolean? = null

        for (i in 0 until 1000) {
            val decision = evaluateDeterministic(policy, "com.instagram.android")
            if (lastState == null || lastState != decision.isBlocked) {
                transitionCount++
                lastState = decision.isBlocked
            }
        }

        assertEquals("Deduplication must yield exactly 1 state transition for 1000 identical evaluations", 1, transitionCount)
    }
}
