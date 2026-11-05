---
name: ai-playtest
description: Play engine-AI-vs-engine-AI games with random sets and sealed decks, have fresh-context subagents read the games for engine bugs, AI misplays and UI/UX flaws, plan the fixes in another subagent, then fix the issues one at a time — each in its own worktree, briefly reviewed, merged once CI is green — before moving to the next. Accepts a plain-words game spec ("5 multiplayer games with 4 players"); defaults to 2 two-player games and 1 four-player game. Use when asked to "playtest the engine", "run some AI games and fix what breaks", "self-play and fix", or "find bugs by watching the AI play".
argument-hint: [game spec in words, e.g. "5 multiplayer games with 4 players" | "3 two-player games, no UI"]
---

# AI playtest → fix

Four phases. The main thread is an **orchestrator**: it runs the games, dispatches subagents, and reads
only their short summaries and the plan's checklist — never a game log, never a diff. Everything heavy
happens in a subagent's own context, and everything a later phase needs is written to the **run dir**,
not carried in conversation.

```
RUN=.claude/loop-runs/playtest-<yyyymmdd-hhmm>/     (gitignored, in the main checkout)
  spec.md            parsed game spec + seeds
  games/<n>p/        game-NN.log per game (copied out of ai/build/game-logs/)
  ui/                spectator screenshots
  findings/          one file per analysed game (+ ui.md)
  plan.md            ordered issue checklist — the source of truth for phase 4
```

Use absolute paths for `$RUN` in every subagent prompt.

## Phase 0 — parse the game spec

Turn the user's words into a list of **batches** `{games, seats, attack}`:

| Words | Batch |
|---|---|
| nothing given | `2 × 2 seats` **and** `1 × 4 seats, LEFT` (the default) |
| "N games" / "N singleplayer / 1v1 / two-player games" | `N × 2 seats` |
| "N multiplayer games" | `N × 4 seats, LEFT` |
| "… with K players" | seats = K (2–8) |
| "attack anyone / all / multiple", "attack right" | attack `ALL` / `RIGHT` (default `LEFT`, the FFA lobby's rule) |
| "no UI" / "logs only" | skip the spectator pass in phase 2 |
| "just analyse" / "don't fix" / "plan only" | stop after phase 3 |

Several clauses combine ("2 singleplayer and 3 multiplayer with 6 players" → two batches). If the words
are genuinely ambiguous (not just unusual), ask once; otherwise pick and state the reading. Pick a fresh
seed per batch (e.g. `date +%Y%m%d%H%M` plus the batch index) so every run sees new sets and decks — the
seed fixes the sets, the decks and the game. Write the batches and seeds to `$RUN/spec.md`.

## Phase 1 — play the games

Play on **current `origin/main`**, not the main checkout's working tree (it holds other agents'
in-flight edits). Keep one long-lived detached worktree for this, so its build outputs survive between runs:

```bash
WT=.claude/worktrees/playtest
[ -d $WT ] || git worktree add --detach $WT origin/main
git fetch origin && git -C $WT switch --detach origin/main
```

Per batch, from `$WT` (each call wipes `ai/build/game-logs/`, so copy out before the next):

```bash
just ai-game-logs <games> <seed> live <seats> <attack>
mkdir -p $RUN/games/<seats>p && cp ai/build/game-logs/*.log $RUN/games/<seats>p/
```

Run batches **sequentially** — `just` already holds a machine-wide build semaphore; never run two at once.
Logs show each seat's set and deck, the board and hand every turn, every decision and its answer, and
`EXCEPTION …` if the game crashed. A crashed game is the top-priority finding; keep its log.

Engine AI is `live` (`AiProfile.LIVE`, what real players face). Don't switch profile unless asked.

## Phase 2 — analyse (fresh subagents, read-only)

Send all analysts **in one message** so they run concurrently. They read and grep only — **no Gradle,
no `just` builds** (parallel builds thrash the box). Each one writes its findings file and returns a
summary of at most ~10 lines; that summary is all the main thread reads.

**Game analysts** — one `general-purpose` subagent per game log (batch small logs two or three to an
agent if there are more than ~6 games). Prompt it with:

