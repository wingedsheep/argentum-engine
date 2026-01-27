# Deployment Guide

This project auto-deploys to your server on every push to `main` via GitHub Actions. The workflow builds Docker images,
pushes them to GitHub Container Registry (GHCR), then deploys via SSH.

## Prerequisites

### On your server

1. **Docker and Docker Compose** must be installed.

2. **Traefik** must be running as a reverse proxy with:
    - An entrypoint called `websecure` (HTTPS on port 443)
    - A certificate resolver called `lets-encrypt`
    - A Docker network called `traefik-proxy`

   If the Traefik network doesn't exist yet:
   ```bash
   docker network create traefik-proxy
   ```

3. **SSH access** — generate a deploy key pair (on your local machine):
   ```bash
   ssh-keygen -t ed25519 -C "github-deploy" -f ~/.ssh/deploy_key
   ```
   Copy the **public** key to the server:
   ```bash
   ssh-copy-id -i ~/.ssh/deploy_key.pub your-user@your-server
   ```
   You'll paste the **private** key contents into GitHub Secrets.

## GitHub Secrets

Go to your repository **Settings → Secrets and variables → Actions** and add:

| Secret         | Description                                                  | Example                                  |
|----------------|--------------------------------------------------------------|------------------------------------------|
| `SERVER_HOST`  | Server IP or hostname                                        | `203.0.113.42`                           |
| `SERVER_USER`  | SSH username on the server                                   | `deploy`                                 |
| `SSH_KEY`      | Contents of the private deploy key (`cat ~/.ssh/deploy_key`) | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `APP_DOMAIN`   | Domain for Traefik routing                                   | `argentum.example.com`                   |
| `PROJECT_NAME` | Unique name to avoid Traefik label collisions                | `argentum`                               |
| `DEPLOY_PATH`  | Absolute path on server for the app files                    | `/home/deploy/apps/argentum`             |

`GITHUB_TOKEN` is provided automatically by GitHub Actions — no setup needed.

## How it works

1. **Build job** — Builds two Docker images from the repo and pushes to GHCR:
    - `ghcr.io/<owner>/<repo>/backend:latest` (Spring Boot game server)
    - `ghcr.io/<owner>/<repo>/frontend:latest` (Nginx serving the React SPA)

2. **Deploy job** — SSHs into your server, copies `docker-compose.yml`, generates a `.env` file with secrets, pulls the
   new images, and restarts the containers.

## Traefik routing

The docker-compose labels configure two Traefik routers:

- **`/game*`** → backend container (port 8080) — WebSocket game traffic
- **Everything else** → frontend container (port 80) — React SPA

The frontend's nginx config also proxies `/game` to the backend internally, but in production Traefik handles routing
before nginx is reached.

## First deploy

1. Configure all GitHub Secrets listed above.
2. Make sure Traefik and the `traefik-proxy` network exist on the server.
3. Push to `main`. The workflow triggers automatically.
4. Check the Actions tab for build/deploy logs.

## Manual deploy (on server)

If you need to redeploy manually:

```bash
cd ~/apps/argentum  # or your DEPLOY_PATH
docker compose pull
docker compose up -d --remove-orphans
```

## Troubleshooting

- **Images not pulling** — Make sure the GitHub package visibility is set correctly. Go to the package settings in GHCR
  and ensure your server user (or the repo) has read access.
- **Traefik not routing** — Verify the containers are on the `traefik-proxy` network:
  `docker network inspect traefik-proxy`
- **WebSocket not connecting** — Check that Traefik's entrypoint supports WebSocket upgrades (it does by default).
- **502 Bad Gateway** — The backend container may still be starting. Spring Boot takes a few seconds to initialize.
  Check `docker compose logs backend`.
