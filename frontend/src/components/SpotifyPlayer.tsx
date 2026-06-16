import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Volume2, AlertCircle } from 'lucide-react';
import { getSpotifyToken } from '../lib/spotifyApi';

interface Props {
  onDeviceId: (id: string) => void;
  onReady?: (ready: boolean) => void;
}

export default function SpotifyPlayer({ onDeviceId, onReady }: Props) {
  const { t } = useTranslation();
  const [isReady, setIsReady] = useState(false);
  const [error, setError] = useState('');
  const initializedRef = useRef(false);

  useEffect(() => {
    if (initializedRef.current) return;
    initializedRef.current = true;

    const initPlayer = () => {
      if (!window.Spotify) return;

      const player = new window.Spotify.Player({
        name: 'Guess Melody Web Player',
        getOAuthToken: (cb: (token: string) => void) => {
          getSpotifyToken()
            .then((token) => cb(token))
            .catch(() => setError(t('player.tokenError')));
        },
        volume: 0.5,
      });

      player.addListener('ready', ({ device_id }: { device_id: string }) => {
        console.log('Ready with Device ID', device_id);
        setIsReady(true);
        onDeviceId(device_id);
        onReady?.(true);
      });

      player.addListener('not_ready', ({ device_id }: { device_id: string }) => {
        console.log('Device ID has gone offline', device_id);
        setIsReady(false);
        onReady?.(false);
      });

      player.addListener('initialization_error', ({ message }: { message: string }) => {
        setError(message);
      });

      player.addListener('authentication_error', () => {
        setError(t('player.authError'));
      });

      player.addListener('account_error', () => {
        setError(t('player.premiumRequired'));
      });

      player.connect().catch((e: unknown) => {
        console.error('Spotify player connect error', e);
      });
    };

    const existingScript = document.getElementById('spotify-player-script');
    if (existingScript) {
      if (window.Spotify) {
        initPlayer();
      } else if (window.onSpotifyWebPlaybackSDKReady) {
        const original = window.onSpotifyWebPlaybackSDKReady;
        window.onSpotifyWebPlaybackSDKReady = () => {
          original();
          initPlayer();
        };
      }
      return;
    }

    const script = document.createElement('script');
    script.id = 'spotify-player-script';
    script.src = 'https://sdk.scdn.co/spotify-player.js';
    script.async = true;
    document.body.appendChild(script);

    window.onSpotifyWebPlaybackSDKReady = initPlayer;
  }, [onDeviceId, onReady, t]);

  if (error) {
    return (
      <div className="bg-red-900/30 border border-red-500/30 rounded-xl p-4 flex items-center gap-3">
        <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0" />
        <div>
          <p className="text-red-400 font-medium">{error}</p>
          <a
            href="/api/spotify/login"
            className="text-sm text-brand-primary hover:underline mt-1 inline-block"
          >
            {t('player.login')}
          </a>
        </div>
      </div>
    );
  }

  return (
    <div
      className={`flex items-center gap-3 rounded-xl p-4 border ${
        isReady
          ? 'bg-green-900/20 border-green-500/30'
          : 'bg-brand-surface/80 backdrop-blur-sm border-brand-border/50'
      }`}
    >
      <Volume2 className={`w-5 h-5 ${isReady ? 'text-green-400' : 'text-brand-muted'}`} />
      <span className={`text-sm ${isReady ? 'text-green-400' : 'text-brand-muted'}`}>
        {isReady ? t('player.ready') : t('player.connecting')}
      </span>
    </div>
  );
}
