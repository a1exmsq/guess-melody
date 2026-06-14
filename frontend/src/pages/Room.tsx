import { useEffect, useState, useRef } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Play, Crown, User } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { Player } from '../types';

interface GameMessage {
  type: string;
  sender: string;
  content?: string;
  trackId?: string;
  position?: number;
  scheduledStartTime?: number;
  scores?: Record<string, number>;
}

export default function Room() {
  const { t } = useTranslation();
  const { code } = useParams<{ code: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const nickname = searchParams.get('nickname') || t('multiplayer.nicknamePlaceholder');
  
  const [players, setPlayers] = useState<Player[]>([]);
  const [isHost, setIsHost] = useState(false);
  const [gameStarted, setGameStarted] = useState(false);
  const [currentTrack, setCurrentTrack] = useState<string>('');
  const [scores, setScores] = useState<Record<string, number>>({});
  const [feedback, setFeedback] = useState('');
  const [guess, setGuess] = useState('');
  
  const stompClient = useRef<Client | null>(null);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      onConnect: () => {
        client.subscribe(`/topic/room/${code}`, (message) => {
          const msg: GameMessage = JSON.parse(message.body);
          handleGameMessage(msg);
        });

        client.publish({
          destination: `/app/room/${code}/join`,
          body: JSON.stringify({ sender: nickname, type: 'JOIN' }),
        });
      },
    });

    client.activate();
    stompClient.current = client;

    return () => {
      client.deactivate();
    };
  }, [code, nickname]);

  const handleGameMessage = (msg: GameMessage) => {
    switch (msg.type) {
      case 'PLAYER_LIST':
        if (msg.content) {
          const list = JSON.parse(msg.content);
          setPlayers(list);
          setIsHost(list.find((p: Player) => p.nickname === nickname)?.isHost || false);
        }
        break;
      case 'ROUND_START':
        setGameStarted(true);
        setFeedback(t('multiplayer.room.listen'));
        if (msg.scheduledStartTime && msg.trackId) {
          const delay = msg.scheduledStartTime - Date.now();
          setTimeout(() => {
            setCurrentTrack(msg.trackId || '');
          }, Math.max(delay, 0));
        }
        break;
      case 'ROUND_END':
        setFeedback(t('multiplayer.room.roundEnd', { answer: msg.content }));
        break;
      case 'SCORE_UPDATE':
        if (msg.scores) setScores(msg.scores);
        break;
      case 'GAME_OVER':
        setFeedback(t('multiplayer.room.gameOver'));
        break;
    }
  };

  const startGame = () => {
    stompClient.current?.publish({
      destination: `/app/room/${code}/start`,
      body: JSON.stringify({ sender: nickname, type: 'START' }),
    });
  };

  const submitGuess = () => {
    if (!guess.trim()) return;
    stompClient.current?.publish({
      destination: `/app/room/${code}/guess`,
      body: JSON.stringify({ sender: nickname, type: 'GUESS', content: guess.trim() }),
    });
    setGuess('');
  };

  return (
    <div className="min-h-screen flex flex-col px-4 py-6 max-w-2xl mx-auto">
      <div className="flex items-center gap-4 mb-6">
        <button onClick={() => navigate('/multiplayer')} className="p-2 hover:bg-brand-surface rounded-full">
          <ArrowLeft className="w-6 h-6" />
        </button>
        <div>
          <h1 className="text-2xl font-bold">{t('multiplayer.room.title', { code })}</h1>
          <p className="text-brand-muted text-sm">{t('multiplayer.room.playersCount', { count: players.length })}</p>
        </div>
      </div>

      <div className="bg-brand-panel rounded-xl p-4 mb-6">
        <h3 className="font-bold mb-3">{t('multiplayer.room.players')}</h3>
        <div className="space-y-2">
          {players.map((player) => (
            <div key={player.nickname} className="flex items-center justify-between py-2 px-3 bg-brand-surface rounded-lg">
              <div className="flex items-center gap-3">
                <User className="w-5 h-5 text-brand-muted" />
                <span>{player.nickname}</span>
                {player.isHost && <Crown className="w-4 h-4 text-yellow-400" />}
              </div>
              <span className="text-brand-primary font-bold">{scores[player.nickname] || 0}</span>
            </div>
          ))}
        </div>
      </div>

      {!gameStarted && isHost && (
        <button
          onClick={startGame}
          disabled={players.length < 2}
          className="bg-brand-primary hover:bg-brand-secondary disabled:opacity-50 text-black font-bold py-4 rounded-xl flex items-center justify-center gap-3 transition-colors"
        >
          <Play className="w-5 h-5" />
          {t('multiplayer.room.startGame')}
        </button>
      )}

      {gameStarted && (
        <div className="space-y-4">
          <div className="bg-brand-panel rounded-xl p-6 text-center">
            <p className="text-brand-muted mb-2">{feedback}</p>
            {currentTrack && (
              <div className="mt-4">
                <p className="text-sm text-brand-muted">{t('multiplayer.room.playingTrack')}</p>
              </div>
            )}
          </div>

          <div className="flex gap-3">
            <input
              type="text"
              value={guess}
              onChange={(e) => setGuess(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && submitGuess()}
              placeholder={t('multiplayer.room.guessPlaceholder')}
              className="flex-1 bg-brand-panel border border-brand-surface rounded-full py-3 px-4 text-white placeholder-brand-muted focus:outline-none focus:border-brand-primary"
            />
            <button
              onClick={submitGuess}
              className="bg-brand-primary hover:bg-brand-secondary text-black font-bold px-6 rounded-full transition-colors"
            >
              {t('multiplayer.room.guess')}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
