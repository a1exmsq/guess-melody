import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Play, Pause, SkipForward } from 'lucide-react';
import SpotifyPlayer from '../components/SpotifyPlayer';
import SearchInput from '../components/SearchInput';

import ProgressBar from '../components/ProgressBar';
import VolumeSlider from '../components/VolumeSlider';
import AttemptHistory from '../components/AttemptHistory';
import TrackReveal from '../components/TrackReveal';
import type { Track } from '../types';

const SNIPPETS = [500, 1000, 2000, 4000, 8000, 16000];
const POINTS = [100, 80, 60, 40, 20, 10];

const normalizeString = (input: string) =>
  input
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/\s*\([^)]*\)/g, '')
    .replace(/\s*\[[^\]]*\]/g, '')
    .replace(/\s+-\s+.*$/, '')
    .trim();

const normalizeTitle = (title: string) => normalizeString(title);

const tokenize = (input: string) =>
  normalizeString(input)
    .split(/[^a-z0-9]+/)
    .filter(t => t.length > 0);

const containsTokensInOrder = (container: string[], target: string[]) => {
  if (target.length === 0) return false;
  let i = 0;
  for (const token of container) {
    if (token === target[i]) {
      i++;
      if (i === target.length) return true;
    }
  }
  return false;
};


interface Attempt {
  text: string;
  type: 'skip' | 'wrong' | 'artist' | 'correct';
}

