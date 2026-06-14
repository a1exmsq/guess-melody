export interface Track {
  id: number;
  spotifyTrackId: string;
  name: string;
  artistName: string;
  albumName: string;
  previewUrl: string | null;
  imageUrl: string | null;
  durationMs: number;
}

export interface GameState {
  currentTrackIndex: number;
  attempts: number;
  score: number;
  isPlaying: boolean;
  artistGuessed: boolean;
  gameOver: boolean;
}

export interface Player {
  nickname: string;
  score: number;
  isHost: boolean;
}

export interface Room {
  code: string;
  name: string;
  status: 'LOBBY' | 'IN_GAME' | 'FINISHED';
  players: Player[];
  currentRound: number;
  maxRounds: number;
}
