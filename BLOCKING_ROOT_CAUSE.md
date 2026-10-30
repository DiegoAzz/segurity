# BLOCKING_ROOT_CAUSE.md — DIAGNÓSTICO Y ANÁLISIS DE CAUSA RAÍZ
## FamilyGuard / AppzSegurity Android Engine

**Fecha:** 30 de Octubre de 2026  
**Rama de trabajo:** `fix/android-blocking-stability`  
**Autor:** Senior Android Concurrency & System Architect  
**Estado:** Confirmado e Identificado de Raíz

---

## 1. RESUMEN EJECUTIVO DEL PROBLEMA

La aplicación de control parental experimentaba un comportamiento crítico: **las aplicaciones se bloqueaban y desbloqueaban repetidamente cada 1 a 2 segundos en un ciclo infinito de parpadeo (flickering)**.

El usuario observaba:
> *"ahora bloquea pero las aplicaciones por unidad no las bloquea, osea que si bloqueo youtube no la bloquea... y si lo hace se cambia solo, entra y sale como si se hubiera vuelto loco"*.

Este informe documenta la inspección exhaustiva de código, la reconstrucción matemática y temporal del fallo, y el mapa de llamadas que originaba la falla.

---

## 2. MAPA DE LLAMADAS Y ARQUITECTURA EXISTENTE

El módulo `android-app` contenía dos servicios ejecutándose simultáneamente en segundo plano, ambos con permisos elevados:

1. **`LockOverlayService` (Servicio en primer plano con `TYPE_APPLICATION_OVERLAY`)**:
   - Mantenía un temporizador `PollRunnable` que consultaba cada **1.500 ms** al endpoint `/api/data`.
   - Mantenía la referencia a la ventana flotante `mOverlayView` en `WindowManager`.
   - Controlaba la variable global estática `sIsLocked`.
   - Si `/api/data` indicaba que la pausa instantánea global no estaba activa (`instantLockActive == false`), ejecutaba:
     ```java
     mService.mHandler.post(new StateUpdater(mService, false));
     // -> hideOverlay(); -> remueve ventana y llama LockActivity.finishIfRunning()
     ```

2. **`AccessibilityMonitorService` (Servicio de Accesibilidad con `typeAllMask`)**:
   - Escuchaba todos los eventos de la pantalla del dispositivo (`onAccessibilityEvent`).
   - Mantenía **su propio temporizador independiente** `SyncRunnable` que consultaba cada **1.000 ms** al endpoint `/api/data`.
   - Mantenía su propio set estático `sBlockedPackages`.
   - Cada vez que el niño abría o interactuaba con una aplicación restringida (ej. `com.google.android.youtube`), ejecutaba:
     ```java
     performGlobalAction(GLOBAL_ACTION_HOME);
     Intent lockIntent = new Intent(this, LockOverlayService.class);
     lockIntent.setAction(LockOverlayService.ACTION_SHOW_LOCK);
     startService(lockIntent);
     startActivity(lockActIntent); // LockActivity
     ```

3. **`LockActivity` (Actividad Fullscreen)**:
   - Pantalla completa con banderas de bloqueo de pantalla.
   - Escuchaba la difusión `com.norton.parental.ACTION_UNLOCK` para auto-destruirse (`finish()`).

---

## 3. LAS 6 CAUSAS RAÍZ IDENTIFICADAS

### CAUSA 1: Conflicto de Autoridad Bicefálica (Split-Brain Concurrency)
- **El Hecho:** Existen dos servicios (`LockOverlayService` y `AccessibilityMonitorService`) tomando decisiones de bloqueo y desbloqueo de forma totalmente desacoplada e independiente.
- **La Colisión:**
  - `AccessibilityMonitorService` detecta que YouTube está en primer plano y envía una orden de **BLOQUEAR** (`showOverlay()`).
  - Concurrentemente, `LockOverlayService.PollWorker` termina su sondeo de cada 1.5s, verifica únicamente el campo global de pausa familiar `instantLockActive` (que es `false`, porque el padre solo bloqueó YouTube, no todo el celular), y concluye erróneamente que el dispositivo debe ser **DESBLOQUEADO**.
  - `LockOverlayService` ejecuta inmediatamente `hideOverlay()`, removiendo la pantalla de bloqueo y permitiendo al niño ver YouTube por un instante.
  - Al segundo siguiente, `AccessibilityMonitorService` vuelve a detectar a YouTube abierto y dispara nuevamente el bloqueo.
  - **Resultado:** Un bucle infinito de bloqueo y desbloqueo alternado a una frecuencia de 0.6 Hz a 1.0 Hz.

```
TIEMPO (ms)    ACCESSIBILITY SERVICE                   LOCK OVERLAY SERVICE
---------------------------------------------------------------------------------------
t = 0 ms       Detecta YouTube abierto
               -> Ejecuta showOverlay() [BLOQUEA]
t = 200 ms     Pantalla roja de bloqueo visible
t = 750 ms                                             Termina sondeo /api/data
                                                       Lee: instantLockActive = false
                                                       -> Ejecuta hideOverlay() [DESBLOQUEA]
t = 800 ms     Pantalla de bloqueo se cierra
               YouTube vuelve a ser visible
t = 1200 ms    Detecta evento de ventana YouTube
               -> Ejecuta showOverlay() [BLOQUEA]
t = 2250 ms                                             Termina siguiente sondeo
                                                       Lee: instantLockActive = false
                                                       -> Ejecuta hideOverlay() [DESBLOQUEA]
[EL CICLO CONTINÚA INFINITAMENTE]
```

