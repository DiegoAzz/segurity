package com.norton.parental.companion;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Single authoritative policy decision engine for FamilyGuard / AppzSegurity.
 *
 * Implements a strict, immutable, deterministic precedence hierarchy:
 * 1. Emergency Dialers & Critical System Components (Always Allowed)
 * 2. Full Device Instant Lock (Parent-enforced instant pause)
 * 3. Schedule Restrictions (Bedtime / Study Hours)
 * 4. Daily Screen Time Quota Exhausted
 * 5. Manual Application Rule (Individual package block)
 * 6. Default (Allowed)
 */
public final class PolicyEngine {

    // Lista blanca incondicional de llamadas de emergencia y servicios esenciales de rescate
    private static final Set<String> EMERGENCY_PACKAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.phone",
            "com.android.server.telecom",
            "com.google.android.apps.messaging",
            "com.android.emergency",
            "com.sec.android.app.easymode",
            "com.coloros.safecenter"
    )));

    // Componentes del sistema que nunca deben bloquearse para no romper Android
    private static final Set<String> SYSTEM_ESSENTIALS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "android",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.oppo.launcher",
            "com.coloros.launcher",
            "com.huawei.android.launcher",
            "com.miui.home",
            "com.norton.parental.companion" // Nuestra propia app (protegida con PIN gate)
    )));

    private PolicyEngine() {
        // Utility class
    }

    /**
     * Evalúa determinísticamente si una aplicación en primer plano debe permitirse o bloquearse.
     *
     * @param packageName Paquete de la aplicación evaluada.
     * @param policy Modelo de política actualmente vigente.
     * @return Decisión formal e inmutable de política.
     */
    public static PolicyDecision evaluate(String packageName, PolicyModel policy) {
        final long version = policy != null ? policy.getPolicyVersion() : 1L;

        if (packageName == null || packageName.trim().isEmpty()) {
            return PolicyDecision.allow("", PolicyDecision.BlockReason.ALLOWED, version);
        }

        final String cleanPkg = packageName.trim().toLowerCase();

        // 1. EVALUACIÓN DE EMERGENCIA Y SISTEMA (Prioridad 1 - Incondicional)
        if (isEmergencyPackage(cleanPkg)) {
            return PolicyDecision.allow(packageName, PolicyDecision.BlockReason.EMERGENCY_WHITELIST, version);
        }
        if (isSystemEssential(cleanPkg)) {
            return PolicyDecision.allow(packageName, PolicyDecision.BlockReason.SYSTEM_ESSENTIAL, version);
        }

        if (policy == null) {
            return PolicyDecision.allow(packageName, PolicyDecision.BlockReason.ALLOWED, 1L);
        }

        // 2. PAUSA INSTANTÁNEA GLOBAL DEL DISPOSITIVO (Prioridad 2)
        if (policy.isInstantLockActive()) {
            return PolicyDecision.block(
                    packageName,
                    PolicyDecision.BlockReason.FULL_DEVICE_LOCK,
                    "🛑 Pausa Instantánea Activa: Tus padres han pausado el acceso a todos los dispositivos.",
                    version
            );
        }

        // 3. RESTRICCIONES HORARIAS PROGRAMADAS (Prioridad 3)
        if (policy.isBedtimeActive()) {
            return PolicyDecision.block(
                    packageName,
                    PolicyDecision.BlockReason.SCHEDULE_BEDTIME,
                    "🌙 Hora de Dormir: El uso de aplicaciones está pausado durante el horario nocturno.",
                    version
            );
        }
        if (policy.isSchoolTimeActive()) {
            return PolicyDecision.block(
                    packageName,
                    PolicyDecision.BlockReason.SCHEDULE_SCHOOL,
                    "📚 Horario Escolar: Esta aplicación está restringida durante horas de estudio.",
                    version
            );
        }

        // 4. LÍMITE DIARIO DE TIEMPO AGOTADO (Prioridad 4)
        if (policy.isTimeQuotaExhausted()) {
            return PolicyDecision.block(
                    packageName,
                    PolicyDecision.BlockReason.DAILY_LIMIT_EXHAUSTED,
                    "⏳ Límite de Tiempo Alcanzado: Has alcanzado tu límite de pantalla diario.",
                    version
            );
        }

        // 5. RESTRICCIÓN MANUAL DE APLICACIÓN INDIVIDUAL (Prioridad 5)
        if (isPackageManuallyBlocked(cleanPkg, policy)) {
            return PolicyDecision.block(
                    packageName,
                    PolicyDecision.BlockReason.MANUAL_APP_BLOCK,
                    "🚫 Aplicación Bloqueada: Esta aplicación ha sido restringida por tus padres.",
                    version
            );
        }

        // 6. PERMISO GENERAL POR DEFECTO (Prioridad 6)
        return PolicyDecision.allow(packageName, PolicyDecision.BlockReason.ALLOWED, version);
    }

    public static boolean isEmergencyPackage(String pkg) {
        if (pkg == null) return false;
        if (EMERGENCY_PACKAGES.contains(pkg)) return true;
        return pkg.contains("dialer") || pkg.contains("emergency") || pkg.contains("telecom");
    }

    public static boolean isSystemEssential(String pkg) {
        if (pkg == null) return false;
        if (SYSTEM_ESSENTIALS.contains(pkg)) return true;
        return pkg.contains("launcher") || pkg.contains("trebuchet") || pkg.equals("com.android.systemui");
    }

    private static boolean isPackageManuallyBlocked(String pkg, PolicyModel policy) {
        if (policy.getBlockedPackages().contains(pkg)) {
            return true;
        }

        // Tratamiento de variantes de YouTube (app principal, kids, music)
        if (policy.getBlockedPackages().contains("com.google.android.youtube")) {
            if (pkg.equals("com.google.android.youtube")
                    || pkg.equals("com.google.android.apps.youtube.kids")
                    || pkg.equals("com.google.android.apps.youtube.music")) {
                return true;
            }
        }

        // Tratamiento de variantes de Roblox / TikTok si se especifican
        if (policy.getBlockedPackages().contains("com.zhiliaoapp.musically") && pkg.equals("com.zhiliaoapp.musically.go")) {
            return true;
        }

        return false;
    }
}
