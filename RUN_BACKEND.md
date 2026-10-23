# Run Backend on Mac (LAN) for Android TV

Goal: Mac runs `Backend API/`, smart TV (same Wi-Fi) connects to it.
Port: `5376`.

## Start

1. Get current LAN IP:
   ```
   ipconfig getifaddr en0
   ```
2. Make sure Docker Desktop is running, then:
   ```
   cd "Backend API"
   docker compose up -d --build
   ```
3. Verify local + LAN:
   ```
   curl http://localhost:5376/health
   curl http://<LAN_IP>:5376/health
   ```
   - If the firewall prompts for Docker/node, click **Allow**.
4. Keep Mac awake while watching:
   ```
   caffeinate -d
   ```
5. On the TV: app -> Settings -> enter `http://<LAN_IP>:5376/` -> **Test Connection** -> **Save & Apply**.

## Stop / logs / restart

```
cd "Backend API"
docker compose logs -f          # logs
docker compose restart          # restart
docker compose down             # stop
```

## Without Docker (Node)

```
cd "Backend API"
npm install
npm run build
npm start
```

## Troubleshooting

- **TV can't connect**: Mac IP changed (redo step 1 + TV setting); TV and Mac on different networks / guest Wi-Fi client isolation; firewall blocking (System Settings -> Network -> Firewall -> allow Docker).
- **Test from another device first**: phone browser -> `http://<LAN_IP>:5376/health`.
- **Container not up after reboot**: start Docker Desktop (`restart: unless-stopped` is set).
- **Trailing slash required** in the TV URL: `http://<LAN_IP>:5376/`.
- Tip: reserve a fixed IP for the Mac in the router so the TV setting never goes stale.

## Build-time default (optional)

Add to `local.properties` (git-ignored) to bake the URL into a locally built APK:
```
backendApiUrl=http://<LAN_IP>:5376/
```
CI-built APKs ignore this; set the URL in-app instead.
