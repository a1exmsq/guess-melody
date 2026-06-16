import { Volume2, VolumeX } from 'lucide-react';
import { spotifyApi } from '../lib/spotifyApi';

interface Props {
  deviceId: string | null;
  volume: number;
  onVolumeChange: (volume: number) => void;
}

export default function VolumeSlider({ deviceId, volume, onVolumeChange }: Props) {
  const isMuted = volume === 0;

  const handleChange = (v: number) => {
    onVolumeChange(v);
    if (deviceId) {
      spotifyApi.volume(deviceId, v).catch(() => {});
    }
  };

  const toggleMute = () => {
    if (isMuted) {
      handleChange(50);
    } else {
      handleChange(0);
    }
  };

  return (
    <div className="flex items-center gap-2">
      <button onClick={toggleMute} className="p-1 hover:bg-brand-surface rounded-full transition-colors">
        {isMuted ? <VolumeX className="w-5 h-5 text-brand-muted" /> : <Volume2 className="w-5 h-5 text-brand-primary" />}
      </button>
      <div className="relative w-24 h-1.5 bg-brand-surface rounded-full">
        <div
          className="absolute left-0 top-0 h-full bg-brand-primary rounded-full"
          style={{ width: `${volume}%` }}
        />
        <input
          type="range"
          min="0"
          max="100"
          value={volume}
          onChange={(e) => handleChange(Number(e.target.value))}
          className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
        />
      </div>
    </div>
  );
}
