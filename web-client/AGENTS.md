# web-client

React 19 + TS + Zustand + Vite. **Dumb terminal** — no game logic; server is the source of truth for
rules, legal actions, and state.

## Commands

```bash
npm run dev          # Dev server at localhost:5173 (proxies /game and /api to :8080)
npm run build        # Type-check + production build
npm run typecheck    # Type-check only
npm run preview
```

E2E tests live in the sibling `e2e-scenarios/` directory, not here.

## Where to look

- **Frontend architecture, slice layout, WebSocket API, decision/targeting/combat flows**:
  [`../docs/web-client-architecture.md`](../docs/web-client-architecture.md)
- **Client/server payloads**: [`../docs/data-contracts.md`](../docs/data-contracts.md)
- Components under `src/components/` — directory names are the categories (game, decisions, combat,
  ui, animations, etc.). Read directly.

## Screenshots for UI/UX PRs

When a change affects what the user sees (layout, new component, restyle, new icon/asset, copy in
the UI), **include a screenshot in the PR**. A reviewer should be able to judge the visual result
without checking out the branch. Capture the actual running app, not a mock-up; for restyles, an
after shot is enough, but a before/after pair is better when the change is subtle.

How to capture (this is the flow that works in this repo):

1. **Run the stack.** UI screens need real data: the deckbuilder hits `/api/sets`, lobby/tournament
   screens need WebSocket lobby state. Start the game server
   (`GAME_DEV_ENDPOINTS_ENABLED=true ./gradlew :game-server:bootRun --args='--spring.profiles.active=local'`,
   ~90s cold) and `npm run dev`. Note the dev server falls back to **:5174** (or higher) if 5173 is
   taken — read the actual port from its log.
2. **Drive it with Playwright via system Chrome** (the cached Playwright browser may not match the
   installed version): `chromium.launch({ channel: 'chrome' })`. Reuse the `playwright` already in
   `node_modules` (CommonJS — `import pkg from '.../playwright/index.js'; const { chromium } = pkg`).
   Navigate, click through any gates (name entry → menu → the screen), `waitForTimeout(~800ms)` so
   webfonts paint, then `page.screenshot()`. Use `deviceScaleFactor: 2` for crisp output. Delete the
   throwaway capture script before committing.
3. **Host the images off the PR diff.** Don't commit PNGs to the feature branch. Push them to a
   separate flat branch (no slashes in the name, so raw URLs resolve) built with plumbing so your
   working tree is untouched:
   ```bash
   export GIT_INDEX_FILE=/tmp/shots.index; rm -f /tmp/shots.index
   b=$(git hash-object -w /tmp/shot.png)
   git update-index --add --cacheinfo 100644,$b,shot.png
   tree=$(git write-tree); c=$(git commit-tree $tree -m "PR screenshots")
   git update-ref refs/heads/<feature>-screenshots $c; unset GIT_INDEX_FILE
   git push -f origin <feature>-screenshots
   ```
4. **Embed** with raw URLs in the PR body:
   `![alt](https://raw.githubusercontent.com/wingedsheep/argentum-engine/<feature>-screenshots/shot.png)`.
   Verify each returns `200` (raw caches ~5 min). Note in the PR that the branch is throwaway.
5. **Clean up.** Stop the dev servers when done.

## Load-bearing rules

- **Never compute legal actions in the client.** Use `legalActions` from the server's state update.
  Filtering, validation, and "is this allowed" checks are server-side.
- **Strict TS is on**, including `noUncheckedIndexedAccess` and `exactOptionalPropertyTypes`. Path
  alias `@/` → `src/`.
- **One store, five slices** combined in `src/store/gameStore.ts`. Prefer derived selectors in
  `src/store/selectors.ts` over computing in components.
