import { useEffect, useState, useRef } from 'react';
import { useTranslation } from 'react-i18next';

const SNIPPETS = [500, 1000, 2000, 4000, 8000, 16000];
const LABELS = ['0.5s', '1s', '2s', '4s', '8s', '16s'];
const MAX_MS = SNIPPETS[SNIPPETS.length - 1]; // 16 000ms

interface Props {
  attempts: number;
  isPlaying: boolean;
  progressMs: number;
  lastPlaybackUpdate: number;
  onSeek: (positionMs: number) => void;
}

export default function ProgressBar({ attempts, isPlaying, progressMs, lastPlaybackUpdate, onSeek }: Props) {
  const { t } = useTranslation();
  const [displayProgress, setDisplayProgress] = useState(0);
  const intervalRef = useRef<number | undefined>(undefined);

  const currentAttempt = Math.min(attempts, SNIPPETS.length - 1);
  const currentMaxMs = SNIPPETS[currentAttempt];
  const currentMaxPct = (currentMaxMs / MAX_MS) * 100;

  useEffect(() => {
    clearInterval(intervalRef.current);

    const update = () => {
      let ms = progressMs;
      if (isPlaying) {
        ms += Date.now() - lastPlaybackUpdate;
      }
      const clamped = Math.min(ms, currentMaxMs);
      const pct = (clamped / MAX_MS) * 100;
      setDisplayProgress(pct);
    };

    update();
    intervalRef.current = window.setInterval(update, 16);

    return () => clearInterval(intervalRef.current);
  }, [isPlaying, progressMs, lastPlaybackUpdate, currentMaxMs]);

  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const pct = Math.max(0, Math.min(x / rect.width, 1));
    const ms = pct * MAX_MS;

    if (ms <= currentMaxMs + 50) {
      onSeek(Math.max(0, ms));
    }
  };

  const formatTime = (ms: number) => {
    const totalSec = Math.floor(ms / 1000);
    const sec = totalSec % 60;
    const min = Math.floor(totalSec / 60);
    const dec = Math.floor((ms % 1000) / 100);
    return `${min}:${String(sec).padStart(2, '0')}.${dec}`;
  };

  const currentMs = isPlaying
    ? Math.min(progressMs + (Date.now() - lastPlaybackUpdate), currentMaxMs)
    : Math.min(progressMs, currentMaxMs);

  return (
    <div className="w-full select-none">
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs uppercase tracking-wider text-slate-400">{t('singleplayer.timelineTitle')}</span>
        <span className="bg-indigo-500 text-white text-xs font-bold px-2 py-0.5 rounded">
          {LABELS[currentAttempt]}
        </span>
      </div>

      <div
        className="relative h-4 bg-slate-900 rounded-full cursor-pointer border border-slate-700 shadow-inner overflow-hidden"
        onClick={handleClick}
      >
        <div
          className="absolute top-0 h-full bg-indigo-500/10"
          style={{ left: 0, width: `${currentMaxPct}%` }}
        />
        <div
          className="absolute left-0 top-0 h-full bg-gradient-to-r from-indigo-500 to-violet-400 shadow-[0_0_16px_rgba(99,102,241,0.6)]"
          style={{ width: `${displayProgress}%` }}
        />
        <div
          className="absolute top-1/2 -translate-y-1/2 h-0.5 border-t-2 border-dashed border-slate-600"
          style={{ left: `${currentMaxPct}%`, width: `${100 - currentMaxPct}%` }}
        />
        {SNIPPETS.slice(0, -1).map((boundMs, i) => {
          const left = (boundMs / MAX_MS) * 100;
          return (
            <div
              key={i}
              className="absolute top-0 h-full w-0.5 bg-slate-600"
              style={{ left: `${left}%` }}
            />
          );
        })}
        <div
          className="absolute top-0 h-full w-1 bg-white shadow-[0_0_10px_rgba(255,255,255,0.8)]"
          style={{ left: `${currentMaxPct}%` }}
        />
      </div>

      <div className="relative h-5 mt-1.5">
        {SNIPPETS.map((boundMs, i) => {
          const left = (boundMs / MAX_MS) * 100;
          return (
            <div
              key={i}
              className={`absolute -translate-x-1/2 text-[10px] font-mono ${
                i === currentAttempt ? 'text-white font-bold' : i < currentAttempt ? 'text-indigo-300' : 'text-slate-600'
              }`}
              style={{ left: `${left}%` }}
            >
              {LABELS[i]}
            </div>
          );
        })}
      </div>

      <div className="flex justify-between mt-1 text-xs font-mono">
        <span className="text-slate-500">0:00.0</span>
        <span className="text-indigo-300 font-bold">
          {formatTime(currentMs)} / {formatTime(MAX_MS)}
        </span>
      </div>
    </div>
  );
}
