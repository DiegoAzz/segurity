/**
 * DeviceAdminFilter.ts
 * 
 * Arquitectura de Integración Nativa de Control Parental (Nivel Sistema Operativo)
 * 
 * En aplicaciones comerciales como Norton Family, la supervisión de bajo nivel
 * requiere permisos especiales del sistema operativo según la plataforma:
 * 
 * 1. ANDROID:
 *    - AccessibilityService: Detecta la URL activa en Chrome/Firefox/Samsung Internet
 *      inspeccionando la barra de direcciones (AccessibilityNodeInfo).
 *    - VPNService (Local Loopback DNS Filter): Redirige las consultas DNS a un proxy
 *      local en 127.0.0.1:53 que bloquea IPs de dominios prohibidos sin requerir root.
 *    - UsageStatsManager: Mide el tiempo exacto en primer plano de cada app instalada.
 *    - DevicePolicyManager (Device Admin / Device Owner): Evita que el niño desinstale
 *      la aplicación sin la contraseña maestra del tutor.
 *    - SYSTEM_ALERT_WINDOW: Dibuja la pantalla de bloqueo sobre cualquier aplicación
 *      cuando se activa la "Pausa Instantánea" o se agota el tiempo diario.
 * 
 * 2. iOS:
 *    - Family Controls Framework & Screen Time API (DeviceActivityReport, ManagedSettings):
 *      API oficial de Apple para restringir apps, limitar tiempo y filtrar navegación web
 *      sin romper el sandbox de iOS.
 *    - NetworkExtension (NEFilterDataProvider / NEDNSProxyProvider): Filtra tráfico web.
 *    - MDM Profile (Mobile Device Management): En instalaciones corporativas o supervisadas.
 */

export interface NativeFilterBridge {
  checkUrl(url: string): Promise<{ blocked: boolean; category?: string; reason?: string }>;
  checkAppAllowed(packageName: string): Promise<boolean>;
  lockScreenImmediately(reason: string): Promise<void>;
  unlockScreen(): Promise<void>;
}

export class MockNativeFilterBridge implements NativeFilterBridge {
  private isLocked: boolean = false;
  private blockedPackages: Set<string> = new Set(['com.discord']);

  async checkUrl(url: string) {
    const prohibitedKeywords = ['casino', 'porn', 'weapons', 'drugs', 'bet365'];
    const isProhibited = prohibitedKeywords.some(kw => url.toLowerCase().includes(kw));

    if (isProhibited) {
      return {
        blocked: true,
        category: 'Alto Riesgo',
        reason: 'Sitio bloqueado por Norton Parental Filter',
      };
    }

    return { blocked: false };
  }

  async checkAppAllowed(packageName: string): Promise<boolean> {
    if (this.isLocked) return false;
    return !this.blockedPackages.has(packageName);
  }

  async lockScreenImmediately(reason: string): Promise<void> {
    this.isLocked = true;
    console.log(`[Norton Native Bridge] Pantalla bloqueada: ${reason}`);
  }

  async unlockScreen(): Promise<void> {
    this.isLocked = false;
    console.log('[Norton Native Bridge] Pantalla reanudada.');
  }
}
