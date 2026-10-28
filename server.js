import http from 'http';
import fs from 'fs';
import path from 'path';
import os from 'os';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const PORT = process.env.PORT || 3080;
const DB_PATH = path.join(__dirname, 'data', 'database.json');
const PUBLIC_DIR = path.join(__dirname, 'public');

function getLocalIpAddress() {
  if (process.env.PUBLIC_URL) {
    return process.env.PUBLIC_URL.replace(/^https?:\/\//, '').replace(/\/$/, '');
  }
  // Dominio de produccion por defecto si existe
  return 'segurity.appzplay.online';
}

const LOCAL_IP = getLocalIpAddress();

// Cargar o inicializar base de datos
function loadDatabase() {
  try {
    if (fs.existsSync(DB_PATH)) {
      const data = fs.readFileSync(DB_PATH, 'utf8');
      return JSON.parse(data);
    }
  } catch (err) {
    console.error('Error al leer database.json:', err);
  }
  return { activeChildId: 'child_1', children: [], webCategories: [], requests: [], activityLogs: [] };
}

function saveDatabase(db) {
  try {
    fs.writeFileSync(DB_PATH, JSON.stringify(db, null, 2), 'utf8');
    return true;
  } catch (err) {
    console.error('Error al guardar database.json:', err);
    return false;
  }
}

let db = loadDatabase();

// Helper para respuestas JSON
function sendJson(res, statusCode, data) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type'
  });
  res.end(JSON.stringify(data));
}

// Helper para parsear body JSON
function parseRequestBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => {
      body += chunk.toString();
    });
    req.on('end', () => {
      try {
        const parsed = body ? JSON.parse(body) : {};
        resolve(parsed);
      } catch (e) {
        reject(e);
      }
    });
    req.on('error', err => reject(err));
  });
}

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

