---
name: ai-game-review
description: Download recent games the engine AI played against real players on the production server (magic.wingedsheep.com), re-simulate them into readable full-information logs, have fresh-context subagents read them for AI misplays (and engine bugs spotted along the way), plan the fixes in another subagent, then fix the issues one at a time — each in its own worktree, reproduced by a failing AI test first — opening a PR per fix. Use when asked to "analyse / review recent AI games from the server", "why is the AI losing to players", "download games the AI played and improve it", or "look at how the AI did against humans".
argument-hint: [N games (default 10)] [game mode, e.g. QUICK_GAME | TOURNAMENT] [days back]
---

# AI game review → fix

Same shape as `ai-playtest`, but the games are real ones the engine AI played against humans on the
server. Four phases. The main thread is an **orchestrator**: it fetches and renders, dispatches
subagents, and reads only their short summaries and the plan's checklist — never a game log, never a
diff. Everything a later phase needs is written to the **run dir**.

```
RUN=.claude/loop-runs/ai-review-<yyyymmdd-hhmm>/     (gitignored, in the main checkout)
  games/       <gameId>.replay.json + <gameId>.log per game, index.json (seats, AI names, winner)
  findings/    the analysts' findings, saved by the orchestrator
  plan.md      ordered issue checklist — the source of truth for phase 4
```

Use absolute paths for `$RUN` in every subagent prompt.

## Phase 1 — fetch and render

Needs the admin password in the environment. If `WINGEDSHEEP_ADMIN_PASSWORD` is unset, stop and ask
the user to `export WINGEDSHEEP_ADMIN_PASSWORD=...` (never write it to a file or pass it as an argument).

Render on **current `origin/main`** in a long-lived detached worktree, so build outputs survive runs:

```bash
WT=.claude/worktrees/ai-review
[ -d $WT ] || git worktree add --detach $WT origin/main
git fetch origin && git -C $WT switch --detach origin/main
$WT/scripts/fetch-ai-games --out $RUN/games --limit <N> [--mode <MODE>] [--days <D>]
(cd $WT && just ai-replay-logs $RUN/games)
```

The fetch keeps finished games with a replay where at least one seat is the AI and one is human
(`--include-ai-vs-ai` adds all-AI games). The render prints one line per game: `EXACT`, `UNVERIFIED`
or `DIVERGED k/n actions` with the recording's engine sha. A **diverged** log stops at the divergence
(current code no longer reproduces a game recorded on an older build) — it is still analysable up to
there. A log ending `RECORD ENDS … game still running` was decided off the action stream (concession,
timeout, disconnect); the header's winner is the server's result. Report the fidelity tally to the
user in one line.

Each log shows both decks, every seat's board **and hand** each turn, every action and decision with
its answer, and for the AI seat what it had on offer when it passed (`passes; had: …`) and the legal
alternatives under each action it took (`options: …`). Its header names the AI seat, the human, the
recorded winner and `engineVersion`.

## Phase 2 — analyse (fresh subagents, read-only)

One analyst reads ~20 logs comfortably (a game renders to ~200 lines); run more only for bigger
samples, **one after another**, so the machine isn't hogged. They read and grep only — **no Gradle, no
`just` builds**. Subagents can't write report files, so each **returns its findings blocks in full**
after a summary of at most ~12 lines; save the blocks to `$RUN/findings/<batch>.md` verbatim and read
only the summary. Prompt each with the log paths, the repo path `$WT` to read code from, and:

- **Focus: what the AI did wrong against a human.** Clear misplays a competent player wouldn't make —
  attacks into a losing board or not attacking for lethal; bad blocks, chump-blocking too early, not
  blocking lethal; removal on the wrong target (a dying creature, its own, a token while a bomb lives);
  wasted or stranded mana, passing with a good play on offer (`had:`), wrong land/spell sequencing;
  mulligans; combat math errors (pump/first strike/deathtouch/trample ignored); stack and response
  timing (not responding to a lethal spell or a removal on its creature, casting sorcery-speed what
  should wait). Weigh how much the misplay cost — the game result and life swing. Skip judgment calls.
- **Also note** engine flaws (a rule applied wrongly, wrong P/T or life, a trigger missing/doubled, an
  illegal option offered) and decision-prompt problems (confusing options, a forced choice asked) seen
  along the way. Check the card's real text (its `cardDef` under `mtg-sets/`) before calling a card
  wrong; verify any CR number cited (AGENTS.md rule).
