package com.norton.parental.companion;

/**
 * Immutable decision evaluated by the PolicyEngine for a specific target package.
 */
public final class PolicyDecision {

    public static final class BlockReason {
        public static final String EMERGENCY_WHITELIST = "EMERGENCY_WHITELIST";
        public static final String SYSTEM_ESSENTIAL = "SYSTEM_ESSENTIAL";
        public static final String FULL_DEVICE_LOCK = "FULL_DEVICE_LOCK";
        public static final String SCHEDULE_BEDTIME = "SCHEDULE_BEDTIME";
        public static final String SCHEDULE_SCHOOL = "SCHEDULE_SCHOOL";
        public static final String DAILY_LIMIT_EXHAUSTED = "DAILY_LIMIT_EXHAUSTED";
        public static final String MANUAL_APP_BLOCK = "MANUAL_APP_BLOCK";
        public static final String WEB_SAFETY_RESTRICTION = "WEB_SAFETY_RESTRICTION";
        public static final String ALLOWED = "ALLOWED";

        private BlockReason() {}
    }

    private final String packageName;
    private final boolean shouldBlock;
    private final String reason;
    private final String userDisplayMessage;
    private final long policyVersion;
    private final long evaluatedAt;

    public PolicyDecision(String packageName,
                          boolean shouldBlock,
                          String reason,
                          String userDisplayMessage,
                          long policyVersion,
                          long evaluatedAt) {
        this.packageName = packageName != null ? packageName : "";
        this.shouldBlock = shouldBlock;
        this.reason = reason != null ? reason : BlockReason.ALLOWED;
        this.userDisplayMessage = userDisplayMessage != null ? userDisplayMessage : "";
        this.policyVersion = policyVersion;
        this.evaluatedAt = evaluatedAt;
    }

    public static PolicyDecision allow(String packageName, String reason, long version) {
        return new PolicyDecision(packageName, false, reason, "", version, System.currentTimeMillis());
    }

    public static PolicyDecision block(String packageName, String reason, String message, long version) {
        return new PolicyDecision(packageName, true, reason, message, version, System.currentTimeMillis());
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean shouldBlock() {
        return shouldBlock;
    }

    public String getReason() {
        return reason;
    }

    public String getUserDisplayMessage() {
        return userDisplayMessage;
    }

    public long getPolicyVersion() {
        return policyVersion;
    }

    public long getEvaluatedAt() {
        return evaluatedAt;
    }

    @Override
    public String toString() {
        return "PolicyDecision{" +
                "pkg='" + packageName + '\'' +
                ", block=" + shouldBlock +
                ", reason=" + reason +
                ", v=" + policyVersion +
                '}';
    }
}
