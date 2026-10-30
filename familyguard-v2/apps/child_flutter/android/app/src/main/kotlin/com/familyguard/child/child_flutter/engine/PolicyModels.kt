package com.familyguard.child.child_flutter.engine

/**
 * Immutable models for the FamilyGuard 2.0 Native Policy Engine.
 */

enum class DecisionReason {
    EMERGENCY_ALLOWED,
    SYSTEM_ESSENTIAL_ALLOWED,
    INSTANT_LOCK_ACTIVE,
    BEDTIME_SCHEDULE_ACTIVE,
    SCHOOL_SCHEDULE_ACTIVE,
    DAILY_LIMIT_EXHAUSTED,
    APP_MANUALLY_BLOCKED,
    APP_CATEGORY_BLOCKED,
    ALLOWED
}

data class PolicySnapshot(
    val version: Int,
    val instantLockActive: Boolean = false,
    val dailyLimitMinutes: Int = 120,
    val dailyUsageMinutes: Int = 0,
    val bedtimeActive: Boolean = false,
    val schoolActive: Boolean = false,
    val blockedPackages: Set<String> = emptySet(),
    val allowedPackages: Set<String> = emptySet()
)

data class EnforcementDecision(
    val packageName: String,
    val isBlocked: Boolean,
    val reason: DecisionReason,
    val policyVersion: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class PermissionDiagnosis(
    val hasAccessibilityPermission: Boolean,
    val hasUsageStatsPermission: Boolean,
    val hasOverlayPermission: Boolean,
    val hasNotificationPermission: Boolean,
    val isBatteryOptimizationIgnored: Boolean,
    val isDeviceAdminActive: Boolean
)
