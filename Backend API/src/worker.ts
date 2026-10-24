import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { CinebyEngine, configureEngine } from './engine/CinebyScraperEngine';
import { globalCache } from './services/CacheService';

interface Env {
  TMDB_API_KEY?: string;
  VIDNEST_BASE_URL?: string;
  VIDNEST_CIPHER_KEY?: string;
}

function cached<T>(key: string, ttlMs: number, load: () => Promise<T>): Promise<T> {
  const hit = globalCache.get<T>(key);
  if (hit !== undefined) return Promise.resolve(hit);
  return load().then((data) => {
    globalCache.set(key, data, ttlMs);
    return data;
  });
}

const app = new Hono<{ Bindings: Env }>();

app.use('*', cors({ origin: '*' }));

app.get('/health', (c) => {
  configureEngine({
    tmdbApiKey: c.env.TMDB_API_KEY,
    vidnestBaseUrl: c.env.VIDNEST_BASE_URL,
    vidnestCipherKey: c.env.VIDNEST_CIPHER_KEY,
  });
  return c.json({ status: 'ok', engine: CinebyEngine.getInstance().getSessionStatus() });
});

function withEnv(c: { env: Env }): void {
  configureEngine({
    tmdbApiKey: c.env.TMDB_API_KEY,
    vidnestBaseUrl: c.env.VIDNEST_BASE_URL,
    vidnestCipherKey: c.env.VIDNEST_CIPHER_KEY,
  });
}

app.get('/api/movies', (c) => {
  withEnv(c);
  const page = parseInt(c.req.query('page') || '1', 10) || 1;
  const sort = c.req.query('sort');
  const genre = c.req.query('genre');
  const key = `movies:${page}:${sort || ''}:${genre || ''}`;
  return cached(key, 15 * 60 * 1000, () =>
    CinebyEngine.getInstance().listMovies(page, sort, genre),
  ).then(
    (data) => c.json(data),
    () => c.json({ error: 'Upstream catalog unavailable' }, 502),
  );
});

app.get('/api/series', (c) => {
  withEnv(c);
  const page = parseInt(c.req.query('page') || '1', 10) || 1;
  const sort = c.req.query('sort');
  const genre = c.req.query('genre');
  const key = `series:${page}:${sort || ''}:${genre || ''}`;
  return cached(key, 15 * 60 * 1000, () =>
    CinebyEngine.getInstance().listSeries(page, sort, genre),
  ).then(
    (data) => c.json(data),
    () => c.json({ error: 'Upstream catalog unavailable' }, 502),
  );
});

app.get('/api/movies/:slug', (c) => {
  withEnv(c);
  const slug = c.req.param('slug');
  return cached(`movie_detail:${slug}`, 30 * 60 * 1000, () =>
    CinebyEngine.getInstance().movieDetail(slug),
  ).then(
    (data) => c.json(data),
    () => c.json({ error: 'Upstream catalog unavailable' }, 502),
  );
});

app.get('/api/series/:slug', (c) => {
  withEnv(c);
  const slug = c.req.param('slug');
  return cached(`series_detail:${slug}`, 30 * 60 * 1000, () =>
    CinebyEngine.getInstance().seriesDetail(slug),
  ).then(
    (data) => c.json(data),
    () => c.json({ error: 'Upstream catalog unavailable' }, 502),
  );
});

app.get('/api/series/:slug/season/:season/episode/:episode', (c) => {
  withEnv(c);
  const { slug, season, episode } = c.req.param();
  const key = `episode:${slug}:${season}:${episode}`;
  return cached(key, 30 * 60 * 1000, () =>
    CinebyEngine.getInstance().episodeDetail(slug, parseInt(season, 10), parseInt(episode, 10)),
  ).then(
    (data) => c.json(data),
    () => c.json({ error: 'Upstream catalog unavailable' }, 502),
  );
});

app.get('/api/search', (c) => {
  withEnv(c);
  const q = (c.req.query('q') || '').trim();
  if (!q) return c.json({ results: [], total: 0 });
  return cached(`search:${q.toLowerCase()}`, 10 * 60 * 1000, () =>
    CinebyEngine.getInstance().search(q),
  ).then(
    (data) => c.json(data),
    () => c.json({ error: 'Upstream catalog unavailable' }, 502),
  );
});

app.get('/api/playback/resolve', (c) => {
  withEnv(c);
  const contentType = c.req.query('contentType');
  const contentId = c.req.query('contentId');
  const contentTitle = c.req.query('contentTitle');
  if (!contentType || !contentId) {
    return c.json({ error: 'contentType and contentId are required' }, 400);
  }
  // ponytail: 5-min cache, streams rotate slowly. Repeat clicks = zero upstream.
  const key = `pb:${contentType}:${contentId}`;
  return cached(key, 5 * 60 * 1000, () =>
    CinebyEngine.getInstance().resolvePlayback(contentType, contentId, contentTitle),
  ).then(
    (playback) => c.json(playback),
    (err: unknown) => c.json({ error: err instanceof Error ? err.message : 'Failed to resolve stream playback' }, 502),
  );
});

export default app;
