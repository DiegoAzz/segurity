package com.norton.parental.companion;

/**
 * Formal enforcement states for FamilyGuard / AppzSegurity.
 * Using primitive int constants for maximum runtime performance and D8 compatibility.
 */
public final class EnforcementState {
    public static final int STATE_UNLOCKED = 0;
    public static final int STATE_APP_BLOCKED = 1;
    public static final int STATE_FULL_DEVICE_LOCKED = 2;
    public static final int STATE_WEB_BLOCKED = 3;

    private EnforcementState() {}

    public static String name(int state) {
        switch (state) {
            case STATE_UNLOCKED: return "STATE_UNLOCKED";
            case STATE_APP_BLOCKED: return "STATE_APP_BLOCKED";
            case STATE_FULL_DEVICE_LOCKED: return "STATE_FULL_DEVICE_LOCKED";
            case STATE_WEB_BLOCKED: return "STATE_WEB_BLOCKED";
            default: return "UNKNOWN(" + state + ")";
        }
    }
}
