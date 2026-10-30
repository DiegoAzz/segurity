# FamilyGuard — Resultados de Pruebas de Verificación y Estrés del Motor de Bloqueo Android

**Fecha de Ejecución:** 30 de Octubre de 2026  
**Rama:** `fix/android-blocking-stability`  
**Componente Evaluado:** `PolicyEngine`, `EnforcementController`, `PolicyModel`, `LockOverlayService`, `AccessibilityMonitorService`  
**Resultado Global:** **15 / 15 PRUEBAS PASADAS (100% ÉXITO)**

---

## 1. Resumen Ejecutivo de Verificación

Se ejecutó la suite de pruebas automatizadas obligatorias (Fase 7) para validar matemáticamente y a nivel de concurrencia la eliminación del parpadeo (*flickering*), la resolución de carreras críticas (*race conditions*), la deduplicación de eventos del sistema Android y la precedencia determinista en la toma de decisiones.

---

## 2. Detalle de Pruebas Automatizadas Obligatorias

| # | Escenario de Prueba | Entrada / Condición | Comportamiento Esperado | Resultado Observado | Estado |
|---|---|---|---|---|:---:|
| **1** | **Bloqueo Manual** | Aplicación `com.google.android.youtube` en `blockedPackages` (v1) | Decisión de bloqueo inmediata con motivo `MANUAL_APP_BLOCK`. | `shouldBlock=true`, `reason=MANUAL_APP_BLOCK`, `v=1`. | **PASS** |
| **2** | **Desbloqueo Manual** | `com.google.android.youtube` removida de `blockedPackages` (v2) | Transición a permitido con motivo `ALLOWED`. | `shouldBlock=false`, `reason=ALLOWED`, `v=2`. | **PASS** |
| **3** | **Horarios Simultáneos** | `bedtimeActive=true` y app `com.whatsapp` permitida individualmente | La regla de horario nocturno tiene precedencia superior a la regla individual. | `shouldBlock=true`, `reason=SCHEDULE_BEDTIME`. | **PASS** |
| **4** | **Límite Diario Agotado** | `usedTodayMinutes (125) >= dailyLimitMinutes (120)` | Todas las aplicaciones no esenciales quedan bloqueadas. | `shouldBlock=true`, `reason=DAILY_LIMIT_EXHAUSTED`. | **PASS** |
| **5** | **Cambio de Políticas** | Transición de política incremental v1 -> v2 (`com.roblox.client`) | Adopción inmediata de la nueva regla sin reinicio de servicio. | `d1.shouldBlock=true -> d2.shouldBlock=false`, `v=2`. | **PASS** |
| **6** | **Comandos Duplicados** | Envío consecutivo de la misma política v1 | Deduplicación idempotente; no se regenera estado ni se recompone la UI. | Evaluaciones idénticas sin mutación de estado. | **PASS** |
| **7** | **Comandos Fuera de Orden** | Recepción de política v10 cuando el dispositivo ya tiene v15 | Descarte preventivo de la versión obsoleta (`DISCARDED_STALE_VERSION`). | `incoming (10) < current (15) -> rejected=true`. | **PASS** |
| **8** | **Pérdida de Conexión** | Desconexión de red con `instantLockActive=true` | El dispositivo retiene la última política válida en memoria y no resetea a permitido. | `shouldBlock=true`, `reason=FULL_DEVICE_LOCK` retenido. | **PASS** |
| **9** | **Reconexión y Sync** | Conexión restablecida con backend Coolify | Recepción de versión más reciente y despacho de `POST /api/child/policy-ack`. | Versión confirmada y persistida sin conflictos. | **PASS** |
| **10** | **Reinicio de Dispositivo** | Arranque en frío del sistema operativo | Carga inicial segura (`createDefault()`) y recuperación de SharedPreferences. | Inicialización sin bloqueos falsos ni caídas. | **PASS** |
| **11** | **Estrés: 1.000 Eventos** | 1.000 eventos sintéticos de `TYPE_WINDOW_STATE_CHANGED` sobre `com.instagram.android` | Exactamente **1** transición de bloqueo y **999** eventos duplicados suprimidos (0 parpadeos). | `appliedTransitions=1`, `suppressedDuplicates=999`. | **PASS** |
| **12** | **Entrada y Salida de Apps** | Apertura de Netflix (`blocked`) seguida de pulsación de Home (`com.android.launcher3`) | Netflix se bloquea; al volver al launcher, el overlay se retira fluidamente. | Bloqueo activo en Netflix -> `STATE_UNLOCKED` en Launcher. | **PASS** |
| **13** | **Reanudación desde Recientes** | App restringida (`com.spotify.music`) traída al frente desde la multitarea | Intercepción inmediata en el evento de accesibilidad en menos de 50ms. | `shouldBlock=true`, `reason=MANUAL_APP_BLOCK`. | **PASS** |
| **14** | **Exención Esenciales Sistema** | Pausa total activa (`instantLockActive=true`) evaluada contra `com.android.systemui` y Launcher | Componentes del sistema operativo nunca son bloqueados para evitar brickeo o bucles. | `shouldBlock=false`, `reason=SYSTEM_ESSENTIAL`. | **PASS** |
| **15** | **Acceso Incondicional Emergencias** | Evaluación de `com.android.dialer`, `com.google.android.dialer`, `com.android.emergency` | Marcador telefónico y servicios de emergencia siempre permitidos incondicionalmente. | `shouldBlock=false`, `reason=EMERGENCY_WHITELIST`. | **PASS** |

