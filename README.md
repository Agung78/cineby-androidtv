# Cineby for Android TV 📺🎬

A modern, high-performance, **100% native Android TV** application for streaming movies and TV series with a Netflix-style Leanback user interface.

The project is split into two complementary modules:
1. **Android TV Client (`app/`)**: Built in Kotlin, JetBrains Compose for TV (`androidx.tv.material3`), and AndroidX Media3 (ExoPlayer). **Zero WebView/Chromium overhead** — lightweight and responsive on entry-level TV hardware.
2. **Backend API (`Backend API/`)**: A Fastify TypeScript service. Catalog comes from **TMDB** (trending, top-rated, genres, search, details); playback resolves **VidNest embed-backend streams** (AES-GCM API, 10-server fallback chain) into ready-to-stream HLS/mp4 endpoints.

> Source model: `com.cineby.*` TV client + self-hosted backend. No WebView, no bundled keys in the APK — the backend holds the TMDB key and VidNest cipher.

---

## 🌟 Features

### 📺 Android TV Client
* **100% Native**: `MediaCodec` via Media3 ExoPlayer. HLS preferred, direct-mp4 fallback per source.
* **Netflix-Style Leanback UI**: floating sidebar, dynamic hero billboard, swimlane rows (Trending / Popular / Series / Genre), Continue Watching with progress bar.
* **In-App Settings**: backend URL + live connection test, persisted on-device — no APK rebuild.
* **Subtitles**: resolver-provided tracks (EN default) with picker modal.

### 🚀 Backend API Service
* **No headless browser**: pure `fetch` — ~128MB RAM, runs on any cheap VPS or locally.
* **TMDB catalog proxy + 15-min LRU cache** (`<15ms` cached responses).
* **All-in-one playback resolver**: `GET /api/playback/resolve` tries VidNest servers in order and returns the first playable HLS/mp4 + subtitles.

---

## 📁 Project Structure

```
cineby-androidtv/
├── app/                          # Android TV (Kotlin + Compose)
│   └── src/main/java/com/cineby/tv/
│       ├── data/
│       │   ├── api/              # CinebyApiService + ApiClient (Retrofit)
│       │   ├── local/            # WatchHistoryStore
│       │   ├── model/            # TMDB-backed + ResolvedPlayback models
│       │   └── repository/       # CinebyRepository (cache + resolve)
│       └── ui/                   # components / navigation / screens / theme
│
├── Backend API/                  # Node.js/Fastify service (no browser)
│   ├── src/
│   │   ├── engine/
│   │   │   └── CinebyScraperEngine.ts  # TMDB proxy + VidNest AES-GCM resolver
│   │   ├── routes/api.ts               # REST endpoints
│   │   └── services/CacheService.ts    # LRU memory cache
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── package.json
```

---

## 🛠️ Getting Started

### 1. Run the Backend API

#### Option A: Docker (recommended)
```bash
cd "Backend API"
docker compose up -d --build
curl http://localhost:5376/health
```

#### Option B: Node.js local
```bash
cd "Backend API"
npm install
npm run build
npm start
```

Optional env (`Backend API/.env.example`):
```bash
TMDB_API_KEY=            # override the built-in TMDB key
VIDNEST_BASE_URL=        # default https://new.vidnest.fun
VIDNEST_CIPHER_KEY=      # pinned to current VidNest web build
```

### 2. Point the TV app at the backend
Settings screen on the TV → enter server IP → Test Connection → Save. Or bake it in before build via `local.properties`:
```properties
backendApiUrl=http://192.168.x.x:5376/
```

### 3. Install the APK on Android TV
```bash
./gradlew assembleRelease
adb connect 192.168.x.tv:5555
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 📡 REST Endpoints (Backend)

| Method | Endpoint | Notes |
| :--- | :--- | :--- |
| `GET` | `/health` | server + resolver status |
| `GET` | `/api/movies?page=&limit=&sort=&genre=` | `sort`: popularity (default), top_rated, now_playing, upcoming; `genre`: TMDB genre id |
| `GET` | `/api/series?page=&limit=&sort=&genre=` | `sort`: popularity (default), top_rated, airing_today |
| `GET` | `/api/movies/:id` | TMDB id, full detail + cast + trailer |
| `GET` | `/api/series/:id` | TMDB id, seasons + S1 episodes |
| `GET` | `/api/series/:id/season/:s/episode/:e` | |
| `GET` | `/api/genres` | TMDB movie+tv genres merged |
| `GET` | `/api/search?q=` | TMDB multi-search, movie+tv only |
| `GET` | `/api/playback/resolve?contentType=&contentId=&contentTitle=` | `contentType`: movie / tv_series / tv / episode; `contentId`: TMDB id, or `seriesId-season-episode` for episodes |

---

## 🔒 Notes
* `local.properties`, keystores (`*.jks`), and env credentials: git-ignored.
* VidNest servers/keys rotate occasionally — if `/api/playback/resolve` starts failing across all servers, re-extract `SERVERS`, the cipher key, and URL shapes from the VidNest web player (`/tmp` notes in build log) and update `CinebyScraperEngine.ts`.
