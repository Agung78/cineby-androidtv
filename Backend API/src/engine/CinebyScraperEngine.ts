import { createDecipheriv, createHash, randomBytes } from 'node:crypto';

// AES-GCM cipher used by the VidNest embed backend (reversed from its web player).
// Response shape: { v: 2, n: <nonce>, d: <base64url(iv|ciphertext)> }
// Key material: SHA-256(`${nonce}|${MASTER_KEY}`), 12-byte IV prefix.
export interface EngineEnv {
  tmdbApiKey?: string;
  vidnestBaseUrl?: string;
  vidnestCipherKey?: string;
}

// ponytail: module-level mutable config, set once per process/worker isolate.
// Upgrade path: pass env through constructor if second consumer needs isolation.
const procEnv: Record<string, string | undefined> =
  typeof process !== 'undefined' && process.env ? process.env : {};
let TMDB_KEY = procEnv.TMDB_API_KEY || '8871b4dba1715cd776c063a458ae8795';
let VIDNEST_BASE = (procEnv.VIDNEST_BASE_URL || 'https://new.vidnest.fun').replace(/\/+$/, '');
let VIDNEST_CIPHER_KEY = procEnv.VIDNEST_CIPHER_KEY || 'K3p8vQ2mX9rL5wZ0fN7bG1sH4tJ6dC9_y8S3wM1nQ5pL2';

export function configureEngine(env: EngineEnv): void {
  if (env.tmdbApiKey) TMDB_KEY = env.tmdbApiKey;
  if (env.vidnestBaseUrl) VIDNEST_BASE = env.vidnestBaseUrl.replace(/\/+$/, '');
  if (env.vidnestCipherKey) VIDNEST_CIPHER_KEY = env.vidnestCipherKey;
}

const TMDB_API = 'https://api.themoviedb.org/3';
const TMDB_IMG = 'https://image.tmdb.org/t/p';

// VidNest server map (reversed from vidnest.fun web player).
// Each entry: path prefix + type determines the payload shape of the decrypted response.
const SERVERS: Array<{ id: string; name: string; movie: string; tv: string; type: string }> = [
  { id: 'lamda', name: 'Lamda', movie: '/ophim/movie', tv: '/ophim/tv', type: 'lamda' },
  { id: 'catflix', name: 'Catflix', movie: '/yflix/movie', tv: '/yflix/tv', type: 'catflix' },
  { id: 'filxer', name: 'Filxer', movie: '/rogflix/movie', tv: '/rogflix/tv', type: 'filxer' },
  { id: 'gama', name: 'Gama', movie: '/vidzee/movie', tv: '/vidzee/tv', type: 'gama' },
  { id: 'beta', name: 'Beta', movie: '/flixhq/movie', tv: '/flixhq/tv', type: 'beta' },
  { id: 'alfa', name: 'Alfa', movie: '/onehd/movie', tv: '/onehd/tv', type: 'alfa' },
  { id: 'prime', name: 'Prime', movie: '/rpmvid/movie', tv: '/rpmvid/tv', type: 'prime' },
  { id: 'zeta', name: 'Zeta', movie: '/nextgencloudfabric/movie', tv: '/nextgencloudfabric/tv', type: 'zeta' },
  { id: 'hexa', name: 'Hexa', movie: '/vidlink/movie', tv: '/vidlink/tv', type: 'hexa' },
  { id: 'sigma', name: 'Sigma', movie: '/hollymoviehd/movie', tv: '/hollymoviehd/tv', type: 'sigma' },
];

const ISO3_TO_2: Record<string, string> = {
  eng: 'en', ind: 'id', spa: 'es', fre: 'fr', fra: 'fr', ger: 'de', deu: 'de', por: 'pt', ita: 'it',
  jpn: 'ja', kor: 'ko', chi: 'zh', zho: 'zh', ara: 'ar', rus: 'ru', tur: 'tr', tha: 'th', vie: 'vi', hin: 'hi',
};

