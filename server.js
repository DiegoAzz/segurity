import http from 'http';
import fs from 'fs';
import path from 'path';
import os from 'os';
import { fileURLToPath } from 'url';

// Blindaje total contra caídas del servidor Node.js
process.on('uncaughtException', (err) => {
  console.error('[FATAL SERVER ERROR - PREVENTED EXIT]', err);
});
process.on('unhandledRejection', (reason, promise) => {
  console.error('[UNHANDLED REJECTION - PREVENTED EXIT]', reason);
});

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const PORT = process.env.PORT || 3080;
const DATA_DIR = process.env.DATA_PATH || path.join(__dirname, 'data');
const DB_PATH = path.join(DATA_DIR, 'database.json');
const PUBLIC_DIR = path.join(__dirname, 'public');

function getLocalIpAddress() {
  if (process.env.PUBLIC_URL) {
    return process.env.PUBLIC_URL.replace(/^https?:\/\//, '').replace(/\/$/, '');
  }
  return 'segurity.appzplay.online';
}

const LOCAL_IP = getLocalIpAddress();

// Plantilla base de categorías de filtrado web
const DEFAULT_WEB_CATEGORIES = [
  {
    id: "cat_adult",
    name: "Contenido para Adultos / Pornografía",
    icon: "🔞",
    description: "Sitios con material explícito, desnudez y pornografía.",
    riskLevel: "high",
    action: "blocked",
    keywords: ["porn", "xxx", "adult", "sex", "erotic", "xvideos", "pornhub", "onlyfans", "redtube", "xnxx", "stripchat"]
  },
  {
    id: "cat_violence",
    name: "Violencia, Odio y Armas",
    icon: "⚔️",
    description: "Material violento, armas de fuego, racismo e incitación al odio.",
    riskLevel: "high",
    action: "blocked",
    keywords: ["weapons", "guns", "gore", "violence", "hate", "armas"]
  },
  {
    id: "cat_gambling",
    name: "Apuestas y Juegos de Azar",
    icon: "🎰",
    description: "Casinos online, apuestas deportivas, póker y ruletas virtuales.",
    riskLevel: "high",
    action: "blocked",
    keywords: ["casino", "bet365", "poker", "slots", "gamble", "apuestas", "wplay", "rushbet", "betplay"]
  },
  {
    id: "cat_drugs",
    name: "Drogas, Tabaco y Sustancias",
    icon: "💊",
    description: "Venta y promoción de estupefacientes, vapeo y alcohol para menores.",
    riskLevel: "high",
    action: "blocked",
    keywords: ["drugs", "vape", "weed", "alcohol", "liquor", "marihuana"]
  },
  {
    id: "cat_social",
    name: "Redes Sociales",
    icon: "💬",
    description: "Intercambio de mensajes, fotos y perfiles públicos.",
    riskLevel: "medium",
    action: "warning",
    keywords: ["instagram", "tiktok", "twitter", "facebook", "snapchat"]
  },
  {
    id: "cat_gaming",
    name: "Videojuegos Online",
    icon: "🎮",
    description: "Portales de juegos en navegador, servidores de juego y streams.",
    riskLevel: "medium",
    action: "allowed",
    keywords: ["roblox", "twitch", "poki", "steam", "minecraft"]
  },
  {
    id: "cat_shopping",
    name: "Compras y Tiendas en Línea",
    icon: "🛍️",
    description: "Sitios con pasarelas de pago y tarjetas de crédito.",
    riskLevel: "medium",
    action: "warning",
    keywords: ["amazon", "aliexpress", "shein", "ebay", "shop", "mercadolibre"]
  },
  {
    id: "cat_education",
    name: "Educación y Ciencias",
    icon: "📚",
    description: "Enciclopedias, plataformas escolares y portales educativos.",
    riskLevel: "low",
    action: "allowed",
    keywords: ["wikipedia", "khanacademy", "duolingo", "edu", "scratch"]
  }
];

function createDefaultFamilyData(childName = 'Mi Hijo', childAge = 11) {
  const childId = 'child_' + Date.now();
  return {
    activeChildId: childId,
    children: [
      {
        id: childId,
        name: childName,
        age: Number(childAge),
        avatar: '👦',
        dailyLimitMinutes: 120,
        usedTodayMinutes: 0,
        instantLockActive: false,
        safeSearchEnabled: true,
        schoolTimeActive: false,
        devices: [
          {
            id: 'primary_phone',
            name: 'Smartphone Android',
            type: 'smartphone',
            os: 'Android 10',
            batteryLevel: 85,
            isOnline: true,
            lastSeen: 'Ahora mismo'
          }
        ],
        currentLocation: {
          lat: 10.3982,
          lng: -75.4933,
          address: 'Conecta tu celular para ver la ubicación GPS en vivo',
          battery: 85,
          updatedAt: 'Pendiente',
          zone: 'Sin registrar'
        }
      }
    ],
    webCategories: JSON.parse(JSON.stringify(DEFAULT_WEB_CATEGORIES)),
    requests: [],
    activityLogs: [
      {
        id: 'log_' + Date.now(),
        childId: childId,
        type: 'info',
        title: 'Familia Activada y Protegida',
        details: `Servicio de control parental activo para ${childName}. Descarga el APK e ingresa tu código familiar para vincular el celular.`,
        status: 'info',
        timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
      }
    ],
    monitoredApps: [],
    customWebRules: [],
    geofences: [
      {
        id: 'geo_1',
        name: 'Hogar / Casa',
        lat: 10.3982,
        lng: -75.4933,
        radiusMeters: 300,
        address: 'Ubicación de Casa',
        notifyOnExit: true
      }
    ]
  };
}

// Cargar o inicializar base de datos con soporte Multi-Tenant
function loadDatabase() {
  let loaded = null;
  try {
    if (!fs.existsSync(DB_PATH)) {
      const templatePath = path.join(__dirname, 'data', 'database.json');
      if (fs.existsSync(templatePath) && templatePath !== DB_PATH) {
        const dir = path.dirname(DB_PATH);
        if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
        fs.copyFileSync(templatePath, DB_PATH);
      }
    }
    if (fs.existsSync(DB_PATH)) {
      const data = fs.readFileSync(DB_PATH, 'utf8');
      loaded = JSON.parse(data);
    }
  } catch (err) {
    console.error('Error al leer database.json:', err);
  }

  if (!loaded) {
    loaded = {};
  }

  // Super Admin Credentials
  if (!loaded.admin) {
    loaded.admin = {
      email: process.env.ADMIN_EMAIL || 'admin@segurity.com',
      password: process.env.ADMIN_PASSWORD || 'admin'
    };
  }

  // Si database.json tenía el formato single-tenant previo, migrar a db.clients
  if (!Array.isArray(loaded.clients)) {
    const defaultData = {
      activeChildId: loaded.activeChildId || 'child_user_1',
      children: Array.isArray(loaded.children) && loaded.children.length > 0 ? loaded.children : createDefaultFamilyData().children,
      webCategories: Array.isArray(loaded.webCategories) && loaded.webCategories.length > 0 ? loaded.webCategories : JSON.parse(JSON.stringify(DEFAULT_WEB_CATEGORIES)),
      requests: Array.isArray(loaded.requests) ? loaded.requests : [],
      activityLogs: Array.isArray(loaded.activityLogs) ? loaded.activityLogs.filter(log => log.id !== 'log_init') : [],
      monitoredApps: Array.isArray(loaded.monitoredApps) ? loaded.monitoredApps : [],
      customWebRules: Array.isArray(loaded.customWebRules) ? loaded.customWebRules : [],
      geofences: Array.isArray(loaded.geofences) ? loaded.geofences : []
    };

    loaded.clients = [
      {
        id: 'client_default',
        name: 'Familia Demo',
        email: 'padre@segurity.com',
        password: 'padre',
        phone: '+57 300 123 4567',
        plan: '1 mes',
        status: 'active',
        pairingCode: '123456',
        createdAt: new Date().toISOString(),
        expiresAt: new Date(Date.now() + 30 * 24 * 3600 * 1000).toISOString(),
        data: defaultData
      }
    ];
  }

  // Limpieza de dispositivos duplicados en todos los clientes
  loaded.clients.forEach(cl => {
    if (cl.data && Array.isArray(cl.data.children)) {
      cl.data.children.forEach(c => {
        if (Array.isArray(c.devices) && c.devices.length > 1) {
          const active = c.devices.find(d => d.batteryLevel && d.batteryLevel !== 85) || c.devices[c.devices.length - 1];
          c.devices = [active];
        }
      });
    }
  });

  if (!loaded.sessions) {
    loaded.sessions = {};
  }

  // Asegurar persistencia inmediata en disco
  saveDatabase(loaded);

  return loaded;
}

function saveDatabase(db) {
  try {
    const dir = path.dirname(DB_PATH);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    fs.writeFileSync(DB_PATH, JSON.stringify(db, null, 2), 'utf8');
    return true;
  } catch (err) {
    console.error('Error al guardar database.json:', err);
    return false;
  }
}

function bumpPolicyVersion(tenantDb) {
  if (!tenantDb) return 1;
  const current = Number(tenantDb.policyVersion) || 1;
  tenantDb.policyVersion = current + 1;
  return tenantDb.policyVersion;
}

let db = loadDatabase();

// Sesiones persistentes (para no perder login ni cuentas al reiniciar)
function getSession(token) {
  if (!token) return null;
  if (!db.sessions) db.sessions = {};
  return db.sessions[token] || null;
}

function setSession(token, data) {
  if (!db.sessions) db.sessions = {};
  db.sessions[token] = data;
  saveDatabase(db);
}

function deleteSession(token) {
  if (db.sessions && token) {
    delete db.sessions[token];
    saveDatabase(db);
  }
}

// Helper para resolver el inquilino (familia / cliente) activo en cada solicitud
function resolveTenant(req, reqUrl, body = {}) {
  let authHeader = req.headers['authorization'] || '';
  let token = '';
  if (authHeader.startsWith('Bearer ')) {
    token = authHeader.substring(7).trim();
  } else if (reqUrl.searchParams.has('token')) {
    token = reqUrl.searchParams.get('token');
  }

  const session = getSession(token);

  // Impersonación por Super Admin o código de emparejamiento desde el APK
  const asClient = reqUrl.searchParams.get('asClient') || req.headers['x-client-id'];
  const pairingCode = reqUrl.searchParams.get('code') || body.code || body.pairingCode;
  const requestedClientId = reqUrl.searchParams.get('clientId') || body.clientId;

  let matchedClient = null;

  // 1. Si viene código de 6 dígitos del teléfono APK
  if (pairingCode && Array.isArray(db.clients)) {
    matchedClient = db.clients.find(c => String(c.pairingCode).trim() === String(pairingCode).trim());
    if (!matchedClient) {
      // Si el APK envió un código pero no coincide con ninguna familia, no hacer fallback a otra familia
      return { client: null, tenantDb: null, session, invalidCode: true };
    }
  }

  // 2. Si se solicita un clientId específico (impersonación de admin o query)
  if (!matchedClient && (asClient || requestedClientId) && Array.isArray(db.clients)) {
    matchedClient = db.clients.find(c => c.id === (asClient || requestedClientId));
  }

  // 3. Si la sesión autenticada pertenece a un cliente padre
  if (!matchedClient && session && session.role === 'client' && Array.isArray(db.clients)) {
    matchedClient = db.clients.find(c => c.id === session.clientId);
  }

  // 4. Fallback por compatibilidad: primer cliente (cliente por defecto)
  if (!matchedClient && Array.isArray(db.clients) && db.clients.length > 0) {
    matchedClient = db.clients[0];
  }

  if (matchedClient && !matchedClient.data) {
    matchedClient.data = createDefaultFamilyData(matchedClient.name + ' Hijo');
  }

  const tenantDb = matchedClient ? matchedClient.data : db;

  // Auto-sanitización de perfiles y aislamiento estricto de aplicaciones entre Mi Hijo y Vicky
  if (tenantDb && Array.isArray(tenantDb.children)) {
    const miHijo = tenantDb.children.find(c => c.id === 'child_user_1' || c.name.toLowerCase().includes('hijo'));
    const vicky = tenantDb.children.find(c => c.id === 'child_vicky' || c.name.toLowerCase().includes('vicky'));
    
    // Si Vicky no tiene sus aplicaciones asignadas o Mi Hijo tiene las 147 apps de ColorOS/OPPO:
    if (vicky && (!vicky.monitoredApps || vicky.monitoredApps.length === 0)) {
      const allApps = tenantDb.monitoredApps || miHijo?.monitoredApps || [];
      const oppoApps = allApps.filter(a => a.packageName && (a.packageName.includes('coloros') || a.packageName.includes('oppo') || a.childId === 'child_vicky'));
      if (oppoApps.length > 0 || allApps.length > 50) {
        // Asignar todas las apps de OPPO a Vicky
        vicky.monitoredApps = allApps.map(a => ({ ...a, childId: 'child_vicky' }));
        // Si Mi Hijo tiene las 147 apps, restaurar a sus 35 apps nativas
        if (miHijo && miHijo.monitoredApps && miHijo.monitoredApps.length > 50) {
          miHijo.monitoredApps = miHijo.monitoredApps.filter(a => !a.packageName.includes('coloros') && !a.packageName.includes('oppo'));
        }
        saveDatabase(db);
      }
    }
  }

  return {
    client: matchedClient,
    tenantDb,
    session
  };
}

// Helper para respuestas JSON
function sendJson(res, statusCode, data) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, x-client-id'
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
        resolve({});
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
  '.ico': 'image/x-icon',
  '.apk': 'application/vnd.android.package-archive'
};

