import { Child, WebCategory, MonitoredApp, Geofence, ActivityLog, ChildRequest, TimeScheduleDay } from './types';

export const initialChildren: Child[] = [
  {
    id: 'child_1',
    name: 'Lucas Martínez',
    age: 11,
    avatar: '👦',
    dailyLimitMinutes: 120, // 2 horas
    usedTodayMinutes: 85,
    instantLockActive: false,
    safeSearchEnabled: true,
    schoolTimeActive: false,
    devices: [
      {
        id: 'dev_1',
        name: 'Galaxy A54 de Lucas',
        type: 'smartphone',
        os: 'Android 14 (Norton Companion v4.8)',
        batteryLevel: 74,
        isOnline: true,
        lastSeen: 'Ahora mismo',
      },
      {
        id: 'dev_2',
        name: 'Tablet Lenovo Tab M10',
        type: 'tablet',
        os: 'Android 13',
        batteryLevel: 42,
        isOnline: false,
        lastSeen: 'Hace 3 horas',
      }
    ],
    currentLocation: {
      lat: 40.4168,
      lng: -3.7038,
      address: 'Colegio San Patricio - Av. de la Paz 42',
      battery: 74,
      updatedAt: 'Hace 4 min',
      zone: 'Colegio (Zona Segura)'
    }
  },
  {
    id: 'child_2',
    name: 'Sofía Martínez',
    age: 14,
    avatar: '👧',
    dailyLimitMinutes: 180, // 3 horas
    usedTodayMinutes: 140,
    instantLockActive: false,
    safeSearchEnabled: true,
    schoolTimeActive: false,
    devices: [
      {
        id: 'dev_3',
        name: 'iPhone 13 de Sofía',
        type: 'smartphone',
        os: 'iOS 17.5 (Norton Family Profile)',
        batteryLevel: 88,
        isOnline: true,
        lastSeen: 'Ahora mismo',
      },
      {
        id: 'dev_4',
        name: 'Laptop HP Pavilion (Habitación)',
        type: 'laptop',
        os: 'Windows 11 (Norton Family Agent)',
        batteryLevel: 100,
        isOnline: true,
        lastSeen: 'Hace 10 min',
      }
    ],
    currentLocation: {
      lat: 40.4200,
      lng: -3.6980,
      address: 'Biblioteca Municipal Central',
      battery: 88,
      updatedAt: 'Hace 12 min',
      zone: 'Biblioteca'
    }
  }
];

export const initialWebCategories: WebCategory[] = [
  {
    id: 'cat_adult',
    name: 'Contenido para Adultos / Pornografía',
    icon: '🔞',
    description: 'Sitios con material explícito, desnudez o contenido sexual para adultos.',
    riskLevel: 'high',
    action: 'blocked',
    examples: ['Sitios para adultos, foros restringidos, contenido explícito']
  },
  {
    id: 'cat_violence',
    name: 'Violencia, Odio y Armas',
    icon: '⚔️',
    description: 'Contenido violento extremo, discriminación, incitación al odio y venta de armas.',
    riskLevel: 'high',
    action: 'blocked',
    examples: ['Armas de fuego, sitios radicales, contenido sangriento']
  },
  {
    id: 'cat_gambling',
    name: 'Juegos de Azar y Apuestas',
    icon: '🎰',
    description: 'Casinos online, apuestas deportivas, loterías y sitios de sorteos pagados.',
    riskLevel: 'high',
    action: 'blocked',
    examples: ['Bet365, PokerStars, Ruleta virtual']
  },
  {
    id: 'cat_drugs',
    name: 'Drogas, Tabaco y Sustancias',
    icon: '💊',
    description: 'Venta, cultivo o promoción de estupefacientes, vapeo y alcohol para menores.',
    riskLevel: 'high',
    action: 'blocked',
    examples: ['Tiendas de vapeo, guías de consumo']
  },
  {
    id: 'cat_social',
    name: 'Redes Sociales',
    icon: '💬',
    description: 'Plataformas de interacción social e intercambio de mensajes masivos.',
    riskLevel: 'medium',
    action: 'warning',
    examples: ['Instagram, TikTok, Twitter/X, Snapchat']
  },
  {
    id: 'cat_gaming',
    name: 'Videojuegos y Entretenimiento',
    icon: '🎮',
    description: 'Sitios web de minijuegos en línea, streams de partidas y servidores de juegos.',
    riskLevel: 'medium',
    action: 'allowed',
    examples: ['Roblox.com, Twitch.tv, Poki.com, Steam']
  },
  {
    id: 'cat_shopping',
    name: 'Compras y Comercio Electrónico',
    icon: '🛍️',
    description: 'Tiendas online que solicitan pagos o tarjetas de crédito.',
    riskLevel: 'medium',
    action: 'warning',
    examples: ['Amazon, AliExpress, Shein']
  },
  {
    id: 'cat_education',
    name: 'Educación y Aprendizaje',
    icon: '📚',
    description: 'Enciclopedias, plataformas escolares, ciencias y recursos educativos.',
    riskLevel: 'low',
    action: 'allowed',
    examples: ['Wikipedia, Khan Academy, Duolingo, Classroom']
  }
];

