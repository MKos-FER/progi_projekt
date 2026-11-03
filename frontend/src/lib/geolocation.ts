// Implementacija Haversine formule https://en.wikipedia.org/wiki/Haversine_formula
// točnije nije potrebno računati
export function calculateDistanceKm(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371;

  const toRadians = (deg: number) => deg * (Math.PI / 180);

  const dLat = toRadians(lat2 - lat1);
  const dLon = toRadians(lon2 - lon1);

  // hav(fi) = hav(dLat) + cos(lat1)cos(lat2)hav(dLon)
  // hav(fi) = sin^2(fi/2)
  const havfi = Math.sin(dLat / 2) ** 2 +
    Math.cos(toRadians(lat1)) *
    Math.cos(toRadians(lat2)) *
    Math.sin(dLon / 2) ** 2;

  // fi = 2 * asin(sqrt(hav(fi)))
  // d = r*fi
  return 2 * R * Math.asin(Math.sqrt(havfi));
}

export interface UserLocation {
  latitude: number;
  longitude: number;
  accuracyRadiusMeters: number;
}

export function getUserLocation(): Promise<UserLocation> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error("Geolocation is not supported"));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          accuracyRadiusMeters: position.coords.accuracy,
        });
      },
      (error) => {
        reject(new Error(`Could not get location: ${error.message}`));
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0,
      }
    );
  });
}