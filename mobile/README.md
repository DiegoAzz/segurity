# 📱 Norton Family Mobile (React Native / Expo)

Este directorio contiene el código fuente completo de la aplicación móvil para **iOS y Android** de Norton Family Clone, permitiendo operar tanto en **Modo Padres** como en **Modo Niño (Companion Agent)**.

---

## 📂 Estructura

- `App.tsx`: Punto de entrada con selector de perfil superior ("Modo Padres" vs "Modo Niño").
- `src/screens/ChildHomeScreen.tsx`: Pantalla del menor con cuenta atrás de tiempo, pantalla de bloqueo por pausa instantánea, botón de pánico SOS y peticiones a padres.
- `src/screens/ParentDashboardScreen.tsx`: Pantalla móvil del tutor para pausar dispositivos, ver ubicación GPS y aprobar solicitudes.
- `src/services/LocationService.ts`: Módulo de cálculo geodésico (Haversine) para detección de entrada y salida de Geocercas (colegio, casa, abuelos).
- `src/services/DeviceAdminFilter.ts`: Especificación y puente para la integración con APIs nativas del sistema operativo (Android AccessibilityService / UsageStats / VPN y Apple Screen Time API).

---

## 🛠️ Ejecución Local con Expo

1. Instala las dependencias de Expo:
   ```bash
   cd c:\REA\norton-parental-clone\mobile
   npm install
   ```

2. Inicia el servidor de desarrollo de Expo:
   ```bash
   npx expo start
   ```

3. Abre la aplicación:
   - Presiona `a` para abrir en emulador de **Android**.
   - Presiona `i` para abrir en simulador de **iOS** (macOS).
   - Presiona `w` para abrir en el **navegador web**.
   - O escanea el código QR con la app **Expo Go** en tu smartphone físico.
