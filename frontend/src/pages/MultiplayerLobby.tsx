import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Plus, DoorOpen } from 'lucide-react';

export default function MultiplayerLobby() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [roomCode, setRoomCode] = useState('');
  const [nickname, setNickname] = useState('');
  const [error, setError] = useState('');

  const createRoom = async () => {
    if (!nickname.trim()) {
      setError(t('multiplayer.errors.emptyNickname'));
      return;
    }
    try {
      const res = await fetch('/api/rooms', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ playerName: nickname.trim() }),
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
    } catch {
      setError(t('multiplayer.errors.createFailed'));
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
        <button onClick={() => navigate('/')} className="p-2 hover:bg-brand-surface rounded-full">
          <ArrowLeft className="w-6 h-6" />
        </button>
        <h1 className="text-2xl font-bold">{t('multiplayer.title')}</h1>
      </div>

      <div className="space-y-4 mb-8">
        <label className="block text-sm text-brand-muted mb-2">{t('multiplayer.nickname')}</label>
        <input
          type="text"
          value={nickname}
          onChange={(e) => setNickname(e.target.value)}
          placeholder={t('multiplayer.nicknamePlaceholder')}
          className="w-full bg-brand-panel border border-brand-surface rounded-xl py-3 px-4 text-white placeholder-brand-muted focus:outline-none focus:border-brand-primary"
        />
      </div>

      <div className="space-y-4">
        <button
          onClick={createRoom}
          className="w-full bg-brand-primary hover:bg-brand-secondary text-black font-bold py-4 rounded-xl flex items-center justify-center gap-3 transition-colors"
        >
          <Plus className="w-5 h-5" />
          {t('multiplayer.createRoom')}
        </button>

        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-brand-surface"></div>
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="px-2 bg-brand-dark text-brand-muted">{t('multiplayer.or')}</span>
          </div>
        </div>

        <div className="flex gap-3">
          <input
            type="text"
            value={roomCode}
            onChange={(e) => setRoomCode(e.target.value)}
            placeholder={t('multiplayer.roomCodePlaceholder')}
            className="flex-1 bg-brand-panel border border-brand-surface rounded-xl py-3 px-4 text-white placeholder-brand-muted focus:outline-none focus:border-brand-primary uppercase"
          />
          <button
            onClick={joinRoom}
            className="bg-brand-surface hover:bg-brand-border px-6 rounded-xl flex items-center gap-2 transition-colors"
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