- the log path, the output path `$RUN/findings/<seats>p-game-NN.md`, and the repo path `$WT` to read code from;
- what to look for:
  - **Engine flaws** — a rule applied wrongly, a trigger that didn't fire or fired twice, wrong P/T or
    life, an illegal action offered or a legal one missing, a card doing something its Oracle text
    doesn't say, a stuck/looping game, an exception. Check the card's real text (its `cardDef` under
    `mtg-sets/`, or Scryfall) before calling a card wrong; verify any CR number cited (AGENTS.md rule).
  - **AI mistakes** — clear misplays a competent player wouldn't make: attacking into a losing board,
    not blocking lethal, wasting removal, passing with mana and a good play, bad targets (own creatures,
    a dying one), mulligan/land decisions, multiplayer threat assessment. Skip judgment calls.
  - **UX hints visible in the log** — decisions with confusing options, prompts that shouldn't be asked
    (a forced choice with one option), unreadable descriptions/humanized text.
- the finding format, one block per finding:

  ```
  ### <short title>
  - category: engine | ai | ui
  - severity: crash | wrong-rules | misplay-major | misplay-minor | cosmetic
  - confidence: high | medium | low
  - evidence: <log file>:<turn/step>, the quoted log lines
  - cards: <names>
  - suspected cause: <file:line if found, else a hypothesis>
  ```
- "Report nothing rather than guess. Low-confidence findings are fine but must say so."

**UI analyst** (skip on "no UI") — one `general-purpose` subagent that spectates a live engine-vs-engine
match in headless Chromium and judges the screen as a player would. Prompt it to:

1. In `$WT`, start `just ui-dev <free-server-port> <free-client-port>` in the background (ports nobody
   else uses — check with `lsof -i :<port>`). Local profile already means engine AI, heuristic decks.
2. `POST http://localhost:<server-port>/api/dev/ai-tournament` with
   `{"playerCount": 2, "gamesPerMatch": 1, "setCodes": [<a set from the logs>]}` — no `models`, so both
   seats are the engine AI. If the set isn't enabled locally, retry with `BLB`. The response carries
   `spectateUrl`.
3. Drive it with a throwaway script in `e2e-scenarios/.ui-check/` (gitignored; `chromium` from
   `@playwright/test`; `localStorage['argentum-player-name']='Spectator'`; open
   `http://localhost:<client-port><spectateUrl>`, click **Watch**). Screenshot every ~10 s and at every
   phase change into `$RUN/ui/`, until game over or ~10 minutes. Look at each PNG with the Read tool.
4. Judge layout, overlap/clipping, stale or wrong highlights, unreadable labels, missing feedback for
   what just happened (combat, triggers, stack), animations that hide state, and anything in the log
   it can't see on screen. Write `$RUN/findings/ui.md` in the same format, each finding naming its
   screenshot. Stop the servers when done.
5. Never write or run Playwright E2E specs (`e2e-scenarios/tests/`, `just e2e*`). The spectator path is
   1v1 only; multiplayer UI is judged from the logs, or with a dev scenario
   (`generate-scenario` skill → `POST /api/dev/scenarios`) if a multiplayer finding needs a look.
   If the app or images can't be run or viewed, say so rather than claiming a UX pass.

## Phase 3 — plan (one fresh subagent)

One `Plan` subagent reads `$RUN/findings/*` and writes `$RUN/plan.md`. Prompt it to:

- **Dedupe** findings across games; **verify** each against the code in `$WT` (read-only) — confirm the
  root cause or drop it. Drop low-confidence findings it can't substantiate; list them under
  "Not pursued" with a one-line reason.
- **Check it isn't already known or in flight**: `gh pr list --state open --search <keywords>`,
  `gh issue list --search <keywords>`, and the "Open bugs" in the memory index. Skip what's covered.
- **Order** by severity (crash > wrong-rules > misplay-major > ui > misplay-minor > cosmetic), then by
  how many games/cards it touches. Split anything too big for one PR.
- Write each issue as a checklist item:

  ```
  - [ ] P1 <slug> — <title>
    category / severity / evidence (findings file + log turn)
    root cause: <file:line>
    fix: <approach — composition first; which skill: add-feature, add-card, or a plain fix>
    test: <the failing test that proves it — scenario test, AI unit test/puzzle, or manual UI check>
    status: todo
  ```
- Return the ordered titles only.

