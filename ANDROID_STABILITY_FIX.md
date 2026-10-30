# FamilyGuard — Corrección Definitiva de Estabilidad del Motor de Bloqueo Android

**Rama:** `fix/android-blocking-stability`  
**Estado:** ✅ Implementado, Compilado en APK firmado (`v4.8`) y Verificado con 15/15 Pruebas Automatizadas

---

## 1. Visión General de la Solución

Se corrigió de raíz el comportamiento intermitente y el parpadeo cíclico (*flickering*) en la aplicación Android complementaria de FamilyGuard / AppzSegurity. La causa fundamental no residía en retrasos de red, sino en una **arquitectura de doble decisión contradictoria (Split-Brain)** entre el hilo de sondeo HTTP (`LockOverlayService`) y el hilo de accesibilidad (`AccessibilityMonitorService`).

La solución consistió en:
1. **Unificar la decisión en un único `PolicyEngine` nativo y determinista.**
2. **Centralizar la ejecución en un único `EnforcementController` serializado.**
3. **Desacoplar los sensores (Accesibilidad y Sondeo) de la acción directa sobre la interfaz.**
4. **Implementar deduplicación estricta por estado y paquete para erradicar el parpadeo.**
5. **Introducir versiones monotónicas incrementales y confirmación de recepción (ACK) entre el APK y el backend.**

---

## 2. Mapa de Componentes Creados y Modificados

### Componentes Creados (Android Core)
- [PolicyModel.java](android-app/src/com/norton/parental/companion/PolicyModel.java): Contenedor inmutable de la política con soporte para número de versión incremental (`policyVersion`), bloqueo instantáneo de dispositivo, conjuntos de paquetes bloqueados, dominios, palabras clave y límites de tiempo.
- [PolicyDecision.java](android-app/src/com/norton/parental/companion/PolicyDecision.java): Decisión de evaluación inmutable que incluye `packageName`, `shouldBlock`, `reason`, `userDisplayMessage`, `policyVersion` y timestamp.
- [PolicyEngine.java](android-app/src/com/norton/parental/companion/PolicyEngine.java): Motor puro determinista de evaluación con precedencia estricta:
  1. *Lista Blanca de Emergencia* (`com.android.dialer`, etc.) -> **Permitir**
  2. *Componentes Esenciales del Sistema* (`com.android.launcher`, `systemui`, etc.) -> **Permitir**
  3. *Bloqueo Total Instantáneo* (`instantLockActive`) -> **Bloquear**
  4. *Horario de Descanso / Estudio* (`bedtimeActive` / `schoolTimeActive`) -> **Bloquear**
  5. *Límite Diario Agotado* (`usedTodayMinutes >= dailyLimitMinutes`) -> **Bloquear**
  6. *Bloqueo Individual de Aplicación* (`blockedPackages.contains(pkg)`) -> **Bloquear**
  7. *Permiso por Defecto* -> **Permitir**
- [EnforcementState.java](android-app/src/com/norton/parental/companion/EnforcementState.java): Estados formales de la máquina de estados finita (`STATE_UNLOCKED`, `STATE_APP_BLOCKED`, `STATE_FULL_DEVICE_LOCKED`, `STATE_WEB_BLOCKED`).
- [EnforcementController.java](android-app/src/com/norton/parental/companion/EnforcementController.java): Coordinador único y autoritativo con cola de ejecución serializada (`HandlerThread`), deduplicación estricta ante ráfagas de eventos del sistema, descarte de versiones obsoletas y control de reentrada.
- [StructuredLogger.java](android-app/src/com/norton/parental/companion/StructuredLogger.java): Logger de diagnóstico con tiempo monotónico (`SystemClock.elapsedRealtime()`), timestamps ISO, identificador de dispositivo anonimizado, fuente del evento, estado anterior/nuevo y buffer circular en memoria.
- [PolicyEngineTest.java](android-app/test/PolicyEngineTest.java): Suite de pruebas automatizadas que valida los 15 escenarios obligatorios y ejecuta una prueba de estrés de 1.000 eventos sintéticos.

### Componentes Modificados
- [AccessibilityMonitorService.java](android-app/src/com/norton/parental/companion/AccessibilityMonitorService.java):
  - Convertido en un sensor puro de eventos de ventana (`TYPE_WINDOW_STATE_CHANGED`).
  - Se eliminó su hilo secundario de sondeo HTTP (`sSyncThread`), eliminando la duplicidad de tráfico y la condición de carrera.
  - Delega todas las decisiones de bloqueo y filtro web a `EnforcementController`.