const server = http.createServer(async (req, res) => {
  const reqUrl = new URL(req.url, `http://${req.headers.host}`);
  const pathname = reqUrl.pathname;

  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type'
    });
    return res.end();
  }

  // --- API ROUTING ---
  if (pathname.startsWith('/api/')) {
    // 1. Obtener estado completo + información de red
    if (pathname === '/api/data' && req.method === 'GET') {
      const isHttps = req.headers['x-forwarded-proto'] === 'https' || (process.env.PUBLIC_URL && process.env.PUBLIC_URL.startsWith('https'));
      const protocol = isHttps ? 'https' : 'http';
      const host = process.env.PUBLIC_URL
        ? process.env.PUBLIC_URL.replace(/^https?:\/\//, '').replace(/\/$/, '')
        : (req.headers.host || `${LOCAL_IP}:${PORT}`);
      const baseUrl = `${protocol}://${host}`;
      const blockedPackages = (db.monitoredApps || [])
        .filter(a => a.action === 'blocked')
        .map(a => a.packageName);

      return sendJson(res, 200, {
        ...db,
        blockedPackages,
        serverNetwork: {
          localIp: LOCAL_IP,
          port: PORT,
          mobileChildUrl: `${baseUrl}/child`,
          parentUrl: baseUrl
        }
      });
    }

    // 2. Telemetría y Registro en vivo del Celular Real
    if (pathname === '/api/child/telemetry' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const child = db.children.find(c => c.id === (body.childId || db.activeChildId)) || db.children[0];
      if (child) {
        if (!child.devices) child.devices = [];

        // Registrar o actualizar dispositivo real
        const devId = body.deviceId || 'real_device_1';
        let dev = child.devices.find(d => d.id === devId);
        
        if (!dev) {
          dev = {
            id: devId,
            name: body.deviceName || 'Mi Celular Personal',
            type: 'smartphone',
            os: body.os || 'Dispositivo Móvil',
            batteryLevel: body.batteryLevel ? Math.round(body.batteryLevel) : 85,
            isOnline: true,
            lastSeen: 'Ahora mismo'
          };
          child.devices.push(dev);
        } else {
          if (body.deviceName) dev.name = body.deviceName;
          if (body.os) dev.os = body.os;
          if (body.batteryLevel !== undefined) dev.batteryLevel = Math.round(body.batteryLevel);
          dev.isOnline = true;
          dev.lastSeen = 'Ahora mismo';
        }

        if (body.lat !== undefined && body.lng !== undefined) {
          child.currentLocation = {
            ...child.currentLocation,
            lat: body.lat,
            lng: body.lng,
            updatedAt: 'Ahora mismo',
            battery: body.batteryLevel || child.currentLocation?.battery || 85
          };
        }
        saveDatabase(db);
        return sendJson(res, 200, { success: true, instantLockActive: child.instantLockActive, device: dev });
      }
      return sendJson(res, 404, { error: 'Hijo no encontrado' });
    }

    // 2b. Crear o editar hijo
    if (pathname === '/api/children/create' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const newChild = {
        id: 'child_' + Date.now(),
        name: body.name || 'Mi Hijo',
        age: Number(body.age || 10),
        avatar: body.avatar || '👦',
        dailyLimitMinutes: Number(body.dailyLimitMinutes || 120),
        usedTodayMinutes: 0,
        instantLockActive: false,
        safeSearchEnabled: true,
        schoolTimeActive: false,
        devices: [],
        currentLocation: {
          lat: 40.4168,
          lng: -3.7038,
          address: 'Esperando primera sincronización GPS...',
          battery: 100,
          updatedAt: 'Pendiente',
          zone: 'Sin registrar'
        }
      };
      db.children.push(newChild);
      db.activeChildId = newChild.id;
      saveDatabase(db);
      return sendJson(res, 200, { success: true, child: newChild });
    }

    // 2c. Limpiar todos los datos de ejemplo (Reset a limpio)
    if (pathname === '/api/children/clean-slate' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const childName = body.name || 'Mi Hijo';
      const childAge = Number(body.age || 11);
      
      const realChild = {
        id: 'child_user_1',
        name: childName,
        age: childAge,
        avatar: body.avatar || '👦',
        dailyLimitMinutes: Number(body.dailyLimitMinutes || 120),
        usedTodayMinutes: 0,
        instantLockActive: false,
        safeSearchEnabled: true,
        schoolTimeActive: false,
        devices: [], // Limpio para que se agregue el celular del usuario al conectar
        currentLocation: {
          lat: 40.4168,
          lng: -3.7038,
          address: 'Conecta tu celular para ver la ubicación GPS en tiempo real',
          battery: 100,
          updatedAt: 'Esperando dispositivo...',
          zone: 'En espera'
        }
      };

      db.children = [realChild];
      db.activeChildId = realChild.id;
      db.requests = [];
      db.activityLogs = [
        {
          id: 'log_init',
          childId: realChild.id,
          type: 'info',
          title: 'Sistema Iniciado y Listo',
          details: `Perfil de ${realChild.name} creado. Escanea el código QR o entra a /child desde tu celular para vincularlo.`,
          status: 'info',
          timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
        }
      ];

      saveDatabase(db);
      return sendJson(res, 200, { success: true, child: realChild, db });
    }

    // 3. Seleccionar hijo activo
    if (pathname === '/api/children/select' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const child = db.children.find(c => c.id === body.childId);
      if (child) {
        db.activeChildId = body.childId;
        saveDatabase(db);
        return sendJson(res, 200, { success: true, activeChildId: db.activeChildId, child });
      }
      return sendJson(res, 404, { error: 'Hijo no encontrado' });
    }

    // 4. Pausa instantánea / Bloqueo de dispositivo (Individual o Todos)
    if (pathname === '/api/instant-lock' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const lockAll = !!body.all;

      if (lockAll) {
        const anyUnlocked = db.children.some(c => !c.instantLockActive);
        const targetState = body.locked !== undefined ? body.locked : anyUnlocked;
        db.children.forEach(c => { c.instantLockActive = targetState; });

        const logEntry = {
          id: 'log_' + Date.now(),
          childId: 'all',
          type: 'time',
          title: targetState ? '🚨 BLOQUEO TOTAL ACTIVADO' : 'Desbloqueo Total de Dispositivos',
          details: targetState
            ? 'El padre ha bloqueado TODOS los dispositivos y celulares de la familia de inmediato.'
            : 'El padre ha reanudado el acceso a todos los dispositivos.',
          status: targetState ? 'blocked' : 'allowed',
          timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
        };
        db.activityLogs.unshift(logEntry);
        saveDatabase(db);
        return sendJson(res, 200, { success: true, instantLockActive: targetState, allLocked: targetState, log: logEntry });
      }

      const childId = body.childId || db.activeChildId;
      const child = db.children.find(c => c.id === childId);
      if (!child) return sendJson(res, 404, { error: 'Hijo no encontrado' });

      child.instantLockActive = body.locked !== undefined ? body.locked : !child.instantLockActive;
      
      const logEntry = {
        id: 'log_' + Date.now(),
        childId: child.id,
        type: 'time',
        title: child.instantLockActive ? '🚨 BLOQUEO DE DISPOSITIVOS ACTIVADO' : 'Dispositivos Reanudados',
        details: child.instantLockActive
          ? 'El padre ha bloqueado todos los dispositivos y celulares de ' + child.name + ' inmediatamente.'
          : 'El padre ha reanudado el acceso a los dispositivos de ' + child.name + '.',
        status: child.instantLockActive ? 'blocked' : 'allowed',
        timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
      };
      db.activityLogs.unshift(logEntry);

      saveDatabase(db);
      return sendJson(res, 200, { success: true, instantLockActive: child.instantLockActive, child, log: logEntry });
    }

    // 5. Añadir tiempo extra
    if (pathname === '/api/time/add' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const childId = body.childId || db.activeChildId;
      const child = db.children.find(c => c.id === childId);
      if (!child) return sendJson(res, 404, { error: 'Hijo no encontrado' });

      const extraMinutes = Number(body.minutes || 30);
      child.dailyLimitMinutes += extraMinutes;

      if (child.usedTodayMinutes < child.dailyLimitMinutes && body.unlockIfFull) {
        child.instantLockActive = false;
      }

      const logEntry = {
        id: 'log_' + Date.now(),
        childId: child.id,
        type: 'time',
        title: `Tiempo Adicional Otorgado (+${extraMinutes} min)`,
        details: `Se han añadido ${extraMinutes} minutos a ${child.name}. Nuevo límite: ${child.dailyLimitMinutes} min.`,
        status: 'allowed',
        timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
      };
      db.activityLogs.unshift(logEntry);

      saveDatabase(db);
      return sendJson(res, 200, { success: true, dailyLimitMinutes: child.dailyLimitMinutes, child });
    }

    // 6. Límite diario exacto
    if (pathname === '/api/time/limit' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const child = db.children.find(c => c.id === (body.childId || db.activeChildId));
      if (!child) return sendJson(res, 404, { error: 'Hijo no encontrado' });

      child.dailyLimitMinutes = Number(body.dailyLimitMinutes);
      saveDatabase(db);
      return sendJson(res, 200, { success: true, child });
    }

    // 7. Horario escolar
    if (pathname === '/api/time/school' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const child = db.children.find(c => c.id === (body.childId || db.activeChildId));
      if (!child) return sendJson(res, 404, { error: 'Hijo no encontrado' });

      child.schoolTimeActive = body.active !== undefined ? body.active : !child.schoolTimeActive;
      saveDatabase(db);
      return sendJson(res, 200, { success: true, schoolTimeActive: child.schoolTimeActive, child });
    }

    // 8. Modificar regla de categoría web
    if (pathname === '/api/web/category' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const category = db.webCategories.find(cat => cat.id === body.categoryId);
      if (!category) return sendJson(res, 404, { error: 'Categoría no encontrada' });

      category.action = body.action;
      saveDatabase(db);
      return sendJson(res, 200, { success: true, category });
    }

    // 9. SafeSearch
    if (pathname === '/api/web/safesearch' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const child = db.children.find(c => c.id === (body.childId || db.activeChildId));
      if (!child) return sendJson(res, 404, { error: 'Hijo no encontrado' });

      child.safeSearchEnabled = body.enabled !== undefined ? body.enabled : !child.safeSearchEnabled;
      saveDatabase(db);
      return sendJson(res, 200, { success: true, safeSearchEnabled: child.safeSearchEnabled });
    }

    // 10. Regla personalizada web
    if (pathname === '/api/web/rule' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      if (body.action === 'delete') {
        db.customWebRules = db.customWebRules.filter(r => r.id !== body.id);
      } else {
        const newRule = {
          id: 'rule_' + Date.now(),
          domain: body.domain.toLowerCase().trim(),
          action: body.policy || 'blocked',
          note: body.note || 'Regla personalizada por el tutor'
        };
        db.customWebRules.push(newRule);
      }
      saveDatabase(db);
      return sendJson(res, 200, { success: true, customWebRules: db.customWebRules });
    }

    // 11. Regla de app (Bloquear / Permitir / Limitar)
    if (pathname === '/api/apps/action' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const app = db.monitoredApps.find(a => a.id === body.appId || a.packageName === body.packageName);
      if (!app) return sendJson(res, 404, { error: 'Aplicación no encontrada' });

      if (body.action) app.action = body.action;
      if (body.dailyLimitMinutes !== undefined) app.dailyLimitMinutes = Number(body.dailyLimitMinutes);

      const logEntry = {
        id: 'log_' + Date.now(),
        childId: db.activeChildId || 'child_user_1',
        type: 'app',
        title: body.action === 'blocked' ? `🚫 Aplicación Bloqueada: ${app.name}` : `Regla actualizada: ${app.name}`,
        details: `El padre ha configurado "${app.name}" como ${body.action.toUpperCase()}.`,
        status: body.action === 'blocked' ? 'blocked' : 'allowed',
        timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
      };
      db.activityLogs.unshift(logEntry);

      saveDatabase(db);
      return sendJson(res, 200, { success: true, app, log: logEntry });
    }

    // 11b. Sincronizar TODAS las aplicaciones reales instaladas en el teléfono del niño
    if (pathname === '/api/child/installed-apps' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const incomingApps = body.apps || [];
      if (!Array.isArray(incomingApps) || incomingApps.length === 0) {
        return sendJson(res, 400, { error: 'Lista de aplicaciones vacía' });
      }

      // Combinar aplicaciones preservando reglas ya guardadas
      const updatedList = [];
      const colors = ['bg-red-600 text-white', 'bg-blue-600 text-white', 'bg-emerald-600 text-white', 'bg-indigo-600 text-white', 'bg-amber-500 text-black', 'bg-purple-600 text-white'];

      incomingApps.forEach((incApp, idx) => {
        const existing = db.monitoredApps.find(a => a.packageName === incApp.packageName);
        if (existing) {
          existing.name = incApp.name || existing.name;
          updatedList.push(existing);
        } else {
          // Nueva aplicación detectada en el teléfono
          updatedList.push({
            id: 'app_' + incApp.packageName.replace(/[^a-zA-Z0-9]/g, '_'),
            name: incApp.name,
            packageName: incApp.packageName,
            iconColor: colors[idx % colors.length],
            category: incApp.category || 'Aplicación Instalada',
            action: incApp.packageName.includes('youtube') ? 'allowed' : 'allowed',
            dailyLimitMinutes: 60,
            usedMinutesToday: 0
          });
        }
      });

      db.monitoredApps = updatedList;
      saveDatabase(db);
      console.log(`[APPS] Sincronizadas ${updatedList.length} aplicaciones reales desde el teléfono.`);
      return sendJson(res, 200, { success: true, count: updatedList.length, monitoredApps: db.monitoredApps });
    }

    // 12. Responder solicitud de un hijo
    if (pathname === '/api/requests/respond' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const request = db.requests.find(r => r.id === body.requestId);
      if (!request) return sendJson(res, 404, { error: 'Solicitud no encontrada' });

      request.status = body.decision;
      const child = db.children.find(c => c.id === request.childId);

      if (body.decision === 'approved') {
        if (request.type === 'extra_time') {
          const added = body.extraMinutes || request.requestedMinutes || 30;
          if (child) {
            child.dailyLimitMinutes += added;
            child.instantLockActive = false;
          }
        } else if (request.type === 'unblock_website') {
          db.customWebRules.push({
            id: 'rule_' + Date.now(),
            domain: request.target.toLowerCase().trim(),
            action: 'allowed',
            note: 'Aprobado a petición de ' + (child ? child.name : 'hijo')
          });
        }
      }

      db.activityLogs.unshift({
        id: 'log_' + Date.now(),
        childId: request.childId,
        type: request.type === 'extra_time' ? 'time' : 'web',
        title: body.decision === 'approved' ? '✅ Solicitud Aprobada' : '❌ Solicitud Rechazada',
        details: `El padre ${body.decision === 'approved' ? 'aprobó' : 'rechazó'} la solicitud de "${request.target}": "${request.message}"`,
        status: body.decision === 'approved' ? 'allowed' : 'blocked',
        timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
      });

      saveDatabase(db);
      return sendJson(res, 200, { success: true, request, db });
    }

    // 13. Enviar solicitud desde el celular del hijo
    if (pathname === '/api/child/request' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const child = db.children.find(c => c.id === (body.childId || db.activeChildId));
      if (!child) return sendJson(res, 404, { error: 'Hijo no encontrado' });

      const newRequest = {
        id: 'req_' + Date.now(),
        childId: child.id,
        childName: child.name,
        type: body.type || 'extra_time',
        target: body.target || 'Tiempo de pantalla',
        requestedMinutes: Number(body.requestedMinutes || 30),
        message: body.message || 'Por favor papá, necesito un poco más de tiempo.',
        timestamp: 'Hace unos instantes',
        status: 'pending'
      };

      db.requests.unshift(newRequest);
      saveDatabase(db);
      return sendJson(res, 200, { success: true, request: newRequest });
    }

    // 14. Navegación web en celular del hijo
    if (pathname === '/api/child/browse' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const child = db.children.find(c => c.id === (body.childId || db.activeChildId));
      const urlString = (body.url || '').toLowerCase().trim();

      if (child && child.instantLockActive) {
        return sendJson(res, 200, {
          allowed: false,
          reason: 'instant_lock',
          message: 'Pausa Instantánea Activa. El acceso a internet está temporalmente bloqueado por tus padres.'
        });
      }

      const customMatch = db.customWebRules.find(r => urlString.includes(r.domain));
      if (customMatch) {
        if (customMatch.action === 'blocked') {
          db.activityLogs.unshift({
            id: 'log_' + Date.now(),
            childId: child.id,
            type: 'web',
            title: 'Sitio Web Bloqueado por Regla Familiar',
            details: `Intento de acceso a "${urlString}" denegado por regla específica.`,
            status: 'blocked',
            timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
          });
          saveDatabase(db);
          return sendJson(res, 200, {
            allowed: false,
            reason: 'custom_rule',
            rule: customMatch,
            message: `El sitio ${customMatch.domain} fue bloqueado explícitamente por tus padres.`
          });
        } else if (customMatch.action === 'allowed') {
          return sendJson(res, 200, {
            allowed: true,
            status: 'allowed',
            message: `Sitio autorizado: ${urlString}`
          });
        }
      }

      for (const cat of db.webCategories) {
        if (cat.keywords && cat.keywords.some(kw => urlString.includes(kw))) {
          if (cat.action === 'blocked') {
            db.activityLogs.unshift({
              id: 'log_' + Date.now(),
              childId: child.id,
              type: 'web',
              title: `Sitio Web Bloqueado (${cat.name})`,
              details: `Intento de acceso a "${urlString}" bloqueado por pertenecer a la categoría ${cat.name}.`,
              status: 'blocked',
              timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
            });
            saveDatabase(db);
            return sendJson(res, 200, {
              allowed: false,
              reason: 'category_blocked',
              category: cat,
              message: `El sitio web fue bloqueado por la regla de seguridad: "${cat.name}".`
            });
          } else if (cat.action === 'warning') {
            return sendJson(res, 200, {
              allowed: true,
              warning: true,
              category: cat,
              message: `Advertencia Norton: Este sitio pertenece a la categoría "${cat.name}". Se supervisa su actividad.`
            });
          }
        }
      }

      db.activityLogs.unshift({
        id: 'log_' + Date.now(),
        childId: child.id,
        type: 'web',
        title: 'Navegación Web Segura',
        details: `Visita a "${urlString}" verificada sin incidentes.`,
        status: 'allowed',
        timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
      });
      saveDatabase(db);

      return sendJson(res, 200, {
        allowed: true,
        message: 'Sitio verificado y permitido.'
      });
    }

    // 14b. Notificación y Auditoría de Reinicio de Dispositivo
    if (pathname === '/api/child/reboot' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const child = db.children.find(c => c.id === (body.childId || db.activeChildId)) || db.children[0];
      const devName = body.deviceName || 'Dispositivo Móvil';
      const nowStr = new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

      const auditLog = {
        id: 'log_' + Date.now(),
        childId: child ? child.id : 'child_user_1',
        type: 'reboot',
        title: '⚠️ ALERTA DE SEGURIDAD: DISPOSITIVO REINICIADO',
        details: `El equipo "${devName}" fue reiniciado a las ${nowStr}. Norton Companion se reactivó automáticamente y restableció el bloqueo y las reglas.`,
        status: 'warning',
        timestamp: 'Hoy, ' + nowStr
      };

      db.activityLogs.unshift(auditLog);
      saveDatabase(db);
      console.log(`[AUDITORÍA SEGURIDAD] Reinicio detectado en ${devName} a las ${nowStr}`);
      return sendJson(res, 200, { success: true, instantLockActive: child ? child.instantLockActive : false, auditLog });
    }
    if (pathname === '/api/child/sos' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const child = db.children.find(c => c.id === (body.childId || db.activeChildId));
      const sosLog = {
        id: 'log_' + Date.now(),
        childId: child ? child.id : 'child_1',
        type: 'location',
        title: '🚨 ALERTA SOS - AUXILIO SOLICITADO',
        details: `${child ? child.name : 'Tu hijo'} ha presionado el botón de alerta SOS desde su teléfono móvil. Ubicación GPS reportada.`,
        status: 'blocked',
        timestamp: '¡AHORA MISMO! ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
      };
      db.activityLogs.unshift(sosLog);
      saveDatabase(db);
      return sendJson(res, 200, { success: true, alert: sosLog });
    }

    return sendJson(res, 404, { error: 'Endpoint no encontrado' });
  }

  // Ruta directa para el celular del niño: /child o /companion
  if (pathname === '/child' || pathname === '/companion') {
    const childFile = path.join(PUBLIC_DIR, 'child.html');
    if (fs.existsSync(childFile)) {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      return fs.createReadStream(childFile).pipe(res);
    }
  }

  // --- SERVIR ARCHIVOS ESTÁTICOS ---
  let filePath = path.join(PUBLIC_DIR, pathname === '/' ? 'index.html' : pathname);

  if (!filePath.startsWith(PUBLIC_DIR)) {
    res.writeHead(403);
    return res.end('Acceso denegado');
  }

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      filePath = path.join(PUBLIC_DIR, 'index.html');
    }

    const ext = path.extname(filePath);
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    fs.readFile(filePath, (readErr, content) => {
      if (readErr) {
        res.writeHead(500);
        return res.end('Error interno del servidor');
      }
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content);
    });
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`\n======================================================`);
  console.log(`🛡️  NORTON FAMILY - SISTEMA DE CONTROL PARENTAL`);
  console.log(`======================================================`);
  console.log(`💻 Panel de Padres en PC:    http://localhost:${PORT}`);
  console.log(`📱 Conexión desde tu Celular: http://${LOCAL_IP}:${PORT}/child`);
  console.log(`======================================================\n`);
});
