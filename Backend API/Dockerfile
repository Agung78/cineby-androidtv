# Node.js 22 Slim — no browser needed (pure TMDB + VidNest HTTP APIs)
FROM node:22-slim

ENV NODE_ENV=production \
    PORT=5376 \
    HOST=0.0.0.0

WORKDIR /app

# Copy package descriptors first to leverage docker layer caching
COPY package*.json ./
RUN npm ci --omit=dev

# Copy built files
COPY dist ./dist

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser \
    && chown -R appuser:appuser /app

USER appuser

EXPOSE 5376

CMD ["node", "dist/server.js"]
