import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Headphones, Users, Music } from 'lucide-react';
import LanguageSwitcher from '../components/LanguageSwitcher';

interface SpotifyProfile {
  displayName: string;
  images: string | null;
}

export default function Home() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [profile, setProfile] = useState<SpotifyProfile | null>(null);

  useEffect(() => {
    fetch('/api/spotify/me')
      .then(r => r.ok ? r.json() : null)
      .then(data => {
        if (data && !data.error) {
          setProfile(data);
        }
      })
      .catch(() => {});
  }, []);

  const connectSpotify = async () => {
    try {
      const res = await fetch('/api/spotify/login');
      const data = await res.json();
      if (data.url) {
        window.location.href = data.url;
      }
    } catch {
      window.location.href = '/api/spotify/login';
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center px-4">
      <div className="text-center mb-12">
        <div className="flex items-center justify-center gap-3 mb-6">
          <Music className="w-12 h-12 text-brand-primary" />
          <h1 className="text-5xl font-bold tracking-tight">Guess Melody</h1>
        </div>
        <p className="text-brand-muted text-lg max-w-md mx-auto">
          {t('home.subtitle')}
        </p>
        <div className="mt-6 flex justify-center">
          <LanguageSwitcher />
        </div>
      </div>

      {profile ? (
        <div className="mb-8 flex items-center gap-4 bg-brand-panel rounded-full px-6 py-3">
          {profile.images ? (
            <img src={profile.images} alt="avatar" className="w-10 h-10 rounded-full" />
          ) : (
            <div className="w-10 h-10 rounded-full bg-brand-primary flex items-center justify-center text-black font-bold">
              {profile.displayName[0]}
            </div>
          )}
          <div className="text-left">
            <div className="font-medium">{profile.displayName}</div>
            <div className="text-xs text-brand-primary">{t('home.spotifyConnected')}</div>
          </div>
        </div>
      ) : (
        <button
          onClick={connectSpotify}
          className="mb-8 bg-brand-primary hover:bg-brand-secondary text-black font-bold py-3 px-8 rounded-full flex items-center gap-3 transition-colors"
        >
          <svg className="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 0C5.4 0 0 5.4 0 12s5.4 12 12 12 12-5.4 12-12S18.66 0 12 0zm5.521 17.34c-.24.359-.66.48-1.021.24-2.82-1.74-6.36-2.101-10.561-1.141-.418.122-.779-.179-.899-.539-.12-.421.18-.78.54-.9 4.56-1.021 8.52-.6 11.64 1.32.42.18.479.659.301 1.02zm1.44-3.3c-.301.42-.841.6-1.262.3-3.239-1.98-8.159-2.58-11.939-1.38-.479.12-1.02-.12-1.14-.6-.12-.48.12-1.021.6-1.141C9.6 9.9 15 10.561 18.72 12.84c.361.181.54.78.241 1.2zm.12-3.36C15.24 8.4 8.82 8.16 5.16 9.301c-.6.179-1.2-.181-1.38-.721-.18-.601.18-1.2.72-1.381 4.26-1.26 11.28-1.02 15.721 1.621.539.3.719 1.02.419 1.56-.299.421-1.02.599-1.559.3z"/>
          </svg>
          {t('home.loginSpotify')}
        </button>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-2xl w-full">
        <button
          onClick={() => navigate('/singleplayer')}
          className="group relative bg-brand-panel hover:bg-brand-surface rounded-2xl p-8 transition-all duration-300 hover:scale-105"
        >
          <div className="absolute top-4 right-4 opacity-20 group-hover:opacity-40 transition-opacity">
            <Headphones className="w-16 h-16 text-brand-primary" />
          </div>
          <h2 className="text-2xl font-bold mb-2">{t('home.singleplayerTitle')}</h2>
          <p className="text-brand-muted">
            {t('home.singleplayerDesc')}
          </p>
        </button>

        <button
          onClick={() => navigate('/multiplayer')}
          className="group relative bg-brand-panel hover:bg-brand-surface rounded-2xl p-8 transition-all duration-300 hover:scale-105"
        >
          <div className="absolute top-4 right-4 opacity-20 group-hover:opacity-40 transition-opacity">
            <Users className="w-16 h-16 text-brand-primary" />
          </div>
          <h2 className="text-2xl font-bold mb-2">{t('home.multiplayerTitle')}</h2>
          <p className="text-brand-muted">
            {t('home.multiplayerDesc')}
          </p>
        </button>
      </div>

      <div className="mt-12 text-brand-muted text-sm">
        {t('home.premiumRequired')}
      </div>
    </div>
  );
}