const server = http.createServer(async (req, res) => {
  const reqUrl = new URL(req.url, `http://${req.headers.host}`);
  const pathname = reqUrl.pathname;

  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, x-client-id'
    });
    return res.end();
  }

  // --- API ROUTING ---
  if (pathname.startsWith('/api/')) {

    // ==========================================
    // 0. AUTHENTICATION & MULTI-TENANT ENDPOINTS
    // ==========================================

    // Login
    if (pathname === '/api/auth/login' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const email = (body.email || '').trim().toLowerCase();
      const password = (body.password || '').trim();

      // Verificar Super Admin
      if (email === db.admin.email.toLowerCase() && password === db.admin.password) {
        const token = 'adm_' + Date.now() + '_' + Math.random().toString(36).substring(2);
        setSession(token, { role: 'admin', email: db.admin.email });
        return sendJson(res, 200, {
          success: true,
          token,
          role: 'admin',
          user: { name: 'Super Administrador', email: db.admin.email, role: 'admin' }
        });
      }

      // Verificar Cliente Padre de Familia
      const client = (db.clients || []).find(c => c.email.toLowerCase() === email && c.password === password);
      if (client) {
        if (client.status === 'suspended') {
          return sendJson(res, 403, {
            success: false,
            error: 'Esta cuenta familiar se encuentra suspendida. Contacta a tu proveedor para reactivarla.'
          });
        }

        const isExpired = client.expiresAt && new Date(client.expiresAt) < new Date();
        if (isExpired) {
          return sendJson(res, 403, {
            success: false,
            error: 'Tu plan de suscripción ha expirado. Contacta a tu proveedor para renovar.'
          });
        }

        const token = 'cli_' + Date.now() + '_' + Math.random().toString(36).substring(2);
        setSession(token, { role: 'client', clientId: client.id, email: client.email });
        return sendJson(res, 200, {
          success: true,
          token,
          role: 'client',
          user: {
            id: client.id,
            name: client.name,
            email: client.email,
            phone: client.phone,
            plan: client.plan,
            pairingCode: client.pairingCode,
            expiresAt: client.expiresAt,
            role: 'client'
          }
        });
      }

      return sendJson(res, 401, { success: false, error: 'Correo o contraseña incorrectos' });
    }

    // Me (Obtener sesión actual)
    if (pathname === '/api/auth/me' && req.method === 'GET') {
      let authHeader = req.headers['authorization'] || '';
      let token = authHeader.startsWith('Bearer ') ? authHeader.substring(7).trim() : reqUrl.searchParams.get('token');
      const sess = getSession(token);
      if (!sess) return sendJson(res, 401, { error: 'No autenticado' });

      if (sess.role === 'admin') {
        return sendJson(res, 200, {
          success: true,
          role: 'admin',
          user: { name: 'Super Administrador', email: db.admin.email, role: 'admin' }
        });
      }

      const client = (db.clients || []).find(c => c.id === sess.clientId);
      if (!client) return sendJson(res, 404, { error: 'Cliente no encontrado' });

      return sendJson(res, 200, {
        success: true,
        role: 'client',
        user: {
          id: client.id,
          name: client.name,
          email: client.email,
          phone: client.phone,
          plan: client.plan,
          pairingCode: client.pairingCode,
          expiresAt: client.expiresAt,
          status: client.status,
          role: 'client'
        }
      });
    }

    // Logout
    if (pathname === '/api/auth/logout' && req.method === 'POST') {
      let authHeader = req.headers['authorization'] || '';
      let token = authHeader.startsWith('Bearer ') ? authHeader.substring(7).trim() : reqUrl.searchParams.get('token');
      if (token) deleteSession(token);
      return sendJson(res, 200, { success: true });
    }

    // ==========================================
    // ADMIN ONLY ENDPOINTS (GESTIÓN DE ALQUILER)
    // ==========================================

    // Listar clientes para el Super Admin
    if (pathname === '/api/admin/clients' && req.method === 'GET') {
      let authHeader = req.headers['authorization'] || '';
      let token = authHeader.startsWith('Bearer ') ? authHeader.substring(7).trim() : reqUrl.searchParams.get('token');
      const sess = getSession(token);
      if (!sess || sess.role !== 'admin') {
        return sendJson(res, 403, { error: 'Acceso exclusivo de administrador' });
      }

      const clientsSummary = (db.clients || []).map(c => {
        const data = c.data || {};
        const children = data.children || [];
        const devicesCount = children.reduce((acc, ch) => acc + (ch.devices?.length || 0), 0);
        return {
          id: c.id,
          name: c.name,
          email: c.email,
          password: c.password,
          phone: c.phone,
          plan: c.plan,
          status: c.status,
          pairingCode: c.pairingCode,
          createdAt: c.createdAt,
          expiresAt: c.expiresAt,
          childrenCount: children.length,
          devicesCount: devicesCount
        };
      });

      return sendJson(res, 200, { success: true, clients: clientsSummary });
    }

    // Crear nuevo cliente de alquiler
    if (pathname === '/api/admin/clients/create' && req.method === 'POST') {
      let authHeader = req.headers['authorization'] || '';
      let token = authHeader.startsWith('Bearer ') ? authHeader.substring(7).trim() : reqUrl.searchParams.get('token');
      const sess = getSession(token);
      if (!sess || sess.role !== 'admin') {
        return sendJson(res, 403, { error: 'Acceso exclusivo de administrador' });
      }

      const body = await parseRequestBody(req);
      const name = (body.name || '').trim();
      const email = (body.email || '').trim().toLowerCase();
      const password = (body.password || '').trim();
      const phone = (body.phone || '').trim();
      const plan = body.plan || '1 mes';
      const childName = (body.childName || 'Mi Hijo').trim();
      const days = parseInt(body.days) || 30;

      if (!name || !email || !password) {
        return sendJson(res, 400, { error: 'Nombre, correo y contraseña son obligatorios' });
      }

      const existing = (db.clients || []).find(c => c.email.toLowerCase() === email);
      if (existing) {
        return sendJson(res, 400, { error: 'Ya existe un cliente registrado con ese correo electrónico' });
      }

      // Generar código único de 6 dígitos
      let pairingCode = '';
      for (let i = 0; i < 10; i++) {
        const candidate = String(Math.floor(100000 + Math.random() * 900000));
        if (!db.clients.some(c => c.pairingCode === candidate)) {
          pairingCode = candidate;
          break;
        }
      }
      if (!pairingCode) pairingCode = String(Date.now()).slice(-6);

      const clientId = 'client_' + Date.now();
      const newClient = {
        id: clientId,
        name,
        email,
        password,
        phone,
        plan,
        status: 'active',
        pairingCode,
        createdAt: new Date().toISOString(),
        expiresAt: new Date(Date.now() + days * 24 * 3600 * 1000).toISOString(),
        data: createDefaultFamilyData(childName)
      };

      db.clients.push(newClient);
      saveDatabase(db);

      console.log(`[SAAS ADMIN] Nuevo cliente creado: ${name} (${email}) - Código: ${pairingCode}`);
      return sendJson(res, 200, { success: true, client: newClient });
    }

    // Cambiar estado (Activar / Suspender)
    if (pathname === '/api/admin/clients/toggle-status' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const client = (db.clients || []).find(c => c.id === body.clientId);
      if (!client) return sendJson(res, 404, { error: 'Cliente no encontrado' });

      client.status = body.status === 'suspended' ? 'suspended' : 'active';
      saveDatabase(db);
      console.log(`[SAAS ADMIN] Cliente ${client.name} ahora está: ${client.status}`);
      return sendJson(res, 200, { success: true, client });
    }

    // Renovar cliente (+días)
    if (pathname === '/api/admin/clients/renew' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const client = (db.clients || []).find(c => c.id === body.clientId);
      if (!client) return sendJson(res, 404, { error: 'Cliente no encontrado' });

      const addDays = parseInt(body.addDays) || 30;
      let currentExp = client.expiresAt ? new Date(client.expiresAt).getTime() : Date.now();
      if (currentExp < Date.now()) currentExp = Date.now();

      client.expiresAt = new Date(currentExp + addDays * 24 * 3600 * 1000).toISOString();
      client.status = 'active';
      saveDatabase(db);

      console.log(`[SAAS ADMIN] Cliente ${client.name} renovado por ${addDays} días hasta: ${client.expiresAt}`);
      return sendJson(res, 200, { success: true, expiresAt: client.expiresAt });
    }

    // Eliminar cliente
    if (pathname === '/api/admin/clients/delete' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      if (db.clients.length <= 1) {
        return sendJson(res, 400, { error: 'No puedes eliminar la única cuenta de cliente existente.' });
      }
      db.clients = db.clients.filter(c => c.id !== body.clientId);
      saveDatabase(db);
      return sendJson(res, 200, { success: true });
    }

    // ==========================================
    // 1. DATA Y TELEMETRÍA (AISLADO POR CLIENTE)
    // ==========================================

    // Lista de hijos de la familia para vinculación rápida en Companion APK
    if (pathname === '/api/family/children' && req.method === 'GET') {
      const { client, tenantDb, invalidCode } = resolveTenant(req, reqUrl);
      if (invalidCode || !tenantDb) {
        return sendJson(res, 404, {
          success: false,
          error: 'Código de vinculación inválido o familia no encontrada',
          children: []
        });
      }
      return sendJson(res, 200, {
        success: true,
        familyName: client ? client.name : 'Familia',
        children: (tenantDb.children || []).map(c => ({
          id: c.id,
          name: c.name,
          age: c.age,
          avatar: c.avatar,
          instantLockActive: !!c.instantLockActive
        }))
      });
    }

    // Actualizar o cambiar código PIN de vinculación familiar
    if (pathname === '/api/family/update-code' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { client, tenantDb } = resolveTenant(req, reqUrl, body);
      const newCode = (body.pairingCode || '').trim();

      if (!newCode || newCode.length < 4 || newCode.length > 8 || !/^\d+$/.test(newCode)) {
        return sendJson(res, 400, { success: false, error: 'El código debe tener entre 4 y 8 dígitos numéricos.' });
      }

      if (Array.isArray(db.clients)) {
        const inUse = db.clients.find(c => c.pairingCode === newCode && (!client || c.id !== client.id));
        if (inUse) {
          return sendJson(res, 400, { success: false, error: 'Este código ya pertenece a otra familia. Elige uno diferente.' });
        }
      }

      if (client) {
        client.pairingCode = newCode;
      }
      db.pairingCode = newCode;
      saveDatabase(db);

      console.log(`[FAMILIA] PIN de vinculación actualizado a: ${newCode} para: ${client ? client.name : 'default'}`);
      return sendJson(res, 200, { success: true, pairingCode: newCode });
    }

    // Obtener estado completo de la familia
    if (pathname === '/api/data' && req.method === 'GET') {
      const { client, tenantDb, invalidCode } = resolveTenant(req, reqUrl);
      if (invalidCode || !tenantDb) {
        return sendJson(res, 401, {
          success: false,
          error: 'Código familiar inválido o familia no encontrada',
          instantLockActive: false,
          children: []
        });
      }
      const isHttps = req.headers['x-forwarded-proto'] === 'https' || (process.env.PUBLIC_URL && process.env.PUBLIC_URL.startsWith('https'));
      const protocol = isHttps ? 'https' : 'http';
      const host = process.env.PUBLIC_URL
        ? process.env.PUBLIC_URL.replace(/^https?:\/\//, '').replace(/\/$/, '')
        : (req.headers.host || `${LOCAL_IP}:${PORT}`);
      const baseUrl = `${protocol}://${host}`;

      const targetChildId = reqUrl.searchParams.get('childId');
      const isApkRequest = reqUrl.searchParams.has('code');
      const deviceParam = reqUrl.searchParams.get('device');
      
      let activeChild = null;
      if (targetChildId) {
        activeChild = (tenantDb.children || []).find(c => c.id === targetChildId || (targetChildId === 'child_1' && c.id === 'child_user_1'));
      }

      // Si el teléfono envía su modelo de dispositivo real (ej: "OPPO CPH2743" o "incar CP20S")
      if (deviceParam) {
        let foundByDevice = (tenantDb.children || []).find(c => 
          (c.devices || []).some(d => d.name && (
            d.name.toLowerCase().trim() === deviceParam.toLowerCase().trim() ||
            deviceParam.toLowerCase().includes(d.name.toLowerCase().trim()) ||
            d.name.toLowerCase().includes(deviceParam.toLowerCase().trim())
          ))
        );
        // Si es el teléfono OPPO de la hija
        if (!foundByDevice && (deviceParam.includes('CPH2743') || deviceParam.toLowerCase().includes('oppo'))) {
          foundByDevice = (tenantDb.children || []).find(c => c.id === 'child_vicky' || c.name.toLowerCase().includes('vicky'));
        }
        // Si es el CP20S de Mi Hijo
        if (!foundByDevice && (deviceParam.includes('CP20S') || deviceParam.toLowerCase().includes('incar'))) {
          foundByDevice = (tenantDb.children || []).find(c => c.id === 'child_user_1' || c.name.toLowerCase().includes('hijo'));
        }
        if (foundByDevice) {
          activeChild = foundByDevice;
        }
      }
      
      if (!activeChild) {
        if (isApkRequest) {
          // Solicitudes del celular APK: SOLO auto-asignar si la familia tiene 1 único hijo
          if ((tenantDb.children || []).length === 1) {
            activeChild = (tenantDb.children || [])[0];
          } else {
            activeChild = null;
          }
        } else {
          // Solicitudes de la web de los padres: usar el hijo que están viendo en pantalla
          activeChild = (tenantDb.children || []).find(c => c.id === tenantDb.activeChildId) || (tenantDb.children || [])[0];
        }
      }

      // Calcular aplicaciones aplicables para este hijo evaluado
      let childAppsForRules = [];
      if (activeChild && Array.isArray(activeChild.monitoredApps) && activeChild.monitoredApps.length > 0) {
        childAppsForRules = activeChild.monitoredApps;
      } else if (Array.isArray(tenantDb.monitoredApps)) {
        childAppsForRules = tenantDb.monitoredApps.filter(a => !a.childId || (activeChild && a.childId === activeChild.id));
      }

      const blockedPackages = childAppsForRules
        .filter(a => a.action === 'blocked')
        .map(a => a.packageName);

      const blockedDomains = (tenantDb.customWebRules || [])
        .filter(r => r.action === 'blocked')
        .map(r => r.domain.toLowerCase().trim());

      const adultCategory = (tenantDb.webCategories || []).find(c => c.id === 'cat_adult');
      const adultBlocked = adultCategory ? (adultCategory.action === 'blocked') : true;

      const blockedKeywords = [];
      (tenantDb.webCategories || []).forEach(cat => {
        if (cat.action === 'blocked' && Array.isArray(cat.keywords)) {
          blockedKeywords.push(...cat.keywords);
        }
      });

      // Garantizar versión monotónica de política
      tenantDb.policyVersion = tenantDb.policyVersion || 1;

      // El estado instantLockActive en la raíz de la respuesta responde únicamente al hijo evaluado
      const instantLockActive = activeChild ? !!activeChild.instantLockActive : false;

      // AISLAMIENTO ESTRICTO PARA EL CELULAR APK:
      // El teléfono del hijo NUNCA debe recibir información ni estados de bloqueo de otros hermanos.
      if (isApkRequest) {
        if (!activeChild) {
          // Si no se pasó device o childId específico, devolver las aplicaciones bloqueadas globales de la familia
          const fallbackBlocked = [];
          (tenantDb.children || []).forEach(c => {
            (c.monitoredApps || []).forEach(a => {
              if (a.action === 'blocked' && a.packageName && !fallbackBlocked.includes(a.packageName)) {
                fallbackBlocked.push(a.packageName);
              }
            });
          });
          (tenantDb.monitoredApps || []).forEach(a => {
            if (a.action === 'blocked' && a.packageName && !fallbackBlocked.includes(a.packageName)) {
              fallbackBlocked.push(a.packageName);
            }
          });

          return sendJson(res, 200, {
            ...tenantDb,
            instantLockActive: false,
            children: [],
            unassigned: true,
            blockedPackages: fallbackBlocked,
            blockedDomains,
            blockedKeywords,
            adultBlocked
          });
        }

        const isolatedChild = {
          ...activeChild,
          instantLockActive: !!activeChild.instantLockActive
        };
        return sendJson(res, 200, {
          ...tenantDb,
          activeChildId: activeChild.id,
          instantLockActive: !!activeChild.instantLockActive,
          children: [isolatedChild],
          monitoredApps: childAppsForRules,
          blockedPackages,
          blockedDomains,
          blockedKeywords,
          adultBlocked,
          clientInfo: client ? {
            id: client.id,
            name: client.name,
            pairingCode: client.pairingCode
          } : null,
          serverNetwork: {
            localIp: LOCAL_IP,
            port: PORT,
            mobileChildUrl: `${baseUrl}/child?code=${client?.pairingCode || ''}`,
            parentUrl: baseUrl
          }
        });
      }

      return sendJson(res, 200, {
        ...tenantDb,
        instantLockActive,
        blockedPackages,
        blockedDomains,
        blockedKeywords,
        adultBlocked,
        clientInfo: client ? {
          id: client.id,
          name: client.name,
          email: client.email,
          phone: client.phone,
          plan: client.plan,
          status: client.status,
          pairingCode: client.pairingCode,
          expiresAt: client.expiresAt
        } : null,
        serverNetwork: {
          localIp: LOCAL_IP,
          port: PORT,
          mobileChildUrl: `${baseUrl}/child?code=${client?.pairingCode || ''}`,
          parentUrl: baseUrl
        }
      });
    }

    // Telemetría y Registro en vivo del Celular Real
    if (pathname === '/api/child/telemetry' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { client, tenantDb } = resolveTenant(req, reqUrl, body);
      
      const targetChildId = body.childId || reqUrl.searchParams.get('childId');
      let child = null;

      // Si la telemetría proviene del teléfono OPPO, vincular directamente al perfil de Vicky
      if (body.deviceName && (body.deviceName.includes('CPH2743') || body.deviceName.toLowerCase().includes('oppo'))) {
        child = (tenantDb.children || []).find(c => c.id === 'child_vicky' || c.name.toLowerCase().includes('vicky'));
      } else if (targetChildId) {
        child = (tenantDb.children || []).find(c => c.id === targetChildId || (targetChildId === 'child_1' && c.id === 'child_user_1'));
      }
      if (!child) {
        child = (tenantDb.children || []).find(c => c.id === 'child_user_1') || (tenantDb.children || [])[0];
      }
      
      if (child) {
        if (!child.devices) child.devices = [];

        let dev = child.devices.find(d => d.type === 'smartphone');
        
        // Solo registrar un nuevo dispositivo si la telemetría proviene de un teléfono real
        const isFromRealApk = body.deviceName && body.deviceName !== 'Smartphone Android';
        if (!dev && isFromRealApk) {
          dev = {
            id: 'real_phone_' + Date.now(),
            name: body.deviceName,
            type: 'smartphone',
            os: body.os || 'Android',
            batteryLevel: (body.batteryLevel !== undefined && body.batteryLevel !== null) ? Math.round(body.batteryLevel) : 85,
            isOnline: true,
            lastSeen: 'Ahora mismo'
          };
          child.devices = [dev];
        } else if (dev) {
          if (body.deviceName && isFromRealApk) dev.name = body.deviceName;
          if (body.os) dev.os = body.os;
          if (body.batteryLevel !== undefined && body.batteryLevel !== null) dev.batteryLevel = Math.round(body.batteryLevel);
          dev.isOnline = true;
          dev.lastSeen = 'Ahora mismo';
          child.devices = [dev];
        }

        if (body.lat !== undefined && body.lng !== undefined) {
          child.currentLocation = {
            ...child.currentLocation,
            lat: body.lat,
            lng: body.lng,
            address: body.address || child.currentLocation?.address || `GPS: ${body.lat.toFixed(5)}, ${body.lng.toFixed(5)}`,
            updatedAt: 'Ahora mismo (GPS en vivo)',
            battery: (body.batteryLevel !== undefined && body.batteryLevel !== null) ? Math.round(body.batteryLevel) : (child.currentLocation?.battery || 85),
            zone: 'Ubicación GPS'
          };

          if (tenantDb.geofences && tenantDb.geofences.length > 0) {
            tenantDb.geofences[0].lat = body.lat;
            tenantDb.geofences[0].lng = body.lng;
            if (body.address) tenantDb.geofences[0].address = body.address;
          }
        }
        saveDatabase(db);
        return sendJson(res, 200, { success: true, instantLockActive: child.instantLockActive, device: dev, family: client?.name });
      }
      return sendJson(res, 404, { error: 'Hijo no encontrado' });
    }

    // Detección automática de ubicación por IP pública
    if (pathname === '/api/ip-location' && req.method === 'GET') {
      try {
        let clientIp = req.headers['x-forwarded-for'] || req.socket.remoteAddress || '';
        if (clientIp.includes(',')) clientIp = clientIp.split(',')[0].trim();
        clientIp = clientIp.replace(/^::ffff:/, '');
        
        const isPrivate = !clientIp || clientIp === '127.0.0.1' || clientIp === '::1' || clientIp.startsWith('192.168.') || clientIp.startsWith('10.') || clientIp.startsWith('172.');
        const ipQueryUrl = isPrivate ? 'https://ipwho.is/' : `https://ipwho.is/${clientIp}`;

        const fetchRes = await fetch(ipQueryUrl, { signal: AbortSignal.timeout(4000) });
        if (fetchRes.ok) {
          const ipData = await fetchRes.json();
          if (ipData.success && ipData.latitude && ipData.longitude) {
            return sendJson(res, 200, {
              success: true,
              lat: ipData.latitude,
              lng: ipData.longitude,
              city: ipData.city || '',
              region: ipData.region || '',
              country: ipData.country || '',
              address: `${ipData.city || ''}, ${ipData.region || ''}, ${ipData.country || ''}`.replace(/^, |, $/g, '')
            });
          }
        }
      } catch (e) {
        console.error('Error fetching ip-location:', e.message);
      }
      return sendJson(res, 200, { success: false, lat: 10.3982, lng: -75.4933, address: 'Cartagena, Colombia' });
    }

    // Crear o editar hijo
    if (pathname === '/api/children/create' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { client, tenantDb } = resolveTenant(req, reqUrl, body);
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
          lat: 10.3982,
          lng: -75.4933,
          address: 'Esperando primera sincronización GPS...',
          battery: 100,
          updatedAt: 'Pendiente',
          zone: 'Sin registrar'
        }
      };
      if (!tenantDb.children) tenantDb.children = [];
      tenantDb.children.push(newChild);
      tenantDb.activeChildId = newChild.id;

      if (!db.children) db.children = [];
      if (!db.children.find(c => c.id === newChild.id)) {
        db.children.push(newChild);
      }
      db.activeChildId = newChild.id;

      saveDatabase(db);
      console.log(`[HIJOS] Nuevo hijo registrado: ${newChild.name} (${newChild.age}a) para: ${client ? client.name : 'default'}`);
      return sendJson(res, 200, { success: true, child: newChild, children: tenantDb.children });
    }

    // Editar perfil de hijo existente (Nombre, edad, avatar)
    if (pathname === '/api/children/update' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      if (!tenantDb) return sendJson(res, 401, { error: 'No autorizado o familia no encontrada' });

      const childId = body.childId || body.id;
      const child = (tenantDb.children || []).find(c => c.id === childId);
      if (!child) return sendJson(res, 404, { error: 'Hijo no encontrado' });

      if (body.name) child.name = body.name.trim();
      if (body.age !== undefined) child.age = Number(body.age);
      if (body.avatar) child.avatar = body.avatar;

      if (Array.isArray(db.children)) {
        const rootChild = db.children.find(c => c.id === child.id);
        if (rootChild) {
          if (body.name) rootChild.name = child.name;
          if (body.age !== undefined) rootChild.age = child.age;
          if (body.avatar) rootChild.avatar = child.avatar;
        }
      }

      saveDatabase(db);
      console.log(`[HIJOS] Perfil actualizado: ${child.name} (${child.age}a) para ${child.id}`);
      return sendJson(res, 200, { success: true, child, children: tenantDb.children });
    }

    // Limpiar todos los datos de ejemplo (Reset a limpio)
    if (pathname === '/api/children/clean-slate' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
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
        devices: [],
        currentLocation: {
          lat: 10.3982,
          lng: -75.4933,
          address: 'Conecta tu celular para ver la ubicación GPS en tiempo real',
          battery: 100,
          updatedAt: 'Esperando dispositivo...',
          zone: 'En espera'
        }
      };

      tenantDb.children = [realChild];
      tenantDb.activeChildId = realChild.id;
      tenantDb.requests = [];
      tenantDb.activityLogs = [
        {
          id: 'log_init',
          childId: realChild.id,
          type: 'info',
          title: 'Sistema Iniciado y Listo',
          details: `Perfil de ${realChild.name} configurado.`,
          status: 'info',
          timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
        }
      ];

      saveDatabase(db);
      return sendJson(res, 200, { success: true, child: realChild, db: tenantDb });
    }

    // Seleccionar hijo activo
    if (pathname === '/api/children/select' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      const child = (tenantDb.children || []).find(c => c.id === body.childId);
      if (child) {
        tenantDb.activeChildId = body.childId;
        db.activeChildId = body.childId;
        saveDatabase(db);
        return sendJson(res, 200, { success: true, activeChildId: tenantDb.activeChildId, child });
      }
      return sendJson(res, 404, { error: 'Hijo no encontrado' });
    }

    // Desvincular / Eliminar dispositivo de un hijo
    if (pathname === '/api/device/unlink' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      if (!tenantDb) {
        return sendJson(res, 401, { error: 'No autorizado o familia no encontrada' });
      }
      const childId = body.childId || tenantDb.activeChildId;
      const child = (tenantDb.children || []).find(c => c.id === childId);
      if (!child) {
        return sendJson(res, 404, { error: 'Hijo no encontrado' });
      }

      const deviceId = body.deviceId;
      if (deviceId) {
        child.devices = (child.devices || []).filter(d => d.id !== deviceId);
      } else {
        child.devices = [];
      }

      // Reiniciar estado asociado al dispositivo eliminado
      if (!child.devices || child.devices.length === 0) {
        child.devices = [];
        child.monitoredApps = [];
        child.currentLocation = null;
        child.instantLockActive = false;
        if (Array.isArray(tenantDb.monitoredApps)) {
          tenantDb.monitoredApps = tenantDb.monitoredApps.filter(a => a.childId !== child.id);
        }
      }

      if (Array.isArray(db.children)) {
        const rootChild = db.children.find(c => c.id === child.id);
        if (rootChild) {
          rootChild.devices = child.devices;
          rootChild.monitoredApps = child.monitoredApps;
          rootChild.currentLocation = child.currentLocation;
          rootChild.instantLockActive = child.instantLockActive;
        }
      }

      const logEntry = {
        id: 'log_' + Date.now(),
        childId: child.id,
        type: 'device',
        title: '📱 Dispositivo Desvinculado',
        details: `Se desvinculó y eliminó el dispositivo del perfil de ${child.name}.`,
        status: 'warning',
        timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
      };
      if (!tenantDb.activityLogs) tenantDb.activityLogs = [];
      tenantDb.activityLogs.unshift(logEntry);

      saveDatabase(db);
      console.log(`[DISPOSITIVO] Dispositivo desvinculado con éxito para ${child.name} (${child.id})`);
      return sendJson(res, 200, { success: true, message: 'Dispositivo desvinculado con éxito', child, log: logEntry });
    }

    // Reasignar dispositivo de un hijo a otro
    if (pathname === '/api/device/reassign' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      if (!tenantDb) return sendJson(res, 401, { error: 'No autorizado o familia no encontrada' });

      const fromChild = (tenantDb.children || []).find(c => c.id === body.fromChildId);
      const toChild = (tenantDb.children || []).find(c => c.id === body.toChildId);
      if (!fromChild || !toChild) return sendJson(res, 404, { error: 'Hijos no encontrados' });

      const devName = body.deviceName;
      let movedDev = null;
      if (fromChild.devices) {
        const devIdx = fromChild.devices.findIndex(d => !devName || d.name === devName || d.id === body.deviceId);
        if (devIdx !== -1) {
          movedDev = fromChild.devices.splice(devIdx, 1)[0];
        }
      }
      if (!movedDev) {
        movedDev = {
          id: 'real_phone_' + Date.now(),
          name: devName || 'Smartphone Android',
          type: 'smartphone',
          os: 'Android',
          batteryLevel: 90,
          isOnline: true,
          lastSeen: 'Ahora mismo'
        };
      }

      if (!toChild.devices) toChild.devices = [];
      toChild.devices = [movedDev];

      // Mover también las aplicaciones sincronizadas si pertenecen a este dispositivo
      if (Array.isArray(tenantDb.monitoredApps)) {
        tenantDb.monitoredApps.forEach(a => {
          if (a.childId === fromChild.id) {
            a.childId = toChild.id;
          }
        });
      }
      if (Array.isArray(fromChild.monitoredApps)) {
        toChild.monitoredApps = fromChild.monitoredApps;
        fromChild.monitoredApps = [];
      }

      saveDatabase(db);
      console.log(`[REASIGNAR] Dispositivo ${movedDev.name} movido de ${fromChild.name} a ${toChild.name}`);
      return sendJson(res, 200, { success: true, movedDev, fromChild, toChild });
    }

    // Eliminar perfil de hijo
    if (pathname === '/api/children/delete' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      if (!tenantDb) {
        return sendJson(res, 401, { error: 'No autorizado o familia no encontrada' });
      }
      const childId = body.childId;
      if (!childId) {
        return sendJson(res, 400, { error: 'Se requiere el ID del hijo a eliminar' });
      }

      if ((tenantDb.children || []).length <= 1) {
        return sendJson(res, 400, { error: 'No puedes eliminar el único hijo registrado. La familia debe tener al menos un perfil.' });
      }

      const deletedChild = (tenantDb.children || []).find(c => c.id === childId);
      tenantDb.children = (tenantDb.children || []).filter(c => c.id !== childId);
      if (Array.isArray(db.children)) {
        db.children = db.children.filter(c => c.id !== childId);
      }

      if (Array.isArray(tenantDb.monitoredApps)) {
        tenantDb.monitoredApps = tenantDb.monitoredApps.filter(a => a.childId !== childId);
      }
      if (Array.isArray(tenantDb.requests)) {
        tenantDb.requests = tenantDb.requests.filter(r => r.childId !== childId);
      }
      if (Array.isArray(tenantDb.activityLogs)) {
        tenantDb.activityLogs = tenantDb.activityLogs.filter(l => l.childId !== childId);
      }

      if (tenantDb.activeChildId === childId) {
        tenantDb.activeChildId = tenantDb.children[0].id;
        db.activeChildId = tenantDb.children[0].id;
      }

      saveDatabase(db);
      console.log(`[HIJOS] Perfil ${childId} (${deletedChild ? deletedChild.name : ''}) eliminado con éxito.`);
      return sendJson(res, 200, {
        success: true,
        message: 'Perfil de hijo eliminado con éxito',
        activeChildId: tenantDb.activeChildId,
        children: tenantDb.children
      });
    }

    // Pausa instantánea / Bloqueo de dispositivo
    if (pathname === '/api/instant-lock' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      if (!tenantDb) {
        return sendJson(res, 401, { error: 'No autorizado o familia no encontrada' });
      }
      const lockAll = !!body.all;

      if (lockAll) {
        const anyUnlocked = (tenantDb.children || []).some(c => !c.instantLockActive);
        const targetState = body.locked !== undefined ? body.locked : anyUnlocked;
        (tenantDb.children || []).forEach(c => { c.instantLockActive = targetState; });

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
        if (!tenantDb.activityLogs) tenantDb.activityLogs = [];
        tenantDb.activityLogs.unshift(logEntry);
        bumpPolicyVersion(tenantDb);
        saveDatabase(db);
        return sendJson(res, 200, { success: true, instantLockActive: targetState, allLocked: targetState, policyVersion: tenantDb.policyVersion, log: logEntry });
      }

      const childId = body.childId || tenantDb.activeChildId || 'child_user_1';
      let child = (tenantDb.children || []).find(c => c.id === childId);
      if (!child && (tenantDb.children || []).length > 0) {
        child = tenantDb.children[0];
      }
      if (!child) return sendJson(res, 404, { error: 'Hijo no encontrado' });

      // Modificar ESTRICTAMENTE el perfil de este hijo específico
      child.instantLockActive = body.locked !== undefined ? !!body.locked : !child.instantLockActive;
      if (Array.isArray(db.children)) {
        const rootChild = db.children.find(c => c.id === child.id);
        if (rootChild) rootChild.instantLockActive = child.instantLockActive;
      }
      
      // Si el padre desbloquea al hijo, resolver automáticamente cualquier solicitud de desbloqueo pendiente
      if (!child.instantLockActive && Array.isArray(tenantDb.requests)) {
        tenantDb.requests.forEach(r => {
          if ((r.childId === child.id || (child.id === 'child_user_1' && r.childId === 'child_1') || !r.childId) && r.status === 'pending') {
            r.status = 'approved';
            r.respondedAt = new Date().toISOString();
          }
        });
      }

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
      if (!tenantDb.activityLogs) tenantDb.activityLogs = [];
      tenantDb.activityLogs.unshift(logEntry);

      bumpPolicyVersion(tenantDb);
      saveDatabase(db);
      console.log(`[BLOQUEO] Estado para ${child.name} (${child.id}): ${child.instantLockActive ? 'BLOQUEADO' : 'DESBLOQUEADO'} (v${tenantDb.policyVersion})`);
      return sendJson(res, 200, { success: true, childId: child.id, instantLockActive: child.instantLockActive, policyVersion: tenantDb.policyVersion, child, log: logEntry });
    }

    // Añadir tiempo extra
    if (pathname === '/api/time/add' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      const childId = body.childId || tenantDb.activeChildId;
      const child = (tenantDb.children || []).find(c => c.id === childId);
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
      if (!tenantDb.activityLogs) tenantDb.activityLogs = [];
      tenantDb.activityLogs.unshift(logEntry);

      saveDatabase(db);
      return sendJson(res, 200, { success: true, dailyLimitMinutes: child.dailyLimitMinutes, child });
    }

    // Límite diario exacto
    if (pathname === '/api/time/limit' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      const child = (tenantDb.children || []).find(c => c.id === (body.childId || tenantDb.activeChildId));
      if (!child) return sendJson(res, 404, { error: 'Hijo no encontrado' });

      child.dailyLimitMinutes = Number(body.dailyLimitMinutes);
      bumpPolicyVersion(tenantDb);
      saveDatabase(db);
      return sendJson(res, 200, { success: true, policyVersion: tenantDb.policyVersion, child });
    }

    // Horario escolar
    if (pathname === '/api/time/school' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      const child = (tenantDb.children || []).find(c => c.id === (body.childId || tenantDb.activeChildId));
      if (!child) return sendJson(res, 404, { error: 'Hijo no encontrado' });

      child.schoolTimeActive = body.active !== undefined ? body.active : !child.schoolTimeActive;
      bumpPolicyVersion(tenantDb);
      saveDatabase(db);
      return sendJson(res, 200, { success: true, schoolTimeActive: child.schoolTimeActive, policyVersion: tenantDb.policyVersion, child });
    }

    // Modificar regla de categoría web
    if (pathname === '/api/web/category' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      const category = (tenantDb.webCategories || []).find(cat => cat.id === body.categoryId);
      if (!category) return sendJson(res, 404, { error: 'Categoría no encontrada' });

      category.action = body.action;
      bumpPolicyVersion(tenantDb);
      saveDatabase(db);
      return sendJson(res, 200, { success: true, policyVersion: tenantDb.policyVersion, category });
    }

    // SafeSearch
    if (pathname === '/api/web/safesearch' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      const child = (tenantDb.children || []).find(c => c.id === (body.childId || tenantDb.activeChildId));
      if (!child) return sendJson(res, 404, { error: 'Hijo no encontrado' });

      child.safeSearchEnabled = body.enabled !== undefined ? body.enabled : !child.safeSearchEnabled;
      bumpPolicyVersion(tenantDb);
      saveDatabase(db);
      return sendJson(res, 200, { success: true, policyVersion: tenantDb.policyVersion, safeSearchEnabled: child.safeSearchEnabled });
    }

    // Regla personalizada web
    if (pathname === '/api/web/rule' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      if (!tenantDb) return sendJson(res, 401, { error: 'No autorizado' });
      if (!tenantDb.customWebRules) tenantDb.customWebRules = [];

      if (body.action === 'delete') {
        tenantDb.customWebRules = tenantDb.customWebRules.filter(r => r.id !== body.id);
      } else {
        const newRule = {
          id: 'rule_' + Date.now(),
          domain: body.domain.toLowerCase().trim(),
          action: body.policy || 'blocked',
          note: body.note || 'Regla personalizada por el tutor'
        };
        tenantDb.customWebRules.push(newRule);
      }
      bumpPolicyVersion(tenantDb);
      saveDatabase(db);
      return sendJson(res, 200, { success: true, policyVersion: tenantDb.policyVersion, customWebRules: tenantDb.customWebRules });
    }

    // Regla de app (Bloquear / Permitir / Limitar)
    if (pathname === '/api/apps/action' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      if (!tenantDb) return sendJson(res, 401, { error: 'No autorizado' });

      const targetChildId = body.childId || tenantDb.activeChildId;
      const targetChild = (tenantDb.children || []).find(c => c.id === targetChildId);

      let app = null;
      let childApp = null;
      // 1. Buscar prioritariamente en el hijo objetivo
      if (targetChild && Array.isArray(targetChild.monitoredApps)) {
        childApp = targetChild.monitoredApps.find(a => a.id === body.appId || (body.packageName && a.packageName === body.packageName));
        if (childApp) app = childApp;
      }
      // 2. Si no se encontró en el hijo, buscar en tenantDb.monitoredApps
      if (!app && Array.isArray(tenantDb.monitoredApps)) {
        app = tenantDb.monitoredApps.find(a => a.id === body.appId || (body.packageName && a.packageName === body.packageName));
      }
      // 3. Fallback: buscar en cualquier otro hijo
      if (!app) {
        (tenantDb.children || []).forEach(c => {
          if (!app && Array.isArray(c.monitoredApps)) {
            const found = c.monitoredApps.find(a => a.id === body.appId || (body.packageName && a.packageName === body.packageName));
            if (found) {
              app = found;
              childApp = found;
            }
          }
        });
      }

      if (!app) return sendJson(res, 404, { error: 'Aplicación no encontrada' });

      if (body.action) app.action = body.action;
      if (body.dailyLimitMinutes !== undefined) app.dailyLimitMinutes = Number(body.dailyLimitMinutes);

      if (childApp && childApp !== app) {
        childApp.action = app.action;
        childApp.dailyLimitMinutes = app.dailyLimitMinutes;
      }

      // Si el hijo objetivo tiene la app, actualizar su estado allí
      if (targetChild && Array.isArray(targetChild.monitoredApps)) {
        const cIdx = targetChild.monitoredApps.findIndex(a => a.id === app.id || a.packageName === app.packageName);
        if (cIdx !== -1) {
          targetChild.monitoredApps[cIdx].action = app.action;
          if (app.dailyLimitMinutes !== undefined) targetChild.monitoredApps[cIdx].dailyLimitMinutes = app.dailyLimitMinutes;
        }
      }

      // Mantener sincronizado tenantDb.monitoredApps para compatibilidad global
      if (!tenantDb.monitoredApps) tenantDb.monitoredApps = [];
      const rootIdx = tenantDb.monitoredApps.findIndex(a => a.id === app.id || a.packageName === app.packageName);
      if (rootIdx !== -1) {
        tenantDb.monitoredApps[rootIdx].action = app.action;
        if (app.dailyLimitMinutes !== undefined) tenantDb.monitoredApps[rootIdx].dailyLimitMinutes = app.dailyLimitMinutes;
      } else {
        tenantDb.monitoredApps.push({ ...app, childId: targetChild ? targetChild.id : undefined });
      }

      const logEntry = {
        id: 'log_' + Date.now(),
        childId: targetChild ? targetChild.id : (tenantDb.activeChildId || 'child_user_1'),
        type: 'app',
        title: body.action === 'blocked' ? `🚫 Aplicación Bloqueada: ${app.name}` : `Regla actualizada: ${app.name}`,
        details: `El padre ha configurado "${app.name}" como ${body.action.toUpperCase()}.`,
        status: body.action === 'blocked' ? 'blocked' : 'allowed',
        timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
      };
      if (!tenantDb.activityLogs) tenantDb.activityLogs = [];
      tenantDb.activityLogs.unshift(logEntry);

      bumpPolicyVersion(tenantDb);
      saveDatabase(db);
      return sendJson(res, 200, { success: true, app, policyVersion: tenantDb.policyVersion, log: logEntry });
    }

    // Confirmación de versión de política aplicada por el dispositivo hijo (ACK)
    if (pathname === '/api/child/policy-ack' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      if (!tenantDb) return sendJson(res, 401, { error: 'No autorizado' });

      const childId = body.childId || tenantDb.activeChildId || 'child_user_1';
      const deviceId = body.deviceId || body.device || 'unknown';
      const appliedVersion = Number(body.policyVersion || body.appliedVersion) || 1;

      if (!tenantDb.policyAcks) tenantDb.policyAcks = {};
      tenantDb.policyAcks[deviceId] = {
        childId,
        policyVersion: appliedVersion,
        ackedAt: new Date().toISOString()
      };

      saveDatabase(db);
      return sendJson(res, 200, { success: true, confirmedVersion: appliedVersion });
    }

    // Sincronizar aplicaciones reales instaladas desde el teléfono del hijo
    if (pathname === '/api/child/installed-apps' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      const incomingApps = body.apps || [];
      if (!Array.isArray(incomingApps) || incomingApps.length === 0) {
        return sendJson(res, 400, { error: 'Lista de aplicaciones vacía' });
      }

      // Encontrar al hijo específico al que pertenecen estas apps
      const targetChildId = body.childId || reqUrl.searchParams.get('childId');
      let targetChild = null;

      // 1. Si el dispositivo reporta ser OPPO o CPH2743 -> pertenece a Vicky
      if (body.deviceName && (body.deviceName.includes('CPH2743') || body.deviceName.toLowerCase().includes('oppo'))) {
        targetChild = (tenantDb.children || []).find(c => c.id === 'child_vicky' || c.name.toLowerCase().includes('vicky'));
      }
      // 2. Si el dispositivo reporta ser CP20S o incar -> pertenece a Mi Hijo
      else if (body.deviceName && (body.deviceName.includes('CP20S') || body.deviceName.toLowerCase().includes('incar'))) {
        targetChild = (tenantDb.children || []).find(c => c.id === 'child_user_1' || c.name.toLowerCase().includes('hijo'));
      }
      // 3. Si viene childId explícito
      else if (targetChildId) {
        targetChild = (tenantDb.children || []).find(c => c.id === targetChildId);
      }
      // 4. Si el deviceName coincide con algún dispositivo ya vinculado a un hijo
      if (!targetChild && body.deviceName) {
        targetChild = (tenantDb.children || []).find(c => (c.devices || []).some(d => d.name && d.name.toLowerCase().trim() === body.deviceName.toLowerCase().trim()));
      }
      if (!targetChild) {
        targetChild = (tenantDb.children || []).find(c => c.devices && c.devices.length > 0) || (tenantDb.children || [])[0];
      }

      if (!tenantDb.monitoredApps) tenantDb.monitoredApps = [];
      const updatedList = [];
      const colors = ['bg-red-600 text-white', 'bg-blue-600 text-white', 'bg-emerald-600 text-white', 'bg-indigo-600 text-white', 'bg-amber-500 text-black', 'bg-purple-600 text-white'];

      incomingApps.forEach((incApp, idx) => {
        const existing = (targetChild?.monitoredApps || []).find(a => a.packageName === incApp.packageName) ||
                         tenantDb.monitoredApps.find(a => a.packageName === incApp.packageName && (!a.childId || a.childId === targetChild?.id));
        if (existing) {
          existing.name = incApp.name || existing.name;
          if (targetChild) existing.childId = targetChild.id;
          updatedList.push(existing);
        } else {
          updatedList.push({
            id: 'app_' + (targetChild?.id === 'child_vicky' ? 'vicky_' : '') + incApp.packageName.replace(/[^a-zA-Z0-9]/g, '_'),
            childId: targetChild ? targetChild.id : undefined,
            name: incApp.name,
            packageName: incApp.packageName,
            iconColor: colors[idx % colors.length],
            category: incApp.category || 'Aplicación Instalada',
            action: 'allowed',
            dailyLimitMinutes: 60,
            usedMinutesToday: 0
          });
        }
      });

      if (targetChild) {
        const prevCount = (targetChild.monitoredApps || []).length;
        targetChild.monitoredApps = updatedList;

        // Solo registrar en actividad si hubo cambio en la cantidad de aplicaciones
        if (prevCount === 0 || Math.abs(prevCount - updatedList.length) > 0) {
          const devName = body.deviceName || 'Smartphone Android';
          const logEntry = {
            id: 'log_' + Date.now(),
            childId: targetChild.id,
            type: 'device',
            title: '🎉 ¡Celular y Aplicaciones Sincronizadas!',
            details: `Se activaron ${updatedList.length} aplicaciones del celular ${devName} para el perfil de ${targetChild.name}.`,
            status: 'allowed',
            timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
          };
          if (!tenantDb.activityLogs) tenantDb.activityLogs = [];
          tenantDb.activityLogs.unshift(logEntry);
        }
      }

      saveDatabase(db);
      console.log(`[APPS] Sincronizadas ${updatedList.length} aplicaciones para ${targetChild ? targetChild.name : 'hijo'}.`);
      return sendJson(res, 200, { success: true, count: updatedList.length, childId: targetChild ? targetChild.id : undefined });
    }

    // Responder solicitud de un hijo
    if (pathname === '/api/requests/respond' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      const request = (tenantDb.requests || []).find(r => r.id === body.requestId);
      if (!request) return sendJson(res, 404, { error: 'Solicitud no encontrada' });

      request.status = body.decision;
      const child = (tenantDb.children || []).find(c => c.id === request.childId);

      if (body.decision === 'approved') {
        if (request.type === 'extra_time') {
          const added = body.extraMinutes || request.requestedMinutes || 30;
          if (child) {
            child.dailyLimitMinutes += added;
            child.instantLockActive = false;
          }
        } else if (request.type === 'unblock_website') {
          if (!tenantDb.customWebRules) tenantDb.customWebRules = [];
          tenantDb.customWebRules.push({
            id: 'rule_' + Date.now(),
            domain: request.target.toLowerCase().trim(),
            action: 'allowed',
            note: 'Aprobado a petición de ' + (child ? child.name : 'hijo')
          });
        }
      }

      if (!tenantDb.activityLogs) tenantDb.activityLogs = [];
      tenantDb.activityLogs.unshift({
        id: 'log_' + Date.now(),
        childId: request.childId,
        type: request.type === 'extra_time' ? 'time' : 'web',
        title: body.decision === 'approved' ? '✅ Solicitud Aprobada' : '❌ Solicitud Rechazada',
        details: `El padre ${body.decision === 'approved' ? 'aprobó' : 'rechazó'} la solicitud de "${request.target}": "${request.message}"`,
        status: body.decision === 'approved' ? 'allowed' : 'blocked',
        timestamp: 'Hoy, ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
      });

      saveDatabase(db);
      return sendJson(res, 200, { success: true, request, db: tenantDb });
    }

    // Enviar solicitud desde el celular del hijo
    if (pathname === '/api/child/request' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      const child = (tenantDb.children || []).find(c => c.id === body.childId || (body.childId === 'child_1' && c.id === 'child_user_1')) ||
                    (tenantDb.children || []).find(c => c.id === tenantDb.activeChildId) ||
                    (tenantDb.children || [])[0];
      if (!child) return sendJson(res, 404, { error: 'Hijo no encontrado' });

      if (!tenantDb.requests) tenantDb.requests = [];

      // Evitar saturar la base de datos si ya existe una solicitud idéntica pendiente en el último minuto
      const recentPending = tenantDb.requests.find(r => r.childId === child.id && r.status === 'pending');
      if (recentPending) {
        return sendJson(res, 200, { success: true, request: recentPending, note: 'Solicitud previa ya registrada' });
      }

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

      tenantDb.requests.unshift(newRequest);
      saveDatabase(db);
      return sendJson(res, 200, { success: true, request: newRequest });
    }

    // Navegación web en celular del hijo
    if (pathname === '/api/child/browse' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      const child = (tenantDb.children || []).find(c => c.id === (body.childId || tenantDb.activeChildId));
      const urlString = (body.url || '').toLowerCase().trim();

      if (child && child.instantLockActive) {
        return sendJson(res, 200, {
          allowed: false,
          reason: 'instant_lock',
          message: 'Pausa Instantánea Activa. El acceso a internet está temporalmente bloqueado por tus padres.'
        });
      }

      const customMatch = (tenantDb.customWebRules || []).find(r => urlString.includes(r.domain));
      if (customMatch) {
        if (customMatch.action === 'blocked') {
          if (!tenantDb.activityLogs) tenantDb.activityLogs = [];
          tenantDb.activityLogs.unshift({
            id: 'log_' + Date.now(),
            childId: child ? child.id : 'child_1',
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

      for (const cat of (tenantDb.webCategories || [])) {
        if (cat.keywords && cat.keywords.some(kw => urlString.includes(kw))) {
          if (cat.action === 'blocked') {
            if (!tenantDb.activityLogs) tenantDb.activityLogs = [];
            tenantDb.activityLogs.unshift({
              id: 'log_' + Date.now(),
              childId: child ? child.id : 'child_1',
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

      if (!tenantDb.activityLogs) tenantDb.activityLogs = [];
      tenantDb.activityLogs.unshift({
        id: 'log_' + Date.now(),
        childId: child ? child.id : 'child_1',
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

    // Notificación directa de bloqueo de sitio web o pornografía desde el agente nativo
    if (pathname === '/api/child/web-block' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      const child = (tenantDb.children || []).find(c => c.id === (body.childId || tenantDb.activeChildId)) || (tenantDb.children || [])[0];
      const blockedItem = body.item || body.domain || body.url || 'Sitio restringido';
      const reason = body.reason || 'Contenido para Adultos / Regla Bloqueada';
      const nowStr = new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });

      const log = {
        id: 'log_' + Date.now(),
        childId: child ? child.id : 'child_1',
        type: 'web',
        title: `🚫 BLOQUEO WEB: ${blockedItem}`,
        details: `Se interceptó y bloqueó un intento de acceso a "${blockedItem}" (${reason}) en el navegador del teléfono.`,
        status: 'blocked',
        timestamp: 'Hoy, ' + nowStr
      };

      if (!tenantDb.activityLogs) tenantDb.activityLogs = [];
      tenantDb.activityLogs.unshift(log);
      saveDatabase(db);
      console.log(`[BLOQUEO WEB NATIVO] Interceptado: ${blockedItem} (${reason})`);
      return sendJson(res, 200, { success: true, log });
    }

    // Notificación y Auditoría de Reinicio de Dispositivo
    if (pathname === '/api/child/reboot' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      const child = (tenantDb.children || []).find(c => c.id === (body.childId || tenantDb.activeChildId)) || (tenantDb.children || [])[0];
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

      if (!tenantDb.activityLogs) tenantDb.activityLogs = [];
      tenantDb.activityLogs.unshift(auditLog);
      saveDatabase(db);
      console.log(`[AUDITORÍA SEGURIDAD] Reinicio detectado en ${devName} a las ${nowStr}`);
      return sendJson(res, 200, { success: true, instantLockActive: child ? child.instantLockActive : false, auditLog });
    }

    // SOS
    if (pathname === '/api/child/sos' && req.method === 'POST') {
      const body = await parseRequestBody(req);
      const { tenantDb } = resolveTenant(req, reqUrl, body);
      const child = (tenantDb.children || []).find(c => c.id === (body.childId || tenantDb.activeChildId));
      const sosLog = {
        id: 'log_' + Date.now(),
        childId: child ? child.id : 'child_1',
        type: 'location',
        title: '🚨 ALERTA SOS - AUXILIO SOLICITADO',
        details: `${child ? child.name : 'Tu hijo'} ha presionado el botón de alerta SOS desde su teléfono móvil. Ubicación GPS reportada.`,
        status: 'blocked',
        timestamp: '¡AHORA MISMO! ' + new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
      };
      if (!tenantDb.activityLogs) tenantDb.activityLogs = [];
      tenantDb.activityLogs.unshift(sosLog);
      saveDatabase(db);
      return sendJson(res, 200, { success: true, alert: sosLog });
    }

    return sendJson(res, 404, { error: 'Endpoint no encontrado' });
  }

  // --- SERVICIO DE PÁGINAS Y RUTAS WEB ---

  // /login
  if (pathname === '/login') {
    const loginFile = path.join(PUBLIC_DIR, 'login.html');
    if (fs.existsSync(loginFile)) {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      return fs.createReadStream(loginFile).pipe(res);
    }
  }

  // /admin
  if (pathname === '/admin') {
    const adminFile = path.join(PUBLIC_DIR, 'admin.html');
    if (fs.existsSync(adminFile)) {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      return fs.createReadStream(adminFile).pipe(res);
    }
  }

  // Healthcheck para Coolify / Traefik / Docker
  if (pathname === '/health' || pathname === '/healthz' || pathname === '/ping') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ status: 'ok', uptime: process.uptime() }));
  }

  // /child o /companion
  if (pathname === '/child' || pathname === '/companion') {
    const childFile = path.join(PUBLIC_DIR, 'child.html');
    if (fs.existsSync(childFile)) {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      return fs.createReadStream(childFile).pipe(res);
    }
  }

  // --- SERVIR ARCHIVOS ESTÁTICOS BLINDADO (SIN RIESGO DE CRASH) ---
  let filePath = path.join(PUBLIC_DIR, pathname === '/' ? 'index.html' : pathname);

  if (!filePath.startsWith(PUBLIC_DIR)) {
    res.writeHead(403);
    return res.end('Acceso denegado');
  }

  // Fallback seguro si el archivo no existe
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(PUBLIC_DIR, 'index.html');
  }

  if (!fs.existsSync(filePath)) {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    return res.end('Archivo no encontrado');
  }

  try {
    const stats = fs.statSync(filePath);
    const ext = path.extname(filePath);
    const contentType = ext === '.apk' ? 'application/vnd.android.package-archive' : (MIME_TYPES[ext] || 'application/octet-stream');

    const headers = {
      'Content-Type': contentType,
      'Content-Length': stats.size,
      'Accept-Ranges': 'bytes'
    };
    if (ext === '.apk') {
      headers['Cache-Control'] = 'no-store, no-cache, must-revalidate';
      headers['Content-Disposition'] = 'attachment; filename="appzsegurity.apk"';
    }

    res.writeHead(200, headers);
    fs.createReadStream(filePath).pipe(res);
  } catch (err) {
    res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Error interno del servidor');
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`\n======================================================`);
  console.log(`🛡️  APPZSEGURITY SAAS - CONTROL PARENTAL Y ALQUILER`);
  console.log(`======================================================`);
  console.log(`👑 Panel Super Admin:       http://localhost:${PORT}/admin`);
  console.log(`🔑 Iniciar Sesión Padres:   http://localhost:${PORT}/login`);
  console.log(`💻 Panel de Padres en PC:   http://localhost:${PORT}/`);
  console.log(`📱 Conexión desde Celular:  http://${LOCAL_IP}:${PORT}/child`);
  console.log(`======================================================\n`);
});
