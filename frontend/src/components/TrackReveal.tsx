import type { Track } from '../types';

interface Props {
  track: Track;
}

export default function TrackReveal({ track }: Props) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm animate-in fade-in duration-300">
      <div className="bg-brand-panel/95 backdrop-blur-sm border border-brand-border/50 rounded-3xl p-8 max-w-sm w-full mx-4 shadow-2xl shadow-brand-primary/10 transform scale-100 animate-in zoom-in-95 duration-300">
        {/* Album cover */}
        <div className="relative mx-auto w-48 h-48 mb-6 rounded-2xl overflow-hidden shadow-xl">
          {track.imageUrl ? (
            <img
              src={track.imageUrl}
              alt={track.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full bg-brand-surface flex items-center justify-center text-4xl">
              🎵
            </div>
          )}
          {/* Top highlight */}
          <div className="absolute inset-0 bg-gradient-to-b from-white/10 to-transparent pointer-events-none" />
        </div>

        {/* Track info */}
        <div className="text-center space-y-2">
          <h3 className="text-2xl font-bold text-brand-text leading-tight">
            {track.name}
          </h3>
          <p className="text-lg text-brand-primary font-medium">
            {track.artistName}
          </p>
          {track.albumName && (
            <p className="text-sm text-brand-muted">
              {track.albumName}
            </p>
          )}
        </div>

        {/* Decorative line */}
        <div className="mt-6 h-1 bg-brand-surface rounded-full overflow-hidden">
          <div className="h-full bg-brand-primary rounded-full animate-[load_2s_ease-out_forwards]" style={{ width: '100%' }} />
        </div>
      </div>
    </div>
  );
}
