module.exports = {
  apps: [
    {
      name: 'cineby-backend-api',
      script: './dist/server.js',
      instances: 1,
      autorestart: true,
      watch: false,
      max_memory_restart: '400M',
      env: {
        NODE_ENV: 'production',
        PORT: 5376,
        HOST: '0.0.0.0',
        LOG_LEVEL: 'info',
      },
    },
  ],
};
