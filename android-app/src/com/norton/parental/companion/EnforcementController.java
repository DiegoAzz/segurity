package com.norton.parental.companion;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Single Authoritative Execution Coordinator for FamilyGuard / AppzSegurity.
 *
 * Implements:
 * - Deterministic Finite State Machine (UNLOCKED, APP_BLOCKED, FULL_DEVICE_LOCKED, WEB_BLOCKED)
 * - Serialized thread-safe execution queue
 * - Strict deduplication to eliminate flickering
 * - Anti-loop reentrancy guards
 * - Stale version rejection
 * - Anti-reboot bypass: persists and restores policy & lock state across device power cycles
 */
public final class EnforcementController {

    private static final String PREFS_POLICY = "norton_policy_cache";
    private static final String KEY_VERSION = "policy_version";
    private static final String KEY_INSTANT_LOCK = "instant_lock";
    private static final String KEY_BLOCKED_PACKAGES = "blocked_packages";
    private static final String KEY_DAILY_LIMIT = "daily_limit";
    private static final String KEY_USED_TODAY = "used_today";
    private static final String KEY_BEDTIME = "bedtime_active";
    private static final String KEY_SCHOOL = "school_active";

    private static EnforcementController sInstance = null;

    private final Context mAppContext;
    private final Handler mSerialHandler;

    // Estado activo
    private volatile PolicyModel mCurrentPolicy;
    private int mCurrentState = EnforcementState.STATE_UNLOCKED;
    private String mCurrentEnforcingPackage = "";
    private String mCurrentActiveForegroundPackage = "";
    private long mLastEnforcementTimeMs = 0;

    // Interface para delegar renderizado al servicio de superposición
    public interface OverlayHost {
        void onShowOverlay(String displayMessage, int state);
        void onHideOverlay();
        void onDismissToHome();
    }

    private OverlayHost mOverlayHost = null;

    public static synchronized void init(Context context) {
        if (sInstance == null) {
            sInstance = new EnforcementController(context.getApplicationContext());
        }
    }