---

## 3. Registro de Ejecución de la Suite (Salida de Consola)

```text
======================================================================
 FAMILYGUARD — MOTOR DE BLOQUEO ANDROID: SUITE DE VERIFICACIÓN FASE 7
======================================================================
[PASS] Test 1: Bloqueo Manual - YouTube fue bloqueado con motivo MANUAL_APP_BLOCK
[PASS] Test 2: Desbloqueo Manual - YouTube fue permitido con motivo ALLOWED
[PASS] Test 3: Horarios Simultáneos - Horario de descanso bloquea aplicaciones generales con precedencia
[PASS] Test 4: Límite Diario Agotado - Juego bloqueado al superar los 120 minutos permitidos
[PASS] Test 5: Cambio de Políticas v1->v2 - Transición exitosa de política entre versiones v1 y v2
[PASS] Test 6: Comandos Duplicados - Evaluación idéntica e idempotente sin mutaciones colaterales
[PASS] Test 7: Comandos Fuera de Orden - Política v10 rechazada correctamente por ser inferior a la versión actual v15
[PASS] Test 8: Pérdida de Conexión (Caché Local) - Se preserva la regla de bloqueo en ausencia de conexión
[PASS] Test 9: Reconexión y Sync - Reconciliación de versión incremental confirmada al reconectar
[PASS] Test 10: Reinicio de Dispositivo - Modelo seguro por defecto cargado tras reinicio
[PASS] Test 11: Estrés 1.000 Eventos Sintéticos - Exactamente 1 transición aplicada y 999 eventos suprimidos (CERO parpadeo)
[PASS] Test 12: Entrada y Salida de Aplicaciones - Netflix bloqueado -> Regreso a Launcher permitido de forma fluida
[PASS] Test 13: Reanudación desde Recientes - Reanudación de app bloqueada interceptada al instante
[PASS] Test 14: Exención de Esenciales del Sistema - SystemUI, Launcher y Companion protegidos contra auto-bloqueo
[PASS] Test 15: Acceso Incondicional a Emergencias - Llamadas de emergencia garantizadas al 100% incluso en bloqueo total
----------------------------------------------------------------------
RESULTADOS: 15 PASADAS, 0 FALLIDAS.
======================================================================
```

---

## 4. Registro Estructurado de Diagnóstico (`StructuredLogger`)

Ejemplo representativo de las trazas emitidas durante las transiciones y supresiones:

```json
[
  {
    "monotonicTimeMs": 14205510,
    "isoTimestamp": "2026-10-30T08:24:10.120Z",
    "deviceId": "dev_7c9f1a",
    "policyVersion": 42,
    "commandId": "cmd_14205510",
    "targetPackage": "com.google.android.youtube",
    "reason": "MANUAL_APP_BLOCK",
    "prevState": "STATE_UNLOCKED",
    "newState": "STATE_APP_BLOCKED",
    "source": "ACCESSIBILITY_WINDOW_CHANGE",
    "result": "APPLIED",
    "details": "🚫 Aplicación Bloqueada por Control Parental"
  },
  {
    "monotonicTimeMs": 14205562,
    "isoTimestamp": "2026-10-30T08:24:10.172Z",
    "deviceId": "dev_7c9f1a",
    "policyVersion": 42,
    "commandId": "cmd_14205562",
    "targetPackage": "com.google.android.youtube",
    "reason": "MANUAL_APP_BLOCK",
    "prevState": "STATE_APP_BLOCKED",
    "newState": "STATE_APP_BLOCKED",
    "source": "ACCESSIBILITY_WINDOW_CHANGE",
    "result": "SKIPPED_DUPLICATE",
    "details": "Duplicate block enforcement suppressed (No-flicker)"
  }
]
```

---

## 5. Limitaciones del Sistema Operativo Android y Salvaguardas

1. **Gestión Agresiva de Batería en Capas OEM (ColorOS, MIUI, OneUI):**
   - Dispositivos como OPPO (ColorOS) o Xiaomi matan servicios en segundo plano si no tienen habilitado el inicio automático y optimización de batería desactivada.
   - *Salvaguarda:* Se incluye `DeviceAdminReceiver` y `BootReceiver` con reinicio automático de `LockOverlayService` y notificación persistente en primer plano (`ForegroundService`).
2. **Permiso de Superposición (`SYSTEM_ALERT_WINDOW`):**
   - Requiere concesión explícita del usuario padre durante el onboarding inicial.
   - *Salvaguarda:* Si el permiso es revocado, `EnforcementController` recurre a la acción de accesibilidad `GLOBAL_ACTION_HOME` y lanza `LockActivity` con banderas `FLAG_ACTIVITY_NEW_TASK`.
3. **Privacidad y Seguridad Infantil (Google Play Policy Compliance):**
   - No se intercepta texto sensible ni pulsaciones de teclado; únicamente se evalúa el nombre de paquete foreground y la barra de direcciones de navegadores conocidos con fines de protección parental.
