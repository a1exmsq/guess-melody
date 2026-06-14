import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Volume2, AlertCircle } from 'lucide-react';

interface Props {
  onDeviceId: (id: string) => void;
  onReady?: (ready: boolean) => void;
}

export default function SpotifyPlayer({ onDeviceId, onReady }: Props) {
  const { t } = useTranslation();
  const [isReady, setIsReady] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const script = document.createElement('script');
    script.src = 'https://sdk.scdn.co/spotify-player.js';
    script.async = true;
    document.body.appendChild(script);

    window.onSpotifyWebPlaybackSDKReady = () => {
      const player = new window.Spotify.Player({
        name: 'Guess Melody Web Player',
        getOAuthToken: (cb: (token: string) => void) => {
          fetch('/api/spotify/token')
            .then(r => r.json())
            .then(data => cb(data.token))
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

      player.addListener('initialization_error', ({ message: _msg }: { message: string }) => {
        setError(_msg);
      });

      player.addListener('authentication_error', () => {
        setError(t('player.authError'));
      });

      player.addListener('account_error', () => {
        setError(t('player.premiumRequired'));
      });

      player.connect();
    };

    return () => {
      document.body.removeChild(script);
    };
  }, [onDeviceId, onReady]);

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
    <div className={`flex items-center gap-3 rounded-xl p-4 border ${
      isReady ? 'bg-green-900/20 border-green-500/30' : 'bg-gray-800 border-gray-700'
    }`}>
      <Volume2 className={`w-5 h-5 ${isReady ? 'text-green-400' : 'text-gray-500'}`} />
      <span className={`text-sm ${isReady ? 'text-green-400' : 'text-gray-500'}`}>
        {isReady ? t('player.ready') : t('player.connecting')}
      </span>
    </div>
  );
}
