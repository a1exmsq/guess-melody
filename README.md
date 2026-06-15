# 🎵 Guess Melody — Real-time Multiplayer Music Guessing Game

A full-stack web application where players compete in real time to guess songs from short audio previews. Built as a portfolio project to practise Java backend development, third-party API integration, WebSocket communication, and modern frontend engineering.

> **Note:** This is an educational pet project. It is not affiliated with or endorsed by Spotify.

---

## ✨ Features

- **Single-player mode** — practise guessing tracks from a default pool
- **Multiplayer rooms** — create or join a room with a 5-character code and play against friends in real time
- **Spotify playlist import** — authenticate with Spotify and import personal playlists for custom games
- **Progressive difficulty** — each round reveals progressively longer audio snippets (0.5s → 1s → ...)
- **Real-time game state** — WebSocket (STOMP) broadcasts round starts, guesses, scores, and winners instantly
- **Room management** — host controls, player list, scoreboard, and round timers
- **Responsive UI** — dark-themed React frontend styled with Tailwind CSS

---

## 🛠️ Tech Stack

### Backend
- **Java 17 / 21**
- **Spring Boot 3.3**
- **Spring WebSocket (STOMP)** — real-time bidirectional communication
- **Spring Data JPA** — data persistence
- **Spring Security** — CORS and basic security configuration
- **Spotify Web API Java wrapper** — playlist import, track search, playback metadata
- **H2** (dev) / **PostgreSQL** (prod)
- **Gradle Kotlin DSL**
- **Lombok**

### Frontend
- **React 19**
- **TypeScript**
- **Vite**
- **React Router**
- **Tailwind CSS**
- **Spotify Web Playback SDK** — audio playback

### DevOps / Tools
- **Docker** + **Docker Compose** (optional)
- **Git / GitHub**
- **IntelliJ IDEA**

---

## 🏗️ Architecture

```
com.guessmelody
├── config/         # WebSocket, Spotify, Security, SPA routing
├── controller/     # REST and WebSocket controllers
├── service/        # Business logic interfaces
├── service/impl/   # Business logic implementations
├── repository/     # Spring Data JPA repositories
├── model/          # JPA entities and enums
├── dto/            # Data transfer objects
├── exception/      # Custom exceptions
├── game/           # In-memory game state
└── util/           # Utility classes
```

### Communication Flow
1. The frontend connects to `/ws` via SockJS/STOMP.
2. Players join rooms through `/app/room/{code}/join`.
3. The host starts the game via `/app/room/{code}/start`.
4. The server broadcasts game events to `/topic/room/{code}`:
   - `ROUND_START`
   - `PLAY_TRACK`
   - `GUESS_RESULT`
   - `ROUND_END`
   - `GAME_OVER`
5. Track metadata is fetched from the Spotify Web API; playback uses the Spotify Web Playback SDK.

---

## 🚀 Getting Started

### Prerequisites
- JDK 17 or 21
- Node.js 18+
- Spotify Developer account (free) — [create one here](https://developer.spotify.com/dashboard)
- Docker (optional)

### 1. Spotify Credentials

Create an app in the Spotify Developer Dashboard and add `http://127.0.0.1:8080/api/spotify/callback` to the allowed redirect URIs.

Copy the example environment file and fill in your credentials:

```bash
cp .env.example .env
```

```env
SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
SPOTIFY_REDIRECT_URI=http://127.0.0.1:8080/api/spotify/callback
SERVER_PORT=8080
```

> **Important:** Never commit the `.env` file. It is already listed in `.gitignore`.

### 2. Run the Backend

```bash
./gradlew bootRun
```

Or on Windows:

```bash
gradlew.bat bootRun
```

The backend will start on `http://localhost:8080`.

### 3. Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will start on `http://localhost:5173`.

### 4. Access the H2 Console (dev only)

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:guessmelodydb`
- User: `sa`
- Password: *(leave empty)*

---

## 🐳 Docker Compose (Optional)

To run the backend together with a PostgreSQL database:

```bash
docker-compose up -d
```

This starts:
- PostgreSQL on port `5432`
- The Spring Boot backend on port `8080`

---

## 🧪 Running Tests

```bash
./gradlew test
```

---

## 📸 Screenshots

*(Screenshots and a short demo GIF will be added here.)*

---

## 🛣️ Roadmap / Possible Improvements

- [ ] Replace in-memory room state with Redis for horizontal scaling
- [x] Replace `java.util.Timer` with `ScheduledExecutorService`
- [x] Add unit and integration tests
- [x] Add CI/CD pipeline with GitHub Actions
- [ ] Deploy to AWS / Azure / Render
- [ ] Add spectator mode and persistent game history
- [ ] Add public/private room visibility

---

## 📄 License

This project is for educational and portfolio purposes only.
