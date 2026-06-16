const API_BASE = 'https://api.spotify.com/v1';

interface TokenResponse {
  token: string;
  expiresIn: number;
}

let cachedToken: string | null = null;
let tokenFetchedAt = 0;
const TOKEN_LIFETIME_MS = 50 * 60 * 1000; // refresh after 50 min

export async function getSpotifyToken(): Promise<string> {
  const now = Date.now();
  if (cachedToken && now - tokenFetchedAt < TOKEN_LIFETIME_MS) {
    return cachedToken;
  }

  const res = await fetch('/api/spotify/token');
  if (!res.ok) {
    cachedToken = null;
    const data = await res.json().catch(() => ({}));
    throw new Error(data.error || 'Spotify authorization required');
  }
  const data: TokenResponse = await res.json();
  cachedToken = data.token;
  tokenFetchedAt = now;
  return cachedToken;
}

export function clearToken() {
  cachedToken = null;
  tokenFetchedAt = 0;
}

async function request(
  method: string,
  endpoint: string,
  body?: unknown,
  retry = true
): Promise<void> {
  const token = await getSpotifyToken();
  const headers: Record<string, string> = {
    Authorization: `Bearer ${token}`,
  };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  const res = await fetch(`${API_BASE}${endpoint}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401 && retry) {
    clearToken();
    return request(method, endpoint, body, false);
  }

  if (!res.ok && res.status !== 204) {
    const text = await res.text().catch(() => '');
    throw new Error(`Spotify API ${res.status}: ${text}`);
  }
}

export const spotifyApi = {
  play(deviceId: string, trackId: string, positionMs = 0) {
    return request(
      'PUT',
      `/me/player/play?device_id=${encodeURIComponent(deviceId)}`,
      {
        uris: [`spotify:track:${trackId}`],
        position_ms: Math.max(0, Math.round(positionMs)),
      }
    );
  },

  pause(deviceId: string) {
    return request(
      'PUT',
      `/me/player/pause?device_id=${encodeURIComponent(deviceId)}`
    );
  },

  seek(deviceId: string, positionMs: number) {
    return request(
      'PUT',
      `/me/player/seek?position_ms=${Math.max(0, Math.round(positionMs))}&device_id=${encodeURIComponent(deviceId)}`
    );
  },

  volume(deviceId: string, volumePercent: number) {
    const vol = Math.max(0, Math.min(100, Math.round(volumePercent)));
    return request(
      'PUT',
      `/me/player/volume?volume_percent=${vol}&device_id=${encodeURIComponent(deviceId)}`
    );
  },
};