export default function Singleplayer() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [deviceId, setDeviceId] = useState<string | null>(null);
  const [playerReady, setPlayerReady] = useState(false);
  const [tracks, setTracks] = useState<Track[]>([]);
  const [currentTrackIndex, setCurrentTrackIndex] = useState(0);
  const [attempts, setAttempts] = useState(0);
  const [score, setScore] = useState(0);
  const [artistGuessed, setArtistGuessed] = useState(false);
  const [gameOver, setGameOver] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);


  const attemptsRef = useRef(0);
  const artistPointsAwardedRef = useRef(0);
  const isPlayingRef = useRef(false);
  const playStartTimeRef = useRef<number | null>(null);
  const pausedProgressMsRef = useRef(0);
  const isLoadingRef = useRef(false);
  const playTimeoutRef = useRef<number | undefined>(undefined);
  const safetyIntervalRef = useRef<number | undefined>(undefined);
  const safetyPauseTimeoutsRef = useRef<number[]>([]);

  const clearPlaybackTimers = () => {
    clearTimeout(playTimeoutRef.current);
    clearInterval(safetyIntervalRef.current);
    safetyPauseTimeoutsRef.current.forEach(id => clearTimeout(id));
    safetyPauseTimeoutsRef.current = [];
  };

  const [feedback, setFeedback] = useState<string>('');
  const [playlistUrl, setPlaylistUrl] = useState('');
  const [importing, setImporting] = useState(false);
  const [gameStarted, setGameStarted] = useState(false);
  const [playStartTime, setPlayStartTime] = useState<number | null>(null);
  const [pausedProgressMs, setPausedProgressMs] = useState(0);
  const [playbackProgressMs, setPlaybackProgressMs] = useState(0);
  const [lastPlaybackUpdate, setLastPlaybackUpdate] = useState<number>(Date.now());
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [attemptHistory, setAttemptHistory] = useState<Attempt[]>([]);
  const [guessInput, setGuessInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [revealedTrack, setRevealedTrack] = useState<Track | null>(null);


  useEffect(() => { attemptsRef.current = attempts; }, [attempts]);
  useEffect(() => { isPlayingRef.current = isPlaying; }, [isPlaying]);
  useEffect(() => { playStartTimeRef.current = playStartTime; }, [playStartTime]);
  useEffect(() => { pausedProgressMsRef.current = pausedProgressMs; }, [pausedProgressMs]);


  useEffect(() => { isLoadingRef.current = isLoading; }, [isLoading]);

  useEffect(() => {
    loadTracks();
  }, []);

  useEffect(() => {
    return () => {
      clearPlaybackTimers();
    };
  }, []);

  const loadTracks = () => {
    fetch('/api/tracks/pool')
      .then(r => r.json())
      .then(data => setTracks(data));
  };

  const importPlaylist = async () => {
    if (!playlistUrl.trim()) return;
    setImporting(true);
    try {
      const res = await fetch('/api/playlists/import', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: playlistUrl.trim() }),
      });
      if (!res.ok) {
        const err = await res.text();
        setFeedback(err || t('singleplayer.feedback.importErrorResponse'));
        setImporting(false);
        return;
      }
      const data = await res.json();
      setFeedback(t('singleplayer.feedback.importSuccess', { name: data.name, count: data.trackCount }));
      loadTracks();
      setPlaylistUrl('');
    } catch {
      setFeedback(t('singleplayer.feedback.importError'));
    }
    setImporting(false);
    setTimeout(() => setFeedback(''), 3000);
  };

  const startGame = () => {
    if (tracks.length === 0) {
      setFeedback(t('singleplayer.needPlaylist'));
      return;
    }

    setTracks(prev => [...prev].sort(() => Math.random() - 0.5));
    setGameStarted(true);
    setCurrentTrackIndex(0);
    setAttempts(0);
    setScore(0);
    setArtistGuessed(false);
    artistPointsAwardedRef.current = 0;
    setGameOver(false);
    setIsPlaying(false);
    setAttemptHistory([]);
    setGuessInput('');
    setPlayStartTime(null);
    setPausedProgressMs(0);
    setPlaybackProgressMs(0);
    setLastPlaybackUpdate(Date.now());
    setRevealedTrack(null);

    attemptsRef.current = 0;
    isPlayingRef.current = false;
    playStartTimeRef.current = null;
    pausedProgressMsRef.current = 0;
  };

  const currentTrack = tracks[currentTrackIndex];

  const nextTrack = useCallback(() => {
    clearPlaybackTimers();
    if (deviceId) {
      fetch('/api/spotify/pause', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deviceId }),
      });
    }

    if (currentTrackIndex >= tracks.length - 1) {
      setGameOver(true);
      return;
    }

    setCurrentTrackIndex(prev => prev + 1);
    setAttempts(0);
    setIsPlaying(false);
    setArtistGuessed(false);
    artistPointsAwardedRef.current = 0;
    setAttemptHistory([]);
    setFeedback('');
    setGuessInput('');
    setPlayStartTime(null);
    setPausedProgressMs(0);
    setPlaybackProgressMs(0);
    setLastPlaybackUpdate(Date.now());
    setRevealedTrack(null);

    attemptsRef.current = 0;
    isPlayingRef.current = false;
    playStartTimeRef.current = null;
    pausedProgressMsRef.current = 0;
  }, [deviceId, currentTrackIndex, tracks.length]);

  const stopPlayback = useCallback(async () => {
    if (!deviceId || isLoadingRef.current) return;
    isLoadingRef.current = true;
    setIsLoading(true);

    clearPlaybackTimers();
    playStartTimeRef.current = null;

    let savedProgress = 0;
    try {
      const res = await fetch('/api/spotify/pause', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deviceId }),
      });
      if (res.ok) {
        const data = await res.json();
        if (typeof data.progressMs === 'number') {
          savedProgress = data.progressMs;
        }
      }
    } catch (e) {
      console.error('Pause error', e);
    }

    const snippetDuration = SNIPPETS[Math.min(attemptsRef.current, SNIPPETS.length - 1)];
    pausedProgressMsRef.current = Math.max(0, Math.min(savedProgress, snippetDuration));

    setIsPlaying(false);
    setPlayStartTime(null);
    setPausedProgressMs(pausedProgressMsRef.current);
    setPlaybackProgressMs(pausedProgressMsRef.current);
    setLastPlaybackUpdate(Date.now());

    isLoadingRef.current = false;
    setIsLoading(false);
  }, [deviceId]);

  const playSnippet = useCallback(async (forcedAttempt?: number, resume = false) => {
    if (!deviceId || !currentTrack || isLoadingRef.current) return;
    isLoadingRef.current = true;
    setIsLoading(true);

    const attempt = forcedAttempt !== undefined ? forcedAttempt : attemptsRef.current;
    const snippetDuration = SNIPPETS[Math.min(attempt, SNIPPETS.length - 1)];
    const startFrom = resume ? Math.min(pausedProgressMsRef.current, snippetDuration) : 0;
    const playDuration = snippetDuration - startFrom;

    if (playDuration <= 0) {

      isLoadingRef.current = false;
      setIsLoading(false);
      setIsPlaying(false);
      isPlayingRef.current = false;
      setPlayStartTime(null);
      playStartTimeRef.current = null;
      setPausedProgressMs(snippetDuration);
      setPlaybackProgressMs(snippetDuration);
      setLastPlaybackUpdate(Date.now());
      pausedProgressMsRef.current = snippetDuration;
      return;
    }

    try {
      const playRes = await fetch('/api/spotify/play', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          trackId: currentTrack.spotifyTrackId,
          positionMs: Math.max(0, startFrom),
          deviceId,
        }),
      });

      if (!playRes.ok) {
        const err = await playRes.text();
        console.error('Play failed:', err);
        setFeedback(t('singleplayer.feedback.playError'));
        setTimeout(() => setFeedback(''), 2000);
        return;
      }

      const now = Date.now();
      playStartTimeRef.current = now;
      setPlayStartTime(now);
      setIsPlaying(true);
      isPlayingRef.current = true;
      setPlaybackProgressMs(startFrom);
      setLastPlaybackUpdate(Date.now());

      clearPlaybackTimers();
      playTimeoutRef.current = window.setTimeout(async () => {
        if (isPlayingRef.current) {
          try {
            await fetch('/api/spotify/pause', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ deviceId }),
            });
          } catch (e) {
            console.error('Auto-pause error', e);
          }
          const snippetDuration = SNIPPETS[Math.min(attemptsRef.current, SNIPPETS.length - 1)];
          setIsPlaying(false);
          isPlayingRef.current = false;
          setPlayStartTime(null);
          playStartTimeRef.current = null;
          setPausedProgressMs(0);
          pausedProgressMsRef.current = 0;
          setPlaybackProgressMs(snippetDuration);
          setLastPlaybackUpdate(Date.now());
          clearInterval(safetyIntervalRef.current);
        }
      }, playDuration);

      const snippetDuration = SNIPPETS[Math.min(attempt, SNIPPETS.length - 1)];
      const startTime = Date.now();
      safetyIntervalRef.current = window.setInterval(async () => {
        if (Date.now() - startTime > snippetDuration + 3000) {
          clearInterval(safetyIntervalRef.current);
          return;
        }
        try {
          const res = await fetch('/api/spotify/playback');
          if (!res.ok) return;
          const data = await res.json();
          if (data.playing && typeof data.progressMs === 'number' && data.progressMs > snippetDuration + 300) {
            await fetch('/api/spotify/pause', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ deviceId }),
            });
            setIsPlaying(false);
            isPlayingRef.current = false;
            setPlayStartTime(null);
            playStartTimeRef.current = null;
            setPausedProgressMs(0);
            pausedProgressMsRef.current = 0;
            setPlaybackProgressMs(snippetDuration);
            setLastPlaybackUpdate(Date.now());
            clearInterval(safetyIntervalRef.current);
          }
        } catch {
          // ignore
        }
      }, 500);

      [500, 1000, 1500, 2000, 3000, 5000].forEach(delay => {
        const id = window.setTimeout(() => {
          if (deviceId) {
            fetch('/api/spotify/pause', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ deviceId }),
            }).catch(() => {});
          }
        }, playDuration + delay);
        safetyPauseTimeoutsRef.current.push(id);
      });
    } catch (e) {
      console.error('Play error', e);
      setFeedback(t('singleplayer.feedback.playFailed'));
      setTimeout(() => setFeedback(''), 2000);
    } finally {
      isLoadingRef.current = false;
      setIsLoading(false);
    }
  }, [deviceId, currentTrack]);

  const advanceAttempt = async () => {
    if (attemptsRef.current >= SNIPPETS.length - 1) {
      clearPlaybackTimers();
      if (deviceId) {
        await fetch('/api/spotify/pause', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ deviceId }),
        });
      }
      setIsPlaying(false);
      isPlayingRef.current = false;
      setPlayStartTime(null);
      playStartTimeRef.current = null;
      setPausedProgressMs(0);
      pausedProgressMsRef.current = 0;
      setPlaybackProgressMs(0);
      setLastPlaybackUpdate(Date.now());
      setFeedback(t('singleplayer.feedback.skipped', { track: currentTrack.name, artist: currentTrack.artistName }));
      setRevealedTrack(currentTrack);
      setTimeout(() => {
        nextTrack();
      }, 3000);
      return;
    }

    clearPlaybackTimers();
    if (deviceId && isPlayingRef.current) {
      await fetch('/api/spotify/pause', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deviceId }),
      });
    }
    setIsPlaying(false);
    isPlayingRef.current = false;
    setPlayStartTime(null);
    playStartTimeRef.current = null;
    setPausedProgressMs(0);
    pausedProgressMsRef.current = 0;
    setPlaybackProgressMs(0);
    setLastPlaybackUpdate(Date.now());

    const nextAttempt = attemptsRef.current + 1;
    setAttempts(nextAttempt);
    attemptsRef.current = nextAttempt;
  };

  const handleGuess = async (guessTrack: Track) => {
    if (!currentTrack || gameOver || isLoadingRef.current) return;

    const guessText = guessTrack.name;
    const guessArtist = guessTrack.artistName;
    const guessTokens = tokenize(guessText);
    const trackTokens = tokenize(normalizeTitle(currentTrack.name));

    const trackMatch = containsTokensInOrder(guessTokens, trackTokens);

    const guessLower = guessText.toLowerCase().trim();
    const artistLower = currentTrack.artistName.toLowerCase().trim();
    const textArtistMatch = guessLower.length >= 2 && artistLower.includes(guessLower);
    const selectedArtistMatch = guessArtist.length > 0 &&
      guessArtist.toLowerCase().trim() === artistLower;
    const artistMatch = textArtistMatch || selectedArtistMatch;

    const basePoints = POINTS[Math.min(attemptsRef.current, POINTS.length - 1)];

    if (trackMatch) {
      // Round score is the better of: track-only score for this attempt, or artist-only score already awarded.
      const trackPoints = Math.max(0, basePoints - artistPointsAwardedRef.current);
      clearPlaybackTimers();
      if (deviceId) {
        fetch('/api/spotify/pause', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ deviceId }),
        });
      }
      setScore(prev => prev + trackPoints);
      setIsPlaying(false);
      isPlayingRef.current = false;
      setPlayStartTime(null);
      playStartTimeRef.current = null;
      setPausedProgressMs(0);
      setPlaybackProgressMs(0);
      setLastPlaybackUpdate(Date.now());
      pausedProgressMsRef.current = 0;
      setFeedback(t('singleplayer.feedback.correct', { points: trackPoints }));
      setAttemptHistory(prev => [...prev, { text: guessText, type: 'correct' }]);
      setGuessInput('');
      setRevealedTrack(currentTrack);

      setTimeout(() => {
        nextTrack();
      }, 2500);
    } else if (artistMatch) {
      if (!artistGuessed) {
        const halfPoints = Math.floor(basePoints / 2);
        artistPointsAwardedRef.current = halfPoints;
        setScore(prev => prev + halfPoints);
        setArtistGuessed(true);
        setFeedback(t('singleplayer.feedback.artist', { points: halfPoints }));
      } else {
        setFeedback(t('singleplayer.feedback.artistAlreadyGuessed'));
      }
      setAttemptHistory(prev => [...prev, { text: guessText, type: 'artist' }]);
      setGuessInput('');
      await advanceAttempt();
    } else {
      setFeedback(t('singleplayer.feedback.wrong'));
      setAttemptHistory(prev => [...prev, { text: guessText, type: 'wrong' }]);
      setGuessInput('');
      await advanceAttempt();
    }
  };

  const skipAttempt = async () => {
    setAttemptHistory(prev => [...prev, { text: '', type: 'skip' }]);
    await advanceAttempt();
  };

  const skipTrack = () => {
    clearPlaybackTimers();
    if (deviceId) {
      fetch('/api/spotify/pause', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deviceId }),
      });
    }
    setFeedback(t('singleplayer.feedback.skipped', { track: currentTrack.name, artist: currentTrack.artistName }));
    setRevealedTrack(currentTrack);
    setTimeout(() => {
      nextTrack();
    }, 2500);
  };

  const handleSeek = async (positionMs: number) => {
    if (!gameStarted || !currentTrack || isLoadingRef.current) return;

    const snippetDuration = SNIPPETS[Math.min(attemptsRef.current, SNIPPETS.length - 1)];
    let targetMs = Math.max(0, Math.min(positionMs, snippetDuration));


    if (targetMs > snippetDuration - 200) {
      targetMs = snippetDuration;
      clearPlaybackTimers();
      if (deviceId && isPlayingRef.current) {
        fetch('/api/spotify/pause', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ deviceId }),
        });
      }
      setIsPlaying(false);
      isPlayingRef.current = false;
      setPlayStartTime(null);
      playStartTimeRef.current = null;
      setPausedProgressMs(targetMs);
      setPlaybackProgressMs(targetMs);
      setLastPlaybackUpdate(Date.now());
      pausedProgressMsRef.current = targetMs;
      return;
    }

    clearPlaybackTimers();


    if (deviceId && isPlayingRef.current) {
      try {
        await fetch('/api/spotify/pause', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ deviceId }),
        });
      } catch (e) {
        console.error('Seek pause error', e);
      }
    }

    setIsPlaying(false);
    isPlayingRef.current = false;
    setPlayStartTime(null);
    playStartTimeRef.current = null;
    setPausedProgressMs(targetMs);
    setPlaybackProgressMs(targetMs);
    setLastPlaybackUpdate(Date.now());
    pausedProgressMsRef.current = targetMs;


    await playSnippet(undefined, true);
  };

  const togglePause = async () => {
    if (isLoadingRef.current) return;
    if (isPlayingRef.current) {
      await stopPlayback();
    } else {
      await playSnippet(undefined, true);
    }
  };

  if (gameOver) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center px-4 bg-transparent">
        <h1 className="text-4xl font-bold mb-4 text-brand-text">{t('singleplayer.gameOver.title')}</h1>
        <p className="text-2xl text-brand-primary mb-8 font-bold">{t('singleplayer.gameOver.score', { score })}</p>
        <button
          onClick={() => navigate('/')}
          className="bg-brand-primary hover:bg-brand-secondary font-semibold text-brand-text font-bold py-3 px-8 rounded-full transition-colors"
        >
          {t('singleplayer.gameOver.home')}
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-transparent text-brand-text px-4 py-4 max-w-xl mx-auto flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <button onClick={() => navigate('/')} className="p-2 hover:bg-brand-surface/50 rounded-full transition-colors">
          <ArrowLeft className="w-5 h-5" />
        </button>
        <VolumeSlider deviceId={deviceId} />
        <div className="text-right">
          <div className="text-2xl font-bold text-brand-primary">{gameStarted ? score : 0}</div>
          <div className="text-xs text-brand-muted">{t('common.points')}</div>
        </div>
      </div>

      <SpotifyPlayer onDeviceId={setDeviceId} onReady={setPlayerReady} />

      {!gameStarted && (
        <div className="mt-6 space-y-4 flex-1">
          <div className="bg-brand-panel/80 backdrop-blur-sm rounded-2xl p-6 border border-brand-border/50">
            <h2 className="text-lg font-bold mb-2">{t('singleplayer.importPlaylist')}</h2>
            <p className="text-brand-muted text-sm mb-4">
              {t('singleplayer.importDesc')}
            </p>
            <div className="flex gap-3">
              <input
                type="text"
                value={playlistUrl}
                onChange={(e) => setPlaylistUrl(e.target.value)}
                placeholder="https://open.spotify.com/playlist/..."
                className="flex-1 bg-brand-surface/80 backdrop-blur-sm border border-brand-border/50 rounded-xl py-3 px-4 text-brand-text placeholder-brand-muted focus:outline-none focus:border-brand-primary"
              />
              <button
                onClick={importPlaylist}
                disabled={importing || !playlistUrl.trim()}
                className="bg-brand-primary hover:bg-brand-secondary disabled:opacity-50 font-semibold text-brand-text font-bold px-5 rounded-xl transition-colors"
              >
                {importing ? '...' : t('common.import')}
              </button>
            </div>
          </div>

          {tracks.length > 0 && (
            <div className="bg-brand-panel/80 backdrop-blur-sm rounded-2xl p-6 text-center border border-brand-border/50">
              <p className="text-lg font-medium">{t('singleplayer.tracksCount', { count: tracks.length })}</p>
              <button
                onClick={startGame}
                className="mt-4 bg-brand-primary hover:bg-brand-secondary font-semibold text-brand-text font-bold py-3 px-8 rounded-full transition-colors"
              >
                <Play className="w-5 h-5 inline mr-2" />
                {t('singleplayer.startGame')}
              </button>
            </div>
          )}

          {feedback && (
            <div className={`text-center py-3 rounded-xl ${
              feedback.startsWith('✅') ? 'bg-brand-primary/30 text-brand-primary border border-brand-primary/20' :
              feedback.startsWith('❌') ? 'bg-red-900/30 text-red-400 border border-red-500/20' :
              'bg-brand-surface/80 backdrop-blur-sm'
            }`}>
              {feedback}
            </div>
          )}
        </div>
      )}

      {gameStarted && currentTrack && (
        <div className="mt-4 flex-1 flex flex-col">
          {/* Track info */}
          <div className="flex justify-center gap-4 mb-4">
            <div className="bg-brand-panel/80 backdrop-blur-sm rounded-full px-4 py-1.5 text-sm border border-brand-border/50">
              <span className="text-brand-muted">{t('singleplayer.trackInfo', { current: currentTrackIndex + 1, total: tracks.length })}</span>
            </div>
          </div>

          {/* Attempt history */}
          <div className="mb-4">
            <AttemptHistory
              attempts={attemptHistory}
              currentAttempt={attempts}
              artistName={currentTrack.artistName}
              trackName={currentTrack.name}
            />
          </div>

          {/* Timeline */}
          <div className="mb-6">
            <ProgressBar
              attempts={attempts}
              isPlaying={isPlaying}
              progressMs={playbackProgressMs}
              lastPlaybackUpdate={lastPlaybackUpdate}
              onSeek={handleSeek}
            />
          </div>

          {/* Play/Pause button */}
          <div className="flex justify-center mb-6">
            <button
              onClick={togglePause}
              disabled={!deviceId || !playerReady || isLoading}
              className="w-20 h-20 bg-brand-surface/80 backdrop-blur-sm hover:bg-brand-surface/70 disabled:opacity-30 rounded-full flex items-center justify-center transition-all hover:scale-105 border border-brand-border/50 shadow-lg shadow-black/50"
            >
              {isPlaying ? (
                <Pause className="w-8 h-8 text-brand-primary" />
              ) : (
                <Play className="w-8 h-8 text-brand-primary ml-1" />
              )}
            </button>
          </div>

          {/* Guess input + skip attempt */}
          <div className="flex gap-3 mb-4">
            <div className="flex-1 relative">
              <SearchInput
                onSelect={handleGuess}
                placeholder={t('singleplayer.guessPlaceholder')}
                value={guessInput}
                onValueChange={setGuessInput}
              />
            </div>
            <button
              onClick={skipAttempt}
              disabled={isLoading}
              className="bg-brand-surface/80 backdrop-blur-sm hover:bg-brand-surface/70 disabled:opacity-50 text-brand-text/90 font-medium px-4 rounded-xl flex items-center gap-2 transition-colors border border-brand-border/50"
            >
              <SkipForward className="w-4 h-4" />
              {t('common.skip')}
            </button>
          </div>

          {/* Skip track */}
          <div className="flex justify-center mb-4">
            <button
              onClick={skipTrack}
              disabled={isLoading}
              className="text-sm text-brand-muted hover:text-brand-text/90 transition-colors"
            >
              {t('singleplayer.skipTrack')}
            </button>
          </div>

          {/* Feedback */}
          {feedback && (
            <div className={`text-center py-3 rounded-xl text-sm font-medium ${
              feedback.startsWith('✅') ? 'bg-brand-primary/30 text-brand-primary border border-brand-primary/20' :
              feedback.startsWith('🎤') ? 'bg-yellow-900/30 text-yellow-400 border border-yellow-500/20' :
              feedback.startsWith('❌') ? 'bg-red-900/30 text-red-400 border border-red-500/20' :
              'bg-brand-surface/80 backdrop-blur-sm text-brand-text/90'
            }`}>
              {feedback}
            </div>
          )}
        </div>
      )}

      {/* Correct-answer overlay */}
      {revealedTrack && <TrackReveal track={revealedTrack} />}
    </div>
  );
}
