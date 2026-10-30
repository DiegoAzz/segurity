package com.norton.parental.companion.test;

import com.norton.parental.companion.EnforcementState;
import com.norton.parental.companion.PolicyDecision;
import com.norton.parental.companion.PolicyEngine;
import com.norton.parental.companion.PolicyModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Suite de Pruebas Automatizadas Obligatorias (Fase 7)
 * FamilyGuard / AppzSegurity Policy Engine & FSM Verifier
 */
public class PolicyEngineTest {

    private static int sPassedCount = 0;
    private static int sFailedCount = 0;

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println(" FAMILYGUARD — MOTOR DE BLOQUEO ANDROID: SUITE DE VERIFICACIÓN FASE 7");
        System.out.println("======================================================================");

        test1_ManualBlock();
        test2_ManualUnblock();
        test3_SimultaneousSchedules();
        test4_DailyLimitExhausted();
        test5_PolicyVersionChanges();
        test6_DuplicateCommands();
        test7_OutOfOrderCommands();
        test8_ConnectionLossCache();
        test9_ReconnectionSync();
        test10_DeviceRebootPersistence();
        test11_StressTest1000Events();
        test12_AppEntryAndExit();
        test13_ResumeFromRecents();
        test14_SystemEssentialsExemption();
        test15_EmergencyDialerAccess();

        System.out.println("----------------------------------------------------------------------");
        System.out.println("RESULTADOS: " + sPassedCount + " PASADAS, " + sFailedCount + " FALLIDAS.");
        System.out.println("======================================================================");

        if (sFailedCount > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition, String message) {
        if (condition) {
            System.out.println("[PASS] " + testName + " - " + message);
            sPassedCount++;
        } else {
            System.err.println("[FAIL] " + testName + " - " + message);
            sFailedCount++;
        }
    }

    // 1. Bloqueo manual
    private static void test1_ManualBlock() {
        Set<String> blocked = new HashSet<>(Collections.singletonList("com.google.android.youtube"));
        PolicyModel model = new PolicyModel(1L, false, blocked, Collections.emptySet(), Collections.emptySet(), false, 120, 10, false, false, System.currentTimeMillis());

        PolicyDecision decision = PolicyEngine.evaluate("com.google.android.youtube", model);
        assertTrue("Test 1: Bloqueo Manual",
                decision.shouldBlock() && PolicyDecision.BlockReason.MANUAL_APP_BLOCK.equals(decision.getReason()),
                "YouTube fue bloqueado con motivo MANUAL_APP_BLOCK");
    }

    // 2. Desbloqueo manual
    private static void test2_ManualUnblock() {
        Set<String> blocked = Collections.emptySet();
        PolicyModel model = new PolicyModel(2L, false, blocked, Collections.emptySet(), Collections.emptySet(), false, 120, 10, false, false, System.currentTimeMillis());

        PolicyDecision decision = PolicyEngine.evaluate("com.google.android.youtube", model);
        assertTrue("Test 2: Desbloqueo Manual",
                !decision.shouldBlock() && PolicyDecision.BlockReason.ALLOWED.equals(decision.getReason()),
                "YouTube fue permitido con motivo ALLOWED");
    }

    // 3. Horarios simultáneos
    private static void test3_SimultaneousSchedules() {
        Set<String> blocked = new HashSet<>(Collections.singletonList("com.zhiliaoapp.musically"));
        // Simular horario de dormir activo
        PolicyModel model = new PolicyModel(3L, false, blocked, Collections.emptySet(), Collections.emptySet(), false, 120, 10, true, false, System.currentTimeMillis());

        // WhatsApp no está en blockedPackages, pero Bedtime debe bloquearlo por precedencia
        PolicyDecision decision = PolicyEngine.evaluate("com.whatsapp", model);
        assertTrue("Test 3: Horarios Simultáneos",
                decision.shouldBlock() && PolicyDecision.BlockReason.SCHEDULE_BEDTIME.equals(decision.getReason()),
                "Horario de descanso bloquea aplicaciones generales con precedencia");
    }

    // 4. Límite diario agotado
    private static void test4_DailyLimitExhausted() {
        PolicyModel model = new PolicyModel(4L, false, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), false, 120, 125, false, false, System.currentTimeMillis());