- **Don't re-report known or fixed issues.** Read `ai/CHANGELOG.md` and `ai/changelog.d/`, `gh pr list --state open --search
  <keywords>`, and — since a game may be recorded on an older build — `git -C $WT log --oneline
  <engineVersion>..origin/main -- ai/ rules-engine/` (skip when the version is `unknown`/`dev`). A misplay
  a later commit already fixed is not a finding; say so in one line.
- the finding format, one block per finding:

  ```
  ### <short title>
  - category: ai | engine | ui
  - severity: crash | wrong-rules | misplay-major | misplay-minor | cosmetic
  - confidence: high | medium | low
  - cost: lost the game | big swing | minor
  - evidence: <log file>:<turn/step>, the quoted log lines
  - cards: <names>
  - suspected cause: <file:line if found, else a hypothesis>
  ```
- "Report nothing rather than guess. Low-confidence findings are fine but must say so."

## Phase 3 — plan (one fresh subagent)

One `Plan` subagent reads `$RUN/findings/*` and returns the plan, which you save as `$RUN/plan.md`.
Prompt it to:

- **Dedupe** across games; **verify** each against the code in `$WT` (read-only) — confirm the root
  cause in `ai/src/main/kotlin/…` (or the engine) or drop it. Unsubstantiated low-confidence findings go
  under "Not pursued" with a one-line reason; so do already-fixed or in-flight ones.
- **Rank** by (how many games it cost or swung × how tractable the fix is): a clear, recurring,
  locally fixable misplay first; crashes and wrong-rules engine bugs above any misplay. Split anything
  too big for one PR.
- Write each issue as a checklist item:

  ```
  - [ ] P1 <slug> — <title>
    category / severity / games affected (findings files + log turns)
    root cause: <file:line>
    fix: <approach — which skill: add-feature, add-card, or a plain fix>
    test: <the AI decision test or puzzle that fails before the fix — the position from the log>
    status: todo
  ```
- Return the checklist in full, preceded by the ordered titles.

Show the user the ordered list (one line each), then proceed — stop here only on "plan only" /
"just analyse".

## Phase 4 — fix, one issue at a time

Strictly sequential: **one implementation in flight, one Gradle build at a time.** For each `todo`
issue, one fresh `general-purpose` subagent, prompted with the issue block, `$RUN`, and:

- Create a worktree off fresh main: `git fetch origin && git worktree add
  .claude/worktrees/ai-review-<slug> -b fix/ai-review-<slug> origin/main`. Every path and command
  targets it; use `/usr/bin/git` there (the rtk rewrite trips the worktree guard).
- **Reproduce first**: rebuild the position from the log as an AI decision test under
  `ai/src/test/kotlin/com/wingedsheep/ai/engine/` (or a puzzle in `ai/.../puzzles/PuzzleCatalog.kt`)
  that fails before the fix and passes after. The AI must clearly prefer the better play — don't trade
  one judgment call for another. Engine bugs get a scenario/engine test instead.
- Follow the matching skill and AGENTS.md hard rules — notably the `ai/changelog.d/<branch>.md` entry for an AI
  behaviour change (`- #<pr> <what the AI now does differently>`; add the PR number after opening it).
- Gate with `just loop-gate`; never `just build`/`just test`.
- Commit, push, open a PR in house style. Body: the misplay, the game evidence (log lines, game id),
  the fix, the test, what was verified. Return PR number, branch, worktree path, one-line summary.

Then mark the issue `status: PR #<n>` in `plan.md` and tell the user in one line. **Default: open PRs
and stop** — merge only if the user asked; then follow `ai-playtest` phase 4b–4d (brief review, CI
watch in the background, squash-merge, remove the worktree). An issue that proves invalid mid-fix is
`status: dropped — <reason>`; close its PR and remove its worktree.

## Finish

Report: games fetched (fidelity tally), findings by category, PRs opened (or merged), dropped /
not-pursued with reasons, and the `$RUN` path. Keep `$RUN`; it is the record.

## Guardrails

- Main thread never reads logs, diffs or test output beyond a failing tail it must route.
- The admin password lives only in the environment. Logs contain player names — keep them in `$RUN`
  (gitignored); quote only what a PR needs.
- Never stash, restore or discard changes you didn't make. One Gradle build at a time; no E2E specs.
