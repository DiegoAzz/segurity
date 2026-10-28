export type PolicyAction = 'blocked' | 'warning' | 'allowed' | 'limited';

export interface Device {
  id: string;
  name: string;
  type: 'smartphone' | 'tablet' | 'laptop' | 'desktop';
  os: string;
  batteryLevel: number;
  isOnline: boolean;
  lastSeen: string;
}

export interface Child {
  id: string;
  name: string;
  age: number;
  avatar: string;
  devices: Device[];
  dailyLimitMinutes: number;
  usedTodayMinutes: number;
  instantLockActive: boolean;
  safeSearchEnabled: boolean;
  schoolTimeActive: boolean;
  currentLocation: {
    lat: number;
    lng: number;
    address: string;
    battery: number;
    updatedAt: string;
    zone?: string;
  };
}

export interface WebCategory {
  id: string;
  name: string;
  icon: string;
  description: string;
  riskLevel: 'high' | 'medium' | 'low';
  action: 'blocked' | 'warning' | 'allowed';
  examples: string[];
}

export interface MonitoredApp {
  id: string;
  name: string;
  packageName: string;
  iconColor: string;
  category: 'social' | 'games' | 'video' | 'messaging' | 'education' | 'utility';
  action: PolicyAction;
  dailyLimitMinutes?: number;
  usedMinutesToday: number;
}

export interface Geofence {
  id: string;
  name: string;
  type: 'home' | 'school' | 'family' | 'other';
  lat: number;
  lng: number;
  radiusMeters: number;
  notifyOnEnter: boolean;
  notifyOnExit: boolean;
}

export interface ActivityLog {
  id: string;
  childId: string;
  type: 'web' | 'app' | 'location' | 'time' | 'search' | 'video';
  title: string;
  details: string;
  status: 'allowed' | 'blocked' | 'warning' | 'info';
  timestamp: string;
}

export interface ChildRequest {
  id: string;
  childId: string;
  childName: string;
  type: 'extra_time' | 'unblock_website' | 'unblock_app';
  target: string;
  requestedMinutes?: number;
  message: string;
  timestamp: string;
  status: 'pending' | 'approved' | 'rejected';
}

export interface TimeScheduleDay {
  day: string;
  dayShort: string;
  allowedHours: number;
  timeSlots: { start: string; end: string }[];
}
