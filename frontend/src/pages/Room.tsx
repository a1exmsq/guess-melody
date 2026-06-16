import { useEffect, useState, useRef } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Play, Crown, User, Music, SkipForward, Volume2 } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import SpotifyPlayer from '../components/SpotifyPlayer';
import { spotifyApi } from '../lib/spotifyApi';

interface GameMessage {
  type: string;
  roomCode?: string;
  playerName?: string;
  message?: string;
  trackId?: string;
  trackName?: string;
  artistName?: string;
  previewUrl?: string;
  roundNumber?: number;
  totalRounds?: number;
  scheduledStartTime?: number;
  points?: number;
  attempt?: number;
  correct?: boolean;
  scores?: Record<string, number>;
  players?: string[];
}

export default function Room() {
  const { t } = useTranslation();
  const { code } = useParams<{ code: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const nickname = searchParams.get('nickname') || t('multiplayer.nicknamePlaceholder');

  const [players, setPlayers] = useState<string[]>([]);
  const [scores, setScores] = useState<Record<string, number>>({});
  const [isHost, setIsHost] = useState(false);
  const [gameStarted, setGameStarted] = useState(false);
  const [roundActive, setRoundActive] = useState(false);
  const [currentTrackId, setCurrentTrackId] = useState<string>('');
  const [currentTrackName, setCurrentTrackName] = useState<string>('');
  const [currentArtistName, setCurrentArtistName] = useState<string>('');
  const [feedback, setFeedback] = useState('');
  const [guess, setGuess] = useState('');
  const [deviceId, setDeviceId] = useState<string | null>(null);
  const [roundGuessCount, setRoundGuessCount] = useState(0);
  const [volume, setVolume] = useState(50);
  const [needsAudioUnlock, setNeedsAudioUnlock] = useState(false);

  const stompClient = useRef<Client | null>(null);
  const playTimeoutRef = useRef<number | undefined>(undefined);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  const upperCode = code?.toUpperCase() || '';

  useEffect(() => {
    const audio = new Audio();
    audio.preload = 'auto';
    audioRef.current = audio;
    return () => {
      audio.pause();
      audio.src = '';
      audioRef.current = null;
    };
  }, []);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      onConnect: () => {
        client.subscribe(`/topic/room/${upperCode}`, (message) => {
          const msg: GameMessage = JSON.parse(message.body);
          handleGameMessage(msg);
        });

        client.publish({
          destination: `/app/room/${upperCode}/join`,
          body: JSON.stringify({ playerName: nickname }),
        });
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
        setFeedback(t('multiplayer.room.connectionError'));
      },
    });

    client.activate();
    stompClient.current = client;

    return () => {
      clearTimeout(playTimeoutRef.current);
      client.deactivate();
    };
  }, [upperCode, nickname, t]);

  const handleGameMessage = (msg: GameMessage) => {
    switch (msg.type) {
      case 'PLAYER_JOINED':
      case 'PLAYER_LEFT':
        if (msg.players) {
          setPlayers(msg.players);
          setIsHost(msg.players[0] === nickname);
        }
        if (msg.scores) {
          setScores(msg.scores);
        }
        if (msg.message) {
          setFeedback(msg.message);
        }
        break;
      case 'GAME_STARTED':
        setGameStarted(true);
        setFeedback(msg.message || t('multiplayer.room.gameStarted'));
        break;
      case 'ROUND_START':
        setGameStarted(true);
        setRoundActive(true);
        setRoundGuessCount(0);
        setCurrentTrackId(msg.trackId || '');
        setCurrentTrackName('');
        setCurrentArtistName('');
        setFeedback(
          msg.message ||
            t('multiplayer.room.roundStart', {
              round: msg.roundNumber,
              total: msg.totalRounds,
            })
        );
        schedulePlay(msg.trackId, msg.previewUrl, msg.scheduledStartTime);
        break;
      case 'PLAY_TRACK':
        if (msg.trackId) {
          schedulePlay(msg.trackId, msg.previewUrl, msg.scheduledStartTime);
        }
        break;
      case 'GUESS_RESULT':
        if (msg.scores) {
          setScores(msg.scores);
        }
        if (msg.message) {
          setFeedback(msg.message);
        }
        break;
      case 'ROUND_END':
        setRoundActive(false);
        setCurrentTrackName(msg.trackName || '');
        setCurrentArtistName(msg.artistName || '');
        setFeedback(
          msg.message || t('multiplayer.room.roundEnd', { answer: `${msg.artistName} — ${msg.trackName}` })
        );
        stopPlayback();
        break;
      case 'GAME_OVER':
        setRoundActive(false);
        setFeedback(msg.message || t('multiplayer.room.gameOver'));
        stopPlayback();
        break;
      case 'ERROR':
        setFeedback(msg.message || t('multiplayer.room.error'));
        break;
    }
  };

  const stopPlayback = () => {
    clearTimeout(playTimeoutRef.current);
    audioRef.current?.pause();
    if (deviceId) {
      spotifyApi.pause(deviceId).catch(() => {});
    }
  };

  const schedulePlay = (trackId?: string, previewUrl?: string, scheduledStartTime?: number) => {
    clearTimeout(playTimeoutRef.current);
    const audio = audioRef.current;

    if (previewUrl && audio) {
      audio.src = previewUrl;
      audio.volume = volume / 100;
      audio.load();
      setNeedsAudioUnlock(false);

      const delay = scheduledStartTime ? Math.max(0, scheduledStartTime - Date.now()) : 0;
      playTimeoutRef.current = window.setTimeout(() => {
        audio.play().catch((e) => {
          console.error('Audio play error', e);
          setNeedsAudioUnlock(true);
          setFeedback(t('multiplayer.room.playError'));
        });
      }, delay);
      return;
    }

    if (deviceId && trackId) {
      const delay = scheduledStartTime ? Math.max(0, scheduledStartTime - Date.now()) : 0;
      playTimeoutRef.current = window.setTimeout(() => {
        spotifyApi.play(deviceId, trackId, 0).catch((e) => {
          console.error('Room play error', e);
          setFeedback(t('multiplayer.room.playError'));
        });
      }, delay);
    }
  };

  const unlockAudio = () => {
    const audio = audioRef.current;
    if (!audio) return;
    audio.play().then(() => {
      audio.pause();
      setNeedsAudioUnlock(false);
      setFeedback('');
    }).catch(() => {});
  };

  const publish = (destination: string, body: object) => {
    stompClient.current?.publish({
      destination,
      body: JSON.stringify(body),
    });
  };

  const startGame = () => {
    publish(`/app/room/${upperCode}/start`, { playerName: nickname });
  };

  const playTrack = () => {
    publish(`/app/room/${upperCode}/play`, { playerName: nickname });
  };

  const endRound = () => {
    publish(`/app/room/${upperCode}/end-round`, { playerName: nickname });
  };

  const submitGuess = () => {
    if (!guess.trim() || !roundActive) return;
    const attempt = roundGuessCount + 1;
    publish(`/app/room/${upperCode}/guess`, {
      playerName: nickname,
      message: guess.trim(),
      attempt,
    });
    setGuess('');
    setRoundGuessCount((prev) => prev + 1);
  };

  const handleVolumeChange = (v: number) => {
    setVolume(v);
    if (audioRef.current) {
      audioRef.current.volume = v / 100;
    }
    if (deviceId) {
      spotifyApi.volume(deviceId, v).catch(() => {});
    }
  };

  return (
    <div className="min-h-screen flex flex-col px-4 py-6 max-w-2xl mx-auto">
      <div className="flex items-center gap-4 mb-6">
        <button onClick={() => navigate('/multiplayer')} className="p-2 hover:bg-brand-surface/80 rounded-full">
          <ArrowLeft className="w-6 h-6" />
        </button>
        <div className="flex-1">
          <h1 className="text-2xl font-bold">{t('multiplayer.room.title', { code: upperCode })}</h1>
          <p className="text-brand-muted text-sm">{t('multiplayer.room.playersCount', { count: players.length })}</p>
        </div>
        <div className="flex items-center gap-2">
          <Volume2 className="w-4 h-4 text-brand-muted" />
          <input
            type="range"
            min="0"
            max="100"
            value={volume}
            onChange={(e) => handleVolumeChange(Number(e.target.value))}
            className="w-20 accent-brand-primary"
          />
        </div>
      </div>

      <SpotifyPlayer onDeviceId={setDeviceId} />

      {needsAudioUnlock && (
        <button
          onClick={unlockAudio}
          className="mb-4 bg-yellow-600/80 hover:bg-yellow-500/80 text-white font-semibold py-2 px-4 rounded-xl"
        >
          🔇 Нажми, чтобы включить звук
        </button>
      )}

      <div className="bg-brand-panel/80 backdrop-blur-sm rounded-xl p-4 mb-6">
        <h3 className="font-bold mb-3">{t('multiplayer.room.players')}</h3>
        <div className="space-y-2">
          {players.map((player) => (
            <div key={player} className="flex items-center justify-between py-2 px-3 bg-brand-surface/80 rounded-lg">
              <div className="flex items-center gap-3">
                <User className="w-5 h-5 text-brand-muted" />
                <span>{player}</span>
                {player === players[0] && <Crown className="w-4 h-4 text-yellow-400" />}
              </div>
              <span className="text-brand-primary font-bold">{scores[player] || 0}</span>
            </div>
          ))}
        </div>
      </div>

      {!gameStarted && isHost && (
        <button
          onClick={startGame}
          disabled={players.length < 1}
          className="bg-brand-primary hover:bg-brand-secondary disabled:opacity-50 font-semibold text-brand-text font-bold py-4 rounded-xl flex items-center justify-center gap-3 transition-colors mb-6"
        >
          <Play className="w-5 h-5" />
          {t('multiplayer.room.startGame')}
        </button>
      )}

      {gameStarted && (
        <div className="space-y-4 mb-6">
          <div className="bg-brand-panel/80 backdrop-blur-sm rounded-xl p-6 text-center">
            <p className="text-brand-muted mb-2">{feedback}</p>
            {currentTrackId && roundActive && (
              <p className="text-sm text-brand-muted mt-4">{t('multiplayer.room.listen')}</p>
            )}
            {!roundActive && currentTrackName && (
              <p className="text-lg font-bold text-brand-primary mt-2">
                {currentArtistName} — {currentTrackName}
              </p>
            )}
          </div>

          {isHost && roundActive && (
            <button
              onClick={endRound}
              className="w-full bg-red-600/80 hover:bg-red-500/80 font-semibold text-brand-text font-bold py-3 rounded-xl flex items-center justify-center gap-3 transition-colors"
            >
              <SkipForward className="w-5 h-5" />
              {t('multiplayer.room.endRound')}
            </button>
          )}

          {isHost && !roundActive && (
            <button
              onClick={playTrack}
              className="w-full bg-brand-surface/80 hover:bg-brand-border font-semibold text-brand-text font-bold py-3 rounded-xl flex items-center justify-center gap-3 transition-colors"
            >
              <Music className="w-5 h-5" />
              {t('multiplayer.room.playTrack')}
            </button>
          )}

          <div className="flex gap-3">
            <input
              type="text"
              value={guess}
              onChange={(e) => setGuess(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && submitGuess()}
              disabled={!roundActive}
              placeholder={t('multiplayer.room.guessPlaceholder')}
              className="flex-1 bg-brand-panel/80 backdrop-blur-sm border border-brand-border/50 rounded-full py-3 px-4 text-brand-text placeholder-brand-muted focus:outline-none focus:border-brand-primary disabled:opacity-50"
            />
            <button
              onClick={submitGuess}
              disabled={!roundActive || !guess.trim()}
              className="bg-brand-primary hover:bg-brand-secondary disabled:opacity-50 font-semibold text-brand-text font-bold px-6 rounded-full transition-colors"
            >
              {t('multiplayer.room.guess')}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
