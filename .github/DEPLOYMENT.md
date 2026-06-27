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

### Optional: accounts + saved decks + stats (PostgreSQL + magic-link email)

Off by default. To turn it on, add these secrets (see `docs/accounts-and-persistence.md` for the full
design). The deploy writes them into the server `.env`; the bundled `postgres` service stores the data.

| Secret                 | Description                                                                 | Example                              |
|------------------------|-----------------------------------------------------------------------------|--------------------------------------|
| `ACCOUNTS_ENABLED`     | Master switch — set to `true` to enable accounts                            | `true`                               |
| `ACCOUNTS_DB_PASSWORD` | Postgres password. **Set this before the first deploy** that starts Postgres (changing it later needs the volume recreated). | (a long random string)               |
| `ACCOUNTS_AUTH_SECRET` | HMAC secret for signing auth tokens — long & random; don't rotate casually  | (a long random string)               |
| `ACCOUNTS_FROM_EMAIL`  | From address — **must be on your verified Mailgun domain**                  | `no-reply@mg.wingedsheep.com`        |
| `MAIL_HOST`            | Mailgun SMTP host — **`smtp.eu.mailgun.org` for EU-region accounts**, else `smtp.mailgun.org` | `smtp.eu.mailgun.org`                |
| `MAIL_PORT`            | SMTP port (optional, defaults to 587)                                       | `587`                                |
| `MAIL_USERNAME`        | Mailgun SMTP username (the domain's SMTP login)                             | `postmaster@mg.wingedsheep.com`      |
| `MAIL_PASSWORD`        | Mailgun SMTP password                                                       | (from the Mailgun dashboard)         |

Notes:
- The magic-link URL is built from `APP_DOMAIN` (`https://<APP_DOMAIN>/login/verify?token=…`), so no extra URL secret is needed.
- With `ACCOUNTS_ENABLED` unset/`false`, none of the above are required and the server runs anonymous + in-memory as before (the idle `postgres` container still starts but is unused).

#### Setting up Mailgun (sending the sign-in emails)

The server sends sign-in links over SMTP. Mailgun's free/flex tier is enough; you need a **verified
sending domain** and its **SMTP credentials**. These steps are for the **EU region** — if your account
is US, swap `eu.mailgun.org` hosts for the non-EU ones and use `app.mailgun.com`.

1. **Create/log into Mailgun in the right region.** EU accounts live at <https://app.eu.mailgun.com>.
   The region is fixed per account and per domain — an EU domain only sends via the EU SMTP host.
   (New accounts must add a payment method before they can send to arbitrary recipients; until then
   you can only send to *authorized recipients* you add under Sending → Overview.)

2. **Add a sending domain.** Sending → Domains → **Add New Domain**. Use a **subdomain**, e.g.
   `mg.wingedsheep.com` (Mailgun recommends a subdomain so it never interferes with your normal
   mail on `wingedsheep.com`). Pick the **EU** region when prompted.

3. **Add the DNS records Mailgun shows you** at your DNS provider (wherever `wingedsheep.com` is
   hosted). Mailgun displays the exact names/values — **copy them verbatim**, they're region- and
   domain-specific. You'll get:
   - **TXT — SPF** on `mg.wingedsheep.com` (e.g. `v=spf1 include:mailgun.org ~all`).
   - **TXT — DKIM** on a `<selector>._domainkey.mg.wingedsheep.com` host (a long `k=rsa; p=…` value).
     This is the one that actually authorizes sending — it must go green.
   - **MX** records (`mxa.eu.mailgun.org`, `mxb.eu.mailgun.org`) — only needed if you also want to
     *receive* at the domain; optional for sending-only.
   - **CNAME** (`email.mg.wingedsheep.com → eu.mailgun.org`) — optional, for open/click tracking.

4. **Verify.** Back in Mailgun, click **Verify DNS Settings**. DNS can take minutes to a few hours to
   propagate; SPF + DKIM must show green before delivery is reliable.

5. **Grab the SMTP credentials.** Open the domain → **SMTP** tab. You'll see:
   - Host `smtp.eu.mailgun.org`, port `587` (STARTTLS — matches the server config).
   - Default login `postmaster@mg.wingedsheep.com` and a generated password (you can reset it here, or
     add a dedicated SMTP user).

6. **Map them to the secrets:**

   | Mailgun value                          | Secret                | Example                          |
   |----------------------------------------|-----------------------|----------------------------------|
   | SMTP host                              | `MAIL_HOST`           | `smtp.eu.mailgun.org`            |
   | SMTP port                              | `MAIL_PORT`           | `587`                            |
   | SMTP login                             | `MAIL_USERNAME`       | `postmaster@mg.wingedsheep.com`  |
   | SMTP password                          | `MAIL_PASSWORD`       | (from the SMTP tab)              |
   | any address on the verified domain     | `ACCOUNTS_FROM_EMAIL` | `no-reply@mg.wingedsheep.com`    |

   `ACCOUNTS_FROM_EMAIL` **must** be on the verified domain (`@mg.wingedsheep.com`), or Mailgun
   rejects the send with a 550.

7. **Test the credentials** before wiring them in (optional but saves a round-trip). With
   [`swaks`](https://github.com/jetmore/swaks):
   ```bash
   swaks --server smtp.eu.mailgun.org:587 --tls \
     --auth-user postmaster@mg.wingedsheep.com --auth-password '<smtp-password>' \
     --from no-reply@mg.wingedsheep.com --to you@youraddress.com \
     --header 'Subject: Mailgun test' --body 'it works'
   ```
   A `250` and the mail landing in your inbox means the secrets are good. (Don't have `swaks`? The
   server's own dev fallback also works: deploy with `ACCOUNTS_ENABLED=true` but the mail secrets
   unset, and the magic link is printed to `docker compose logs backend` so you can confirm the rest
   of the flow before DNS is fully verified.)

> Sandbox domains (`sandboxXXXX.mailgun.org`) only deliver to authorized recipients — fine for a first
> test, but use the verified custom domain for real users.

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
