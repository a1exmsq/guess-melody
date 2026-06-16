import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Plus, DoorOpen, Music } from 'lucide-react';

export default function MultiplayerLobby() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [roomCode, setRoomCode] = useState('');
  const [nickname, setNickname] = useState('');
  const [playlistUrl, setPlaylistUrl] = useState('');
  const [importing, setImporting] = useState(false);
  const [error, setError] = useState('');

  const importPlaylist = async (): Promise<number | null> => {
    const trimmed = playlistUrl.trim();
    if (!trimmed) return null;

    const res = await fetch('/api/playlists/import', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ url: trimmed }),
    });

    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.error || t('singleplayer.feedback.importError'));
    }

    const data = await res.json();
    return data.id as number;
  };

  const createRoom = async () => {
    if (!nickname.trim()) {
      setError(t('multiplayer.errors.emptyNickname'));
      return;
    }

    setImporting(true);
    setError('');

    try {
      let playlistId: number | null = null;
      if (playlistUrl.trim()) {
        playlistId = await importPlaylist();
      }

      const body: { playerName: string; playlistId?: number } = {
        playerName: nickname.trim(),
      };
      if (playlistId != null) {
        body.playlistId = playlistId;
      }

      const res = await fetch('/api/rooms', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const err = await res.text();
        setError(err || t('multiplayer.errors.createFailed'));
        return;
      }
      const data = await res.json();
      if (data.code) {
        navigate(`/room/${data.code}?nickname=${encodeURIComponent(nickname.trim())}`);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : t('multiplayer.errors.createFailed'));
    } finally {
      setImporting(false);
    }
  };

  const joinRoom = async () => {
    if (!nickname.trim()) {
      setError(t('multiplayer.errors.emptyNickname'));
      return;
    }
    if (!roomCode.trim()) {
      setError(t('multiplayer.errors.emptyCode'));
      return;
    }
    navigate(`/room/${roomCode.trim().toUpperCase()}?nickname=${encodeURIComponent(nickname)}`);
  };

  return (
    <div className="min-h-screen flex flex-col px-4 py-6 max-w-md mx-auto">
      <div className="flex items-center gap-4 mb-8">
        <button onClick={() => navigate('/')} className="p-2 hover:bg-brand-surface/80 rounded-full">
          <ArrowLeft className="w-6 h-6" />
        </button>
        <h1 className="text-2xl font-bold">{t('multiplayer.title')}</h1>
      </div>

      <div className="space-y-4 mb-6">
        <label className="block text-sm text-brand-muted">{t('multiplayer.nickname')}</label>
        <input
          type="text"
          value={nickname}
          onChange={(e) => setNickname(e.target.value)}
          placeholder={t('multiplayer.nicknamePlaceholder')}
          className="w-full bg-brand-panel/80 backdrop-blur-sm border border-brand-border/50 rounded-xl py-3 px-4 text-brand-text placeholder-brand-muted focus:outline-none focus:border-brand-primary"
        />
      </div>

      <div className="space-y-4 mb-8">
        <div className="bg-brand-panel/80 backdrop-blur-sm rounded-2xl p-6 border border-brand-border/50">
          <h2 className="text-lg font-bold mb-2 flex items-center gap-2">
            <Music className="w-5 h-5" />
            {t('multiplayer.importPlaylist')}
          </h2>
          <p className="text-brand-muted text-sm mb-4">{t('multiplayer.importDesc')}</p>
          <input
            type="text"
            value={playlistUrl}
            onChange={(e) => setPlaylistUrl(e.target.value)}
            placeholder="https://open.spotify.com/playlist/..."
            className="w-full bg-brand-surface/80 backdrop-blur-sm border border-brand-border/50 rounded-xl py-3 px-4 text-brand-text placeholder-brand-muted focus:outline-none focus:border-brand-primary"
          />
        </div>

        <button
          onClick={createRoom}
          disabled={importing || !nickname.trim()}
          className="w-full bg-brand-primary hover:bg-brand-secondary disabled:opacity-50 font-semibold text-brand-text font-bold py-4 rounded-xl flex items-center justify-center gap-3 transition-colors"
        >
          <Plus className="w-5 h-5" />
          {importing ? t('common.loading') : t('multiplayer.createRoom')}
        </button>

        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-brand-border/50"></div>
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="px-2 bg-transparent text-brand-muted">{t('multiplayer.or')}</span>
          </div>
        </div>

        <div className="flex gap-3">
          <input
            type="text"
            value={roomCode}
            onChange={(e) => setRoomCode(e.target.value)}
            placeholder={t('multiplayer.roomCodePlaceholder')}
            className="flex-1 bg-brand-panel/80 backdrop-blur-sm border border-brand-border/50 rounded-xl py-3 px-4 text-brand-text placeholder-brand-muted focus:outline-none focus:border-brand-primary uppercase"
          />
          <button
            onClick={joinRoom}
            className="bg-brand-surface/80 backdrop-blur-sm hover:bg-brand-border px-6 rounded-xl flex items-center gap-2 transition-colors"
          >
            <DoorOpen className="w-5 h-5" />
            {t('multiplayer.joinRoom')}
          </button>
        </div>
      </div>

      {error && (
        <div className="mt-4 bg-red-900/30 text-red-400 px-4 py-3 rounded-xl text-center">
          {error}
        </div>
      )}
    </div>
  );
}