    public static synchronized EnforcementController getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("EnforcementController must be initialized with init(context)");
        }
        return sInstance;
    }

    public static boolean isInitialized() {
        return sInstance != null;
    }

    private EnforcementController(Context context) {
        mAppContext = context;

        // Cargar política persistida desde disco (Anti-Bypass de reinicio)
        mCurrentPolicy = loadPolicyFromPrefs(context);

        HandlerThread thread = new HandlerThread("FamilyGuard-EnforcementQueue");
        thread.start();
        mSerialHandler = new Handler(thread.getLooper());

        // Si antes de reiniciar el celular estaba bloqueado, restaurar de inmediato el estado
        if (mCurrentPolicy.isInstantLockActive()) {
            mCurrentState = EnforcementState.STATE_FULL_DEVICE_LOCKED;
            applyEnforcementToUi("🚨 Dispositivo Bloqueado por Control Parental", mCurrentState);
        }
    }

    public synchronized void setOverlayHost(OverlayHost host) {
        mOverlayHost = host;
        // Si ya arrancamos bloqueados en frío, forzar el overlay en cuanto el host se conecte
        if (mCurrentState != EnforcementState.STATE_UNLOCKED && mOverlayHost != null) {
            mOverlayHost.onShowOverlay("🚨 Dispositivo Bloqueado por Control Parental", mCurrentState);
        }
    }

    public PolicyModel getCurrentPolicy() {
        return mCurrentPolicy;
    }

    public int getCurrentState() {
        return mCurrentState;
    }

    public String getCurrentEnforcingPackage() {
        return mCurrentEnforcingPackage;
    }

    // =========================================================================
    // PERSISTENCIA LOCAL (ANTI-BYPASS POR REINICIO O APAGADO)
    // =========================================================================

    private void savePolicyToPrefs(PolicyModel policy, int state) {
        if (mAppContext == null || policy == null) return;
        try {
            SharedPreferences prefs = mAppContext.getSharedPreferences(PREFS_POLICY, Context.MODE_PRIVATE);
            StringBuilder sb = new StringBuilder();
            if (policy.getBlockedPackages() != null) {
                for (String p : policy.getBlockedPackages()) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(p);
                }
            }
            boolean shouldLock = policy.isInstantLockActive() || state == EnforcementState.STATE_FULL_DEVICE_LOCKED;
            prefs.edit()
                    .putLong(KEY_VERSION, policy.getPolicyVersion())
                    .putBoolean(KEY_INSTANT_LOCK, shouldLock)
                    .putString(KEY_BLOCKED_PACKAGES, sb.toString())
                    .putInt(KEY_DAILY_LIMIT, policy.getDailyLimitMinutes())
                    .putInt(KEY_USED_TODAY, policy.getUsedTodayMinutes())
                    .putBoolean(KEY_BEDTIME, policy.isBedtimeActive())
                    .putBoolean(KEY_SCHOOL, policy.isSchoolTimeActive())
                    .apply();
        } catch (Throwable ignored) {}
    }

    private PolicyModel loadPolicyFromPrefs(Context context) {
        if (context == null) return PolicyModel.createDefault();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_POLICY, Context.MODE_PRIVATE);
            if (!prefs.contains(KEY_VERSION)) {
                return PolicyModel.createDefault();
            }
            long version = prefs.getLong(KEY_VERSION, 1L);
            boolean instantLock = prefs.getBoolean(KEY_INSTANT_LOCK, false);
            String rawPkgs = prefs.getString(KEY_BLOCKED_PACKAGES, "");
            Set<String> blocked = new HashSet<>();
            if (rawPkgs != null && !rawPkgs.isEmpty()) {
                String[] parts = rawPkgs.split(",");
                for (String p : parts) {
                    if (!p.trim().isEmpty()) blocked.add(p.trim());
                }
            }
            int dailyLimit = prefs.getInt(KEY_DAILY_LIMIT, 120);
            int usedToday = prefs.getInt(KEY_USED_TODAY, 0);
            boolean bedtime = prefs.getBoolean(KEY_BEDTIME, false);
            boolean school = prefs.getBoolean(KEY_SCHOOL, false);

            return new PolicyModel(
                    version,
                    instantLock,
                    blocked,
                    Collections.emptySet(),
                    Collections.emptySet(),
                    true,
                    dailyLimit,
                    usedToday,
                    bedtime,
                    school,
                    System.currentTimeMillis()
            );
        } catch (Throwable t) {
            return PolicyModel.createDefault();
        }
    }

    // =========================================================================
    // TAREAS SERIALIZADAS (RUNNABLES NOMBRADOS PARA COMPILACIÓN LIMPIA D8)
    // =========================================================================

    private static class UpdatePolicyTask implements Runnable {
        private final EnforcementController mController;
        private final PolicyModel mNewPolicy;
        private final String mSource;

        public UpdatePolicyTask(EnforcementController controller, PolicyModel newPolicy, String source) {
            mController = controller;
            mNewPolicy = newPolicy;
            mSource = source;
        }

        @Override
        public void run() {
            if (mController == null || mNewPolicy == null) return;

            if (mNewPolicy.getPolicyVersion() < mController.mCurrentPolicy.getPolicyVersion()) {
                StructuredLogger.logTransition(
                        mSource,
                        mNewPolicy.getPolicyVersion(),
                        "-",
                        "STALE_VERSION_REJECTED",
                        EnforcementState.name(mController.mCurrentState),
                        EnforcementState.name(mController.mCurrentState),
                        StructuredLogger.ApplyResult.DISCARDED_STALE_VERSION,
                        "Incoming v" + mNewPolicy.getPolicyVersion() + " < Current v" + mController.mCurrentPolicy.getPolicyVersion()
                );
                return;
            }

            final long prevVersion = mController.mCurrentPolicy.getPolicyVersion();
            mController.mCurrentPolicy = mNewPolicy;

            // Persistir inmediatamente a SharedPreferences para que sobreviva a reinicios
            mController.savePolicyToPrefs(mNewPolicy, mController.mCurrentState);

            StructuredLogger.logTransition(
                    mSource,
                    mNewPolicy.getPolicyVersion(),
                    "-",
                    "POLICY_UPDATED",
                    "v" + prevVersion,
                    "v" + mNewPolicy.getPolicyVersion(),
                    StructuredLogger.ApplyResult.APPLIED,
                    mNewPolicy.toString()
            );

            // Re-evaluar de inmediato sobre la aplicación activa
            mController.evaluateAndEnforce(mController.mCurrentActiveForegroundPackage, mSource);
        }
    }

    private static class ForegroundChangeTask implements Runnable {
        private final EnforcementController mController;
        private final String mPackageName;
        private final String mSource;

        public ForegroundChangeTask(EnforcementController controller, String packageName, String source) {
            mController = controller;
            mPackageName = packageName;
            mSource = source;
        }

        @Override
        public void run() {
            if (mController == null || mPackageName == null) return;
            mController.mCurrentActiveForegroundPackage = mPackageName;
            mController.evaluateAndEnforce(mPackageName, mSource);
        }
    }

    private static class WebViolationTask implements Runnable {
        private final EnforcementController mController;
        private final String mMatchedItem;
        private final String mReason;

        public WebViolationTask(EnforcementController controller, String matchedItem, String reason) {
            mController = controller;
            mMatchedItem = matchedItem;
            mReason = reason;
        }

        @Override
        public void run() {
            if (mController == null) return;
            final int prevState = mController.mCurrentState;

            if (mController.mCurrentState == EnforcementState.STATE_FULL_DEVICE_LOCKED) {
                // Pausa completa ya tapa todo, no degradar
                return;
            }

            mController.mCurrentState = EnforcementState.STATE_WEB_BLOCKED;
            mController.mCurrentEnforcingPackage = mMatchedItem;

            StructuredLogger.logTransition(
                    StructuredLogger.EventSource.ACCESSIBILITY_WINDOW_CHANGE,
                    mController.mCurrentPolicy.getPolicyVersion(),
                    mMatchedItem,
                    mReason,
                    EnforcementState.name(prevState),
                    EnforcementState.name(mController.mCurrentState),
                    StructuredLogger.ApplyResult.APPLIED,
                    "WebSafety filter triggered for: " + mMatchedItem
            );

            mController.applyEnforcementToUi("🚫 Sitio Web Restringido: " + mReason, mController.mCurrentState);
        }
    }

    private static class ParentPinUnlockTask implements Runnable {
        private final EnforcementController mController;

        public ParentPinUnlockTask(EnforcementController controller) {
            mController = controller;
        }

        @Override
        public void run() {
            if (mController == null) return;
            final int prevState = mController.mCurrentState;
            mController.mCurrentState = EnforcementState.STATE_UNLOCKED;
            mController.mCurrentEnforcingPackage = "";

            // Guardar desbloqueo en persistencia local
            mController.savePolicyToPrefs(mController.mCurrentPolicy, mController.mCurrentState);

            StructuredLogger.logTransition(
                    StructuredLogger.EventSource.USER_PIN_GATE,
                    mController.mCurrentPolicy.getPolicyVersion(),
                    "-",
                    "PARENT_PIN_AUTH",
                    EnforcementState.name(prevState),
                    EnforcementState.name(mController.mCurrentState),
                    StructuredLogger.ApplyResult.APPLIED,
                    "Parent entered valid authorization PIN"
            );

            if (mController.mOverlayHost != null) {
                mController.mOverlayHost.onHideOverlay();
            }
        }
    }

    private static class UiEnforcementRunnable implements Runnable {
        private final EnforcementController mController;
        private final String mDisplayMessage;
        private final int mState;

        public UiEnforcementRunnable(EnforcementController controller, String displayMessage, int state) {
            mController = controller;
            mDisplayMessage = displayMessage;
            mState = state;
        }

        @Override
        public void run() {
            if (mController == null) return;
            if (mController.mOverlayHost != null) {
                mController.mOverlayHost.onDismissToHome();
                mController.mOverlayHost.onShowOverlay(mDisplayMessage, mState);
            }

            try {
                Intent lockActIntent = new Intent(mController.mAppContext, LockActivity.class);
                lockActIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                lockActIntent.putExtra("blocked_reason", mDisplayMessage);
                mController.mAppContext.startActivity(lockActIntent);
            } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // MÉTODOS PÚBLICOS
    // =========================================================================

    public void updatePolicy(final PolicyModel newPolicy, final String source) {
        if (newPolicy == null) return;
        mSerialHandler.post(new UpdatePolicyTask(this, newPolicy, source));
    }

    public void onForegroundAppChanged(final String packageName, final String source) {
        if (packageName == null || packageName.isEmpty()) return;
        mSerialHandler.post(new ForegroundChangeTask(this, packageName, source));
    }

    public void onWebSafetyViolation(final String matchedItem, final String reason) {
        mSerialHandler.post(new WebViolationTask(this, matchedItem, reason));
    }

    public void onParentPinUnlock() {
        mSerialHandler.post(new ParentPinUnlockTask(this));
    }

    /**
     * Núcleo del evaluador de políticas y transiciones de estado.
     */
    private void evaluateAndEnforce(String targetPackage, String source) {
        final PolicyDecision decision = PolicyEngine.evaluate(targetPackage, mCurrentPolicy);
        final int prevState = mCurrentState;

        // 1. Si la decisión es BLOQUEAR
        if (decision.shouldBlock()) {
            final int targetState = (PolicyDecision.BlockReason.FULL_DEVICE_LOCK.equals(decision.getReason()))
                    ? EnforcementState.STATE_FULL_DEVICE_LOCKED
                    : EnforcementState.STATE_APP_BLOCKED;

            // DEDUPLICACIÓN ESTRICTA: Si ya estamos en el estado objetivo para este mismo paquete, descarta
            if (mCurrentState == targetState && mCurrentEnforcingPackage.equals(targetPackage)) {
                StructuredLogger.logTransition(
                        source,
                        decision.getPolicyVersion(),
                        targetPackage,
                        decision.getReason(),
                        EnforcementState.name(prevState),
                        EnforcementState.name(targetState),
                        StructuredLogger.ApplyResult.SKIPPED_DUPLICATE,
                        "Duplicate block enforcement suppressed (No-flicker)"
                );
                return;
            }

            mCurrentState = targetState;
            mCurrentEnforcingPackage = targetPackage != null ? targetPackage : "";
            mLastEnforcementTimeMs = System.currentTimeMillis();

            // Guardar estado en persistencia local
            savePolicyToPrefs(mCurrentPolicy, mCurrentState);

            StructuredLogger.logTransition(
                    source,
                    decision.getPolicyVersion(),
                    targetPackage,
                    decision.getReason(),
                    EnforcementState.name(prevState),
                    EnforcementState.name(mCurrentState),
                    StructuredLogger.ApplyResult.APPLIED,
                    decision.getUserDisplayMessage()
            );

            applyEnforcementToUi(decision.getUserDisplayMessage(), mCurrentState);

        } else {
            // 2. Si la decisión es PERMITIR (no debe bloquearse)
            // ¿Estábamos bloqueando una aplicación específica y el niño ya salió de ella?
            if (mCurrentState == EnforcementState.STATE_APP_BLOCKED || mCurrentState == EnforcementState.STATE_WEB_BLOCKED) {
                // El niño regresó al launcher o a una app permitida
                mCurrentState = EnforcementState.STATE_UNLOCKED;
                mCurrentEnforcingPackage = "";

                savePolicyToPrefs(mCurrentPolicy, mCurrentState);

                StructuredLogger.logTransition(
                        source,
                        decision.getPolicyVersion(),
                        targetPackage,
                        decision.getReason(),
                        EnforcementState.name(prevState),
                        EnforcementState.name(mCurrentState),
                        StructuredLogger.ApplyResult.APPLIED,
                        "Restricted app exited -> Transitioned to UNLOCKED"
                );

                if (mOverlayHost != null) {
                    mOverlayHost.onHideOverlay();
                }
            } else if (mCurrentState == EnforcementState.STATE_FULL_DEVICE_LOCKED) {
                // Si la pausa total ya no está activa, liberar
                if (!mCurrentPolicy.isInstantLockActive()) {
                    mCurrentState = EnforcementState.STATE_UNLOCKED;
                    mCurrentEnforcingPackage = "";

                    savePolicyToPrefs(mCurrentPolicy, mCurrentState);

                    StructuredLogger.logTransition(
                            source,
                            decision.getPolicyVersion(),
                            targetPackage,
                            "INSTANT_LOCK_RELEASED",
                            EnforcementState.name(prevState),
                            EnforcementState.name(mCurrentState),
                            StructuredLogger.ApplyResult.APPLIED,
                            "Full device lock cleared by policy update"
                    );

                    if (mOverlayHost != null) {
                        mOverlayHost.onHideOverlay();
                    }
                }
            }
        }
    }

    private void applyEnforcementToUi(String displayMessage, int state) {
        new Handler(Looper.getMainLooper()).post(new UiEnforcementRunnable(this, displayMessage, state));
    }
}
