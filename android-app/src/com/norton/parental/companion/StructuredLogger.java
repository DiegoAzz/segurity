package com.norton.parental.companion;

import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Structured diagnostic logger for FamilyGuard / AppzSegurity Policy Enforcement.
 * Tracks monotonic timestamps, ISO dates, anonymized device ID, policy version,
 * package name, reason, previous/next states, and execution results.
 */
public final class StructuredLogger {

    private static final String TAG = "FamilyGuard-Engine";
    private static final int MAX_BUFFER_SIZE = 200;
    private static final LinkedList<LogEntry> sRingBuffer = new LinkedList<>();

    private static String sAnonymizedDeviceId = null;

    public static final class EventSource {
        public static final String POLL_SYNC = "POLL_SYNC";
        public static final String ACCESSIBILITY_WINDOW_CHANGE = "ACCESSIBILITY_WINDOW_CHANGE";
        public static final String ENFORCEMENT_CONTROLLER = "ENFORCEMENT_CONTROLLER";
        public static final String USER_PIN_GATE = "USER_PIN_GATE";
        public static final String LOCAL_TIMER = "LOCAL_TIMER";
        public static final String BOOT_EVENT = "BOOT_EVENT";
        public static final String MANUAL_TEST = "MANUAL_TEST";

        private EventSource() {}
    }

    public static final class ApplyResult {
        public static final String APPLIED = "APPLIED";
        public static final String SKIPPED_DUPLICATE = "SKIPPED_DUPLICATE";
        public static final String DISCARDED_STALE_VERSION = "DISCARDED_STALE_VERSION";
        public static final String ERROR = "ERROR";

        private ApplyResult() {}
    }

    public static final class LogEntry {
        public final long monotonicTimeMs;
        public final String isoTimestamp;
        public final String deviceId;
        public final long policyVersion;
        public final String commandId;
        public final String targetPackage;
        public final String reason;
        public final String prevState;
        public final String newState;
        public final String source;
        public final String result;
        public final String details;

        public LogEntry(long monotonicTimeMs,
                        String isoTimestamp,
                        String deviceId,
                        long policyVersion,
                        String commandId,
                        String targetPackage,
                        String reason,
                        String prevState,
                        String newState,
                        String source,
                        String result,
                        String details) {
            this.monotonicTimeMs = monotonicTimeMs;
            this.isoTimestamp = isoTimestamp;
            this.deviceId = deviceId;
            this.policyVersion = policyVersion;
            this.commandId = commandId;
            this.targetPackage = targetPackage;
            this.reason = reason;
            this.prevState = prevState;
            this.newState = newState;
            this.source = source != null ? source : "UNKNOWN";
            this.result = result != null ? result : ApplyResult.APPLIED;
            this.details = details != null ? details : "";
        }

        @Override
        public String toString() {
            return "[" + isoTimestamp + " | mono=" + monotonicTimeMs + "ms] " +
                    "dev=" + deviceId + " | v=" + policyVersion + " | src=" + source + " | res=" + result +
                    " | pkg=" + targetPackage + " | reason=" + reason +
                    " | (" + prevState + " -> " + newState + ") " + details;
        }
    }

    private StructuredLogger() {
    }

    public static synchronized String getAnonymizedDeviceId() {
        if (sAnonymizedDeviceId == null) {
            String raw = Build.MANUFACTURER + "_" + Build.MODEL;
            int hash = Math.abs(raw.hashCode());
            sAnonymizedDeviceId = "dev_" + Integer.toHexString(hash);
        }
        return sAnonymizedDeviceId;
    }

    public static void logTransition(String source,
                                      long policyVersion,
                                      String targetPackage,
                                      String reason,
                                      String prevState,
                                      String newState,
                                      String result,
                                      String details) {
        final long mono = SystemClock.elapsedRealtime();
        final String iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date());
        final String devId = getAnonymizedDeviceId();
        final String cmdId = "cmd_" + mono;

        LogEntry entry = new LogEntry(
                mono,
                iso,
                devId,
                policyVersion,
                cmdId,
                targetPackage != null ? targetPackage : "-",
                reason != null ? reason : "-",
                prevState != null ? prevState : "-",
                newState != null ? newState : "-",
                source,
                result,
                details != null ? details : ""
        );

        synchronized (sRingBuffer) {
            if (sRingBuffer.size() >= MAX_BUFFER_SIZE) {
                sRingBuffer.removeFirst();
            }
            sRingBuffer.addLast(entry);
        }

        // Emit to Android Logcat
        if (ApplyResult.ERROR.equals(result)) {
            Log.e(TAG, entry.toString());
        } else if (ApplyResult.APPLIED.equals(result)) {
            Log.i(TAG, entry.toString());
        } else {
            Log.d(TAG, entry.toString());
        }
    }

    public static List<LogEntry> getRecentLogs() {
        synchronized (sRingBuffer) {
            return new ArrayList<>(sRingBuffer);
        }
    }
}