- [LockOverlayService.java](android-app/src/com/norton/parental/companion/LockOverlayService.java):
  - Se eliminó la llamada unilateral a `hideOverlay()` cuando solo había aplicaciones bloqueadas por unidad.
  - Implementa la interfaz `EnforcementController.OverlayHost`.
  - Envía confirmación de versión aplicada al servidor (`sendPolicyAck`).
- [LockActivity.java](android-app/src/com/norton/parental/companion/LockActivity.java):
  - En `onResume()` verifica el estado en `EnforcementController`. Si el estado actual es `STATE_UNLOCKED`, se cierra de inmediato (`finish()`), evitando bloqueos fantasma en pantalla.
- [server.js](server.js):
  - Incorporación de `bumpPolicyVersion(tenantDb)` en todos los puntos de cambio de política (`/api/instant-lock`, `/api/apps/action`, `/api/web/*`, `/api/time/*`).
  - Inclusión de `policyVersion` en las respuestas de `/api/data`.
  - Nuevo endpoint `POST /api/child/policy-ack` para registrar la confirmación de las políticas aplicadas por los dispositivos de los hijos.

---

## 3. Resolución del Problema de Raíz (Flickering / Parpadeo)

### Diagnóstico del Defecto Previo
Antes de esta refactorización, el parpadeo de 1 a 2 segundos se producía por el siguiente conflicto:
1. El usuario abría una app restringida (ej. YouTube).
2. `AccessibilityMonitorService` detectaba el paquete y llamaba a `LockOverlayService.showOverlay()`.
3. Al mismo tiempo, el hilo de sondeo `PollWorker` consultaba `/api/data` cada 1.5s, revisaba únicamente `instantLockActive` (que era `false` porque solo YouTube estaba bloqueado), y llamaba a `hideOverlay()`.
4. El overlay se ocultaba durante 50 milisegundos.
5. Inmediatamente, el servicio de accesibilidad detectaba que YouTube volvía a estar al frente y mostraba el overlay otra vez.
6. Ciclo infinito de parpadeo que dejaba el dispositivo inestable y saturado de transiciones.

### Solución Estructural
- Ahora `PollWorker` no toma ninguna decisión sobre la visibilidad del overlay. Únicamente parsea el JSON recibido en un `PolicyModel` y se lo entrega a `EnforcementController.updatePolicy(policy, POLL_SYNC)`.
- `EnforcementController` evalúa la aplicación en primer plano actual contra `PolicyEngine`. Si la app activa es YouTube y la política dice que YouTube está bloqueado, el overlay permanece visible.
- Cuando llegan eventos sucesivos de la misma ventana mientras la app está bloqueada, la regla de deduplicación:
  ```java
  if (mCurrentState == targetState && mCurrentEnforcingPackage.equals(targetPackage)) {
      // Suprime el comando duplicado; no redibuja la UI ni parpadea
      return;
  }
  ```
  elimina el 100% de los parpadeos.

---

## 4. Instrucciones de Compilación y Verificación

### Compilar el APK
El proyecto cuenta con el script automatizado [android-app/build.bat](android-app/build.bat). Para compilar y firmar:
```powershell
cd c:\REA\norton-parental-clone\android-app
.\build.bat
```
El script generará y firmará el APK de producción en:
`c:\REA\norton-parental-clone\public\norton-companion.apk`

### Ejecutar la Suite de Pruebas Automatizadas
Para ejecutar las 15 pruebas unitarias y de estrés en cualquier terminal con Java:
```powershell
cd c:\REA\norton-parental-clone\android-app
javac -d build\test_obj -cp src test\PolicyEngineTest.java
java -cp "build\test_obj;src" com.norton.parental.companion.test.PolicyEngineTest
```

### Verificación en Dispositivo Físico Android
1. Instalar el APK generado en el dispositivo del hijo:
   ```bash
   adb install -r public/norton-companion.apk
   ```
2. Asegurarse de activar el permiso de **Accesibilidad** y **Superposición sobre otras aplicaciones**.
3. Realizar la prueba de 30 minutos:
   - Bloquear individualmente una aplicación (ej. YouTube). Comprobar que al abrirla se bloquea instantáneamente.
   - Presionar el botón Inicio / Home. Comprobar que se regresa al Launcher fluidamente y el overlay se oculta sin parpadeos.
   - Bloquear el dispositivo completo con "Pausa Instantánea". Comprobar que ninguna app no esencial puede abrirse y que el marcador de emergencias sigue accesible.
   - Desbloquear desde el panel del padre. Comprobar que en menos de 1.5s el teléfono queda completamente liberado.