export const initialApps: MonitoredApp[] = [
  {
    id: 'app_1',
    name: 'TikTok',
    packageName: 'com.zhiliaoapp.musically',
    iconColor: 'bg-black text-white',
    category: 'social',
    action: 'limited',
    dailyLimitMinutes: 30,
    usedMinutesToday: 28,
  },
  {
    id: 'app_2',
    name: 'Roblox',
    packageName: 'com.roblox.client',
    iconColor: 'bg-gray-800 text-white',
    category: 'games',
    action: 'limited',
    dailyLimitMinutes: 45,
    usedMinutesToday: 35,
  },
  {
    id: 'app_3',
    name: 'YouTube',
    packageName: 'com.google.android.youtube',
    iconColor: 'bg-red-600 text-white',
    category: 'video',
    action: 'allowed',
    dailyLimitMinutes: 60,
    usedMinutesToday: 22,
  },
  {
    id: 'app_4',
    name: 'Discord',
    packageName: 'com.discord',
    iconColor: 'bg-indigo-600 text-white',
    category: 'messaging',
    action: 'blocked',
    usedMinutesToday: 0,
  },
  {
    id: 'app_5',
    name: 'Brawl Stars',
    packageName: 'com.supercell.brawlstars',
    iconColor: 'bg-yellow-500 text-black',
    category: 'games',
    action: 'allowed',
    dailyLimitMinutes: 30,
    usedMinutesToday: 15,
  },
  {
    id: 'app_6',
    name: 'WhatsApp',
    packageName: 'com.whatsapp',
    iconColor: 'bg-emerald-600 text-white',
    category: 'messaging',
    action: 'allowed',
    usedMinutesToday: 12,
  },
  {
    id: 'app_7',
    name: 'Duolingo',
    packageName: 'com.duolingo',
    iconColor: 'bg-green-500 text-white',
    category: 'education',
    action: 'allowed',
    usedMinutesToday: 15,
  }
];

export const initialGeofences: Geofence[] = [
  {
    id: 'geo_1',
    name: 'Casa Familiar',
    type: 'home',
    lat: 40.4168,
    lng: -3.7038,
    radiusMeters: 150,
    notifyOnEnter: true,
    notifyOnExit: true
  },
  {
    id: 'geo_2',
    name: 'Colegio San Patricio',
    type: 'school',
    lat: 40.4180,
    lng: -3.7020,
    radiusMeters: 250,
    notifyOnEnter: true,
    notifyOnExit: true
  },
  {
    id: 'geo_3',
    name: 'Casa de los Abuelos',
    type: 'family',
    lat: 40.4120,
    lng: -3.7090,
    radiusMeters: 200,
    notifyOnEnter: true,
    notifyOnExit: false
  }
];

export const initialRequests: ChildRequest[] = [
  {
    id: 'req_1',
    childId: 'child_1',
    childName: 'Lucas Martínez',
    type: 'extra_time',
    target: 'Tiempo General de Pantalla',
    requestedMinutes: 30,
    message: 'Papá, estoy terminando una maqueta de Ciencias y necesito 30 minutos más para consultar Wikipedia.',
    timestamp: 'Hace 8 minutos',
    status: 'pending'
  },
  {
    id: 'req_2',
    childId: 'child_1',
    childName: 'Lucas Martínez',
    type: 'unblock_website',
    target: 'scratch.mit.edu',
    message: 'El profesor de computación nos pidió entrar a Scratch para el proyecto escolar.',
    timestamp: 'Hace 35 minutos',
    status: 'pending'
  }
];

export const initialActivityLogs: ActivityLog[] = [
  {
    id: 'log_1',
    childId: 'child_1',
    type: 'location',
    title: 'Entrada a Zona Segura',
    details: 'Lucas llegó a "Colegio San Patricio" a las 08:45 AM.',
    status: 'info',
    timestamp: 'Hoy, 08:45 AM'
  },
  {
    id: 'log_2',
    childId: 'child_1',
    type: 'web',
    title: 'Sitio Web Bloqueado',
    details: 'Intento de acceso a "casino-online-slots.xyz" bloqueado por regla de Categoría (Juegos de Azar).',
    status: 'blocked',
    timestamp: 'Hoy, 12:15 PM'
  },
  {
    id: 'log_3',
    childId: 'child_1',
    type: 'search',
    title: 'Búsqueda en Google (SafeSearch activo)',
    details: 'Búsqueda registrada: "¿cómo hacer volcanes de bicarbonato?"',
    status: 'info',
    timestamp: 'Hoy, 01:10 PM'
  },
  {
    id: 'log_4',
    childId: 'child_1',
    type: 'app',
    title: 'Límite de App Alcanzado',
    details: 'TikTok alcanzó el límite diario de 30 min. La aplicación ha sido pausada.',
    status: 'warning',
    timestamp: 'Hoy, 02:40 PM'
  },
  {
    id: 'log_5',
    childId: 'child_2',
    type: 'web',
    title: 'Advertencia de Red Social',
    details: 'Sofía visitó Instagram. Se le mostró recordatorio de uso responsable.',
    status: 'warning',
    timestamp: 'Hoy, 03:00 PM'
  }
];

export const defaultWeekSchedule: TimeScheduleDay[] = [
  { day: 'Lunes', dayShort: 'Lun', allowedHours: 2.0, timeSlots: [{ start: '16:00', end: '20:00' }] },
  { day: 'Martes', dayShort: 'Mar', allowedHours: 2.0, timeSlots: [{ start: '16:00', end: '20:00' }] },
  { day: 'Miércoles', dayShort: 'Mié', allowedHours: 2.0, timeSlots: [{ start: '16:00', end: '20:00' }] },
  { day: 'Jueves', dayShort: 'Jue', allowedHours: 2.0, timeSlots: [{ start: '16:00', end: '20:00' }] },
  { day: 'Viernes', dayShort: 'Vie', allowedHours: 3.0, timeSlots: [{ start: '15:00', end: '21:00' }] },
  { day: 'Sábado', dayShort: 'Sáb', allowedHours: 4.0, timeSlots: [{ start: '10:00', end: '21:00' }] },
  { day: 'Domingo', dayShort: 'Dom', allowedHours: 3.5, timeSlots: [{ start: '10:00', end: '20:00' }] },
];
