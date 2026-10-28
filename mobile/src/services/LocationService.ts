/**
 * LocationService.ts
 * Servicio de geolocalización en segundo plano y detección de geocercas
 * para la aplicación complementaria del niño (Norton Companion).
 */

export interface Coordinates {
  latitude: number;
  longitude: number;
}

export interface GeofenceZone {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  radiusMeters: number;
}

// Cálculo de distancia mediante fórmula de Haversine
export function calculateDistanceMeters(coord1: Coordinates, coord2: Coordinates): number {
  const R = 6371e3; // Radio de la Tierra en metros
  const φ1 = (coord1.latitude * Math.PI) / 180;
  const φ2 = (coord2.latitude * Math.PI) / 180;
  const Δφ = ((coord2.latitude - coord1.latitude) * Math.PI) / 180;
  const Δλ = ((coord2.longitude - coord1.longitude) * Math.PI) / 180;

  const a =
    Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
    Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c;
}

export class LocationTracker {
  private geofences: GeofenceZone[] = [];
  private currentZoneId: string | null = null;

  constructor(geofences: GeofenceZone[]) {
    this.geofences = geofences;
  }

  /**
   * Procesa una nueva coordenada GPS y determina si entró o salió de una geocerca
   */
  public handleLocationUpdate(
    currentLocation: Coordinates,
    onEnter: (zone: GeofenceZone) => void,
    onExit: (zone: GeofenceZone) => void
  ) {
    let matchedZone: GeofenceZone | null = null;

    for (const zone of this.geofences) {
      const distance = calculateDistanceMeters(currentLocation, {
        latitude: zone.latitude,
        longitude: zone.longitude,
      });

      if (distance <= zone.radiusMeters) {
        matchedZone = zone;
        break;
      }
    }

    if (matchedZone && this.currentZoneId !== matchedZone.id) {
      this.currentZoneId = matchedZone.id;
      onEnter(matchedZone);
    } else if (!matchedZone && this.currentZoneId !== null) {
      const exitedZone = this.geofences.find(z => z.id === this.currentZoneId);
      this.currentZoneId = null;
      if (exitedZone) onExit(exitedZone);
    }
  }
}
