# 🛡️ Norton Family Clone | Sistema de Control Parental y Cuidado Infantil

Clon funcional y completo de la plataforma de control parental **Norton Family Premier** ([us.norton.com](https://us.norton.com/)), diseñado para supervisar de manera segura el uso de dispositivos, filtrar contenido web, restringir aplicaciones, gestionar horarios de pantalla y ubicar a los niños en tiempo real mediante geocercas.

---

## 🌟 Características Principales

### 1. ⏱️ Gestión y Supervisión del Tiempo de Pantalla
- **Pausa Instantánea (Instant Lock):** Botón de emergencia para tutores que bloquea inmediatamente todas las pantallas de los dispositivos del niño.
- **Límites de Tiempo Diarios:** Slider interactivo para fijar el número máximo de horas por día (ej. 2h diarias).
- **Horario Semanal Permitido:** Calendario de Lunes a Domingo con rangos horarios autorizados.
- **Modo Horario Escolar (School Time):** Bloqueo automático de distracciones (juegos y redes sociales) de 08:00 AM a 02:00 PM, dejando únicamente herramientas educativas.
- **Toque de Queda Nocturno:** Apagado de pantalla programado para horas de descanso (ej. 21:00 a 07:00).
- **Adición de Tiempo en 1 Clic:** Botones de acceso rápido para conceder `+15 min`, `+30 min` o `+1 hora`.

### 2. 🌐 Supervisión Web y Filtro de Contenido
- **8 Categorías de Seguridad:**
  - 🔞 Contenido para Adultos / Pornografía *(Bloqueado)*
  - ⚔️ Violencia, Odio y Armas *(Bloqueado)*
  - 🎰 Apuestas y Casinos Online *(Bloqueado)*
  - 💊 Drogas, Tabaco y Sustancias *(Bloqueado)*
  - 💬 Redes Sociales *(Advertencia / Supervisión)*
  - 🎮 Videojuegos en Línea *(Permitido con horario)*
  - 🛍️ Tiendas y Pasarelas de Pago *(Advertencia)*
  - 📚 Educación y Ciencias *(Permitido)*
- **Búsqueda Segura Forzada (SafeSearch):** Filtro de contenido explícito en Google, Bing y YouTube.
- **Listas Blancas y Negras:** Añade o elimina dominios web concretos con reglas específicas (`Bloquear`, `Advertir`, `Permitir`).

### 3. 📱 Supervisión de Aplicaciones Móviles
- Monitoreo de apps instaladas (TikTok, Roblox, YouTube, Discord, WhatsApp, Brawl Stars, Duolingo, etc.).
- Políticas individuales: **Permitida**, **Bloqueada** o **Con límite diario** (ej. TikTok máx 30 min/día).
- Contador de minutos consumidos hoy por aplicación.

### 4. 📍 Ubicación en Tiempo Real y Geocercas (Zonas Seguras)
- Mapa interactivo con posición GPS en vivo del dispositivo del menor.
- Geocercas configuradas:
  - 🏫 **Colegio San Patricio** (Radio 250m)
  - 🏠 **Casa Familiar** (Radio 150m)
  - 👨‍👩‍👧 **Casa de los Abuelos** (Radio 200m)
- Alertas de entrada y salida automática.
- Botón de **Check-in ("Ya Llegué")** para que el niño notifique su llegada con un solo toque.

### 5. 🔔 Bandeja de Solicitudes y Alertas Parentales
- Cuando el niño agota su tiempo o necesita acceder a un sitio restringido, envía una petición con un mensaje explicativo.
- El padre revisa la petición y puede **Aprobar** o **Rechazar** con un clic. Al aprobar, el tiempo se suma automáticamente sin reiniciar el dispositivo.

### 6. 📲 Simulador Interactivo de Dispositivo del Niño
- En la pestaña **"Simulador Dispositivo Niño"** podrás interactuar con un marco de smartphone real:
  - Ver el contador de tiempo restante en vivo.
  - Probar la reacción inmediata al activar la **Pausa Instantánea**.
  - Probar el navegador web simulado: escribe URLs como `casino-slots.com` o `wikipedia.org` y observa el bloqueo oficial de Norton en tiempo real.
  - Botón de **Pánico SOS** con alerta prioritaria inmediata.

---

## 🚀 Cómo Ejecutar la Aplicación

### Requisitos
- **Node.js** v18 o superior instalado.
- Ninguna dependencia externa pesada requerida (cero fallos de npm).

### Inicio Rápido
1. Abre tu terminal en esta carpeta:
   ```bash
   cd c:\REA\norton-parental-clone
   ```
2. Inicia el servidor:
   ```bash
   npm start
   # o bien:
   node server.js
   ```
3. Abre tu navegador en:
   ```
   http://localhost:3080
   ```

---

## 📱 Aplicación Móvil (React Native / Expo)

Dentro de la carpeta `mobile/` se encuentra la aplicación móvil completa:
- **Modo Padres:** Dashboard móvil para gestionar perfiles, activar pausa instantánea y aprobar solicitudes sobre la marcha.
- **Modo Niño (Companion Agent):** Pantalla del menor con cuenta atrás, bloqueo forzado, llamadas a padres autorizadas y botón SOS.
- **Servicios Nativos:** Lógica de Geocercas (`LocationService.ts`) y arquitectura de integración nativa de bajo nivel (`DeviceAdminFilter.ts`).

Para ejecutar el módulo móvil:
```bash
cd mobile
npm install
npx expo start
```

---

## 🏗️ Arquitectura Técnica en Producción

| Capa | Implementación Norton Family | Implementación en este Clon |
| :--- | :--- | :--- |
| **Frontend Padres** | Portal Web Angular/React + App Móvil | Responsive Web SPA (Tailwind CSS + Lucide Icons) + App Móvil React Native |
| **Backend & API** | Microservicios REST + WebSocket Push | Node.js HTTP Server nativo + REST API modular |
| **Almacenamiento** | Bases de datos distribuidas en nube | `data/database.json` persistente y editable en caliente |
| **Agente Android** | AccessibilityService + VPNService Loopback | `mobile/src/services/DeviceAdminFilter.ts` + Simulador Web |
| **Agente iOS** | Apple FamilyControls & Screen Time API | Especificación y servicio adaptado en React Native |
