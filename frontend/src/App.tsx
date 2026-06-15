import { BrowserRouter, Routes, Route } from 'react-router-dom';
import BackgroundShapes from './components/BackgroundShapes';
import Home from './pages/Home';
import Singleplayer from './pages/Singleplayer';
import MultiplayerLobby from './pages/MultiplayerLobby';
import Room from './pages/Room';

function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-brand-dark text-brand-text relative">
        <BackgroundShapes />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/singleplayer" element={<Singleplayer />} />
          <Route path="/multiplayer" element={<MultiplayerLobby />} />
          <Route path="/room/:code" element={<Room />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

export default App;