export interface CinebySource {
  url: string;
  quality: string;
  type: string; // 'hls' | 'mp4'
  headers?: Record<string, string>;
  server: string;
}

export interface ResolvedPlaybackResult {
  title: string;
  hlsMasterUrl: string;
  mp4Url?: string;
  subtitles: Array<{ lang: string; label: string; path: string }>;
  durationSec: number;
  videoId: string;
  server: string;
  sources: CinebySource[];
}

// ---- untrusted-JSON narrowing (TMDB + VidNest payloads arrive as unknown) ----

type Json = Record<string, unknown>;

function rec(v: unknown): Json {
  return v !== null && typeof v === 'object' && !Array.isArray(v) ? (v as Json) : {};
}

function str(o: Json, k: string): string | null {
  const v = o[k];
  return typeof v === 'string' ? v : null;
}

function num(o: Json, k: string): number | null {
  const v = o[k];
  return typeof v === 'number' ? v : null;
}

function arr(o: Json, k: string): unknown[] {
  const v = o[k];
  return Array.isArray(v) ? v : [];
}

function strMap(v: unknown): Record<string, string> | undefined {
  const o = rec(v);
  const out: Record<string, string> = {};
  for (const [k, val] of Object.entries(o)) out[String(k).trim()] = String(val).trim();
  return out;
}