Show the user the ordered list (one line each). Then proceed — the user asked for the full loop; stop
here only if they said "plan only". Don't ask for permission per issue.

## Phase 4 — fix, one issue at a time

Strictly sequential: **at most one implementation in flight and one Gradle build at a time.** For each
`todo` issue in `plan.md`, in order:

**4a. Implement** — one fresh `general-purpose` subagent. Prompt it with the issue block, `$RUN`, and:

- Create a worktree off fresh main: `git fetch origin && git worktree add
  .claude/worktrees/playtest-<slug> -b fix/playtest-<slug> origin/main`. Every path and command targets
  that worktree; use `/usr/bin/git` there (the rtk rewrite trips the worktree guard).
- Follow the matching skill (`add-feature` for engine/SDK/AI/server/client capability, `add-card` for a
  card's definition) and the AGENTS.md hard rules — SDK reference + `just sdk-index` on SDK changes,
  `ai/changelog.d/<branch>.md` entry for AI behaviour changes, Help wiki for user-facing changes, one test file per
  card.
- **Reproduce first**: a test that fails before the fix and passes after (a `<Card>ScenarioTest`, an
  engine/mechanic test, an AI decision test or puzzle). A UI fix gets the manual UI check from
  `LOOP_PR_PROTOCOL` in `scripts/loop-lib.sh` instead, screenshots in the PR body.
- Gate with `just loop-gate` (`--rebless` if card goldens move); never `just build`/`just test`.
- Commit, push, open the PR titled `[agent-loop: <MODEL_ID>] <house-style title>` (resolve `<MODEL_ID>`
  from this session's model id, minus any `[1m]` suffix, and bake it into the prompt). Body: the issue,
  the evidence from the game log, the fix, the test, what was verified.
- Return: PR number, branch, worktree path, one-line summary. Nothing else.

**4b. Brief review** — one fresh `general-purpose` subagent runs the `review-changes` skill on that
branch (`origin/main...HEAD`) in a **single pass** (it's one focused fix — no area fan-out), told that
`just loop-gate` already passed, so it re-runs nothing. It returns blocking/important findings only.
If there are any, hand them to a fresh fix subagent in the same worktree (fix, `just loop-gate`, push).
Then post a PR comment `loop-reviewed <head sha>`.

**4c. Wait for CI** — `gh pr checks <n> --watch --fail-fast` with `run_in_background: true`; you're
notified when it exits. Don't poll, don't start the next issue meanwhile.

- **Failed** → a fresh subagent reads only the failing tail (`gh run view <id> --log-failed | tail -200`),
  fixes it in the worktree, gates, pushes, posts a new `loop-reviewed` marker; back to 4c. A known-flaky
  test: `gh run rerun <id> --failed`. A failure unrelated to the diff (someone else broke main) → report
  it and pause, per AGENTS.md. mtgish-only failures don't block.
- **Conflicts** with main → rebase in the worktree (re-bless goldens / re-bake generated files rather
  than hand-merging), gate, push, new marker.
- **Green** → `git worktree remove .claude/worktrees/playtest-<slug>`, then
  `gh pr merge <n> --squash --delete-branch` from the main checkout.

**4d. Record** — mark the issue `status: merged #<n>` and tick its box in `plan.md`; one line to the user
(`✓ P1 <title> — #<n>`). If an issue turns out invalid or blocked mid-fix, mark it
`status: dropped — <reason>`, close the PR if one exists, remove the worktree, and continue.

Then the next issue. The playtest worktree is not re-synced mid-run; each fix branches from fresh
`origin/main`.

## Finish

Report: games played (batches + seeds), findings count by category, issues merged (PR numbers),
dropped/not-pursued with reasons, and the `$RUN` path. Don't delete `$RUN` — the logs and plan are the
record. If a finding revealed a non-obvious engine trap, save it to memory.

## Guardrails

- Main thread never reads logs, diffs or test output beyond a failing tail it must route.
- Never stash, restore or discard changes you didn't make; never touch another agent's worktree or ports.
- No E2E specs. No `just build`/`just test`. One Gradle build at a time.
- A finding without evidence isn't a fix target. Don't "fix" an AI judgment call into a different
  judgment call — an AI fix needs a position where the old choice is clearly worse.