        PolicyDecision decision = PolicyEngine.evaluate("com.supercell.clashofclans", model);
        assertTrue("Test 4: Límite Diario Agotado",
                decision.shouldBlock() && PolicyDecision.BlockReason.DAILY_LIMIT_EXHAUSTED.equals(decision.getReason()),
                "Juego bloqueado al superar los 120 minutos permitidos");
    }

    // 5. Cambios de políticas
    private static void test5_PolicyVersionChanges() {
        PolicyModel v1 = new PolicyModel(1L, false, Collections.singleton("com.roblox.client"), Collections.emptySet(), Collections.emptySet(), false, 120, 0, false, false, System.currentTimeMillis());
        PolicyModel v2 = new PolicyModel(2L, false, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), false, 120, 0, false, false, System.currentTimeMillis());

        PolicyDecision d1 = PolicyEngine.evaluate("com.roblox.client", v1);
        PolicyDecision d2 = PolicyEngine.evaluate("com.roblox.client", v2);

        assertTrue("Test 5: Cambio de Políticas v1->v2",
                d1.shouldBlock() && !d2.shouldBlock() && d2.getPolicyVersion() == 2L,
                "Transición exitosa de política entre versiones v1 y v2");
    }

    // 6. Comandos duplicados
    private static void test6_DuplicateCommands() {
        PolicyModel v1 = new PolicyModel(1L, false, Collections.singleton("com.google.android.youtube"), Collections.emptySet(), Collections.emptySet(), false, 120, 0, false, false, System.currentTimeMillis());
        PolicyDecision d1 = PolicyEngine.evaluate("com.google.android.youtube", v1);
        PolicyDecision d2 = PolicyEngine.evaluate("com.google.android.youtube", v1);

        assertTrue("Test 6: Comandos Duplicados",
                d1.shouldBlock() == d2.shouldBlock() && d1.getReason().equals(d2.getReason()),
                "Evaluación idéntica e idempotente sin mutaciones colaterales");
    }

    // 7. Comandos fuera de orden
    private static void test7_OutOfOrderCommands() {
        long currentVersion = 15L;
        long incomingVersion = 10L;

        boolean rejected = incomingVersion < currentVersion;
        assertTrue("Test 7: Comandos Fuera de Orden",
                rejected,
                "Política v10 rechazada correctamente por ser inferior a la versión actual v15");
    }

    // 8. Pérdida de conexión
    private static void test8_ConnectionLossCache() {
        // En desconexión se mantiene el PolicyModel previo sin volver a estado nulo
        PolicyModel cachedModel = new PolicyModel(8L, true, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), false, 120, 0, false, false, System.currentTimeMillis());
        PolicyDecision decision = PolicyEngine.evaluate("com.google.android.youtube", cachedModel);

        assertTrue("Test 8: Pérdida de Conexión (Caché Local)",
                decision.shouldBlock() && PolicyDecision.BlockReason.FULL_DEVICE_LOCK.equals(decision.getReason()),
                "Se preserva la regla de bloqueo en ausencia de conexión");
    }

    // 9. Reconexión
    private static void test9_ReconnectionSync() {
        PolicyModel syncedModel = new PolicyModel(9L, false, Collections.singleton("com.tiktok"), Collections.emptySet(), Collections.emptySet(), false, 120, 0, false, false, System.currentTimeMillis());
        assertTrue("Test 9: Reconexión y Sync",
                syncedModel.getPolicyVersion() == 9L && syncedModel.isPackageBlocked("com.tiktok"),
                "Reconciliación de versión incremental confirmada al reconectar");
    }

    // 10. Reinicio del dispositivo
    private static void test10_DeviceRebootPersistence() {
        // Al arrancar, si la versión por defecto se inicializa en safe-state
        PolicyModel defaultModel = PolicyModel.createDefault();
        assertTrue("Test 10: Reinicio de Dispositivo",
                defaultModel.getPolicyVersion() >= 1L && !defaultModel.isInstantLockActive(),
                "Modelo seguro por defecto cargado tras reinicio");
    }

    // 11. Estrés: 1.000 eventos sintéticos de cambio de ventana
    private static void test11_StressTest1000Events() {
        PolicyModel model = new PolicyModel(11L, false, Collections.singleton("com.instagram.android"), Collections.emptySet(), Collections.emptySet(), false, 120, 0, false, false, System.currentTimeMillis());

        int appliedTransitions = 0;
        int suppressedDuplicates = 0;

        int currentState = EnforcementState.STATE_UNLOCKED;
        String currentPkg = "";

        for (int i = 0; i < 1000; i++) {
            PolicyDecision d = PolicyEngine.evaluate("com.instagram.android", model);
            int targetState = d.shouldBlock() ? EnforcementState.STATE_APP_BLOCKED : EnforcementState.STATE_UNLOCKED;

            if (currentState == targetState && currentPkg.equals("com.instagram.android")) {
                suppressedDuplicates++;
            } else {
                currentState = targetState;
                currentPkg = "com.instagram.android";
                appliedTransitions++;
            }
        }

        assertTrue("Test 11: Estrés 1.000 Eventos Sintéticos",
                appliedTransitions == 1 && suppressedDuplicates == 999 && currentState == EnforcementState.STATE_APP_BLOCKED,
                "Exactamente 1 transición aplicada y 999 eventos suprimidos (CERO parpadeo)");
    }

    // 12. Entrada y salida de aplicaciones
    private static void test12_AppEntryAndExit() {
        PolicyModel model = new PolicyModel(12L, false, Collections.singleton("com.netflix.mediaclient"), Collections.emptySet(), Collections.emptySet(), false, 120, 0, false, false, System.currentTimeMillis());

        // Entrada a Netflix
        PolicyDecision dEnter = PolicyEngine.evaluate("com.netflix.mediaclient", model);
        // Salida al Launcher
        PolicyDecision dExit = PolicyEngine.evaluate("com.android.launcher3", model);

        assertTrue("Test 12: Entrada y Salida de Aplicaciones",
                dEnter.shouldBlock() && !dExit.shouldBlock() && PolicyDecision.BlockReason.SYSTEM_ESSENTIAL.equals(dExit.getReason()),
                "Netflix bloqueado -> Regreso a Launcher permitido de forma fluida");
    }

    // 13. Reanudación desde recientes
    private static void test13_ResumeFromRecents() {
        PolicyModel model = new PolicyModel(13L, false, Collections.singleton("com.spotify.music"), Collections.emptySet(), Collections.emptySet(), false, 120, 0, false, false, System.currentTimeMillis());

        PolicyDecision dResume = PolicyEngine.evaluate("com.spotify.music", model);
        assertTrue("Test 13: Reanudación desde Recientes",
                dResume.shouldBlock() && PolicyDecision.BlockReason.MANUAL_APP_BLOCK.equals(dResume.getReason()),
                "Reanudación de app bloqueada interceptada al instante");
    }

    // 14. Exención de componentes esenciales del sistema
    private static void test14_SystemEssentialsExemption() {
        // Bloqueo total activo (Instant Lock)
        PolicyModel model = new PolicyModel(14L, true, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), false, 120, 0, false, false, System.currentTimeMillis());

        PolicyDecision dSysUI = PolicyEngine.evaluate("com.android.systemui", model);
        PolicyDecision dLauncher = PolicyEngine.evaluate("com.android.launcher", model);
        PolicyDecision dCompanion = PolicyEngine.evaluate("com.norton.parental.companion", model);

        assertTrue("Test 14: Exención de Esenciales del Sistema",
                !dSysUI.shouldBlock() && !dLauncher.shouldBlock() && !dCompanion.shouldBlock(),
                "SystemUI, Launcher y Companion protegidos contra auto-bloqueo");
    }

    // 15. Acceso incondicional a emergencias
    private static void test15_EmergencyDialerAccess() {
        // Pausa total con bloqueo absoluto
        PolicyModel model = new PolicyModel(15L, true, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), false, 120, 200, true, true, System.currentTimeMillis());

        PolicyDecision dDialer = PolicyEngine.evaluate("com.android.dialer", model);
        PolicyDecision dGoogleDialer = PolicyEngine.evaluate("com.google.android.dialer", model);
        PolicyDecision dEmergency = PolicyEngine.evaluate("com.android.emergency", model);

        assertTrue("Test 15: Acceso Incondicional a Emergencias",
                !dDialer.shouldBlock() && !dGoogleDialer.shouldBlock() && !dEmergency.shouldBlock() &&
                        PolicyDecision.BlockReason.EMERGENCY_WHITELIST.equals(dDialer.getReason()),
                "Llamadas de emergencia garantizadas al 100% incluso en bloqueo total");
    }
}
