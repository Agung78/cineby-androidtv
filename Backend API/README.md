# Cineby TV - Backend API (TMDB catalog + VidNest playback)

Pure HTTP proxy, no browser. Android TV (ExoPlayer) streams video direct from upstream CDNs.

## Fitur Utama
1. **TMDB Catalog**: movie/series list, detail, search via TMDB REST API.
2. **In-Memory Caching (LRU)**: 10-30 min per endpoint, <15ms on hit.
3. **All-in-One Playback Resolver**: `GET /api/playback/resolve` tries 10 VidNest servers serially, decrypts AES-GCM, returns HLS master URL + subs.
4. **Ringan**: ~60 MB RAM, single Node process or Cloudflare Worker.

---

## Cara Menjalankan di VPS (Rekomendasi Docker)

1. Clone repo atau upload folder `Backend API` ke VPS Anda:
   ```bash
   cd "Backend API"
   ```

2. Bangun dan jalankan kontainer:
   ```bash
   docker compose up -d --build
   ```

3. Cek log apakah service sudah aktif:
   ```bash
   docker compose logs -f
   ```

4. Uji endpoint:
   ```bash
   curl http://localhost:5376/health
   curl "http://localhost:5376/api/movies?page=1&limit=5"
   ```

---

## Cara Menjalankan Secara Lokal (Node.js)

1. Masuk ke direktori:
   ```bash
   cd "Backend API"
   ```
2. Install dependensi:
   ```bash
   npm install
   ```
3. Jalankan development server:
   ```bash
   npm run dev
   ```
4. Atau bangun untuk produksi:
   ```bash
   npm run build
   npm start
   ```

---

## REST Endpoints Tersedia
- `GET /health` : Status server
- `GET /api/movies?page=1&limit=20&sort=popularity&genre=slug` : Daftar film
- `GET /api/series?page=1&limit=20&sort=popularity&genre=slug` : Daftar serial TV
- `GET /api/movies/:slug` : Detail film
- `GET /api/series/:slug` : Detail serial beserta episode
- `GET /api/series/:slug/season/:season/episode/:episode` : Detail episode
- `GET /api/search?q=:query` : Pencarian film/serial
- `GET /api/playback/resolve?contentType=:type&contentId=:id&contentTitle=:title` : Resolusi tuntas playlist video HLS & subtitle

---

## Deploy: Cloudflare Workers (free, no CC) vs Lokal Mac

Same engine, two entries. `src/server.ts` (Fastify, local) untouched; `src/worker.ts` (Hono, edge) shares `CinebyScraperEngine`.

```bash
cd "Backend API"
npx wrangler login
# optional overrides, else baked-in defaults are used:
npx wrangler secret put TMDB_API_KEY
npx wrangler secret put VIDNEST_CIPHER_KEY
npx wrangler deploy
```

TV app: Settings -> enter `https://<worker>.workers.dev/` -> Test Connection -> Save & Apply.
Fallback: if the worker dies, follow `../RUN_BACKEND.md` (Mac LAN `http://<LAN_IP>:5376/`), flip the TV URL back. No rebuild.