---

### CAUSA 2: Ausencia de una Máquina de Estados Tipada (Flag Booleano `sIsLocked` Sobrecargado)
- En el código anterior, existía una única variable estática:
  ```java
  private static boolean sIsLocked = false;
  ```
- Esta variable representaba indiscriminadamente:
  1. Bloqueo total del dispositivo (Pausa Instantánea).
  2. Bloqueo manual de una aplicación individual (ej. YouTube, Roblox).
  3. Bloqueo web por categoría para adultos.
  4. Modo de prueba temporal (Test Lock).
  5. Agotamiento de tiempo de pantalla.
- Al no existir una máquina de estados que distinga **qué capa causó el bloqueo**, la liberación de una condición (ej. no hay pausa total) borraba de un plumazo el bloqueo legítimo de las demás capas (la aplicación prohibida).

---

### CAUSA 3: Tormenta de Eventos de Reentrada en AccessibilityService
- `accessibility_service_config.xml` configuraba:
  ```xml
  android:accessibilityEventTypes="typeAllMask"
  android:notificationTimeout="50"
  ```
- Cuando `LockOverlayService` añadía `mOverlayView` mediante `WindowManager.addView()`, la actividad subyacente (YouTube) no era destruida inmediatamente por Android, sino pausada.
- Elementos internos de YouTube (subtítulos, hilos de renderizado, notificaciones multimedia, cambios de foco accesibles) continuaban emitiendo eventos de accesibilidad a razón de 20 a 50 eventos por segundo.
- Al no contar con deduplicación de estado, cada evento individual ejecutaba:
  - `performGlobalAction(GLOBAL_ACTION_HOME)`
  - Creación y envío de un nuevo `Intent` para `LockActivity` con `FLAG_ACTIVITY_NEW_TASK`
  - Llamada a `startService(lockIntent)`
- Esto saturaba la cola de mensajes de Android (`Looper.getMainLooper()`), provocando caída de cuadros, congelamiento momentáneo y reinicios del proceso por ANR (`Application Not Responding`).

---

### CAUSA 4: Ausencia de Versionado Monotónico de Políticas (`policyVersion`)
- El backend (`server.js`) y la APK intercambiaban datos sin un número de versión secuencial.
- Debido a la latencia móvil variable (4G/Wi-Fi):
  - Solicitud A (emitida en $t=100$) podía responder en $t=900$.
  - Solicitud B (emitida en $t=400$ con nueva regla) podía responder en $t=600$.
  - La respuesta rezagada de la Solicitud A sobrescribía la Solicitud B, anulando el cambio más reciente del padre.

---

### CAUSA 5: Interferencia entre WindowManager Overlay y LockActivity
- `LockOverlayService` añadía una vista superpuesta (`mOverlayView`) y a la vez lanzaba `LockActivity`.
- Al minimizarse o recibir el broadcast `ACTION_UNLOCK`, `LockActivity` ejecutaba:
  ```java
  public static void unlockEverything(Context context) {
      Intent hideIntent = new Intent(context, LockOverlayService.class);
      hideIntent.setAction(LockOverlayService.ACTION_HIDE_LOCK);
      context.startService(hideIntent);
  }
  ```
- Si el usuario presionaba botón de inicio o el sistema cerraba la actividad por memoria, se enviaba un desbloqueo total no intencional al servicio de overlay.

---

### CAUSA 6: Múltiples Hilos HTTP no Sincronizados
- En cada ciclo de 1 y 1.5 segundos se instanciaban hilos anónimos (`new FetchRulesThread().start()` y `new PollWorker().start()`).
- No existía cola serializada ni exclusión mutua; múltiples hilos leían y escribían concurrentemente sobre colecciones compartidas sin sincronización de ciclo de vida.

---

## 4. CONCLUSIÓN Y HOJA DE RUTA DE CORRECCIÓN

Para resolver este problema de manera definitiva y sin soluciones temporales o retrasos artificiales, se requiere:
1. **Unificar la toma de decisiones en un único `PolicyEngine` determinista**, con jerarquía estricta de precedencia.
2. **Centralizar la ejecución en un único `EnforcementController` con máquina de estados finita**, serializado en un solo hilo y con deduplicación estricta de estados.
3. **Eliminar los sondeos duplicados**, dejando una única fuente de sincronización que actualice el `PolicyEngine`.
4. **Implementar versionado monotónico (`policyVersion`) en backend y cliente**, descartando cualquier carga desactualizada.
5. **Filtrar la reentrada de eventos de accesibilidad**, garantizando que un paquete ya bloqueado no dispare nuevas acciones si el bloqueo ya está en pantalla.
