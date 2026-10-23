import 'dotenv/config';
import Fastify from 'fastify';
import cors from '@fastify/cors';
import { apiRoutes } from './routes/api.js';
import { CinebyEngine } from './engine/CinebyScraperEngine.js';

const fastify = Fastify({
  logger: {
    level: process.env.LOG_LEVEL || 'info',
  },
});

async function main() {
  await fastify.register(cors, {
    origin: true,
  });

  await fastify.register(apiRoutes);

  const port = parseInt(process.env.PORT || '5376', 10);
  const host = process.env.HOST || '0.0.0.0';

  try {
    await fastify.listen({ port, host });
    console.log(`[Server] Cineby Backend API listening on http://${host}:${port}`);

    // No browser session needed: catalog is TMDB, playback is VidNest HTTP API.
    await CinebyEngine.getInstance().init().catch((err: unknown) => {
      console.warn(`[Server] Engine init notice: ${err instanceof Error ? err.message : String(err)}`);
    });
  } catch (err) {
    fastify.log.error(err);
    process.exit(1);
  }
}

// Graceful shutdown
const shutdown = async () => {
  console.log('\n[Server] Shutting down gracefully...');
  await CinebyEngine.getInstance().close();
  await fastify.close();
  process.exit(0);
};

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

main();
