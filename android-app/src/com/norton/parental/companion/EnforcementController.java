package com.norton.parental.companion;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * Single Authoritative Execution Coordinator for FamilyGuard / AppzSegurity.
 *
 * Implements:
 * - Deterministic Finite State Machine (UNLOCKED, APP_BLOCKED, FULL_DEVICE_LOCKED, WEB_BLOCKED)
 * - Serialized thread-safe execution queue
 * - Strict deduplication to eliminate flickering
 * - Anti-loop reentrancy guards
 * - Stale version rejection
 */
public final class EnforcementController {

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
        mCurrentPolicy = PolicyModel.createDefault();

        HandlerThread thread = new HandlerThread("FamilyGuard-EnforcementQueue");
        thread.start();
        mSerialHandler = new Handler(thread.getLooper());
    }

    public synchronized void setOverlayHost(OverlayHost host) {
        mOverlayHost = host;
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
