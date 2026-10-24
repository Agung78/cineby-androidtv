import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { CinebyEngine } from '../engine/CinebyScraperEngine.js';
import { globalCache } from '../services/CacheService.js';

export const apiRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  const engine = CinebyEngine.getInstance();

  // Health and Status
  fastify.get('/health', async () => {
    const session = engine.getSessionStatus();
    return { status: 'ok', engine: session };
  });

  // Movies list
  fastify.get<{
    Querystring: { page?: string; limit?: string; sort?: string; genre?: string };
  }>('/api/movies', async (req, reply) => {
    const { page = '1', limit = '20', sort, genre } = req.query;
    void limit;
    const cacheKey = `movies:${page}:${sort || ''}:${genre || ''}`;

    const cached = globalCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const data = await engine.listMovies(parseInt(page, 10) || 1, sort, genre);
      globalCache.set(cacheKey, data, 15 * 60 * 1000); // 15 min cache
      return data;
    } catch (err) {
      req.log.error(err, 'Failed to list movies');
      return reply.status(502).send({ error: 'Upstream catalog unavailable' });
    }
  });

  // Series list
  fastify.get<{
    Querystring: { page?: string; limit?: string; sort?: string; genre?: string };
  }>('/api/series', async (req, reply) => {
    const { page = '1', limit = '20', sort, genre } = req.query;
    void limit;
    const cacheKey = `series:${page}:${sort || ''}:${genre || ''}`;

    const cached = globalCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const data = await engine.listSeries(parseInt(page, 10) || 1, sort, genre);
      globalCache.set(cacheKey, data, 15 * 60 * 1000);
      return data;
    } catch (err) {
      req.log.error(err, 'Failed to list series');
      return reply.status(502).send({ error: 'Upstream catalog unavailable' });
    }
  });

  // Movie detail (id = TMDB id)
  fastify.get<{ Params: { slug: string } }>('/api/movies/:slug', async (req, reply) => {
    const { slug } = req.params;
    const cacheKey = `movie_detail:${slug}`;

    const cached = globalCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const data = await engine.movieDetail(slug);
      globalCache.set(cacheKey, data, 30 * 60 * 1000); // 30 min cache
      return data;
    } catch (err) {
      req.log.error(err, 'Failed to fetch movie detail');
      return reply.status(502).send({ error: 'Upstream catalog unavailable' });
    }
  });

  // Series detail (id = TMDB id)
  fastify.get<{ Params: { slug: string } }>('/api/series/:slug', async (req, reply) => {
    const { slug } = req.params;
    const cacheKey = `series_detail:${slug}`;

    const cached = globalCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const data = await engine.seriesDetail(slug);
      globalCache.set(cacheKey, data, 30 * 60 * 1000);
      return data;
    } catch (err) {
      req.log.error(err, 'Failed to fetch series detail');
      return reply.status(502).send({ error: 'Upstream catalog unavailable' });
    }
  });

  // Episode detail
  fastify.get<{
    Params: { slug: string; season: string; episode: string };
  }>('/api/series/:slug/season/:season/episode/:episode', async (req, reply) => {
    const { slug, season, episode } = req.params;
    const cacheKey = `episode:${slug}:${season}:${episode}`;

    const cached = globalCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const data = await engine.episodeDetail(slug, parseInt(season, 10), parseInt(episode, 10));
      globalCache.set(cacheKey, data, 30 * 60 * 1000);
      return data;
    } catch (err) {
      req.log.error(err, 'Failed to fetch episode detail');
      return reply.status(502).send({ error: 'Upstream catalog unavailable' });
    }
  });

  // Search
  fastify.get<{ Querystring: { q?: string } }>('/api/search', async (req, reply) => {
    const query = req.query.q?.trim();
    if (!query) {
      return { results: [], total: 0 };
    }

    const cacheKey = `search:${query.toLowerCase()}`;
    const cached = globalCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const data = await engine.search(query);
      globalCache.set(cacheKey, data, 10 * 60 * 1000);
      return data;
    } catch (err) {
      req.log.error(err, 'Failed to search');
      return reply.status(502).send({ error: 'Upstream catalog unavailable' });
    }
  });

  // High-performance All-in-One Playback Resolver
  // contentType: 'movie' | 'tv' | 'tv_series' | 'episode'
  // contentId: TMDB movie id, TMDB series id, or seriesId-season-episode (see episodeDetail ids)
  fastify.get<{
    Querystring: { contentType?: string; contentId?: string; contentTitle?: string };
  }>('/api/playback/resolve', async (req, reply) => {
    const { contentType, contentId, contentTitle } = req.query;
    if (!contentType || !contentId) {
      return reply.status(400).send({ error: 'contentType and contentId are required' });
    }

    // ponytail: 5-min cache, streams rotate slowly. Repeat clicks = zero upstream.
    const cacheKey = `pb:${contentType}:${contentId}`;
    const hit = globalCache.get(cacheKey);
    if (hit) return hit;

    try {
      const playback = await engine.resolvePlayback(contentType, contentId, contentTitle);
      globalCache.set(cacheKey, playback, 5 * 60 * 1000);
      return playback;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to resolve stream playback';
      req.log.error(err, 'Failed to resolve stream playback');
      return reply.status(502).send({ error: message });
    }
  });
};
