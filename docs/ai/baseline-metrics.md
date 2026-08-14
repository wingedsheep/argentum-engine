# Engine AI — Baseline Metrics

The numbers every later phase of [`backlog/engine-ai-improvement.md`](../../backlog/engine-ai-improvement.md)
is budgeted against. Phase 0 produces them; each later phase appends its own section rather than
overwriting this one, so regressions stay visible.

**Measured:** 2026-07-27, Phase 0.

---

## How to reproduce

```bash
just benchmark-throughput 100 BLB     # the table below
just benchmark-random 200 BLB         # the raw-engine random-action baseline
```

`SimulationThroughputBenchmark` drives **real `AIPlayer` games on both seats** — not random
actions. Branching factor and projection cost are both state-dependent, and random play reaches
states no real game reaches, so the state distribution has to be realistic.

It discards `cores × 2` warmup games before measuring. That matters: before warmup existed, the
same code reported **838 / 1,299 / 2,332** `process()`/sec at 20 / 40 / 200 games. Any number
quoted from a run without warmup is measuring the JIT, not the engine.

**Hardware:** Apple M1 Pro, 8 logical cores, 32 GB, Corretto JDK 21.0.6. Rates are **per thread
with all 8 threads busy**, which is the condition the arena will run under. A single-threaded
number would be higher and would not describe anything we actually do.

---

## Simulation throughput

100 games, BLB sealed, 16 warmup games discarded. Two independent runs, agreeing within ±3%:

| Metric | Run A | Run B | Use |
|---|---|---|---|
| `ActionProcessor.process`, **candidate mix** (casts / activations) | 2,386/s · 0.419 ms | 2,493/s · 0.401 ms | cost of scoring one candidate |
| `ActionProcessor.process`, **as-played mix** (mostly passes) | 3,367/s · 0.297 ms | 3,413/s · 0.293 ms | **the rollout mix** |
| `GameSimulator.simulate` (incl. `resolveToQuietState`) | 1,288/s · 0.776 ms | 1,365/s · 0.733 ms | what the Strategist pays per candidate today |
| `LegalActionEnumerator.enumerate` | 2,419/s · 0.413 ms | 2,550/s · 0.392 ms | paid at every priority window |
| `StateProjector.project`, **cold** | 47.1 µs | 46.3 µs | 11.2–11.5% of one candidate `process()` |

`simulate` costs ~1.8× a candidate `process()` — that ratio is the auto-pass work hidden inside
`resolveToQuietState`.

Projection is timed on `state.copy()`, whose `by lazy` is unforced. That is deliberately the
**zero-cache-hit** case, because a rollout visits each state exactly once and never benefits from
the per-instance memo.

## Branching factor

| Metric | Value |
|---|---|
| Priority windows per game | **483.9** |
| Legal actions per window, pre-filter | 6.36 |
| Candidates per window, post-filter | **0.42** |
| Windows offering **zero** candidates | **76.2%** (36,879 / 48,393) |
| Candidates per window when non-empty | **1.75** |
| Pending decisions per game | 11.6 |
| Turns per game | 10.9 |

Post-filter = the Strategist's current filter: `affordable && !isManaAbility && != PassPriority`.

## Current AI decision cost

| Metric | Value |
|---|---|
| `Strategist.chooseAction` mean | 2.6–2.7 ms |
| AI thinking per game | ~1.25 s |
| Games completed | 100 / 100, 0 engine exceptions |

---

## What these numbers change in the plan

Three of the plan's working assumptions do not survive measurement. None of them invalidate the
phase ordering, but two of them re-rank effort.

### 1. The Phase 5 throughput target is already met — perf is not a rollout blocker

Phase 5 sets a target of **1,500–2,000 `process()`/sec/thread**, derived from the stale
`~404 actions/sec/thread` figure in [`engine-performance.md`](../../backlog/engine-performance.md).

Measured today: **~3,400/sec** at the as-played mix, **~2,400/sec** at the candidate mix. Steps 1–3
of the performance plan (component keying, `getBattlefield()` memoization) landed since that
baseline and evidently did most of the work.

So **Phase 5a and 5c are no longer prerequisites for Phase 7.** They remain worthwhile as standing
engine wins — `ManaSolver.findAvailableManaSources` is still O(n²) and was 59% inclusive — but the
rollout evaluator can be built without them. Treat 5a as an independent perf task, not a gate.

### 2. The branching factor is ~4× smaller than assumed, which moves Phase 4's payoff

Phase 5's budget arithmetic assumes "**candidates after Phase 4 filter ≈ 8**". Measured, *before*
any Phase 4 work, the mean is **1.75 candidates** on the 24% of windows that offer any at all.

Two consequences:

- **`filterMeaningful` has little left to cut.** At 1.75 candidates the Strategist is already
  scoring a nearly minimal set. Do not expect the "branching factor down 30–50%" exit criterion in
  Phase 4 to be reachable — it was written against the wrong number.
- **`shouldAutoPass` is where the whole win is, and it is still real.** 76.2% of windows already
  yield zero candidates, so the Strategist already passes immediately there — but it pays
  `enumerate` (0.40 ms) first. That is `0.762 × 483.9 × 0.40 ms ≈ 148 ms per game` spent
  enumerating windows with nothing to do, and a rollout pays it on every crossing. Phase 4a should
  be scoped as "skip the enumeration", not "shrink the candidate list".

### 3. Rollouts are comfortably affordable at today's speed

At the as-played rate, a 2 s NORMAL budget buys **~6,800 `process()` calls**.

A 2-turn-horizon rollout crosses ≈ 89 priority windows (483.9 windows ÷ 10.9 turns ≈ 44 per turn),
costing ~1.0 `process()` at a pass window and ~1.8 where something resolves — call it ~110
`process()` calls per rollout. That is **~60 rollouts per decision**, or **~35 per candidate**
across the 1.75 candidates a real window offers.

Phase 7 wants R = 3–4 rollouts per (candidate, world) with K = 1–2 determinizations. The measured
budget clears that by an order of magnitude. **The constraint on Phase 7 is leaf-evaluator quality
(Phases 3, 6, 9), not simulation speed** — which is what the plan's Gate 3 already argued, now with
a number behind it.

### 4. Projection: the decision to skip a projection cache is confirmed

The May 2026 profile put `StateProjector.project` at 7.4% inclusive and Phase 5b dropped the
projection-cache idea on that basis. Measured cold, with no memo hit, it is **11.2–11.5% of one
`process()` call**. Same conclusion, independently: a perfect cross-state projection cache caps out
around a 12% win, nowhere near the 2–5× a cache of that complexity and silent-wrongness risk would
need to justify itself.

---

## Not yet refreshed