function b64urlEncode(bytes: Uint8Array): string {
  let bin = '';
  for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i]);
  return btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function b64urlDecode(s: string): Uint8Array {
  let t = s.replace(/-/g, '+').replace(/_/g, '/');
  while (t.length % 4 !== 0) t += '=';
  const bin = atob(t);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

async function decryptCipherResponse(payload: unknown): Promise<unknown> {
  const o = rec(payload);
  if (o.v === 2 && typeof o.d === 'string' && typeof o.n === 'string') {
    const key = createHash('sha256').update(`${o.n}|${VIDNEST_CIPHER_KEY}`, 'utf8').digest();
    const raw = b64urlDecode(o.d);
    const decipher = createDecipheriv('aes-256-gcm', key, raw.subarray(0, 12));
    const ct = raw.subarray(12);
    decipher.setAuthTag(ct.subarray(ct.length - 16));
    const a = new Uint8Array(decipher.update(ct.subarray(0, ct.length - 16)));
    const b = new Uint8Array(decipher.final());
    const pt = new Uint8Array(a.length + b.length);
    pt.set(a, 0);
    pt.set(b, a.length);
    const text = new TextDecoder().decode(pt);
    try { return JSON.parse(text) as unknown; } catch { return text; }
  }
  return payload;
}

// ---- TMDB mapping helpers (backend returns the app's existing shapes) ----

function img(path: unknown, size = 'w500'): string | null {
  return typeof path === 'string' && path ? `${TMDB_IMG}/${size}${path}` : null;
}

interface GenreRef { id: string; name: string; slug: string }

function mapMovie(m: Json, genreName: (id: string) => string) {
  const genres: GenreRef[] = arr(m, 'genre_ids')
    .filter((g): g is number => typeof g === 'number')
    .map((g) => ({ id: String(g), name: genreName(String(g)), slug: String(g) }));
  const countries = arr(m, 'origin_country').filter((c): c is string => typeof c === 'string');
  return {
    id: String(num(m, 'id') ?? str(m, 'id') ?? ''),
    title: str(m, 'title') ?? str(m, 'name') ?? '',
    slug: String(num(m, 'id') ?? str(m, 'id') ?? ''),
    posterPath: img(m.poster_path),
    backdropPath: img(m.backdrop_path, 'w1280'),
    releaseDate: str(m, 'release_date') ?? str(m, 'first_air_date'),
    voteAverage: num(m, 'vote_average') != null ? String(m.vote_average) : str(m, 'vote_average'),
    viewCount: Math.round((num(m, 'popularity') ?? 0) * 100),
    quality: 'HD',
    country: countries[0] ?? null,
    runtime: null,
    originalLanguage: str(m, 'original_language'),
    contentType: 'movie',
    genres,
    hasVideo: true,
  };
}

function mapSeries(s: Json, genreName: (id: string) => string) {
  const genres: GenreRef[] = arr(s, 'genre_ids')
    .filter((g): g is number => typeof g === 'number')
    .map((g) => ({ id: String(g), name: genreName(String(g)), slug: String(g) }));
  const countries = arr(s, 'origin_country').filter((c): c is string => typeof c === 'string');
  return {
    id: String(num(s, 'id') ?? str(s, 'id') ?? ''),
    title: str(s, 'name') ?? str(s, 'title') ?? '',
    slug: String(num(s, 'id') ?? str(s, 'id') ?? ''),
    posterPath: img(s.poster_path),
    backdropPath: img(s.backdrop_path, 'w1280'),
    firstAirDate: str(s, 'first_air_date'),
    voteAverage: num(s, 'vote_average') != null ? String(s.vote_average) : str(s, 'vote_average'),
    viewCount: Math.round((num(s, 'popularity') ?? 0) * 100),
    numberOfSeasons: null,
    country: countries[0] ?? null,
    contentType: 'tv_series',
    genres,
    hasVideo: true,
  };
}

export class CinebyEngine {
  private static instance: CinebyEngine | null = null;
  private genres: Array<{ id: number; name: string }> | null = null;

  private constructor() {}

  public static getInstance(): CinebyEngine {
    if (!CinebyEngine.instance) CinebyEngine.instance = new CinebyEngine();
    return CinebyEngine.instance;
  }

  public getSessionStatus(): { isReady: boolean; targetUrl: string } {
    return { isReady: true, targetUrl: VIDNEST_BASE };
  }

  public async init(): Promise<void> {}

  public async close(): Promise<void> {}

  // ---- TMDB passthrough ----

  private async tmdb(path: string, params: Record<string, string | number> = {}): Promise<Json> {
    const qs = new URLSearchParams({ api_key: TMDB_KEY, language: 'en-US' });
    for (const [k, v] of Object.entries(params)) qs.set(k, String(v));
    const ctrl = new AbortController();
    const t = setTimeout(() => ctrl.abort(), 15000);
    try {
      const res = await fetch(`${TMDB_API}${path}?${qs}`, {
        headers: { Accept: 'application/json', 'User-Agent': 'CinebyTV/1.0 (Android TV)' },
        signal: ctrl.signal,
      });
      if (!res.ok) throw new Error(`TMDB ${res.status} for ${path}`);
      return rec(await res.json());
    } finally {
      clearTimeout(t);
    }
  }

  public async getGenres(): Promise<GenreRef[]> {
    if (!this.genres) {
      const [movie, tv] = await Promise.all([this.tmdb('/genre/movie/list'), this.tmdb('/genre/tv/list')]);
      const seen = new Map<number, string>();
      for (const list of [arr(movie, 'genres'), arr(tv, 'genres')]) {
        for (const g of list.map(rec)) {
          const id = num(g, 'id');
          const name = str(g, 'name');
          if (id != null && name && !seen.has(id)) seen.set(id, name);
        }
      }
      this.genres = [...seen.entries()].map(([id, name]) => ({ id, name }));
    }
    return this.genres.map((g) => ({ id: String(g.id), name: g.name, slug: String(g.id) }));
  }

  private genreName(id: string): string {
    return this.genres?.find((x) => String(x.id) === id)?.name ?? '';
  }

  public async listMovies(page = 1, sort?: string, genre?: string) {
    let data: Json;
    if (genre) {
      data = await this.tmdb('/discover/movie', { page, sort_by: 'popularity.desc', with_genres: genre });
    } else if (sort === 'top_rated') {
      data = await this.tmdb('/movie/top_rated', { page });
    } else if (sort === 'now_playing') {
      data = await this.tmdb('/movie/now_playing', { page });
    } else if (sort === 'upcoming') {
      data = await this.tmdb('/movie/upcoming', { page });
    } else {
      data = await this.tmdb('/trending/movie/week', { page });
    }
    await this.getGenres().catch(() => []);
    const gn = (id: string) => this.genreName(id);
    const items = arr(data, 'results').map((m) => mapMovie(rec(m), gn));
    return { data: items, pagination: { page: num(data, 'page') ?? page, limit: 20, total: num(data, 'total_results') ?? items.length, totalPages: num(data, 'total_pages') ?? 1 } };
  }

  public async listSeries(page = 1, sort?: string, genre?: string) {
    let data: Json;
    if (genre) {
      data = await this.tmdb('/discover/tv', { page, sort_by: 'popularity.desc', with_genres: genre });
    } else if (sort === 'top_rated') {
      data = await this.tmdb('/tv/top_rated', { page });
    } else if (sort === 'airing_today') {
      data = await this.tmdb('/tv/airing_today', { page });
    } else {
      data = await this.tmdb('/trending/tv/week', { page });
    }
    await this.getGenres().catch(() => []);
    const gn = (id: string) => this.genreName(id);
    const items = arr(data, 'results').map((s) => mapSeries(rec(s), gn));
    return { data: items, pagination: { page: num(data, 'page') ?? page, limit: 20, total: num(data, 'total_results') ?? items.length, totalPages: num(data, 'total_pages') ?? 1 } };
  }

  private creditsCast(m: Json) {
    return arr(rec(m.credits), 'cast').slice(0, 20).map((c) => {
      const o = rec(c);
      return { id: String(num(o, 'id') ?? ''), name: str(o, 'name') ?? '', character: str(o, 'character'), profilePath: img(o.profile_path, 'w185') };
    });
  }

  private trailer(m: Json): string | null {
    const vids = arr(rec(m.videos), 'results').map(rec);
    const t = vids.find((v) => v.site === 'YouTube' && (v.type === 'Trailer' || v.type === 'Teaser'));
    const key = t ? str(t, 'key') : null;
    return key ? `https://www.youtube.com/watch?v=${key}` : null;
  }

  public async movieDetail(id: string) {
    const m = await this.tmdb(`/movie/${encodeURIComponent(id)}`, { append_to_response: 'credits,videos' });
    await this.getGenres().catch(() => []);
    const countries = arr(m, 'origin_country').filter((c): c is string => typeof c === 'string');
    return {
      id: String(num(m, 'id') ?? id), tmdbId: num(m, 'id') ?? null, imdbId: str(m, 'imdb_id'),
      title: str(m, 'title') ?? '', slug: String(num(m, 'id') ?? id),
      originalTitle: str(m, 'original_title'), overview: str(m, 'overview'), tagline: str(m, 'tagline'),
      posterPath: img(m.poster_path), backdropPath: img(m.backdrop_path, 'w1280'),
      releaseDate: str(m, 'release_date'), duration: num(m, 'runtime'), runtime: num(m, 'runtime'),
      voteAverage: num(m, 'vote_average') != null ? String(m.vote_average) : null,
      viewCount: Math.round((num(m, 'popularity') ?? 0) * 100), quality: 'HD',
      country: countries[0] ?? null,
      genres: arr(m, 'genres').map(rec).map((g) => ({ id: String(num(g, 'id') ?? ''), name: str(g, 'name') ?? '', slug: String(num(g, 'id') ?? '') })),
      cast: this.creditsCast(m),
      trailerUrl: this.trailer(m),
    };
  }

  public async seriesDetail(id: string) {
    const s = await this.tmdb(`/tv/${encodeURIComponent(id)}`, { append_to_response: 'credits,videos' });
    await this.getGenres().catch(() => []);
    const countries = arr(s, 'origin_country').filter((c): c is string => typeof c === 'string');
    const seasons = arr(s, 'seasons').map(rec)
      .filter((x) => (num(x, 'season_number') ?? -1) >= 0)
      .map((x) => ({
        id: `${String(num(s, 'id') ?? id)}-${num(x, 'season_number') ?? 0}`, seasonNumber: num(x, 'season_number') ?? 0,
        name: str(x, 'name') ?? `Season ${num(x, 'season_number') ?? 0}`,
        overview: str(x, 'overview'), posterPath: img(x.poster_path), airDate: str(x, 'air_date'), episodeCount: num(x, 'episode_count') ?? 0,
      }));
    const defNum = seasons.find((x) => x.seasonNumber === 1)?.seasonNumber ?? seasons[0]?.seasonNumber ?? 1;
    const episodes = await this.seasonEpisodes(String(num(s, 'id') ?? id), defNum).catch(() => []);
    return {
      id: String(num(s, 'id') ?? id), tmdbId: num(s, 'id') ?? null, imdbId: null,
      title: str(s, 'name') ?? '', slug: String(num(s, 'id') ?? id),
      originalTitle: str(s, 'original_name'), overview: str(s, 'overview'), tagline: str(s, 'tagline'),
      posterPath: img(s.poster_path), backdropPath: img(s.backdrop_path, 'w1280'), logoPath: null, backdrops: [],
      firstAirDate: str(s, 'first_air_date'),
      numberOfSeasons: num(s, 'number_of_seasons') ?? seasons.length,
      numberOfEpisodes: num(s, 'number_of_episodes') ?? episodes.length,
      voteAverage: num(s, 'vote_average') != null ? String(s.vote_average) : null,
      viewCount: Math.round((num(s, 'popularity') ?? 0) * 100),
      country: countries[0] ?? null,
      status: str(s, 'status'),
      genres: arr(s, 'genres').map(rec).map((g) => ({ id: String(num(g, 'id') ?? ''), name: str(g, 'name') ?? '', slug: String(num(g, 'id') ?? '') })),
      seasons,
      cast: this.creditsCast(s),
      defaultSeason: { id: `${String(num(s, 'id') ?? id)}-${defNum}`, seasonNumber: defNum, name: `Season ${defNum}`, overview: null, posterPath: null, episodes },
    };
  }

  private async seasonEpisodes(seriesId: string, season: number) {
    const s = await this.tmdb(`/tv/${encodeURIComponent(seriesId)}/season/${season}`);
    return arr(s, 'episodes').map(rec).map((e) => ({
      id: `${seriesId}-${season}-${num(e, 'episode_number') ?? 0}`, seasonId: `${seriesId}-${season}`, tvSeriesId: seriesId,
      episodeNumber: num(e, 'episode_number') ?? 0, name: str(e, 'name') ?? `Episode ${num(e, 'episode_number') ?? 0}`,
      overview: str(e, 'overview'), stillPath: img(e.still_path), airDate: str(e, 'air_date'), runtime: num(e, 'runtime'),
      voteAverage: num(e, 'vote_average') != null ? String(e.vote_average) : null, hasVideo: true,
    }));
  }

  public async episodeDetail(seriesId: string, season: number, episode: number) {
    const series = await this.seriesDetail(seriesId);
    const episodes = season === series.defaultSeason.seasonNumber
      ? series.defaultSeason.episodes
      : await this.seasonEpisodes(seriesId, season).catch(() => []);
    return {
      series, season: { ...series.defaultSeason, seasonNumber: season, episodes },
      episode: episodes.find((e) => e.episodeNumber === episode) ?? null,
      allSeasons: series.seasons,
    };
  }

  public async search(q: string) {
    const data = await this.tmdb('/search/multi', { query: q, page: 1, include_adult: 'false' });
    const results = arr(data, 'results').map(rec)
      .filter((r) => (r.media_type === 'movie' || r.media_type === 'tv') && (r.poster_path || r.backdrop_path))
      .slice(0, 20)
      .map((r) => {
        const isTv = r.media_type === 'tv';
        return {
          id: String(num(r, 'id') ?? ''), contentType: isTv ? 'tv_series' : 'movie',
          title: str(r, 'title') ?? str(r, 'name') ?? '',
          originalTitle: str(r, 'original_title') ?? str(r, 'original_name'), overview: str(r, 'overview'), genres: [],
          releaseDate: str(r, 'release_date'), firstAirDate: str(r, 'first_air_date'),
          originalLanguage: str(r, 'original_language'),
          voteAverage: num(r, 'vote_average') != null ? String(r.vote_average) : null,
          viewCount: Math.round((num(r, 'popularity') ?? 0) * 100),
          posterPath: img(r.poster_path), backdropPath: img(r.backdrop_path, 'w1280'), quality: 'HD',
          slug: String(num(r, 'id') ?? ''), numberOfSeasons: null, commentCount: null,
        };
      });
    return { results, total: num(data, 'total_results') ?? results.length };
  }

  // ---- VidNest playback resolver ----

  private async vidnestGet(path: string, tmdbId: string, season?: number, episode?: number, timeoutMs = 20000, signal?: AbortSignal): Promise<unknown> {
    const n = b64urlEncode(randomBytes(16));
    const suffix = season != null && episode != null ? `/${tmdbId}/${season}/${episode}` : `/${tmdbId}`;
    const url = `${VIDNEST_BASE}${path}${suffix}?_n=${encodeURIComponent(n)}`;
    const ctrl = new AbortController();
    const onAbort = (): void => ctrl.abort();
    if (signal) {
      if (signal.aborted) ctrl.abort();
      else signal.addEventListener('abort', onAbort, { once: true });
    }
    const t = setTimeout(() => ctrl.abort(), timeoutMs);
    try {
      const res = await fetch(url, {
        headers: { Accept: 'application/json', 'User-Agent': 'CinebyTV/1.0 (Android TV)', Referer: 'https://vidnest.fun/' },
        signal: ctrl.signal,
      });
      if (!res.ok) throw new Error(`VidNest ${res.status} ${path}`);
      return decryptCipherResponse(await res.json());
    } finally {
      clearTimeout(t);
      if (signal) signal.removeEventListener('abort', onAbort);
    }
  }

  // ponytail: 4-wide pool; losers abort on first hit. Sequential 10x20s was the tail risk.
  private async raceServers(
    isTv: boolean, tmdbId: string, season?: number, episode?: number,
  ): Promise<{ dec: unknown; srv: (typeof SERVERS)[number] }> {
    const errors: string[] = [];
    const queue = [...SERVERS];
    const shared = new AbortController();
    const workers = Array.from({ length: Math.min(4, queue.length) }, async () => {
      while (queue.length > 0 && !shared.signal.aborted) {
        const srv = queue.shift();
        if (!srv) return null;
        try {
          const dec = await this.vidnestGet(isTv ? srv.tv : srv.movie, tmdbId, season, episode, 7000, shared.signal);
          if (!this.pickSources(dec, srv.type, srv.id).length) { errors.push(`${srv.id}: empty`); continue; }
          shared.abort();
          return { dec, srv };
        } catch (e) {
          if (!shared.signal.aborted) errors.push(`${srv.id}: ${e instanceof Error ? e.message : String(e)}`);
        }
      }
      return null;
    });
    const results = await Promise.all(workers);
    const hit = results.find((r): r is NonNullable<typeof r> => r !== null);
    if (hit) return hit;
    throw new Error(`No streams found (${errors.slice(0, 3).join('; ')})`);
  }

  private pickSources(dec: unknown, type: string, server: string): CinebySource[] {
    const out: CinebySource[] = [];
    const d = rec(dec);
    const push = (url: unknown, quality: unknown, kind: unknown, headers?: unknown) => {
      if (typeof url !== 'string' || !url) return;
      const k = typeof kind === 'string' ? kind.toLowerCase() : '';
      out.push({
        url, quality: typeof quality === 'string' && quality ? quality : 'auto',
        type: k === 'hls' || url.includes('.m3u8') ? 'hls' : 'mp4',
        headers: headers !== undefined ? strMap(headers) : undefined, server,
      });
    };
    const stream = rec(d.data).stream;
    const playlist = typeof stream === 'object' && stream !== null ? str(rec(stream), 'playlist') : null;
    if (type === 'hexa' && playlist) {
      push(
        `https://megacloud.animanga.fun/proxy?url=${encodeURIComponent(playlist)}&headers=${encodeURIComponent(JSON.stringify(strMap(d.headers) ?? {}))}`,
        'auto', 'hls', { Referer: 'https://megacloud.animanga.fun/' },
      );
      return out;
    }
    if (typeof d.url === 'string') {
      push(d.url, str(d, 'quality'), d.type === 'mp4' ? 'mp4' : 'hls');
      return out;
    }
    const streams = Array.isArray(d.streams) ? d.streams : Array.isArray(d.sources) ? d.sources : [];
    for (const item of streams.map(rec)) {
      if (typeof item.url !== 'string' || !item.url) continue;
      if (type === 'zeta' || type === 'sigma') {
        push(
          `https://megacloud.animanga.fun/proxy?url=${encodeURIComponent(item.url)}&headers=${encodeURIComponent(JSON.stringify(strMap(item.headers) ?? {}))}`,
          str(item, 'quality') ?? str(item, 'language'), 'hls',
        );
      } else {
        push(item.url, str(item, 'quality') ?? str(item, 'language'), item.type === 'mp4' ? 'mp4' : 'hls', item.headers);
      }
    }
    return out;
  }

  private parseSubtitles(dec: unknown): Array<{ lang: string; label: string; path: string }> {
    return arr(rec(dec), 'subtitles').map(rec)
      .filter((s) => typeof s.url === 'string' && s.url)
      .slice(0, 40)
      .map((s) => {
        const raw = String(str(s, 'lang') ?? str(s, 'label') ?? 'en').toLowerCase();
        const lang = ISO3_TO_2[raw] ?? raw.slice(0, 8); // app matches 2-letter codes ("en")
        return { lang, label: String(str(s, 'label') ?? raw), path: String(s.url) };
      });
  }

  public async resolvePlayback(contentType: string, contentId: string, contentTitle?: string): Promise<ResolvedPlaybackResult> {
    const isTv = contentType === 'tv' || contentType === 'tv_series' || contentType === 'episode';
    let tmdbId = contentId;
    let season: number | undefined;
    let episode: number | undefined;

    if (contentType === 'tv_series') {
      season = 1; episode = 1;
    } else if (contentType === 'episode') {
      const m = /^(\d+)-(\d+)-(\d+)$/.exec(contentId);
      if (m) { tmdbId = m[1]; season = parseInt(m[2], 10); episode = parseInt(m[3], 10); }
    }

    let title = contentTitle || (isTv && season != null ? `TMDB ${tmdbId} S${season}E${episode}` : `TMDB ${tmdbId}`);
    // Meta + race start together; title resolves in background, never blocks HLS.
    const metaP = (isTv ? this.seriesDetail(tmdbId) : this.movieDetail(tmdbId)).catch(() => null);
    const raceP = this.raceServers(isTv, tmdbId, season, episode);
    const [{ dec, srv }, meta] = await Promise.all([raceP, metaP]);
    if (meta?.title) title = meta.title;

    const sources = this.pickSources(dec, srv.type, srv.id);
    const hls = sources.find((s) => s.type === 'hls');
    const mp4 = sources.find((s) => s.type === 'mp4');
    let subs = this.parseSubtitles(dec);
    // Most servers (e.g. lamda) ship no subtitles; borrow them from flixhq, which does.
    if (!subs.length && srv.id !== 'beta') {
      try {
        subs = this.parseSubtitles(await this.vidnestGet(isTv ? '/flixhq/tv' : '/flixhq/movie', tmdbId, season, episode, 5000));
      } catch { /* playback still works without subtitles */ }
    }
    return {
      title, hlsMasterUrl: (hls ?? sources[0]).url, mp4Url: mp4?.url,
      subtitles: subs, durationSec: 0, videoId: tmdbId, server: srv.id, sources,
    };
  }

}
