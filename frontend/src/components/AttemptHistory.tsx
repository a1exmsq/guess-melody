import { useTranslation } from 'react-i18next';

interface Attempt {
  text: string;
  type: 'skip' | 'wrong' | 'artist' | 'correct';
}

interface Props {
  attempts: Attempt[];
  currentAttempt: number;
}

const TYPE_STYLES = {
  skip: { bg: 'bg-brand-surface/60', text: 'text-brand-muted', labelKey: 'history.skip' },
  wrong: { bg: 'bg-brand-surface/60', text: 'text-brand-text/80', labelKey: null },
  artist: { bg: 'bg-yellow-900/20 border-yellow-500/30', text: 'text-yellow-400', labelKey: 'history.artist' },
  correct: { bg: 'bg-brand-primary/20 border-brand-primary/30', text: 'text-brand-primary', labelKey: 'history.correct' },
};

export default function AttemptHistory({ attempts, currentAttempt }: Props) {
  const { t } = useTranslation();
  const totalSlots = 6;
  
  return (
    <div className="space-y-2">
      {Array.from({ length: totalSlots }).map((_, i) => {
        const attempt = attempts[i];
        const isCurrent = i === currentAttempt;
        
        if (!attempt && !isCurrent) {
          return (
            <div key={i} className="h-12 bg-brand-surface/40 rounded-xl" />
          );
        }
        
        const style = attempt ? TYPE_STYLES[attempt.type] : null;
        
        return (
          <div
            key={i}
            className={`h-12 rounded-xl flex items-center justify-center text-sm font-medium transition-all ${
              isCurrent 
                ? 'bg-brand-surface/80 backdrop-blur-sm border-2 border-brand-primary text-brand-text' 
                : style 
                  ? `${style.bg} ${style.text} border ${attempt.type === 'artist' || attempt.type === 'correct' ? 'border-opacity-30' : 'border-transparent'}`
                  : 'bg-brand-surface/40'
            }`}
          >
            {isCurrent && !attempt && (
              <span className="text-brand-muted animate-pulse">...</span>
            )}
            {attempt && (
              <span className={isCurrent ? '' : ''}>
                {attempt.type === 'skip' ? t('history.skip') : attempt.text}
                {style?.labelKey && <span className="ml-2 text-xs opacity-70">{t(style.labelKey)}</span>}
              </span>
            )}
          </div>
        );
      })}
    </div>
  );
}