~~`just benchmark-random 200 BLB`~~ — **re-run in Phase 5a**, see
[below](#phase-5a--the-on-battlefield-scans). The finding is that it is not comparable to the
`~404 actions/sec/thread` figure it was supposed to be compared against, for reasons that have
nothing to do with engine speed.

## Not measured here

Deliberately out of scope for Phase 0, each owned by a later phase:

- **Puzzle pass rate per category** — Phase 2. The "non-creature valuation" category is expected to
  baseline near 0%.
- ~~**Arena win rate vs `LEGACY_V0`**~~ — Phase 1, below.
- **p50/p95 decision latency per budget tier** — Phase 4b, once `DecisionBudget` exists. The 2.6 ms
  mean above is the whole distribution's mean with no tiering.

---

# Phase 1 — Arena baselines

**Measured:** 2026-07-27. Same hardware. How to run and read these:
[`measurement.md`](measurement.md).

All runs: BLB sealed, seed 20260727, mirror decklists, `skipMulligans`, `maxTurns = 50`.

## The scoreboard's own calibration

| Run | Result | What it proves |
|---|---|---|
| `just arena v0 v0 300` | **50.0%**, CI **[50.0%, 50.0%]**, 300/300 completed | No seat or seed leak. A mirror is exact, not merely "within the interval" |
| `just arena v0 v0-blind 200` | **100.0%**, 200-0 | The harness **discriminates**. Without this, a 50% reading is indistinguishable from a broken harness |

`v0-blind` is `LEGACY_V0` with every evaluation weight zeroed, so its Strategist can never prefer
an action to passing. Losing 200-0 to a greedy 1-ply agent is the expected floor.

## Reference-opponent baselines

| Agent A | Agent B | Games | Pair win % | 95% CI | Verdict |
|---|---|---|---|---|---|
| `v0` | `v0` | 300 | 50.0% | [50.0%, 50.0%] | mirror (harness check) |
| `v0` | `blb-advisors` | 1,000 | **50.0%** | **[49.3%, 50.8%]** | **not distinguishable** |
| `v0` | `v0-blind` | 200 | 100.0% | [100.0%, 100.0%] | v0 wins |

## Throughput

**~5 games/sec on 8 threads** (~1.6 s of wall clock per game, ~11 turns, ~497 actions). A
1,000-game merge gate is **3.5 minutes**, not the ~30–60 minutes the plan budgeted.

The plan's 111-CPU-hour estimate assumed a 2 s `DecisionBudget` per decision. That budget does not
exist yet — combat has a 1 s cap and nothing else is bounded at all. **So the plan's "run the arena
at a reduced ~150 ms budget" mitigation is not needed in Phase 1, and Phase 4b must re-measure this
table before it ships a budget**: it is the budget, not the game count, that decides whether a
1,000-game gate is affordable.

---

## What Phase 1 found

### 1. The BLB card advisors are not an improvement over generic `v0`

1,000 paired games: **50.0%, CI [49.3%, 50.8%]**, 500W-500L-0D. Only ~3% of pairs came out
differently at all, and those split evenly. The advisors change behaviour — the CI is non-degenerate,
unlike the `v0` mirror's `[0.000, 0.000]` — they just do not change results.

`AdvisorBenchmark`, run at the same 1,000 games for comparison, reports **46.1%** for the advised
side. The two do not contradict each other so much as bracket the same conclusion: *advisors are
not helping*. The difference is explained by `AdvisorBenchmark` being **unseeded** — it sets no
`GameConfig.seed`, so the two games of its "pair" are different shuffles, and its estimator is
therefore unpaired. On the same 1,000 games the arena's paired interval is **±0.8 pp** against the
unpaired **±3.1 pp** — a **4× variance reduction**, well beyond the 15–30% the plan predicted,
because mirror decklists plus an identical game seed make the pairing unusually tight.

This is worth stating plainly because Phase 6 plans to retire advisors that `CardIntent` reproduces:
**the retirement bar for the 42 BLB/ONS advisor entries is lower than it looked.** Two of them had
already been silently overwritten before Phase 0 fixed the registry collision, and the module as a
whole is measurably neutral.

### 2. The AI proposes illegal actions roughly once per game

The arena is a free bug finder at scale, and it found one immediately. Across the 1,000-game run:

| Count | Rejection |
|---|---|
| 889 | `CastSpell: No valid targets available` |
| 33 | `CastSpell: Not enough mana to cast this spell` |
| 23 | `ActivateAbility: Must choose 1 card(s) to discard` |

Every one of these is a defect — either the enumerator offered an action it should not have, or the
Strategist mangled it on the way out. The runner recovers with a safe fallback and continues, so
none of them break a game, but ~0.9 wasted decisions per game is real.

The rate is **advisor-dependent**: 0.34/game in the `v0` mirror versus 0.95/game with
`blb-advisors` on one side. That points at a `CardAdvisor` recommending a cast whose target
selection then fails, and it is a concrete lead rather than a vague one. **Not fixed in Phase 1** —
it is an AI/enumerator bug, not scoreboard work — but it is now measured, and any fix has a
number to move.

### 3. Seat 0 is not worth what you would guess

Seat 0 (on the play) wins **46–51%** depending on the run — at 300 games it was 46.0%, at 1,000 it
was 51.0%. In BLB sealed against this AI, being on the play is close to neutral and may be slightly
negative. It does not bias any result here (both agents sit in both seats), but it is a reminder
that "on the play wins more" is an assumption about human play, not a property of the engine.

---

# Phase 2 — Puzzle baselines

**Measured:** 2026-07-27, `just arena-puzzles` (the whole suite, ~15 s including Gradle startup).
How to read one: [`measurement.md`](measurement.md#the-puzzle-suite).

48 hand-authored positions, 8 categories × 6. Each asks the AI for exactly one move and asserts a
*predicate* over it — "removal targets the 3/3 it can kill, not the 6/4 it bounces off" — never an
exact `GameAction`.

## Per-category baseline

| Category | `v0` | `production` | `v0-blind` | What it catches |
|---|---|---|---|---|
| lethal | 6/6 | 6/6 | 6/6 | Missing an alpha strike / burn-to-face kill |
| blocking | 6/6 | 6/6 | 6/6 | Chump vs trade vs no-block; deathtouch / first strike |
| removal | 6/6 | 6/6 | 0/6 | Shooting the 1/1 instead of the bomb |
| instants | 3/6 | 3/6 | 2/6 | Casting a combat trick in your own main phase |
| sequencing | 5/6 | 5/6 | 0/6 | Land before spell; the land that unlocks the spell |
| wipe | 6/6 | 6/6 | 3/6 | Wrathing while ahead |
| race | 5/6 | 5/6 | 5/6 | Attack-vs-hold when both players are on a clock |
| **noncreature** | **2/6** | **2/6** | 0/6 | Ignoring an opposing O-Ring / mana rock / anthem |
| **total** | **39/48 (81%)** | **39/48 (81%)** | 22/48 (46%) | |

`v0-blind` (every evaluation weight zeroed) is the discrimination control, the same one the arena
uses. 22/48 versus 39/48 is the suite proving it measures something; the always-on
`PuzzleSuiteTest` asserts that gap rather than leaving it to a manual run.

## What Phase 2 found

### 1. Non-creature blindness is real, and it is a *casting* failure before it is a targeting one

The plan predicted ~0% here and named `heuristicTargetRank`'s `else -> 0.0` as the cause. The
measurement is more specific: on all four failures the AI does not mis-target the Disenchant, **it
never casts it at all**. Destroying an artifact moves `BoardPresence` by `permanentValue`'s flat
`0.5` (weight 1.5, so +0.75) and costs a card (`CardAdvantage`, weight 1.0, −1.5 at a typical hand
size). Passing scores higher, so the removal is held forever.

That reframes Phase 6's exit criterion slightly: `staticPriorValue` has to be large enough to clear
the *card-advantage* cost of casting, not merely to outrank a sibling target.

The two that pass are the two whose effect shows up in **creature** stats, which the evaluator can
already see: `noncreature-05` (an anthem pumping three creatures) and `noncreature-06` (Disenchant
on a Pacifism that is holding down a 6/4). So the deficit is precisely "permanents whose value is
not visible in someone's power and toughness."

### 2. The last card in hand never gets played

`sequencing-02` and `sequencing-04` are the same decision — play a land — with one card of
difference in hand. 04 passes, 02 fails. The cause is `CardAdvantage.cardValue(0) = -3.0` against
`cardValue(1) = 1.0`: emptying your hand reads as a 4-point disaster, which swamps the land drop's
tempo and board gain, so the AI would rather hold its last land indefinitely. Land drops are free;
this is a hand-drawn constant that Phase 9's logistic fit should remove.

### 3. A one-ply evaluator cannot see a prevention effect

`instants-05` — Fog at 2 life facing 9 power of attackers — is passed up. After the simulation
resolves Fog, the state has the same life totals as passing did: the prevention only shows up when
combat damage would be dealt, which is past the one-ply horizon. Fog therefore evaluates as "−1
card" in every position, which is also why `instants-04` (hold Fog in your own main) *passes* —
right answer, wrong reason. This is a Phase 7 rollout puzzle, not a weight-tuning one.

### 4. Combat is carried by `CombatAdvisor`'s heuristics, not by the evaluator

`v0-blind` scores 6/6 on lethal and 6/6 on blocking despite scoring every board identically. The
combat categories are measuring `CombatAdvisor`'s seed heuristics, which are evaluator-independent.
That is worth knowing before reading a future improvement: a change to `BoardFeatures.kt` will not
move those two categories, and a change to `CombatAdvisor` will.

The one combat position the evaluator does own is `race-03` (send the flier, keep the ground
blocker home), and it fails for both — there is no model of holding a creature back at all.

### 5. The card advisors are neutral here too

`v0` and `production` score identically, 39/48, category for category. That is the same conclusion
Phase 1's arena reached at 1,000 paired games (50.0%, CI [49.3%, 50.8%]), from a completely
independent measurement. Two signals agreeing lowers the retirement bar for the 42 advisor entries
further.

---

# Phase 3 — Multiplayer baselines

**Measured:** 2026-07-28, `just arena-pod <table> <a> <b> <games>`, BLB sealed, seed 20260727, on
the same 8-core M1 Pro. How to read one:
[`measurement.md`](measurement.md#the-pod-arena).

Phase 3 made the evaluator see a whole table instead of one neighbour. Before it, every feature in
`BoardFeatures.kt` opened with `soleOpponent(playerId)`; the pod arena (`just arena-pod`) is the
scoreboard that can tell whether that mattered.

## The pod scoreboard's own calibration

One agent in a field of the other, rotated through every team position. **The null is 1/teams, not
50%.**

| Table | Seats | Null | `v0` vs a field of `v0-blind` | Games |
|---|---|---|---|---|
| `ffa3` | 3 | 33.3% | **100.0%** CI [100%, 100%] | 150 |
| `ffa4` | 4 | 25.0% | **100.0%** CI [100%, 100%] | 120 |
| `2hg` | 4 (2 teams) | 50.0% | **100.0%** CI [100%, 100%] | 120 |

Every game, at every table. `v0-blind` zeroes the evaluation weights, so this is the pod arena's
discrimination control, the same one Phases 1 and 2 use — and a cleaner sweep than the head-to-head
arena's 200-0, because a blind agent in a pod is being hunted by two or three opponents rather than
one.

## Health at a pod table

Multiplayer is the least-exercised engine path in the repo, so these matter as much as the win
share:

| Table | Completion | Mean rounds | Mean actions | Mean game | Rejected AI actions / game |
|---|---|---|---|---|---|
| `ffa3` (`v0` vs blind) | 150/150 | 7.7 | 615 | 1.3 s | 0.18 |
| `ffa4` (`v0` vs blind) | 120/120 | 7.9 | 1,133 | 2.3 s | 0.53 |
| `2hg` (`v0` vs blind) | 120/120 | 8.3 | 601 | 1.0 s | 0.08 |
| `ffa3` (`production` vs `v0`) | 286/300 | 13.2 | 1,244 | 14.3 s | 1.26 |

Zero engine exceptions across all 690 games. Note "rounds", not turns —
`GameState.turnNumber` counts rounds, so 13.2 at a three-seat table is ~40 player turns.

**A pod game is 5-10× the wall clock of a duel.** 14.3 s per game in the `production` vs `v0` run
against ~1.6 s for a head-to-head game: three agents deciding instead of two, over boards that keep
growing because nobody is closing the game out. A 1,000-game pod merge gate is hours, not the
head-to-head arena's 3.5 minutes. Size pod runs accordingly — 300 games is already a 10-minute run.

## Reference-opponent baselines

| Matchup | Table | Win share | CI | Null | Games |
|---|---|---|---|---|---|
| `production` vs a field of `v0` | `ffa3` | 31.7% | [29.3%, 33.7%] | 33.3% | 300 |

**The BLB + ONS card advisors are neutral in a pod too.** That is now three independent
measurements agreeing — Phase 1's 1,000 paired duels (50.0%, CI [49.3%, 50.8%]), Phase 2's puzzle
suite (39/48 for both `v0` and `production`, category for category), and this one. It lowers the
retirement bar for the 42 advisor entries in Phase 6 again.

## What Phase 3 found

### 1. The plan's diagnosis was wrong in a way that made the bug sound smaller

The plan says the five features' `state.soleOpponent(playerId) ?: return 0.0` meant "in any
multiplayer game the evaluator returns exactly 0.0 for every candidate". It does not.
`soleOpponent` was `getOpponents(playerId).firstOrNull()`, and in a pod `getOpponents` returns two
or three players — so the helper returned the **first opponent in turn order** and the evaluator
scored the position as a two-player game against one arbitrary neighbour.

That is not "no evaluation". It is worse in one respect and better in another:

- **Better:** the AI was not choosing at random. It was playing a real, if one-eyed, game.
- **Worse:** a one-eyed evaluation is *confidently* wrong. The runaway leader across the table is
  invisible; a removal spell aimed at them scores exactly 0.0 while the same spell aimed at the
  first opponent scores normally, so the AI systematically attacks the wrong player.

And in Two-Headed Giant the same expression produced three separate failures at once: the teammate's
board did not count, the teammate's cards in hand did not count, and — the sharpest one —
`LifeDifferential` read `getEntity(playerId).get<LifeTotalComponent>()` directly. A 2HG team's life
lives on the team's canonical owner (`GameState.teamLifeOwnerOf`); the *other* member's component is
never written again after setup. So for half the table the life differential was **frozen at the
starting 30 for the whole game**.

`MultiplayerEvaluationTest` asserts each of these as a positive claim rather than describing them.

### 2. `GameState.turnNumber` stopped advancing after the first elimination — fixed since

As measured, `TurnManager.startTurn` incremented `turnNumber` only when
`playerId == state.turnOrder.first()`, and `turnOrder` keeps eliminated players. So in a pod, the
moment seat 0 was knocked out, **`turnNumber` never changed again** — the game played on for another
twenty turns at "turn 16". The arena's wedge detector and length cap both keyed on it and declared
every healthy three-way endgame stuck.

This was never an AI bug. `turnNumber` now counts **player turns**, so it advances on every turn at
any table size and the harness reads it directly again. See `backlog/multiplayer.md` for the engine
side — the same freeze reached delayed triggers and everything else that read `turnNumber + 1` as
"next turn".

One thing that survives the fix and is still worth knowing before writing a pod harness: a pod turn
costs more actions than a duel turn, because the Strategist simulates every candidate against three
or four growing boards. An action budget tuned on duels is too tight regardless of the clock.

### 3. `ThreatAssessment` has a ~130-point cliff at "opponent has no creatures"

`turnsUntilDead` falls back to a sentinel `99.0` when a side has no attack power, and the score is
`(99 − turnsUntilWeKill) × 1.5`. So removing an opponent's **last** creature is worth about 130
points while removing one of their four is worth about 2. The cliff was always there; a pod makes it
visible, because it now competes across opponents and can outweigh any amount of progress against
the actual leader. One more hand-drawn constant for Phase 9's fit.

### 4. `heuristicTargetRank` can never rank an opponent *player* as a target

Found while auditing the same neighbourhood, **not fixed** — it is a 1v1 bug, it would move the
frozen baseline, and `Strategist.heuristicTargetRank` is Phase 6's consumer (b) anyway.

`heuristicTargetRank` computes `isOpponent` from `projected.getController(entityId)`. `ProjectedState`
is built from `state.getBattlefield()` only, so `getController` on a *player* entity returns null —
and the player branch therefore always takes the `else` arm and returns **−5.0**. Every burn spell
ranks its own controller and its opponent identically badly. Phase 6 should fix this when it
rewrites the function's `else -> 0.0`.

---

# Phase 4 — Branching factor + budget

**Measured:** 2026-07-28, Phase 4. Same hardware as above.

```bash
just arena v0 v0-meaningful 1000     # the filter alone
just arena v0 v0-phase4 1000         # filter + tiered budget — what the phase proposes to ship
just arena-budget-scaling 300        # the monotonicity ladder
just arena-pod ffa3 v0-phase4 v0 150 # pod crash check
```

## Agent baselines

| Matchup | Win share for A | CI | Games | Verdict |
|---|---|---|---|---|
| `v0-meaningful` vs `v0` | 51.3% | [49.8%, 52.7%] | 1,000 | neutral — spans parity |
| `v0-phase4` vs `v0` | 50.8% | [49.4%, 52.2%] | 1,000 | neutral — spans parity |
| `v0-phase4` vs a field of `v0` (`ffa3`) | 30.0% | [25.3%, 34.7%] | 150 | neutral — spans the 33.3% null |

**Phase 4 is enabling infrastructure and it measures like it.** Neither agent is a demonstrated
improvement, and neither is a regression — which is the result the phase was designed to produce.
The exit criterion was `just arena v0 v0-meaningful 1000` **≥50%**, phrased that way precisely
because a filtered agent that *loses* is discarding a real option. It does not lose.

## Budget scaling — the safety net, and it passes

`just arena-budget-scaling 300`. The same agent, differing in nothing but the size of its
`DecisionBudget`, played against itself. **Strength is monotone in the budget, with every rung's
lower CI bound above parity:**

| Rung | Win share for the bigger budget | CI |
|---|---|---|
| 1000 ms vs 100 ms | 55.7% | [52.7%, 58.7%] |
| 3000 ms vs 1000 ms | 54.0% | [51.0%, 57.0%] |
| 3000 ms vs 100 ms (end to end) | 55.3% | [52.0%, 59.0%] |

This is the single most important number in the phase. It says the search the AI already has
converts *more thinking* into *more winning*, so when Phase 7 stacks rollouts on top of it there is
a calibrated instrument that will notice if that stops being true. Note the end-to-end rung is not
larger than the first: returns diminish above ~1 s, and the three runs share decks and seeds, so
they are correlated — read the ladder as "monotone", not as "linear".

**Allowances are counted, not timed.** `SearchAllowances` converts a budget into a number of
simulations once, and the wall clock is only a hard safety stop. A stopwatch-driven search would
have made the arena non-reproducible and `ArenaHarnessTest`'s "identical at 8 threads and at 1"
assertion flaky, which would have cost more than it bought.

## Enumeration skipping — what 4a actually saves

Phase 0 re-scoped 4a from "shrink the candidate list" (there are only 1.75 candidates to shrink) to
"skip the enumeration". `MeaningfulActionFilter.canAutoPassWithoutEnumerating` decides a whole
priority window from the state alone, without calling `LegalActionEnumerator.enumerate` at all.

Measured over 884 real priority windows from two full AI-vs-AI games (`AutoPassParityTest`, which
prints the figure): **40% of windows are decided without enumerating.** That is below Phase 0's
76.2% "windows offering zero candidates" because the fast path deliberately declines every window
whose verdict depends on what the player is holding — both main phases, both combat declarations,
first-strike damage, end of combat, and the opponent's end step. At ~0.40 ms per `enumerate` and
~380 windows per game, it is ~60 ms per game today, and a Phase 7 rollout would have paid it again
on every window it crossed.

## What Phase 4 found

### 1. The Phase 1 illegal-action finding was a *targeting* bug, not a filtering one

Phase 1 measured the AI proposing ~0.9 illegal actions per game, **889 of 945 being exactly
`CastSpell: No valid targets available`**, and left it open. The meaningful-action filter was the
obvious suspect and it turned out not to be the cause at all.

The actual mechanism: `Strategist.resolveTargetsForSimulation` and `chooseCommittedTargets` both
opened with `if (targetInfos.any { it.validTargets.isEmpty() }) return action.action` — abandoning
target selection for the **whole spell** the moment *any* requirement had no legal target, and then
submitting an untargeted cast that the engine rejects. Almost every instance is an **optional**
trailing slot. Conduct Electricity is "destroy target creature" *and* "up to one target creature
token"; with no token on the board the AI declines to target the mandatory creature either, and
throws the card away on a rejection.

`Strategist.fillableRequirements` now fills the slots it can. Targets are submitted as one flat list
that `TargetValidator` slices back by max counts, so an unfilled slot can only ever be a trailing
one — the function returns null (old behaviour) when a mandatory slot is empty, or when a *later*
slot has targets that a skipped one would displace.

Measured effect, mirror matches over 200 games: **`No valid targets available` 36 → 0.** Only 10
`Not enough mana` rejections remain, a separate bug.

It is behind `AiProfile.useMeaningfulFilter` rather than applied unconditionally. Not because the
old behaviour is defensible, but because `LEGACY_V0` is the permanent reference opponent and quietly
strengthening it would silently rebase every number ever published against it. `FrozenBaselineTest`
would not have caught this: its frozen game is all-vanilla Portal, which has no multi-requirement
spell.

### 2. `validTargets` cannot see a multi-requirement spell's second slot

The same neighbourhood, a different consumer. `LegalAction.validTargets` and
`LegalActionInfo.validTargets` only ever mirror the *first* target requirement, so the obvious
"targeted spell with no legal target" test passes a two-requirement spell whose second mandatory
slot is empty — an action the engine will reject.

`PriorityAction.hasUnfillableTargetRequirement` asks the real question (any **mandatory**
requirement with no legal target), and both the AI's candidate filter and the client's auto-pass now
use it. The client half is a UX fix in its own right: it was stopping the player on spells they
could not cast.

### 3. The "next stop point" button used a weaker notion of "meaningful" than the stop itself

`GameSession` computed `hasMeaningfulActions` inline as "not PassPriority, and not a plain mana
ability" — which counts unaffordable spells and zero-target spells that the actual stop decision
discards. So the Pass button could promise a stop that never arrived. It now calls
`AutoPassManager.getMeaningfulActions`, the same code path the stop uses.

### 4. Threading a budget through `DecisionResponder` would have changed no number

The plan lists `DecisionResponder` in the budget's threading chain. Every scan in it is already
bounded by construction — a yes/no is 2 simulations, a colour is 5, a number is sampled to 11,
targets are pre-ranked and truncated to 8 — which is at or below what even the ROUTINE tier allows.
The budget is wired into the one place it can bind (the target pre-rank cut) and deliberately not
into the other twenty responders.

### 5. Two tiers from the plan's table are not implemented, on purpose

`BudgetPolicy`'s CRITICAL tier fires on combat declaration and on either side being within one
swing of lethal. The plan also lists "sweeper castable or on the stack" and "a real counterspell
window". Both need to know what a card *does* — Phase 6's `CardIntent` — and guessing them from a
mana cost would put the most expensive tier on the wrong windows, which is worse than leaving those
windows at NORMAL.

---

# Phase 5a — the O(n²) battlefield scans

**Measured:** 2026-07-28. Same hardware. Ships
[`engine-performance.md`](../../backlog/engine-performance.md) Step 4; the code is
`mechanics/mana/ManaStaticsIndex.kt` and `event/BattlefieldStaticsIndex.kt`.

## Three points, not two

`just benchmark-random 200 BLB`, 8 threads, all three runs the same afternoon on the same box.
The middle row is the first cut of this change, which had a bug the profiler found (see finding 1):

| | Before 5a | 5a, eager index | **5a, threaded** |
|---|---|---|---|
| Engine CPU, enumerate | 764 s | 894 s | **688 s** |
| Engine CPU, process | 287 s | 266 s | **144 s** |
| **Engine CPU, total** | **1,051 s** | 1,159 s (+10%) | **832 s (−21%)** |
| Wall clock, 200 games | 133 s | 154 s | **108 s** |
| Completed / crashed | 200/200 · 0 | 200/200 · 0 | 200/200 · 0 |

`process` nearly **halves** — that is the trigger-detection half of the change, which runs inside
action processing. `enumerate` comes down 10%, which is the mana half.

Take the sizes, not the digits: per-game CPU on this box spans 1 s to 25 s depending on what else is
running, so ±5% between two runs means nothing. A 21% drop reproduced alongside a profile that shows
the targeted leaves at zero is a different matter — and the middle column is the useful evidence
that the benchmark *can* detect a regression of this size, because it detected one.

**The load-independent measurement is still the async-profiler share table below**, and that is
where the mechanism is demonstrated. Reproduce it with the commands in `engine-performance.md`'s
"Methodology" (60 games, itimer, 2 ms) — note the agent path must not contain a space, or
`JAVA_TOOL_OPTIONS` silently fails to start the JVM.

### The targeted leaves, before and after

"Before" is the May 2026 profile in `engine-performance.md` — pre-Steps-1–3, so its absolute shares
are of a bigger total and are indicative, not exact. "After" is 57,370 samples, 60 BLB games.

| Method | Before (inclusive) | After (inclusive) | Self, after |
|---|---|---|---|
| `ManaSolver.findAvailableManaSources` | **~59%** | **3.1%** | 0.10% |
| `ManaSolver.getStaticGrantedManaAbilities` | 3.5% *self* | **0.00%** | 0.00% |
| `TriggerAbilityResolver.getWardTriggeredAbilities` | **13.7%** | **0.19%** | 0.05% |
| `TriggerAbilityResolver.isWardSuppressed` | (inside the above) | **0.00%** | 0.00% |
| `GameState.getBattlefield()` | 19% | **0.28%** | 0.01% |
| `TriggerDetector.detectTriggers` | 16.3% | **7.1%** | 0.03% |
| `StateProjector.project` | 7.4% | **6.8%** | 0.57% |

The two new index types cost **0.58%** (`ManaStaticsIndex`) and **0.13%**
(`BattlefieldStaticsIndex`) inclusive. `StateProjector.project` is unmoved, which is the control:
nothing in this change touches it, and it reads the same as it did in May.

## Three findings

### 1. An eagerly-built index is a hotspot of its own, and the first cut had one

The first version gave `getTriggeredAbilities` a **default argument** that built a
`BattlefieldStaticsIndex`. Kotlin evaluates a default per call, and there are ~19 call sites, many
inside per-entity loops — so `BattlefieldStaticsIndex.build` came back at **5.2% inclusive / 2.2%
self**, roughly the size of the hotspot the hoist had just removed. It is threaded on `TriggerIndex`
now, which `detectTriggers` already builds once per pass: **5.2% → 0.13%**, and
`detectTriggers` **20.7% → 7.1%**. In the benchmark that is the whole difference between the middle
column above (10% *worse* than before the change) and the right one (21% better).

`ManaSolver` had the same trap in miniature and is fixed the same way, with a local
`lazy(LazyThreadSafetyMode.NONE)`: `findAvailableManaSources` runs on every affordability check, and
a tapped-out player has no candidate source, so eager building charged a battlefield walk to exactly
the calls that used to do no scanning at all.

**The general lesson for the rest of this plan:** hoisting work out of an inner loop is only half
the fix. The other half is making sure the hoisted work is paid *once per pass*, not once per call
that might have needed it — and a Kotlin default argument is the easy way to get that wrong.

### 2. `PredicateEvaluator.matchesCardPredicate` is now the engine's top hotspot

**20.4% self**, more than 3× the next entry (`HashMap.getNode`, 5.9%). Nothing else is close, and it
is reached from both the enumerator and the filter matching these indexes still do. That is the
next perf item, and it is a different shape of problem from Step 4 — not a redundant scan but the
per-call cost of the predicate language itself.

### 3. Phase 5c stays dropped, now with a fresh number

The allocation cluster the persistent-collections idea targets is **`Arena::grow` 1.37% +
`posix_madvise` ~0.7%** — about 2% of the profile, against the 4–6 days and the serialization work
`engine-ai-improvement.md` scopes for it. The plan says 5c is profile-gated; the profile does not
justify it. Leave it dropped until something else changes.

## Why the `~404 actions/sec` comparison is void

The benchmark was re-run at last (Phase 0 left it un-run and said so), which also settles what it
can be compared against — and the answer is: not its own recorded baseline.

| | May 2026 (pre-Steps-1–3) | 2026-07-28, before 5a |
|---|---|---|
| Completed / crashed | 200/200 · 0 | 200/200 · 0 |
| Turns per game | 26.5 | **54.6** |
| Actions per game | 1,569 | 1,526 |
| Throughput per thread | ~404 actions/sec | ~290 actions/sec |
| Enumerate / Process | 57% / 43% | **73% / 27%** |

**Do not read the throughput row as a regression.** Two things changed underneath it that have
nothing to do with engine speed:

- **`GameState.turnNumber` counts player turns now, not rounds** (the Phase 3 fix, see
  `backlog/multiplayer.md`), so the same game reports about twice the turns. The near-identical
  actions-per-game figure is the giveaway that the games themselves are the same length.
- **The BLB card pool has roughly doubled** since May, so sealed decks are richer and each priority
  window enumerates more.

What the row *does* say is that enumeration has grown from 57% to 73% of the workload, which is
consistent with the second point and is why `PredicateEvaluator` — an enumeration cost — is now the
top leaf. (After 5a it is 83%, because the change took more out of `process` than out of
`enumerate`.)

**The practical rule for the next perf step:** compare a run against another run from the same
afternoon on the same machine, and use the profile to say *why*. The recorded absolute figure from
three months ago is a record of a different workload, not a target.

---

# Phase 6 — `CardIntent`

**Measured:** 2026-07-28, 8-core M1 Pro, BLB sealed, seed 20260727.

```bash
just arena-puzzles                # the tactical signal — the phase's exit criterion
just arena v0 v0-intent 1000      # the merge gate
just arena-pod ffa3 v0-intent v0 300
```

Phase 6 replaced the AI's card knowledge — 19 hand-written `CardAdvisor`s covering 42 card names
across 2 sets — with a structural analyzer over every card the engine can load
(`ai/.../engine/knowledge/`). Three consumers read it: the flat `0.5` every non-creature permanent
was worth in `BoardPresence`, the flat `0.0` every non-creature permanent ranked at in
`Strategist.heuristicTargetRank`, and a new intent-driven `HoldPolicy` in place of one hardcoded
end-step discount.

## Puzzles — 39/48 → 44/48

`AiProfile.PRODUCTION`, the same profile Phase 2 baselined.

| Category | Phase 2 | **Phase 6** | Change |
|---|---|---|---|
| lethal | 6/6 | 6/6 | — |
| blocking | 6/6 | 6/6 | — |
| removal | 6/6 | 6/6 | — |
| instants | 3/6 | **5/6** | +2 (`instants-01`, `instants-06`) |
| sequencing | 5/6 | 5/6 | — |
| wipe | 6/6 | 6/6 | — |
| race | 5/6 | 5/6 | — |
| **noncreature** | **2/6** | **5/6** | **+3** (`noncreature-01`, `-03`, `-04`) |
| **total** | **39/48 (81%)** | **44/48 (92%)** | **+5** |

**The exit criterion was noncreature 2/6 → ≥5/6, and "holding instants up".** Both are met. The
discrimination control still holds: `v0-blind` scores 22/48 against 44/48.

Four remain, and none is a Phase 6 problem: `instants-05` (Fog — needs Phase 7's rollout to see a
prevention effect), `sequencing-02` and `noncreature-02` (both the `cardValue(0)` cliff, below),
`race-03` (no model of holding a blocker back).

## Arena — neutral, and the exit criterion is *not* met

| Matchup | Win share for A | CI | Null | Games | Verdict |
|---|---|---|---|---|---|
| `v0-intent` vs `v0` | 50.9% | [49.1%, 52.8%] | 50.0% | 1,000 | neutral — spans parity |
| `v0-intent` vs a field of `v0` (`ffa3`) | 35.7% | [32.3%, 39.0%] | 33.3% | 300 | neutral — spans the null |

The pod run is the one that exercises more than one opponent, and the plan asks for it on any change
to evaluation. It lands the same way: point estimate a little above the null, interval straddling it.

The plan's third exit criterion was "arena lower CI bound above 50%". **It is not met**, and the
puzzle result above is what makes that interesting rather than merely disappointing: the AI is
measurably better at the tactical decisions the phase targeted, and no better at winning BLB sealed
games. Two readings, both probably true in part:

- **BLB sealed is the wrong environment to detect this.** The blindness Phase 6 removes is about
  artifacts, enchantments and planeswalkers, and a sealed BLB deck is mostly creatures and one or
  two removal spells. Phase 1 reached the same shape of conclusion about the 42 card advisors
  (50.0%, CI [49.3%, 50.8%]) and Phase 2 confirmed it independently — this arena is not sensitive to
  card knowledge. A format with more permanent-based interaction would be a better test, and running
  one is cheap.
- **A better leaf evaluator is worth more per rollout than it is per greedy decision.** The plan
  sequences 6 before 7 precisely because averaging playouts of a blind evaluator produces confident
  wrong answers; the corollary is that fixing the evaluator pays off *when the rollouts arrive*.

What the number does establish is the thing a merge gate is for: **this is not a regression.**

## What Phase 6 found

### 1. A timing penalty large enough to work is large enough to break the phase

The plan specified a `HoldPolicy` that would "penalize casting an INSTANT COMBAT_TRICK / REMOVAL /
COUNTERSPELL in our own main phase with no forcing reason". Built as specified, with a −2.0 penalty,
it **cancelled the phase's own headline fix**: `noncreature-01` is an instant-speed Disenchant cast
in our own main phase, so the removal branch fired and vetoed exactly the cast that Phase 6 exists
to enable. The measured margin was +0.35 board points for casting, against a −2.0 penalty.

Lowering the constant does not resolve it, because the two cases are not symmetric:

- **A combat trick outside combat does nothing** — it wears off at cleanup. That is structurally
  certain, and it can be asserted.
- **Holding removal is a preference**, not a provable gain: it buys the option of a better target
  later and costs certainty now. Nothing in the state tells you which is worth more.

So the shipped policy asserts the first and says nothing about the second. Pricing "what if a better
target shows up" is a rollout question (Phase 7), not a constant.

### 2. A penalty cannot beat a mis-measurement — a *floor* can

`instants-01` (hold Giant Growth in your own main phase) is not close: `ThreatAssessment` reads the
+3/+3 as a permanently faster clock — 2 power to 5 power takes "turns until we kill them" from 10 to
4 — and pays **+10.8** for it. No defensible penalty constant outvotes that, and the first
implementation at −3.0 did not.

The fix is not a bigger number but a different *kind* of statement. `TimingVerdict.NoWindow` says
"whatever the simulation reports, this is not better than passing", and the Strategist scores the
candidate just below the pass score. That closed `instants-01` and `instants-06` at once. It is
reserved for cases where "does nothing" is structurally certain, never for a preference — which is
the same distinction as finding 1, arrived at from the other side.

(The underlying flaw is real and still open: `ThreatAssessment.attackPotential` counts P/T that
expires at cleanup, while `BoardPresence.creatureValue` already discounts it. Fixing that in the
feature would be better than flooring it in the policy, but it changes `LEGACY_V0` and so needs its
own switch and its own arena run.)

### 3. `noncreature-02` fails by 0.40 points, and it is not a card-knowledge failure

Exact arithmetic, `AiProfile.PRODUCTION`, Disenchant in hand against an opposing Glorious Anthem
behind an empty board:

| | pass | cast | Δ | × weight |
|---|---|---|---|---|
| `BoardPresence` | −1.1 | +1.3 | +2.4 | +3.60 |
| `CardAdvantage` | +4.0 | 0.0 | −4.0 | −4.00 |
| everything else | — | — | 0 | 0 |
| **total** | **7.75** | **7.35** | | **−0.40** |

The AI now sees the anthem (prior 3.0, up from 0.5) and ranks it correctly — `noncreature-01`, `-03`
and `-04` all pass on the same machinery. What it cannot outvote is `CardAdvantage.cardValue(0) =
−3.0`, which prices emptying your hand as a 4-point disaster. That is the same constant
`sequencing-02` fails on, and Phase 9's logistic fit is what replaces it. Raising the anthem prior
from 3.0 to 3.3 would pass this puzzle; it would also be tuning one hand-drawn guess to cancel
another, so it was not done.

### 4. The two rating stores are not duplicates, and chaining beats consolidating

The plan called `ai/src/main/resources/draftai/ratings/` a "duplicate store" of
`rules-engine/src/main/resources/ratings/` and asked for consolidation. They hold different things:
the first is a curated 0–5 pick rating covering **44 sets**, the second is raw 17Lands win-rate and
game-count data covering **one** (BLB). Deleting either loses information.

`LimitedCardRater.rate` now chains them — measured win rate, then curated pick rating, then the
heuristic — and both set lists come from a `_manifest.json` instead of a hardcoded
`listOf("BLB")`. The rater went from real data on one set to real data on 44, with no new files.
Cube tables (`vintage_cube`, `arena_powered_cube`) are deliberately out of the manifest: they rate
cards for a powered cube, not for a set's limited environment.

### 5. What the analyzer cannot read, and why that is the safe direction

`EffectWalker` descends through gates (`if`/`may`) and composites, and treats modal interiors,
pipeline stages and `ForEach` bodies as leaves — the traversal `LimitedCardRater` already had, moved
out so both scorers share one walk. Widening it would change every limited rating and therefore
every generated sealed deck, so it is a follow-up rather than a free improvement.
(`CardIntentAnalyzer` reads `ForEachEffect` itself, which is how a wrath is recognized at all.)

A card the analyzer cannot interpret gets `CardIntent.UNKNOWN`, whose `staticPriorValue` is the
historical flat `0.5`. And the prior is applied as a **floor**, never a ceiling: no permanent is
ever valued lower than it was before Phase 6. Both choices point the same way — the failure mode is
"no better than before", never "confidently wrong".

---

# Phase 2b — Puzzle suite, second pass

Measured 2026-07-29. Three of the six planned categories shipped — the three that needed no
framework change. `just arena-puzzles`.

## Why the suite needed a second pass

Phase 6 took it to 44/48, and **two of the four remaining failures were the same constant**
(`CardAdvantage.cardValue(0) = −3.0`, which Phase 9 exists to refit). So the plan's most expensive
phase, the rollout evaluator, had a **two-puzzle** localizing signal — and its written exit
criterion, phrased over sequencing / race math / board-wipe timing, could move exactly **one**,
because those categories stood at 5/6, 5/6 and 6/6 and the one sequencing miss is the Phase 9
constant.

The 48 also shared five properties by construction rather than by choice: every position probed
`chooseAction` at a clean priority window (so all 18 `PendingDecision` branches in
`DecisionResponder.kt` were unmeasured), scored **one** action (so a *line* was inexpressible), was
1v1, never had anything on the stack, and never asserted on an `ActivateAbility` — even though
`PuzzleMove` had spoken one since Phase 2.

## Per-category baseline — 48 → 66 puzzles

`AiProfile.PRODUCTION`, with the zero-weight `v0-blind` control.

| Category | `production` | `v0-blind` |
|---|---|---|
| lethal | 6/6 | 6/6 |
| blocking | 6/6 | 6/6 |
| removal | 6/6 | 0/6 |
| instants | 5/6 | 2/6 |
| sequencing | 5/6 | 0/6 |
| wipe | 6/6 | 3/6 |
| race | 5/6 | 5/6 |
| noncreature | 5/6 | 0/6 |
| **respond** *(new)* | **5/6** | **1/6** |
| **activate** *(new)* | **5/6** | **2/6** |
| **keywords** *(new)* | **6/6** | **5/6** |
| **total** | **60/66 (91%)** | **30/66 (45%)** |

The AI solves **16 of the 18** new positions. The discrimination control holds at the new size,
which was the criterion that mattered: a set of 18 positions that a blind agent solves as often as
the real one would be 18 coin flips.

## What Phase 2b found

**1. A latent harness bug, hiding behind the fact that no earlier puzzle had twin blockers.**
`PuzzleMove.blockAssignments` was a `Map<String, List<String>>` keyed by **card name**, so two
Grizzly Bears gang-blocking one attacker collapsed into a single map entry and
`shouldBlockWithAtLeast` counted 1. `keywords-03` (menace) therefore reported a failure the AI had
not made — it finds the double block correctly. It is a `List<Pair<…>>` now.

The engine was never at fault, and `PuzzleRunner` had already said so: it processes every chosen
move and fails the puzzle when the engine rejects it, and it did not reject this one.
`BlockPhaseManager.validateMenaceRequirements:504` is correct. **A puzzle reporting an illegal move
that the engine accepts is a harness bug, every time** — the legality gate is the tell.

**2. `keywords` barely discriminates — 6/6 against 5/6 blind — which is Phase 2's finding again.**
Combat is carried by `CombatAdvisor`'s seed heuristics, not by `BoardFeatures.kt`, exactly as
`lethal` and `blocking` already showed. Trample, menace and reach are a regression net for *that*
code and a `BoardFeatures` change will not move them. The one position in the category that the
evaluator owns is `keywords-06` — Murder with an indestructible creature and a Craw Wurm to choose
between — and it passes: `chooseCommittedTargets`' simulation refinement does overrule
`heuristicTargetRank`'s +3.0 indestructible bonus in time. That was a predicted failure and the
prediction was wrong, which is worth more than the pass.

**3. Both new failures are `instants-05`'s shape, and that is the deliverable.**

| Puzzle | Why it fails |
|---|---|
| `respond-05` | A regeneration shield is bought *before* the destruction it answers. At the moment of the activation the board is unchanged and two mana are gone |
| `activate-05` | Firebreathing an unblocked attacker pays now for damage that lands at the combat-damage step. `evaluate1Ply` simulates to the next quiet state, which is still inside declare-blockers |

Both are "pay now for an effect that materializes a step later", which is precisely what
`instants-05` (Fog at 2 life) has failed on since Phase 2. **Phase 7's signal is now four puzzles
rather than two** — `instants-05`, `race-03`, `respond-05`, `activate-05` — and two of the four are
non-combat, so `CombatAdvisor` cannot carry them the way it carries `lethal` and `blocking`.

## Not built

`walker`, `lines`, `decisions` and `pod` need framework work first, itemized in
`backlog/engine-ai-improvement.md` § Phase 2b. One concrete finding from scoping them:
`withCardOnBattlefield` attaches no `CountersComponent`, so a **planeswalker placed directly on the
battlefield enters at 0 loyalty and dies to state-based actions immediately** — the walker category
cannot be written until `ScenarioBuilder` can seed counters.

---

# Phase 7 — Rollout evaluator

Measured 2026-07-29 on this machine (8 arena threads, BLB, seed 20260727). Reproduce with
`just arena v0 v0-rollout-8 300` and `just arena-puzzles-compare`.

`RolloutCandidateEvaluator` replaces the greedy leaf — one `BoardEvaluator.evaluate` on the state
right after a candidate resolves — with the mean of several short playouts. Architecture and seams:
[`architecture.md`](architecture.md).

## Headline

| Measurement | Result |
|---|---|
| **`just arena v0 v0-rollout 300`** (shipped, 16 playouts) | **`v0-rollout` 56.0%, CI [52.0%, 59.7%]** |
| `just arena v0 v0-rollout-8 300` | `v0-rollout-8` 57.3%, CI [53.0%, 61.7%] |
| Puzzle suite, `v0-rollout` vs `v0` | **55/66 vs 55/66** — neutral: instants +1, respond −1 |
| `instants-05` (Fog at 2 life) | **closed** — the puzzle the plan assigned to this phase |
| Shipped playout count | **16**, measured — not the ~60 a 2 s tier affords |

The exit criterion was "arena ≥53% with the lower CI bound above 50%". 57.3% with a lower bound of
53.0% clears it, on 300 paired games rather than the nominal 1,000 — see correction 4 for why.

### Strength rises to ~8 playouts, then plateaus

The rollout ladder, which is Phase 7's own safety net:

| matchup | games | result |
|---|---|---|
| `v0` vs `v0-rollout-4` | 300 | 53.7%, CI [49.3%, 57.3%] — spans parity |
| `v0` vs `v0-rollout-8` | 300 | **57.3%, CI [53.0%, 61.7%]** |
| `v0-rollout-4` vs `v0-rollout-32` | 400 | 50.7%, CI [47.5%, 53.7%] — flat |

**Not the alarming shape.** The risk register's failure mode is strength *falling* with more search,
which would mean the rollouts are noise; this is saturation, which means they are signal with a
ceiling. The gain appears between 4 and 8 playouts and 8× more buys nothing measurable.

The plateau says the rollout term is **bias-limited, not variance-limited**. It carries a quarter of
each score (`staticWeight = 0.75`), common random numbers already pair its comparisons so its
between-candidate noise is small at any count, and what it *cannot* see — tempo, correction 2 — no
amount of sampling reveals. More samples sharpen an estimate that was never the bottleneck.

So `SearchAllowances.NORMAL_PLAYOUTS` ships at **16**: above the 8 that demonstrated the win, below
the 32 that demonstrably adds nothing, and 4× cheaper than the budget arithmetic would have spent.
The headroom is deliberate — the plateau was measured on one set at one `staticWeight`, and the
honest generalization is "few playouts suffice here", not "few playouts suffice".

## Per-category puzzle scores

`staticWeight` sweep on the 66-puzzle suite (Phase 2b's three new categories included). `v0` is the
greedy reference; `v0-rollout-pure` is the same agent with the static leaf mixed out entirely.

| Category | v0 | v0-rollout-pure | v0-rollout-25 | v0-rollout (0.75) | v0-phase4-intent | +rollout |
|---|---|---|---|---|---|---|
| lethal | 6/6 | 6/6 | 6/6 | 6/6 | 6/6 | 6/6 |
| blocking | 6/6 | 6/6 | 6/6 | 6/6 | 6/6 | 6/6 |
| removal | 6/6 | 2/6 | 5/6 | 6/6 | 6/6 | 6/6 |
| **instants** | 3/6 | 5/6 | 4/6 | **4/6** | 3/6 | 3/6 |
| sequencing | 5/6 | 3/6 | 5/6 | 5/6 | 5/6 | 5/6 |
| wipe | 6/6 | 6/6 | 6/6 | 6/6 | 6/6 | 6/6 |
| race | 5/6 | 5/6 | 5/6 | 5/6 | 5/6 | 5/6 |
| noncreature | 2/6 | 0/6 | 2/6 | 2/6 | 5/6 | 4/6 |
| **respond** | 5/6 | 4/6 | 4/6 | **4/6** | 5/6 | 4/6 |
| activate | 5/6 | 5/6 | 6/6 | 5/6 | 5/6 | 5/6 |
| keywords | 6/6 | 6/6 | 6/6 | 6/6 | 6/6 | 6/6 |
| **total** | **55/66** | **48/66** | **55/66** | **55/66** | **58/66** | **56/66** |

**On the suite the rollout is neutral, and the two moves cancel exactly.** It gains `instants-05` —
Fog against a lethal alpha strike, which Phase 2 diagnosed precisely ("a one-ply evaluator cannot see
prevention: the post-simulation state has the same life totals as passing") and assigned to this
phase — and it loses `respond-02`, "do not spend the only Counterspell on a 2/2 with seven lands
still open".

`respond-02` is a **horizon effect, and the honest cost of the mechanism**: countering the 2/2 shows
a concrete board gain inside the two-turn horizon, while the price — not having the Counterspell for
something that matters — falls outside it. A longer horizon is not the fix (it costs samples and the
card may not be needed for many turns); knowing what the card is *for* is, and that is
`CardIntent`/`HoldPolicy` territory. Note `production`, which has both, keeps 5/6.

Two readings worth keeping separate. **On top of Phases 4 and 6 the rollout is a small net
negative** — 56/66 against 58/66, losing `noncreature-05` and `respond-02` — so what ships is not
"turn everything on"; the arena is what says the rollouts earn their place, and the suite is what
says where they do not. And the `staticWeight` sweep that looked monotone on 48 puzzles is **flat at
0.25 / 0.5 / 0.75 on 66** (55 / 55 / 55). Only the pure rollout is clearly bad. So 0.75 is no longer
*selected* by the suite, merely not contradicted by it — the arena number was measured at 0.75 and
the value stays there, but it is now an unvalidated choice inside a plateau rather than a peak, and
it belongs in Phase 9's fit with the rest of the guesses.

## Five corrections the build produced

### 1. Squashing the *absolute* board score makes the search report "certain loss" for every line

The plan says to squash a score into a win probability with `1 / (1 + exp(-s / SCALE))` and average
there. It does not say what to squash, and the obvious reading — the leaf's own score — does not
work, because **the evaluator has no calibrated zero**. `ThreatAssessment` prices "we can never kill
them" with a 99-turn sentinel (`turnsUntilWeKill = 99.0`, `score -= (99 - turnsUntilDead) * 1.5`),
so an ordinary turn-1 position where one side has no creatures scores **−176** while a close board
is single digits. At any `SCALE` small enough to discriminate between real candidates, −176 and
−156 both squash past the clamp to the same number.

Measured before the fix: every candidate on `removal-01` and `instants-05` scored exactly
`logit(1e-4)`, and the suite fell to 32/48 — measured on the 48-puzzle suite, before Phase 2b — with
the failures uniformly "chose PassPriority".

The fix is to squash **the delta from the decision's root**. The baseline is shared by every
candidate, so the arbitrary offset cancels and only the differences the Strategist actually compares
survive — which also lets `SCALE` be calibrated against score *differences* (single digits) instead
of absolute scores. `WinProbability.asBaseline` guards the terminal sentinel; everything else passes
through raw, because routing the baseline through `squash` first would clamp away exactly the
magnitude it exists to subtract.

The 99-turn sentinel is not itself a bug for a greedy agent — both candidates carry the same offset,
so only the difference matters — but it is a standing hazard for anything that reads an absolute
score, and Phase 9's logistic fit should replace it rather than rescale it.

### 2. A pure rollout is **weaker** than the greedy AI it replaces, for a structural reason

At `staticWeight = 0` the agent scores 48/66, against `v0`'s 55: removal 6/6 → **2/6**, sequencing
5/6 → 3/6 and non-creature valuation 2/6 → 0/6, every failure an unnecessary pass.

The cause is not sampling noise. **Passing in your own main phase does not end the turn — it
advances a step — and the playout policy then casts the very spell you just declined**, a window or
a phase later. Two turns downstream the "cast it now" line and the "pass" line have converged to the
same board, so the rollout mean genuinely cannot see the tempo difference between them, and the
Strategist's strict `best > pass` sends the tie to passing.

The static leaf sees exactly that tempo, and the rollout sees exactly what one ply cannot
(prevention, the crack-back, whether a race is won). Mixing them recovers both.
`RolloutSettings.staticWeight` is the mixing fraction; the sweep is monotone up to 0.75:

| `staticWeight` | 0.0 | 0.25 | 0.5 | 0.75 | (1.0 = `v0`) |
|---|---|---|---|---|---|
| 48-puzzle suite | 34/48 | 39/48 | 39/48 | **40/48** | 39/48 |
| 66-puzzle suite | 48/66 | 55/66 | 55/66 | 55/66 | 55/66 |

The 48-puzzle suite selected 0.75 and the arena was measured there. **The expanded suite does not
reproduce the peak** — 0.25, 0.5 and 0.75 are indistinguishable at 55/66 — which is a useful lesson
about picking a constant on 48 samples, and exactly why Phase 9 fits rather than sweeps. What both
suites agree on is the shape that matters: mixing *some* static leaf back in is worth ~7 puzzles
over not mixing at all. The parameter remains its own control, since `v0-rollout-pure` and `v0` are
the two endpoints of the same knob.

### 3. Puzzle positions had empty libraries, which is invisible to a greedy agent and fatal to a searching one

`ScenarioBuilder.withPlayers` initialises every zone to empty and no puzzle stocked a library.
`BoardFeatures` never reads library size and a single simulated action never crosses a draw step, so
`v0` cannot tell — but a playout runs two turns forward, hits the draw step with nothing to draw,
and **every line ends in a decking race decided by whose draw step comes first** (CR 104.3c).

`PuzzleRunner` now stocks 30 basics per seat through the engine's own `CardEntityFactory`. The
existing baselines are unchanged by it (`production` still scores what Phase 2b baselined it at,
`FrozenBaselineTest` still green), which is the evidence that it fixes the harness rather than the
positions.

### 4. Phase 7 is the first search whose cost the arena can feel

A rollout decision is *N* playouts × ~40 engine actions, so at the 64 the budget arithmetic
suggested it is ~2,400 `process()` calls: **~70 s per rollout-vs-`v0` game** against `v0`'s ~0.07 s,
and the nominal 1,000-game merge gate goes from 3.5 minutes to hours. At the shipped 16 it is ~4×
better and a 300-game run is minutes.

Phase 1's report predicted exactly this ("the budget, not the game count, is what makes an arena
expensive") and Phase 4b did not need the mitigation because a filtered greedy agent costs nothing.
Phase 7 does. `RolloutBudgetPolicy` resizes the playout allowance, and the ladder of counts
(`v0-rollout-4/8/16/32`) is both what makes the arena affordable and the phase's own safety net:
strength must be monotone in playouts, or the search is generating noise rather than signal.

One caution the ladder taught about itself: `v0-rollout-4` vs `v0-rollout-8` measured 50.0%, CI
[43.3%, 56.7%] over 60 games. That is a statement about the sample size, not about the agents —
size a rung to the gap you expect to see, and prefer 4-against-32 to 4-against-8.

### 5. A pre-existing "Not enough mana" rejection, surfaced but not caused

The 300-game shipped run reports **13 rejected actions**, all `CastSpell: Not enough mana to cast
this spell` — the enumerator marked a spell affordable and the processor disagreed. Phase 1's
illegal-action finding was closed in Phase 4a, so this looks like a regression and is not one:

| agent pair | 300 games | rejections |
|---|---|---|
| `v0` vs `blb-advisors` (pre-Phase-6) | 300 | **42** |
| `v0` vs `v0-intent` (Phase 6) | 300 | 19 |
| `v0` vs `v0-rollout` (Phase 7) | 300 | **13** |

Every agent that plays *different lines from `v0`* surfaces it, at rates that put the rollout agent
at the bottom of the three. `v0` never reaches the states that trigger it, which is why
`ArenaHarnessTest`'s clean-games assertion — a `v0` mirror — stays green and why this went unnoticed
until an expensive run printed a histogram.

Left open, deliberately, the same posture Phase 1 took with its own illegal-action finding: it is an
affordability bug somewhere in the enumerator or the mana solver, not evaluator work, and diagnosing
it inside a phase about leaf scoring would be scope creep. It wants its own reproduction — the
histogram gives the error string and the arena gives the seed.

### 6. The batch scoring API is what makes sequential halving possible at all

The plan specifies `CandidateEvaluator.score(root, afterAction, playerId, budget)` — one candidate —
and separately specifies sequential halving over candidates. Those are incompatible: an evaluator
that sees one candidate cannot decide to spend four times as much on it as on its siblings.

`scoreAll` is the resolution, and it defaults to `map(::score)`, so `StaticCandidateEvaluator` needs
no override and `LEGACY_V0` comes out bit-identical — `FrozenBaselineTest` is what proves it. The
Strategist's loop became three passes (simulate all, score all, adjust all) which is also simply a
clearer shape than the old interleaving.

---

# Phase 8 — Determinization

Measured 2026-07-29, 8-core M1 Pro, BLB sealed, seed 20260727.

```bash
just arena v0-rollout v0-rollout-determinized 100
just arena v0 v0-rollout-determinized 100
```

The initial paired smoke found no detectable fairness cost:

| Matchup | Win share for A | Pair CI | Games | Completed |
|---|---:|---:|---:|---:|
| full-information rollout vs determinized rollout | 49.0% | [43.0%, 55.0%] | 100 | 100% |
| determinized rollout vs frozen `v0` | **55.0%** | [48.0%, 62.0%] | 100 | 100% |

This sample prices only a large regression; it does not prove equivalence. Its useful result is that
sampling one shared known-deck world did not produce the expected catastrophic dip or destabilize
games. The fair agent's 55% point estimate against `v0` retains Phase 7's strength signal, while the
smoke-sized interval still spans parity. The 1,000-game merge-sized run remains the
publication-quality measurement.

The run reported three `CastSpell: Not enough mana to cast this spell` rejections, all on one paired
seed and present in both seat orientations. That is the same pre-existing enumerator/processor
disagreement Phase 7 records above, not a new error class introduced by determinization.

---

# Promotion — `production-candidate-tuned` goes live

Measured 2026-08-07, 8-core M1 Pro, BLB, seed 20260727. Every arena run is 300 paired games,
100% completed, 0 illegal actions. Reproduce with `just arena production <agent> 300` and
`just arena-puzzles-compare`.

Phases 4, 7 and 8 shipped as code and were never switched on: `AiProfile.PRODUCTION` set only the
card advisors and `useCardIntent`, and `EngineAiPlayerController` hardcoded it. This is the run
that promoted them, plus two targeted fixes built here.

## The gate nobody had run

Every number above is quoted against `v0`, which carries neither the card advisors nor `CardIntent`
— so "the rollouts are worth +6%" was a statement about an agent nobody plays against. The
promotion gate is against **`production`**, and it needed a profile that did not exist.

| Matchup | Win share for B | Pair CI | Puzzles (B) |
|---|---:|---:|---:|
| `production` vs `production-candidate` | **57.3%** | [53.3%, 61.7%] | 56/66 |
| `production` vs `production-horizon-concave-2` | 50.3% | [49.3%, 51.3%] | **63/66** |
| **`production` vs `production-candidate-tuned`** | **56.7%** | **[53.0%, 60.7%]** | 60/66 |

`production` itself scores 60/66.

**The two scoreboards measure nearly orthogonal things here, and the result is a combination rather
than a choice.** The rollouts win +7.3% and *cost* four puzzles; the two cheap fixes win three
puzzles and are arena-neutral. Layering both keeps the win rate intact (+6.7%, statistically the
same as +7.3%) and returns the suite to parity with what shipped.

Latency, which was listed as an unrecorded Phase 4b deliverable: 25.0 s per game over ~504 actions
with rollouts on one seat, so ~100 ms per candidate decision — an order of magnitude inside the
2 s NORMAL tier and far under the server's 500 ms `thinkingDelayMs`.

## Two targeted fixes, each isolated

| Agent | Puzzles | Closes | Breaks |
|---|---:|---|---|
| `production` | 60/66 | — | — |
| `production-horizon` | **62/66** | `instants-05`, `activate-05` | none |
| `production-concave` (−1.0) | 61/66 | `sequencing-02`, `noncreature-02` | `respond-02` |
| `production-concave-2` (−2.0) | 61/66 | `noncreature-02` | none |
| `production-horizon-concave-2` | **63/66** | all three above | none |

**1. `resolveThroughCombatDamage`.** A quiet state is "the stack is empty", which inside combat is
*before damage*. Fog, a regeneration shield and a firebreathing pump therefore read as pure cost.
Carrying the simulation one step further — only once blockers are declared, since before that the
outcome still depends on blocks nothing here would declare — closes `instants-05`, the Fog puzzle
Phase 2 explicitly assigned to Phase 7's rollouts, for one extra step instead of 16 playouts.

**2. `EvaluationWeights.topdeckPenalty`.** `CardAdvantage`'s marginal card value ran 4.0 / 1.5 /
1.5 / 0.8 — the first card broke concavity by nearly 3×, which is why holding the last card beat
casting a spell that puts a 2/2 on the board. −2.0 makes the curve concave everywhere. −1.0 also
closes `sequencing-02` but starts spending the last Counterspell on a 2/2 with seven lands open;
`respond-02` is the negative control that exists to catch exactly that, so −2.0 is what ships.

## Three findings worth carrying forward

1. **The card advisors are not tactically neutral.** Three prior measurements (arena, pod, puzzles)
   agreed they were worth nothing, and on win rate they are. But `production` scores 60/66 against
   `v0-phase4-intent`'s 58/66 — on tactics they are worth +2.
2. **Phase 4 costs two puzzles.** `production-tuned` (which adds `useMeaningfulFilter` +
   `TieredBudgetPolicy` on top of the two fixes) scores 61/66 rather than 63/66, losing
   `instants-02` and `instants-03` — combat tricks, i.e. the budget cutting combat simulations.
   Neutral in the arena, so it ships, but the nominal tier sizes deserve their own look.
3. **`priceCrackBackAsLife` is a no-op and `race-03`'s diagnosis was wrong.** Replacing
   `evaluateAttackPlan`'s flat −3.0-at-lethal with the life actually lost changes nothing on the
   suite: `incomingNextTurnDamage` is computed on the **post-combat** state, and in `race-03` the
   blocker has already traded away by then, so there is no crack-back left to price. The cliff it
   removes is real (at 5 life, 4 damage was free and 5 cost −3.0) but unmeasured. Shipped **off**.

## What the suite still cannot solve

`production-horizon-concave-2` leaves three, and none is a constant away:

- **`sequencing-02`** — only reachable by moving the curve to −1.0, at the cost of a negative
  control. The real fix is that a land drop should not be charged as card loss at all; it converts
  a card to mana rather than spending it. **Closed the next day — see below.**
- **`race-03`** — needs the crack-back estimated on the *pre*-combat state, or a genuine
  attack-vs-hold model. The rollouts do not close it either.
- **`respond-05`** — unexplained. `resolveToQuietState` should already resolve the Wrath and see
  the Troll survive, so the recorded explanation does not match the code path.

---

# Promotion — `production-candidate-landdrop` goes live

Measured 2026-08-07, 8-core M1 Pro, BLB, seed 20260727, 0 illegal actions and 100% completed on
both runs. Reproduce with `just arena production production-landdrop 400`,
`just arena production-candidate-tuned production-candidate-landdrop 300` and
`just arena-puzzles-compare`.

This is the item the section above left open: **a land drop is not card loss.**

## The defect, measured on the position rather than argued

`sequencing-02` is one Forest on the battlefield and one Forest in hand. Playing it moves
`CardAdvantage` from `cardValue(1) = 1.0` to `cardValue(0) = −2.0`:

| feature | Δ playing the land | weight | contribution |
|---|---:|---:|---:|
| `CardAdvantage` | −3.0 | 1.0 | **−3.00** |
| `Tempo` (1 → 2 lands) | +2.0 | 0.6 | +1.20 |
| `BoardPresence` (untapped land) | +0.6 | 1.5 | +0.90 |
| | | | **−0.90 → pass** |

The land drop was a strict debit: it paid 1.5–3.0 of card advantage to buy 2.1 of board and tempo,
and at the empty-hand cliff it did not cover the difference. So the AI sat on its last land — and
unlike the rest of the suite's failures, that position is not a contrivance. Every game reaches
several turns whose only card in hand is a land.

## Why not the constant, again

Moving `topdeckPenalty` to −1.0 also closes `sequencing-02`. The promotion run above measured that
and rejected it, because it makes an empty hand cheaper *everywhere* and cost `respond-02` — the
negative control that exists for it. `AiProfile.landDropIsNotCardLoss` changes what a hand
*contains* instead: `CardAdvantage.heldCardCount` holds back one land per unused land drop, so the
drop is **exactly** card-neutral and the cliff stays where it is. `respond-02`'s verdict does not
move.

The earmark reads `LandDropsComponent.remaining` rather than the enumerator's `canPlayLand`, which
also demands main phase / empty stack / your turn. Land drops reset for *every* player at cleanup,
so `remaining` is 1 for the non-active player too — the earmark is symmetric across the table and
survives a turn boundary rather than flickering on and off inside one. It misses the
`GrantAdditionalLandDrop` statics `LandDropUtils` adds, which want a `CardRegistry` the feature does
not have; under an Exploration the second drop is still charged, which is the old behaviour rather
than a new error.

## Both scoreboards

| Matchup | Win share for B | Pair CI | Games | Puzzles (B) |
|---|---:|---:|---:|---:|
| `production` vs `production-landdrop` | 50.5% | [46.8%, 52.3%] for A | 400 | 61/66 |
| `production-candidate-tuned` vs `production-candidate-landdrop` | **51.0%** | **[46.3%, 51.3%] for A** | 300 | **61/66** |

Both verdicts are *not distinguishable* — the CI spans parity, which is the same bar
`production-horizon-concave-2` shipped on. What the puzzles say is unambiguous, and it is one line:

| Agent | Puzzles | Closes | Breaks |
|---|---:|---|---|
| `production` | 60/66 | — | — |
| `production-landdrop` | **61/66** | `sequencing-02` | none |
| `production-horizon-concave-2` | 63/66 | — | — |
| `production-horizon-concave-2` + land drop | **64/66** | `sequencing-02` | none |
| `production-candidate-tuned` (was live) | 60/66 | — | — |
| `production-candidate-landdrop` (**live**) | **61/66** | `sequencing-02` | none |

`sequencing-02` is the only verdict that moves in any column, which is what makes the delta
attributable to this change and nothing else.

## Two findings worth carrying forward

1. **The live agent's failing set is not the baseline's.** `production` and
   `production-candidate-tuned` both score 60/66 and they are *not the same 60*: the rollouts trade
   `instants-05`, `noncreature-02` and `activate-05` for `instants-02`, `instants-03` and
   `respond-02`. Two consequences. `respond-02` — the negative control that chose
   `concave-hand-2` over `concave-hand` — is **already failing on the live agent**, so the argument
   that pinned that constant no longer describes what ships and the choice deserves re-measuring.
   And a suite total is not a suite result; only the failing *set* localizes.
2. **A rollout promotion gate costs 40 minutes, not minutes.** 300 paired games with rollouts on
   *both* seats ran 2,435 s wall clock at 44 s/game on 8 threads — ~26× the 1.7 s/game of the
   rollout-free pair. Price the mechanism against a cheap baseline first (`production` vs
   `production-landdrop` took 103 s and gave the same answer), and spend the expensive run only on
   the gate that actually decides.

---

# Promotion — `production-candidate-raceclock` goes live

**Measured:** 2026-08-07. Flag `AiProfile.discountedRaceClock`, profiles `production-raceclock` and
`production-candidate-raceclock`. The first evaluator fix in this file whose **arena** result is
positive rather than merely neutral — and the first promotion carried by the arena half of the bar
rather than the puzzle half.

## The defect

`ThreatAssessment` scores the race as `(theirClock − ourClock) × 2.0` when we are faster and `× 1.5`
when we are not, where a clock is `life / attackPower` **in turns**, and a side with no attacker is
handed the sentinel `99.0`.

The sentinel goes into the subtraction. An empty board facing a single 2/2 scores
`(99 − 10) × 1.5 = −133.5` raw, **−160 weighted**; one 2/2 against an empty board reads **+178**, a
6/4 reads **+191**. For scale, in the same evaluator a point of life is worth 1.0 and that 2/2 is
worth 3.6 of board presence. So on every position where one side has no creature — most of the first
four turns, every post-sweeper board, most of a puzzle position — this single term decides the move.

The sentinel is the symptom. Measuring the race in **turns** is the cause, and it is wrong twice
over even where no sentinel is involved:

- **Distance is not discounted.** The gap between dying on turn 10 and turn 20 counts for as much as
  the gap between dying next turn and the turn after. A great deal happens in ten turns.
- **It is sublinear in power**, which is backwards. Going from 2 power to 4 halves a 20-life clock
  from 10 turns to 5 — worth 7.5. Going from 4 to 8 halves it again, 5 turns to 2.5 — worth 3.75.
  Each step adds the same damage.

`lastchance-05` is the clean demonstration. Unsummon, Murder on the stack targeting our Serra Angel,
their Grizzly Bears also legal. `Strategist.chooseCommittedTargets` **does** simulate both targets
and picks the wrong one on the merits: saving the Angel leaves their 2/2 against our empty board
(−160), throwing it away clears the board (0). Everything else in the evaluator — a 4/4 flier back
in hand against a 2/2 back in theirs — is worth +3.8 combined.

## The fix

Score the race in **urgency** — `power / life`, the share of a life total removed per turn — rather
than in turns. Urgency is `1 / turns`, so it discounts a distant clock the way distance should be
discounted (1 turn → 1.0, three turns → 0.33, ten turns → 0.1), it is linear in power, and a side
with no creatures is **0.0** with no sentinel and no special case. Same slopes (2.0 ahead, 1.5
behind), capped at 1.0 because nothing kills you more than dead this turn.

`RACE_URGENCY_SCALE` is **4.0**, swept on the suite against `production`:

| scale | 0 | 2 | 4 | 6 | 10 | 15 | 20 | unbounded (today) |
|---|---|---|---|---|---|---|---|---|
| passes | 69 | 70 | **71** | 70 | 68 | 68 | 69 | 71 |

Scale 0 deletes the term; losing two puzzles to it is what says the race is worth scoring at all.
The value that would *reproduce* the old term at a typical 3-turn race is ~10 (near a symmetric
clock `T`, urgency is turns divided by `T²`), and 10 measures worse — because the turns form was
double-counting life that `LifeDifferential` already prices at 1.0 a point and power that
`BoardPresence` already prices.

## Both scoreboards

| | `production` | live (`production-candidate-trickwindow`) |
|---|---|---|
| puzzles, baseline | 71/83 | 76/83 |
| puzzles, with the fix | 71/83 | 76/83 |
| closes | `lastchance-05`, `race-03`, `timing-01` | `lastchance-05`, `race-03` |
| loses | `activate-04`, `instants-03`, `removal-03` | `activate-04`, `removal-03` |

**Arena — both columns, neither CI touching parity:**

| run | baseline win % | CI | record |
|---|---|---|---|
| `production` vs `production-raceclock` (isolation) | **43.7%** | [40.0%, 47.7%] | 131W-169L |
| `production-candidate-trickwindow` vs `production-candidate-raceclock` (**gate**) | **45.3%** | [41.3%, 49.3%] | 136W-164L |

300 games each, 300/300 completed, 0 illegal actions, rollouts on both seats for the gate (2050 s
wall clock). This is a genuine strength gain, not the usual "cannot resolve a difference at 300
games" — and it is why a puzzle-*level* result promotes here where the standing bar asks for a
puzzle ahead. `race-03` closing is the visible half of it: the term is now linear in the power we
leave unblocked, so keeping a blocker home finally scores.

`EngineAiPlayerController` and `AiProfileSelector`'s fallback now point at
`production-candidate-raceclock`. To back it out, revert those two call sites rather than the flag.

## The finding: the sentinel was standing in for `BoardPresence`

Every puzzle the fix costs is one that passes today *because* the term is out of scale, and each
names a real mispricing underneath:

- **`activate-04`** (do not point 1 damage at a 3/3) passes only because with a 1-power board,
  `d(ourClock)/d(theirLife) = 1`, so one point of face damage moves the clock a whole turn.
  Underneath, `BoardPresence.creatureValue` discounts a **damaged** creature by up to half —
  `×(0.5 + 0.5 × healthFraction)` — though marked damage wears off at cleanup. That is exactly the
  case the `temporaryPTModification` discount five lines above it was written to avoid.
- **`removal-03`** (kill the Hill Giant, not the Pacifism'd Craw Wurm) passes only because the
  pacified creature contributes no `attackPotential`, so killing the *other* one sends the opponent
  to the 99-turn sentinel. Underneath, a creature that cannot attack at all is discounted by ×0.85.
- **`blocking-02` / `keywords-01`** (do not chump) survive the urgency form but died under every
  hard cap tried, for the same reason: on the merits the margin is 0.6 — a 2/2's 3.6 of board
  presence against 3 life — thin enough that any reweighting flips it.

Fix the damaged-creature and cannot-attack prices and this should become a straight gain rather than
a trade. Each is its own flag, its own attribution column and its own arena run.

## Three shapes that look more principled and measure worse

All bound the sentinel just as well; all were swept and rejected before urgency:

- **Cap each clock at `H` turns, no-clock at `H`** (swept 10–20). Erases the gap between a 1-power
  and a 2-power board, so the AI chump-blocks — after the chump its own clock reads unchanged.
- **Cap at `H`, no-clock at `H + 1`** (swept 8–12). Holds the chump-block puzzles and closes
  `lastchance-05` on `production`, but not on the live agent, whose shallower `concave-hand-2` curve
  halves the card-advantage margin the race term has to be outvoted by.
- **Clamp the *difference*, or saturate it through `tanh`** (swept C = 4–6, K = 3–8). Both trade
  `activate-04` for `race-03` at every setting, because both flatten the gradient a real race runs
  on while leaving the turns unit — and the sublinearity in power — in place.

The common lesson: bounding the sentinel is not the fix. Changing the unit is.

---

# Promotion — `production-candidate-patience` goes live

**Measured:** 2026-08-07. Flag `AiProfile.holdRemovalForBetterTargets`, profiles
`production-patience` and `production-candidate-patience`. Implementation:
`ai/.../engine/knowledge/RemovalPatience.kt`.

> **The arena could not measure this one.** 100 games came back at exactly 50.0%, CI
> [50.0%, 50.0%], with **all 50 pairs at 1-1-0** — zero decisive pairs, where the three runs before
> it had 10, 36 and 37 out of 150. Read that as *no detectable effect*, not as *neutral with
> confidence*. See "The arena half" below for what it does and does not license.

## The defect

A one-ply evaluator scores the board right after the removal resolves, sees an opposing creature
gone, and has **no term at all for the option the card was**. So the removal fires at the first
legal target on the board, every game, whatever it is — the Pacifism goes on the turn-one 1/1 and
the Craw Wurm that shows up on turn six is unanswerable.

This is not new, and it is not undiagnosed. Phase 6 built the obvious fix — a **constant** penalty
on "instant removal in our own main phase" — measured it, and removed it, because the value large
enough to change behaviour (−2.0) also vetoed `noncreature-01`, the instant-speed Disenchant at the
only artifact on the table. `HoldPolicy`'s KDoc and `KNOWN_FAILURES`' entry for `timing-01` both
record the verdict: *holding removal is a preference between two futures, and a constant cannot
price one.*

## The fix: charge the target, not the window

The Phase 6 diagnosis is right about a constant and wrong about the question. The mistake is not
"the AI casts removal in its main phase" — it is "the AI casts removal at a **target that is not
worth a card**", and that is a comparison rather than a preference:

> A removal spell should answer a creature at least as expensive as itself. The discount is what the
> target falls **short** of that bar, priced at the profile's `boardPresence` weight.

`FAIR_TRADE_VALUE_PER_MANA = 1.4` is read off `BoardPresence.creatureValue`'s own scale rather than
chosen — a vanilla creature prices at about 1.4 per mana on it (Grizzly Bears 2.8, Hill Giant 4.2,
Craw Wurm 7.6, Air Elemental 8.3). So the bar moves with the card: a Shock is content with a 1/1
and a Murder is not.

| removal | bar | vs 1/1 (1.4) | vs 3/3 (4.2) | vs 6/4 (7.6) |
|---|---|---|---|---|
| Lightning Bolt {R} | 1.4 | 0 | 0 | 0 |
| Pacifism {1}{W} | 2.8 | −2.1 | 0 | 0 |
| Murder {1}{B}{B} | 4.2 | −4.2 | 0 | 0 |

Two things fall out of the shape that a constant could not give:

- **The Disenchant is safe by construction, not by tuning.** The discount only applies to a
  *creature* target — a creature is the thing a better one replaces next turn, which is the whole
  bet. An opposing artifact is a fixed, already-visible quantity with nothing better coming.
- **It ends four ways.** A **hard veto** whenever the opponent has lethal on board; a hand at the
  discard limit (`>= MaximumHandSize.DEFAULT`, so the next draw is already the pitch); a bar that
  decays linearly from turn 6 to nothing by turn 14; and, short of lethal, a board score that
  outvotes the discount whenever the kill is genuinely urgent — it is capped at
  `1.4 × manaValue × boardPresence`, small next to what `ThreatAssessment` pays for a creature that
  is actually racing us.

## The lethal veto, and why a magnitude argument was not enough

The first draft had only the last three exits, on the reasoning that the evaluator already prices
being dead on board at `−10.0` raw against a discount capped near 6, so the AI was going to cast
anyway. That is an argument about **magnitudes**, and the rule it stands in for is not: *the AI must
never sit on removal on a turn where doing nothing loses the game.* A magnitude argument holds until
someone refits the weights — Phase 9 is explicitly going to — and holds only for the profiles that
were measured. So the release is a veto, and it costs one boolean.

`ThreatAssessment.lethalOnBoardAgainst` is the predicate, extracted from the `−10.0` term itself so
there is one definition of "they have lethal" rather than two that can drift. It reads *unblockable*
damage — `attackPower >= life && attackPower > defense` — so a 0/8 in the way means the 1/1 is not
killing anyone and patience survives, which `RemovalPatienceTest` pins from both sides.

The narrow reading (lethal **now**, not lethal in two turns) is sufficient because it re-asks every
turn: at 4 life against a lone 2/2 nothing fires, and by the time the same board reads lethal — at
2 life — the removal is released with a turn still in hand.

## The card-knowledge gap it exposed

Pacifism — the card that motivated the request — carried **no `IntentTag` at all**. The analyzer
reads `ModifyStats` and `GrantKeyword` statics; `CantAttack` / `CantBlock` fell through to the empty
set, so no policy in the AI could see that Pacifism answers anything. `IntentTag.NEUTRALIZE` fixes
that, gated on `filter.scope == Scope.AttachedTo` so a creature's own printed drawback ("this
creature can't block") is not read as removal. It is deliberately **absent from
`priorValueOf`'s ladder**: it changes what the AI does with the card in hand and nothing about what
the permanent on the battlefield is worth, so no frozen `BoardPresence` number moves.

## Puzzles — 87 positions, four new

`removal-07` … `10` are one board (Murder, a lone 1/1, three cards of slack) with one thing moved at
a time: hold at four cards on turn one, **cast** at eight cards, **cast** on turn twenty, **cast**
at 1 life with the blocker taken away. "Hold it" is only defensible if every exit is pinned too, and
`removal-10` is the one that pins the rule Vincent stated as non-negotiable.

| | `production` | live (`production-candidate-raceclock`) |
|---|---|---|
| puzzles, baseline | 74/87 | 79/87 |
| puzzles, with the fix | 74/87 | **81/87** |
| closes | — | `removal-07`, `timing-01` |
| loses | — | — |

The candidate's failing set is a **strict subset** of the live agent's, so nothing was traded.
`removal-08` / `09` / `10` and `noncreature-01` — the four negative controls — stay passing in
every column, `removal-10` for every profile in the table including `v0`.

The `production` column is flat, and that is expected rather than disappointing: `timing-01` cannot
move on an agent that scores the race in turns, where the `99.0` no-attacker sentinel prices killing
the opponent's only creature at about +160 and no defensible discount competes. Read that column for
what patience *costs* on the 86 positions that have nothing to do with it — which is nothing.

## The arena half — measured, and null

```
just arena production-candidate-raceclock production-candidate-patience 100
```

| | value |
|---|---|
| Baseline (`production-candidate-raceclock`) win % | **50.0%**, CI [50.0%, 50.0%] |
| Record | 50W-50L-0D |
| Completed / illegal actions | 100/100, 0 |
| Wall clock | 627 s on 8 threads |

By the letter of the standing bar a CI spanning parity is a pass. This one does not merely span
parity, it **is** parity, with zero width — and a degenerate CI is a fact about the measurement
before it is a fact about the agent.

The pair distribution is where the information actually is. A pair is the same decks and seed played
with the seats swapped, so `1-1` means swapping the agents did not change who won:

| run | pairs | `1-1` (tie) | decisive |
|---|---|---|---|
| `production-candidate-trickwindow` vs `-raceclock` | 150 | 114 | **36** |
| `production` vs `production-raceclock` | 150 | 113 | **37** |
| `production-candidate-landdrop` vs `-landseq` | 150 | 140 | **10** |
| `production-candidate-raceclock` vs `-patience` | 50 | 50 | **0** |
| `production` vs `production-patience` (isolation) | 50 | 50 | **0** |

High tie rates are normal here — mirror decklists and a shared seed mean the draw usually decides
the game — but *zero* is not, and it happened twice: the isolation run on `production` (30 s, no
rollouts on either seat) returned the same 50/50, CI [50.0%, 50.0%], 0 decisive pairs. Combined
that is 0 of 100 pairs; under the land-sequencing rate, the mildest prior comparison at 6.7%
decisive, that has p ≈ 0.001.

That is small enough that "the case is rare" stopped being an adequate explanation, so it was
measured directly — see the next section. The answer is that the term fires **about once a game**,
which is a real effect and a far smaller one than 50 pairs can resolve.

What that licenses, precisely:

- **It is not a demonstrated strength gain.** Nothing here says the agent got better at winning.
- **It is not costing anything either.** Zero decisive pairs means it never lost one, and the four
  negative controls plus a strict-subset failing set say the same thing on the tactical side.
- **A larger run would be needed to resolve an effect this small**, and at ~6 s a game that is an
  expensive way to measure something the mechanism predicts is rare. The isolation column
  (`just arena production production-patience 300`) is cheaper — no rollouts on either seat — and is
  the one to run if this is revisited.

## How often does it actually fire?

The null result deserved a direct measurement rather than more reasoning about it, so: instrument
`discount()`, play 20 `production-patience` games (463 turns), and count. The instrumentation was
temporary and is gone.

| | count |
|---|---|
| `discount()` calls (every cast candidate is scored) | 1469 |
| … not a removal spell at all (untagged, or a creature body) | −1320 |
| … no single opposing *permanent* target (mostly burn aimed at the face) | −109 |
| … stopped by a release: turn window 7, hand full 4, facing lethal 2 | −13 |
| **reached the bar** | **27** |
| **non-zero discount — patience actually bit** | **19** |

**About once per game.** Not "never", which was the hypothesis this measurement killed, and not
often enough for 50 pairs to see. A term that changes roughly one decision a game — a decision that
is frequently not outcome-relevant even when it is right — is exactly the size of effect this arena
cannot resolve, which reconciles the null result with the puzzle result without either being wrong.

The second finding is that **the three releases are not what makes it rare**: 13 stops across 20
games, against 27 that reached the bar. The turn window in particular cost only 7. If this is ever
tuned for *more* effect, the lever is `FAIR_TRADE_VALUE_PER_MANA` or the `no-single-opposing-target`
path — not lengthening the patience window, which is not the binding constraint.

## A harness caveat found on the way

Two `production`-class games on the same deck, the same seed and the **same agent object** produce
different action streams — 4/4 attempts. So action-stream divergence is not a usable instrument for
these profiles, and two attempts to use it here were discarded (the first also confounded by
`TieredBudgetPolicy`'s wall-clock budget, which varies search depth with machine load). Counting
inside the term, as above, is immune to this and is the right instrument.

`FrozenBaselineTest` is unaffected: it pins `LEGACY_V0` on an all-vanilla Portal deck, which is
reproducible. The cause is undiagnosed and is its own piece of work — most likely an
identity-hash-ordered collection somewhere in the advisor or intent path, since that varies between
two constructions inside one JVM run.

## Promotion status

`EngineAiPlayerController` and `AiProfileSelector`'s fallback point at
`production-candidate-patience`, promoted on the puzzle half with the arena half returning null.
To back it out, revert those two call sites rather than the flag: it is off for every other profile,
so backing the promotion out costs nothing and loses no measurement.

The honest one-line summary: the AI is **tactically better** at two positions it used to get wrong,
with nothing traded on the other 85, and its **strength is unmeasured and probably unmeasurable at
this sample size**.

---

# Creature valuation — the two terms the race clock exposed

**Measured:** 2026-08-07. Same hardware, same harness.
`AiProfile.creatureValuation`, promoted as `production-candidate-boardvalue`.

`PRODUCTION_RACECLOCK`'s KDoc closed with a prediction: its arena win came with a puzzle trade, both
losses were `BoardPresence.creatureValue` weaknesses the old `99.0` no-attacker sentinel had been
masking, and "fix those two and the trade should become a straight gain." This is that measurement.
Both terms were wrong in **shape**, not in size, which is why neither was reachable by tuning:

- **Marked damage.** `value *= 0.5 + 0.5 × healthFraction` made one point on a 3/3 read as 0.7 of
  board progress (4.2 → 3.5). Damage wears off at cleanup — the exact case the
  `temporaryPTModification` subtraction five lines above it exists to avoid, arrived at from the
  other end. That is `activate-04`: a Prodigal Sorcerer spending its turn pinging a creature it
  cannot kill.
- **"Can't attack."** A flat `×0.85`, where the *same restriction* spelled `DEFENDER` cost
  `power × 0.8` — a factor of four between two spellings of one thing. A Pacifism'd Craw Wurm kept
  5.49 of a 7.6 body and so outranked an untouched Hill Giant at 4.2, which is `removal-03`. The
  multiplicative form is wrong in shape as well as size: it scales *toughness* down too, so it takes
  more off a creature that can still block, and it lands hardest on the big creatures where the
  restriction is worth most. Subtracting the power leaves the body a pacified 6/4 still walls a 3/3
  with.

## Puzzles — 87 positions

| | `production` | live (`production-candidate-patience`) |
|---|---|---|
| baseline | 74/87 | 81/87 |
| `markedDamageFadesAtCleanup` alone | 74/87 | — |
| `cantAttackCostsPower` alone | 74/87 | — |
| both (`production-candidate-boardvalue`) | — | **83/87** |
| closes | — | `activate-04`, `removal-03` |
| loses | — | — |

The candidate's failing set is a **strict subset** of the live agent's, so nothing was traded.

Both attribution columns are deliberately *empty*: each term leaves `production`'s failing set
**identical**, id for id. Neither can close its own puzzle on a baseline that still scores the race
in turns — that is the point of the prediction being about an interaction — and neither costs
anything across the other 86 positions. A term that moves nothing on the agent it cannot help is the
cheapest evidence available that it is not quietly taxing every creature in the suite.

## The arena half — the largest margin in the sequence

```
just arena production-candidate-patience production-candidate-boardvalue 300
```

| | value |
|---|---|
| Baseline (`production-candidate-patience`) win % | **45.3%**, CI [42.0%, 48.7%] |
| Pair score | −0.093, CI [−0.160, −0.027] |
| Record | 136W-164L-0D |
| Completed / illegal actions | 300/300, 0 |
| Seat 0 wins | 152/300 (50.7%) — first-player advantage, cancelled by pairing |
| Wall clock | 3,149 s on 8 threads |

The whole interval sits below parity, so this did not merely clear the standing "arena-neutral and a
puzzle ahead" bar — it cleared the arena half outright, which only `discountedRaceClock` had managed
before it. Note the contrast with the run above: the patience promotion returned a degenerate
50.0% CI [50.0%, 50.0%] with **zero** decisive pairs, because that term fires about once a game.
These two fire on every board that has a damaged or restricted creature on it, which is most boards
after the first combat, and the pair distribution reflects it.

## Promotion status

`EngineAiPlayerController` and `AiProfileSelector`'s fallback point at
`production-candidate-boardvalue`. To back it out, revert those two call sites rather than the
flags: both are off for every other profile, so backing the promotion out costs nothing and loses no
measurement.

Four puzzles remain unsolved by the live agent, and each names a subsystem rather than a constant:
`respond-02` (spends the only Counterspell on a 2/2 — the counterspell twin of `RemovalPatience`),
`respond-05` (a regeneration shield bought before the destruction it answers), `timing-03` (taps out
for a Hill Giant while holding a Counterspell — held mana as options, which nothing prices), and
`timing-05` (a cantrip left uncast at the opponent's end step).

## The cantrip window, and the harness bug it exposed

`AiProfile.cashCantripsInTheEndStep`. `HoldPolicy` only ever handed the opponent's end step back to
`REMOVAL`, so a DRAW-tagged instant got nothing at all and a cantrip — board value ~= 0, a card drawn
against a card spent — lost to passing at every window through to cleanup. The fix is a window on the
**tag**, not a discount on the **step**, which is what keeps `instants-06` answered: an expiring pump
is `COMBAT_TRICK` and is caught by the branch above it. That distinction is why `Strategist`'s old
blanket `passScore - 1.5` had to go.

It first measured **level** with its baseline — closing `timing-05` on `production` (74 -> 75) and
moving nothing on the live agent. The term was right; the harness was wrong.

### The diagnosis

The insight sink prints the per-candidate table, and it was decisive in one run — the window fires on
both agents, note and all. What differs is the *leaf*:

| profile | pass | cast Opt | deficit | window | result |
|---|---|---|---|---|---|
| `production` | 11.20 | 10.75 | -0.45 | +1.50 | casts |
| live (`production-candidate-boardvalue`) | 10.20 | 7.20 | **-3.00** | +1.50 | passes |

Bisecting the promotion chain one flag at a time isolates it to **`landDropIsNotCardLoss`**, live
since 2026-08-08: `production` + that flag alone prices casting Opt at **-4.45**. Nothing else in the
chain — `resolveThroughCombatDamage`, `concave-hand-2`, the rollouts, the budget tiers — moves the
number at all.

The mechanism: `PuzzleRunner.stockLibraries` filled every puzzle library with 30 `Forest`, on the
reasoning that "a land is the most inert card in Magic". `landDropIsNotCardLoss` stops *counting* one
land per unused land drop. So the card Opt draws is a Forest, a lone earmarked land counts as an
**empty hand**, and the cantrip's own draw steps off the topdeck cliff. Four points of pure harness,
against a 1.5-point window.

That zeroing is itself a simplification worth its own entry, below: a land in hand **is** a card with
value, and this feature prices it at nothing.

Swapping the filler to `Craw Wurm` — inert to the *evaluator* rather than inert in Magic — restores
`-0.45` on every profile and the live agent casts the Opt.

### Blast radius of the harness fix

Re-measured across all 34 profiles. Four verdicts move, all by +1, all upward:

| profile | Forest | Craw Wurm |
|---|---|---|
| `production` | 74/87 | 74/87 |
| `production-candidate-boardvalue` (live) | 83/87 | 83/87 |
| `production-candidate` | 70/87 | **71/87** |
| `v0-rollout-pure` | 60/87 | **61/87** |
| `v0-phase4-intent-rollout` | 70/87 | **71/87** |
| `production-candidate-cantrip` | 83/87 | **84/87** |

`production` and every promotion baseline are unchanged, so `PuzzleSuiteTest.KNOWN_FAILURES` needs no
edit and no published number rebases. All four that move are *searching* agents — an all-basic-land
library is a distribution only a rollout is deep enough to notice.

**The rule this cost us:** a puzzle library's filler must be a card **no evaluator term
special-cases**. "Inert in Magic" is not that property, and the gap between the two is silent.

**And the debugging lesson**, which generalizes past this bug: *a term that closes its puzzle on the
isolation column and does nothing on the live agent is a signal to go and read the leaf scores.* The
two obvious hypotheses here — the rollout mixture swamping a static adjustment, and the window
grading below its budget tier — were both wrong, and one insight-sink dump settled it faster than
either could have been argued.

### A related modelling flaw, found on the way — and fixed

`landDropIsNotCardLoss` bought land-drop neutrality by subtracting the earmarked land from the hand
count outright. The transition is then genuinely neutral — `sequencing-02` is the proof — but **both
sides of it are priced as the empty-hand disaster rather than as a resource in hand**. At
`concave-hand-2`'s `-2.0`:

| hand (land drop unused) | held count | card advantage |
|---|---|---|
| `[]` | 0 | **-2.0** |
| `[Forest]` | 0 | **-2.0** |
| `[Grizzly Bears]` | 1 | +1.0 |
| `[Forest, Grizzly Bears]` | 1 | +1.0 |

The earmarked land contributed exactly zero, always. Shortest proof that this is wrong: an opponent
handed the choice of which card to strip would take that land, and the feature priced the Duress at
**zero**.

---

# Lands priced as mana

**Measured:** 2026-08-08. `AiProfile.priceLandsInHandAsMana`, as `production-candidate-manalands`.

The model, in one sentence: **a land on the battlefield is worth more than a land in your hand, a
land in your hand is still worth something, and it is worth more when you are short of mana than
when you are already rich.**

That makes the land drop positive *by construction*, so it **supersedes** `landDropIsNotCardLoss`
rather than stacking with it — the earmark existed only to force neutrality, and nothing needs
forcing once the two zones are priced honestly. The hand curve goes back to pricing **business**;
lands are priced beside it on their own schedule.

The schedule is `Tempo`'s curve read one zone earlier rather than a new guess, and each land in hand
is priced at the count it would actually *arrive* at — the first at today's land count, the second as
if the first had been played. That is what makes a hand of seven lands score as the flood it is
without a second rule about hand contents.

| lands already available | land in hand | same land on the battlefield | drop is |
|---|---|---|---|
| 0-2 | 0.9 | +2.1 (`BoardPresence` 0.9 + `Tempo` 1.2) | +1.2 |
| 3-5 | 0.5 | +1.62 | +1.12 |
| 6+ | 0.2 | +1.14 | +0.94 |

Every rung sits below the field value at the same rung, so playing a land is a gain at every stage;
`CardAdvantageLandDropTest` pins that against `Tempo.landValueAt` directly rather than against
remembered constants.

## What it fixes that the earmark could not

| hand | old (earmark) | new |
|---|---|---|
| `[Forest]` vs `[]` — the Duress | identical | **+0.9** |
| 7 lands vs 7 spells | 8.4 vs 9.2 — within one card | **4.4 vs 9.2** |

The second row is the one to care about: **the AI could not see flood at all.** Past the one
earmarked land the old model counted lands as full cards, so a hand of seven lands scored within a
card of a hand full of business.

## Puzzles — 87 positions

| | `production` | live (`production-candidate-cantrip`) |
|---|---|---|
| baseline | 74/87 | 84/87 |
| with the model | **75/87** | 84/87 |
| closes | `sequencing-02` | — |
| loses | — | — |

The isolation column closing `sequencing-02` is the result that mattered: that is the puzzle the
earmark was built for, and pricing the land honestly closes it *without* the earmark. The model does
everything the mechanism it replaces did. Level on the live agent is expected — that agent already
closes `sequencing-02` the old way.

## The arena half

Unlike the four promotions before it, this one is **expected to move the arena rather than sit at
parity**, and should be read that way. Every other term in the sequence fires on a specific shape — a
pacified creature, a cantrip at an end step, removal aimed at a 1/1 — and the two that returned
degenerate null CIs did so because those shapes are rare. This one changes what *every hand
containing a land* is worth, on every evaluation, inside every rollout. A null result here would
itself be surprising.

---

# Counterspell patience

**Measured:** 2026-08-08. `AiProfile.holdCountersForBetterSpells`, as
`production-candidate-counterpatience`. The counterspell twin of removal patience: `RemovalPatience`
asks whether a *creature* is worth the removal, `CounterPatience` asks whether a *spell on the stack*
is worth the counter.

The target is `respond-02` — a Counterspell spent on a Grizzly Bears while the opponent still holds
cards and five untapped lands. What made it worth doing before anything was built is the margin. The
live agent's own scores, off the insight sink:

| position | opponent's lands | AI's move | advantage over passing |
|---|---|---|---|
| `respond-01` (Serra Angel) | 5/5 **tapped** | counters ✓ | +10.45 |
| **`respond-02`** (Grizzly Bears) | 2 tapped, **5 open** | counters ✗ | **+1.28** |
| `respond-03` (Wrath) | 4/4 tapped | counters ✓ | +19.61 |
| `respond-04` (Murder) | 3/3 tapped | counters ✓ | +15.97 |
| `respond-06` (Wrath, Negate) | 4/4 tapped | Negates ✓ | +14.17 |

The mistake is worth an order of magnitude less than every counter the AI *should* make, and the one
position where it happens is the only one whose caster has mana left. Both facts came out of one
insight dump, and together they say a discount can fix this without endangering anything correct.

## The bar is their open mana

`RemovalPatience` bets a better target arrives on some future turn, which is a bet a constant can
only approximate. A counterspell's bar is sharper and it is visible on the table: **a counter should
answer the best spell they can still cast *this turn*, what they can still cast is bounded by the
mana they have left, and holding costs us nothing — our own mana stays up either way.**

So the bar is `1.4 × their untapped lands`, at the same per-mana rate the removal bar uses, and the
discount is what the spell in front of us falls short of it. A tapped-out caster scores a bar of
zero, which is why the four negative controls above are untouched by construction rather than by the
size of a number. It shares `Patience`'s three releases (lethal on board, hand at the discard limit,
decay to nothing by turn 14) with the removal bar and adds one of its own: an opponent with an empty
hand has nothing better coming.

## Priced by what the spell is, not what it cost

Pricing the countered spell at its mana value would close `respond-02` just as well and would be a
worse model — it would tell the AI to let a two-mana 5/5 resolve, the commonest real position where
countering a cheap spell is right. So the worth comes from `BoardPresence.spellValue`, the same scale
the evaluator prices the battlefield on, read off the spell's printed body or its `CardIntent` prior.

That is also what makes an anthem come out right in both directions, and it is the case that decided
the shape: "creatures you control get +1/+1" cast into an empty board is worth 3.0 and gets let
through; the same card cast by a player with five creatures is worth 4.25 and clears the bar.
`CounterPatienceTest` pins both, on the same card with the same mana open.

Instants and sorceries are **declined outright**, by the reasoning that makes `RemovalPatience`
decline on non-creature permanents: their worth *is* what they do to the board, which the leaf score
already simulates. That answers `respond-03` and `respond-04` before any arithmetic runs.

## Puzzles — 87 positions

| | `production` | live (`production-candidate-cantrip`) |
|---|---|---|
| baseline | 74/87 | 84/87 |
| with the term | 74/87 | **85/87** |
| closes | — | `respond-02` |
| loses | — | — |

The isolation column is quiet **because it cannot be anything else**: `production` already passes
`respond-02` — a greedy agent does not counter there for reasons of its own — so what that column
measures is what the term *costs* across the other 86 positions, and the answer is nothing. Same
shape as `production-patience`'s and `production-pacified`'s columns. The candidate's failing set is
a strict subset of its baseline's: `respond-05` and `timing-03`, both of which need a horizon past
the current resolution rather than a term.

## The arena half

`just arena production-candidate-cantrip production-candidate-counterpatience 100`:

```
Record:       49W-49L-2D for production-candidate-cantrip
Pair win %:   50.0%  CI [50.0%, 50.0%]   <- the merge gate
Completed:    98 / 100     Illegal acts: 0
Wall clock:   1002s on 8 threads
```

The third **degenerate null** in this sequence, and the same reading as the patience and cantrip
promotions: every scored pair came back 1-1-0, which says the term changes the outcome of a real
sealed game *rarely*, not that it is worthless. That is what the mechanism predicts — it fires only
on a turn where the AI holds a counterspell against a spell whose caster still has mana up. A CI
spanning parity is a pass under the standing bar, and the puzzle side is the evidence.

The two unfinished games were a `NoClassDefFoundError` on a test worker's classpath
(`sdk.scripting.RetainUnspentColoredMana`, which exists in source and in `mtg-sdk/build` — a stale
jar, most likely a build race with a parallel agent). Both games were the same pair, so it cancels
out of the comparison. Unrelated to this change, which touches `:ai` only.

**Promoted 2026-08-08** in `EngineAiPlayerController` and `AiProfileSelector`'s fallback.

# The ambush window — flash creatures held for the opponent's turn

Reported from a real game, not found on the suite. Turn 7, the AI's own precombat main, four Plains
untapped and a Restoration Angel in hand — and it jams it. The decision record says why:

```
Cast Restoration Angel   score  3.86   advantage +4.06   <- chosen
Pass priority            score -0.19   baseline
```

That advantage *is* the creature's board value. A one-ply evaluator scores the board right after the
spell resolves, and a 3/4 flier scores the same there whichever window it landed in, so every reason
flash is printed — hold the mana, see their attack, ambush an attacker — is worth exactly zero. The
Angel cannot attack this turn either way. What casting now spends is the whole card's edge.

## Why the removal branch's shape does not transfer

`HoldPolicy` already has a well-argued answer for "wrong window", and it is a **bonus on the good
window**: instant-speed removal is paid at the opponent's end step and charged nothing for being cast
early. That branch's KDoc records that the symmetric penalty was built, measured and removed, because
holding removal is a preference between two futures and the constant big enough to change behaviour
was big enough to veto casting the removal at all.

Neither half of that reasoning reaches this case:

1. **A bonus on the good window cannot fix it.** The removal bonus works because the comparison it
   corrects happens *at* the end step. Here the mistake happens in our own main phase, where the
   comparison is "cast now vs. pass now" and a bonus three steps later is invisible. Removal survives
   the asymmetry because removal held is still removal; a flash creature dumped in main one has
   already spent the thing being paid for.
2. **The claim is provable, not preferential.** Casting a no-haste flash permanent now is dominated
   by casting the identical spell at the next free window *unless the permanent does something in
   between* — and that list is finite and readable off the card. That is exactly the standard
   `TimingVerdict.NoWindow` sets, so the verdict says the honest thing rather than picking a number
   to lose an argument with.

`AmbushWindow` declines the floor on any of the four: printed haste, something on the stack, an ETB
that changes a combat, an ETB that hands us a resource to spend this turn. It inherits `Patience`'s
three releases whole — lethal on board, hand at the discard limit, decay to nothing by turn 14 — and
adds one of its own: once attackers are declared, holding longer buys nothing.

## The tag that lied

The first build of this did nothing at all, and the reason is worth recording. `CardIntentAnalyzer`
tags Restoration Angel **`REMOVAL, EXILE_REMOVAL`** — for blinking a creature *we control*.
`hitsAnotherPermanent` decides "does this take someone else's permanent off the battlefield" from the
`EffectTarget` alone, and a bound target carries no filter, so it falls through to `else -> true`.

Two consequences, and both are in the change:

- The branch had to move **above** `HoldPolicy`'s removal branch, or it was unreachable for the exact
  card it was written for.
- The guard needed a question the tag cannot answer — *whose* permanents does this point at. That is
  `CardIntent.targetsOnlyOurPermanents`, computed from the target requirements' controller predicate,
  and read by exactly one consumer.

Fixing `hitsAnotherPermanent` itself is the real repair and is deliberately **not** here: `REMOVAL`
feeds `staticPriorValue`'s ladder and `RemovalPatience`'s bar, so re-tagging every "target creature
you control" card is an evaluation change owed its own flag and its own arena run. `AmbushWindowTest`
pins the wrong tag on purpose, so that when the repair lands the assertion fails and the workaround
gets deleted instead of quietly living forever.

## Puzzles — 92 positions, five new

| | `production` | live (`production-candidate-counterpatience`) |
|---|---|---|
| baseline | 78/92 | 89/92 |
| with the term | **79/92** | **90/92** |
| closes | `instants-09` | `instants-09` |
| loses | — | — |

The `instants` category goes 10/13 → 11/13 in isolation and 12/13 → **13/13** on the candidate; every
other category is identical in both columns. Unlike the patience and counter-patience terms, the
isolation column *can* close its own puzzle here — `production` already reads the card's flash and
types it `Speed.INSTANT`, it simply had no branch that claimed it.

The candidate's failing set is a strict subset of its baseline's: `respond-05` and `timing-03`, the
same two that need a horizon rather than a term.

Four of the five new positions are controls that pass on `production` already and must keep passing:
`instants-10` (the ambush itself, at their declare-attackers), `-11` (flash *and* haste), `-12` (an
ETB that taps a blocker before we attack) and `-13` (the same board as `-09`, past the patience
horizon). `instants-10` is not a fix and is not counted as one — it is there because a category made
only of "don't cast" positions scores 100% for an agent that never casts anything.

## The arena half

`just arena production-candidate-counterpatience production-candidate-ambush 100`:

```
Pair win %:   50.0%  CI [50.0%, 50.0%]   <- the merge gate
Game score %: 50.0%  Wilson [40.4%, 59.6%]
Completed:    100 / 100     Illegal acts: 0
Avg turns:    22.0   avg actions: 526
Wall clock:   1625s on 8 threads
```

The **fourth degenerate null** in this sequence, and it reads the same way the patience, cantrip and
counter-patience promotions did: every scored pair came back 1-1-0, which says the term changes the
outcome of a real sealed game *rarely*, not that it is worthless. That is what the mechanism
predicts — it fires only on a turn where the AI holds a flash permanent with the ambush window still
ahead, and BLB sealed pools are thin in flash creatures. A CI spanning parity is a pass under the
standing bar, and the puzzle side is the evidence.

Cleaner than the counter-patience run in one respect worth noting: 100/100 completed with 0 illegal
actions, where that one lost two games to a stale-jar `NoClassDefFoundError` on a test worker.
