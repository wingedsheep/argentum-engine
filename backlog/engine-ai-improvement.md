# Engine AI — Structural Improvement Plan

A phased plan to make the in-game engine AI measurably stronger, on a scoreboard we trust, using
mechanisms that generalize across the whole card catalog rather than per-card special cases.

**Status:** **Phases 0–8 shipped**, plus 3 of Phase 2b's 6 categories; **Phase 9 is underway**
(resource-backed evaluation vectors landed on 2026-07-31) —
baselines in [`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md), measurement guide in
[`docs/ai/measurement.md`](../docs/ai/measurement.md). Four scoreboards now exist: the arena
(`just arena`), the 66-puzzle suite (`just arena-puzzles`, **60/66 today**), the multiplayer pod
arena (`just arena-pod`) and the budget-scaling ladder (`just arena-budget-scaling`, **monotone**).
The primary strength lever is in: the rollout evaluator beats `v0` **56.0%, CI [52.0%, 59.7%]**.
Phase 8 removes exact opponent-hand and library-order knowledge from rollout search. Its paired
smoke found no detectable dip against the full-information control (49%, CI [43%, 55%]) and retained
the Phase 7 strength point estimate against `v0` (55%, CI [48%, 62%]); the merge-sized measurement
remains intentionally separate because rollout arenas are expensive. Next in Phase 9 is
**raw-feature collection for Texel-style evaluation tuning**.

**Related:** [`engine-performance.md`](engine-performance.md) — the CPU profile this plan's
performance phase built on. **Every step in that document is now closed**; Phase 5a was its Step 4
and Phase 5c retired its Step 5. See "Cross-reference" below.

> **Phase 0's measurements moved two later phases.** Simulation is ~3,400 `process()`/sec/thread,
> already above Phase 5's 1,500–2,000 target, so **Phase 5 is no longer a gate on Phase 7**. And the
> real branching factor is **1.75 candidates** on the 24% of priority windows that offer any (not the
> ~8 this plan assumed), so **Phase 4a's value is skipping enumeration, not shrinking the candidate
> list**. Details and the corrected budget arithmetic are in the baseline doc.

---

## Why

The engine AI (`ai/src/main/kotlin/com/wingedsheep/ai/engine/`, ~5,575 LOC) is the opponent players
actually face. It should be better, but today we can neither say how good it is nor tell whether a
change helped. Three findings set the shape of the plan.

### 1. It is greedy 1-ply, and the search it appears to have is dead code

> **Resolved in Phase 0** — `Searcher.kt` and the dead `CombatMath` helpers are deleted,
> `CardAdvisor.attackPenalty` is wired in. The Strategist is now *honestly* greedy 1-ply.

`Strategist.chooseAction` scored each candidate with one simulation and picked the max.
`Searcher.kt` (alpha-beta, 315 L) was **unreachable**: `recommendDepth` bailed because
`canRespond(state, opponentId)` opened with

```kotlin
if (state.priorityPlayerId != playerId) return false
```

and the Strategist only ever runs on *our* priority — so `canRespond(…, opponentId)` was always
false and `recommendDepth` always returned 1. `deepSearch`, `opponentPly`, `ourPly` never
executed in production; only two tests reached them.

Latent behind that: the −∞ sentinel was `Double.MIN_VALUE / 2`, which is **`0.0`**
(`Double.MIN_VALUE` is the smallest *positive* subnormal, 4.9E-324). Had the reachability bug been
fixed, the search would have scored **illegal** actions above any position where we're behind
(board scores are signed differentials and routinely negative), and `opponentPly`'s
`if (alpha >= currentBeta) break` would have skipped the whole opponent ply exactly when we're losing.

Also dead: `CardAdvisor.attackPenalty` was never invoked — `CombatAdvisor.advisorRegistry` was a
write-only field. And `CombatMath.calculateAggressionLevel`, `turnsToKill`,
`simulateAttritionalAttack` (~150 LOC) had no production callers.

### 2. The evaluator is blind to most of Magic

- `BoardFeatures.permanentValue` flat-values **every non-creature permanent at `0.5`** regardless of
  text (`BoardFeatures.kt:94-98`) — a signet, an Oblivion Ring and a Bitterblossom are the same
  number. The code comments this as a known limitation.
- `Strategist.heuristicTargetRank` (`:289-310`) has `else -> 0.0`, so an opponent's non-creature
  permanent ranks equal to nothing. **The AI cannot Disenchant correctly at all.**
- ~~All five features open with `state.soleOpponent(playerId) ?: return 0.0`, so in **any multiplayer
  game (FFA, Commander, 2HG) the evaluator returns exactly 0.0 for every candidate** — the AI is
  choosing with no evaluation whatsoever.~~ **Wrong, and fixed in Phase 3.** `soleOpponent` returned
  the *first opponent in turn order*, never null, so the evaluator scored a pod as a duel against one
  arbitrary neighbour — confidently wrong rather than absent. In 2HG it also read a
  `LifeTotalComponent` the engine stops maintaining once life pools on the team. See Phase 3.
- Every weight (`AIPlayer.kt:179-187`) and every constant inside `BoardFeatures.kt` is a hand-guessed
  literal. There is no tuning harness.
- `Tempo` counts lands only — mana rocks, rituals and colour availability are invisible.

### 3. There is no scoreboard

`AIBenchmark` (game-server, 644 L) and `AdvisorBenchmark` (`:ai`) both have the correct paired-swap
methodology, but: no `GameConfig.seed` is ever set, no confidence intervals, no frozen reference
opponent, and `AIBenchmark` is **sequential** (`for (pairId in 1..numGames)`). Nothing aggregates
win rates across agent versions.

### And the card-knowledge approach doesn't scale

Card knowledge is 19 hand-written `CardAdvisor`s covering **42 card names across 2 sets** (BLB, ONS)
out of a ~30-set catalog. Worse, `CardAdvisorRegistry.register` **silently overwrites on collision** —
`Starfall Invocation` and `Wildfire Howl` are registered by both `BoardWipeAdvisor` and
`GiftBoardWipeAdvisor` (`BloomburrowAdvisorModule.kt:30,32`), and the later registration wins, so
those two cards have already lost their "only wipe when behind" logic.

**Intended outcome:** a measurably stronger AI, on a trustworthy scoreboard, built on generalizing
mechanisms.

---

## Decisions taken

- **Time budget:** one global `DecisionBudget` — ~0 ms trivial, ~200 ms routine priority windows,
  **2 s** normal, **5 s** hard cap on critical decisions. Today only combat has a budget at all
  (1000 ms, `CombatAdvisor.kt:181`); everything else is unbounded.
- **Hidden information:** the AI must **play fair**. It currently reads the fully unmasked
  `GameState` — opponent's hand contents and library order. Build a visibility oracle +
  determinization.
- **Primary strength lever:** **rollout evaluation** — keep the heuristic candidate generator,
  replace each candidate's static leaf score with the mean of K short playouts. Not alpha-beta, not
  full MCTS as the first move.

---

## Strategy

Three gates, in order. Skipping one wastes the next.

1. **You cannot know an improvement without a trustworthy scoreboard** → Arena + puzzles first.
2. **You cannot afford rollouts without cheap simulation** → cut branching factor, then raw speed.
3. **A rollout is only as good as its leaf evaluator** → fix evaluation blindness before stacking
   samples on top of it.

Gate 3 is why **CardIntent (Phase 6) is sequenced before rollouts (Phase 7)**, even though rollouts
are the headline lever. Averaging many playouts of an evaluator that cannot see an Oblivion Ring
produces a confident wrong answer.

---

## How we measure

Four independent signals. No single one is trusted alone.

### 1. Arena win rate — the merge gate

Agent A vs agent B, **paired-swap seats** (identical decks, identical game seed, both seat orders),
seeded decks, parallel across cores.

- **Reference opponent is `AiProfile.LEGACY_V0`, permanently** — every version reports against it so
  numbers stay comparable across months.
- Report **pair-level score** (−1 / 0 / +1 per pair, paired bootstrap CI over 2,000 resamples)
  alongside per-game win rate with a **Wilson interval**. Pairing is the estimator; report it as such.

| Purpose | Games | CI at p=0.5 |
|---|---|---|
| Smoke ("did I break it") | 100 (50 pairs) | ±10% |
| Directional | 300 (150 pairs) | ±5.7% |
| **Merge gate** | **1,000 (500 pairs)** | **±3.1%** |
| Publish / Elo | 3,000 | ±1.8% |

At p≈0.5 unpaired, n = (1.96/0.03)² · 0.25 ≈ 1,067 games; pairing on identical decks and seeds buys
15–30% variance, so ~800–900 games is a real ±3% read. For 80% power to detect a true 55% vs 50%,
n ≈ 780. **A change that cannot clear 53% over 1,000 games is not a demonstrated improvement.**

**Wall-clock reality:** 1,000 games × ~200 decisions × 2 s ≈ **111 CPU-hours**. Unusable. So the
arena runs at a **reduced budget (~150 ms)**, sized so 1,000 games fit in ~30–60 min on 10 cores,
with 300-game full-budget runs as a cross-check.

**`ArenaBudgetScalingTest` is the key safety net:** the same agent at 100 / 1000 / 3000 ms.
**If strength is not monotone in budget, the search is generating noise, not signal.** Build this in
Phase 4, *before* rollouts exist — it is the single best early warning for "search made it slower
*and* worse."

**Gauntlet + full pairwise matrix, not just Elo.** MTG agents are frequently non-transitive (an
aggressive agent beats a controlling one that beats a midrange one that beats the aggressive one).
Fit Bradley–Terry/Elo by iterative MM (~40 LOC, no dependency) but **report the matrix**.
Promotion rule: a new version must beat **both** V0 **and** the immediately preceding version, and
must not lose to any gauntlet member worse than 45%.

### 2. Tactical puzzle suite — the localizing signal

Win rate says *that* you regressed; a puzzle says *what*. Runs in ~15 s, CI-gated.
**66 puzzles, 11 categories × 6.** Categories 1–8 built in Phase 2, 9–11 in Phase 2b. The baseline
column is the measured 2026-07-27 figure for `v0`/`production` (2026-07-29 for the Phase 2b
categories), with the zero-weight `v0-blind` control in brackets; **"Today"** is `production` now,
after Phase 6.

| Category | Baseline | Today | What it catches |
|---|---|---|---|
| Lethal detection | 6/6 [6/6] | 6/6 | Missing an alpha strike / burn-to-face kill |
| Blocking | 6/6 [6/6] | 6/6 | Chump vs trade vs no-block; deathtouch / first strike |
| Removal targeting | 6/6 [0/6] | 6/6 | Shooting the 1/1 instead of the bomb (`heuristicTargetRank`) |
| Holding instants | 3/6 [2/6] | 5/6 | Casting a combat trick in your own main phase |
| Sequencing | 5/6 [0/6] | 5/6 | Land before spell; the land that unlocks the spell |
| Board-wipe timing | 6/6 [3/6] | 6/6 | Wrathing while ahead |
| Race math | 5/6 [5/6] | 5/6 | Attack-vs-hold when both players are on a clock |
| Non-creature valuation | 2/6 [0/6] | 5/6 | Ignoring an opposing O-Ring / mana rock / anthem |
| Stack response | 5/6 [1/6] | 5/6 | Never answering a spell that is already on the stack |
| Activated abilities | 5/6 [2/6] | 5/6 | Pingers, tappers and pump abilities left unused |
| Combat keywords | 6/6 [5/6] | 6/6 | Trample / menace / reach / indestructible read as ordinary stats |
| **total** | | **60/66** | `v0-blind`: 30/66 |

Two readings the bracketed column forces. Lethal and blocking are carried entirely by
`CombatAdvisor`'s seed heuristics — the blind agent matches the real one — so they are a regression
net for *that* code, not for `BoardFeatures.kt`. And the plan's "expect ~0%" on non-creature
valuation was close but for the wrong reason: see Phase 2's findings.

**At 44/48 this signal was nearly exhausted, which is why Phase 2b exists.** Two of the four
remaining failures were the same Phase 9 constant, so rollouts had exactly **two** puzzles to move —
and Phase 7's own exit criterion, phrased over sequencing / race math / board-wipe timing, was
capped at **one**. Phase 2b's first three categories took that to **four**: `respond-05` and
`activate-05` both fail for `instants-05`'s reason — paying now for an effect that materializes a
step later — and two of the four are non-combat, so `CombatAdvisor` cannot carry them.

### 3. Latency

p50/p95 decision latency in production, per budget tier. A strength gain that blows the 5 s cap is
not shippable.

### 4. Health

Games completed %, draw-reason histogram, stuck-game detection, and distinct engine exceptions —
`RandomActionBenchmark` already groups exceptions, so the arena is a free crash-finder at scale.

### 5. Pod win share — added in Phase 3

`just arena-pod <table> <a> <b> <games>` over `ffa3` / `ffa4` / `2hg`. One agent against a field of
the other, rotated through every team position so turn order cancels; **the null is 1/teams, not
50%**. This is the only signal that exercises teammates, multiple opponents, and elimination at all,
and it is the cheapest crash finder we have for the engine's least-travelled path. A pod game costs
5–10× a duel, so size runs accordingly.

---

## Phases

### Phase 0 — Instrumentation + honesty fixes · *2–3 d* — ✅ **DONE 2026-07-27**

Produce the numbers everything else is budgeted against, and kill the bugs a rollout engine would
hit thousands of times more often than production does.

**Created** `ai/src/test/.../engine/SimulationThroughputBenchmark.kt` (`just benchmark-throughput`).
It drives real `AIPlayer` games on both seats rather than random actions — branching factor and
projection cost are state-dependent, so the state distribution has to be realistic — and discards
`cores × 2` warmup games, without which the reported rate is measuring the JIT (838 → 1,299 → 2,332
`process()`/sec at 20 → 40 → 200 games, same code).

Reports per thread: `ActionProcessor.process` calls/sec at two action mixes (candidate and
as-played) · `GameSimulator.simulate` calls/sec (incl. `resolveToQuietState`) · mean ns in
`StateProjector.project`, timed cold on `state.copy()` · legal-action count per priority window pre-
and post-filter, **plus the share of windows offering zero candidates** · priority windows per game.

**Fixed, in `:ai`:**
- `CardAdvisorRegistry.register` — now throws on collision. `Starfall Invocation` / `Wildfire Howl`
  were claimed by both `BoardWipeAdvisor` and `GiftBoardWipeAdvisor`, and the later registration
  won, so both cards had already lost their "only wipe when behind" logic. The two advisors are
  merged; `Valley Rally`'s duplicate between `CombatTrickAdvisor` and `GiftCombatTrickAdvisor` is
  resolved in favour of the gift advisor, which delegates its cast timing back.
- `respondBudgetModal` — `while (mode.cost <= remaining)` spun forever on a zero-cost mode.
- `LimitedCardRater.ratingsCache` → `ConcurrentHashMap` + `computeIfAbsent`.
- `CombatAdvisor.advisorRegistry` / `CardAdvisor.attackPenalty` — wired into `evaluateAttackPlan`
  (subtracted from the plan score) and the heuristic seed (a discouraged creature is left out of the
  seed but can still be added back by the local search). The lethal alpha-strike path deliberately
  ignores penalties. No advisor declares one today, so this is behaviour-neutral.
- `Searcher.kt` **deleted** rather than deprecated, along with `Strategist.deepSearch`, its two
  tests, and the ~150 LOC of unreachable `CombatMath` (`calculateAggressionLevel`, `turnsToKill`,
  `simulateAttritionalAttack`, `simulateOneTrade`). The broken sentinels were not repaired — Phase 7
  replaces the mechanism outright, and a repaired-but-untested alpha-beta is worse than none.

**Not done:** `just benchmark-random 200 BLB` was not re-run, so `engine-performance.md`'s
`~404 actions/sec/thread` is still stale. It is one command; do it before Phase 5a.

**Exit:** ✅ [`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md) committed with
throughput, projection share and branching factor — plus the three plan corrections they imply.

---

### Phase 1 — The Arena · *4–6 d* — ✅ **DONE 2026-07-27**

> **Shipped.** Baselines and findings in
> [`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md#phase-1--arena-baselines); how to
> run and read a report in [`docs/ai/measurement.md`](../docs/ai/measurement.md).
>
> Three corrections the build produced:
>
> 1. **The arena is ~100× cheaper than budgeted.** ~5 games/sec on 8 threads, so a 1,000-game
>    merge gate is **3.5 minutes**, not 30–60. The 111-CPU-hour estimate assumed a 2 s
>    `DecisionBudget` that does not exist yet, so **the "reduced ~150 ms budget" mitigation was not
>    needed and was not built.** Phase 4b must re-measure before shipping a budget — the budget, not
>    the game count, is what makes an arena expensive.
> 2. **The v0-vs-v0 exit criterion was weaker than what is actually true.** With the same agent on
>    both seats the two games of a pair are *literally the same game*, so the mirror is **exactly**
>    50% with CI `[0.000, 0.000]`. That is asserted in the always-on suite rather than checked by
>    hand. A control run (`v0` vs a zero-weight `v0-blind`) proves the harness discriminates: 200-0.
> 3. **The BLB advisors are measurably neutral** — 1,000 paired games, 50.0%, CI [49.3%, 50.8%].
>    `AdvisorBenchmark`'s 46.1% on the same game count is the *unpaired, unseeded* view of the same
>    conclusion; the arena's paired interval is **4× tighter**. This lowers the retirement bar for
>    the 42 advisor entries in Phase 6.
>
> Also found, and **not fixed** (an AI/enumerator bug, not scoreboard work): the AI proposes an
> illegal action ~0.9 times per game, 889 of 945 being `CastSpell: No valid targets available`, at
> ~3× the rate when a `CardAdvisor` is in play. Quantified in the baseline doc.
>
> **Deferred:** `arena-puzzles` ships with the puzzles in Phase 2 — a recipe pointing at a test
> that does not exist is worse than no recipe. `ArenaBudgetScalingTest` stays in Phase 4b as
> planned; there is no budget to scale yet.

**Lives in `ai/src/test/kotlin/com/wingedsheep/ai/arena/`.** `:ai` already declares
`testImplementation(testFixtures(project(":rules-engine")))` and `testImplementation(project(":mtg-sets"))`,
and `just benchmark` already targets `:ai:test` — zero new build wiring. Not `gym-trainer`: its
`GameEnvironment.playGame` no-agent fallback is `actions.first { it.affordable }`
(`GameEnvironment.kt:314`), a near-random bot, despite its KDoc claiming the built-in AI.

Files: `ArenaAgent.kt`, `Arena.kt`, `ArenaGameRunner.kt`, `ArenaStats.kt`, `ArenaReport.kt`,
`ArenaBenchmark.kt`.

**Reuse `AdvisorBenchmark.playAdvisorGame` (`ai/src/test/.../AdvisorBenchmark.kt:162`) as the game
loop — not `AIBenchmark.playGame`**, which round-trips through `ClientStateTransformer.transform` +
`LegalActionEnricher.enrich` on every action to satisfy the `AiPlayerController` DTO interface. For
engine-vs-engine that is pure overhead. Take `AIBenchmark`'s stuck-detector
(`actionCount - lastProgressAction > 300 && turns == lastProgressTurn`) and draw taxonomy, and
`GameBenchmark`'s `ExecutorCompletionService` pool — submit **pairs** as units so a pair never
straddles a partial run.

**Create `ai/src/main/kotlin/com/wingedsheep/ai/engine/AiProfile.kt`** — the versioning seam and the
switchboard for every feature this plan adds (`candidateEvaluator`, `rolloutsPerCandidate`,
`determinizations`, `evalWeightsId`, `useMeaningfulFilter`, `useCardIntent`, budget overrides).
`AiProfile.LEGACY_V0` pins everything off and reproduces today's AI by construction.
`AIPlayer.create(registry, playerId, profile)` becomes the single constructor; the existing 3-arg
overload delegates with `AiProfile.CURRENT`.

**Guard against baseline drift:** `FrozenBaselineTest` runs one fixed-seed, fixed-deck game with
`LEGACY_V0` on both seats and asserts the action stream hashes to a golden constant. If a refactor
moves the baseline you find out immediately. Far cheaper and more honest than copying 5,575 LOC into
a `frozen/v0/` package.

**Seeding gaps to close:**
- Set `GameConfig.seed` explicitly — `GameConfig.seed: Long?` exists (`GameInitializer.kt:91`) and
  `InitializationResult.seed` records the drawn value, but no benchmark sets it.
- Add a `random: Random` param to `ConstructedDeckGenerator` (`:65` constructs `RandomDeckGenerator`
  without one). `RandomDeckGenerator.kt:21` is already seedable.
- `skipMulligans = true` for determinism — note this puts mulligan quality **out of test**; schedule
  a separate mulligan A/B later.

**justfile:** `arena A B GAMES="300"`, `arena-gauntlet GAMES="200"`. (`arena-puzzles` moved to
Phase 2, with the puzzles it would run.) Gauntlet membership in
`ai/src/test/resources/arena/gauntlet.json`. Results go to `benchmarks/arena/<timestamp>-<a>-vs-<b>/`
(results.csv + summary.md) — **gitignored**; there was no committed `benchmarks/ai-benchmark-*`
convention at the repo root, that claim was wrong.

**Exit:** ✅ `just arena v0 v0 300` returns exactly 50%, CI `[50.0%, 50.0%]` — a seat/seed-leak
detector, asserted in the always-on suite rather than eyeballed. ✅ `just arena v0 blb-advisors 1000`
returns 50.0% CI [49.3%, 50.8%] against `AdvisorBenchmark`'s unpaired 46.1% — the same conclusion
(advisors are not an improvement) at 4× the precision.

---

### Phase 2 — Tactical puzzle suite · *3–4 d* — ✅ **DONE 2026-07-27**

> **Shipped.** 48 puzzles, 8 categories × 6, in
> `ai/src/test/kotlin/com/wingedsheep/ai/puzzles/`. `just arena-puzzles` is the gate (~15 s);
> `just arena-puzzles-compare` runs the same suite across `v0` / `production` / `v0-blind`.
> Per-category baseline and findings:
> [`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md#phase-2--puzzle-baselines); how to
> write one: [`docs/ai/measurement.md`](../docs/ai/measurement.md#the-puzzle-suite).
>
> **Baseline: 39/48 (81%).** `v0` and `production` score identically, category for category — the
> same "advisors are neutral" conclusion Phase 1's arena reached, from an independent measurement.
> `v0-blind` scores 22/48, which is the suite proving it discriminates; that gap is asserted in the
> always-on suite rather than eyeballed.
>
> Four corrections the build produced:
>
> 1. **Non-creature blindness is a *casting* failure before it is a targeting one.** The plan
>    predicted ~0% and blamed `heuristicTargetRank`'s `else -> 0.0`. In all four failures the AI
>    never casts the Disenchant at all: destroying an artifact is worth `permanentValue`'s flat
>    `0.5` (+0.75 weighted) and costs a card (−1.5 weighted), so passing wins. **Phase 6's
>    `staticPriorValue` has to clear the card-advantage cost of casting, not merely outrank a
>    sibling target.** The two that pass are the two whose effect shows up in *creature* stats
>    (an anthem on three bodies; Disenchanting a Pacifism off a 6/4) — so the deficit is exactly
>    "permanents whose value is invisible in someone's P/T".
> 2. **`CardAdvantage.cardValue(0) = -3.0` means the last card in hand is never played.**
>    `sequencing-02` and `-04` are the same land drop one card apart; 04 passes, 02 fails. Land
>    drops are free. One more hand-drawn constant for Phase 9.
> 3. **A one-ply evaluator cannot see prevention.** Fog at 2 life facing lethal is passed up,
>    because the post-simulation state has the same life totals as passing — the prevention only
>    materializes at the damage step. It also means "hold Fog in your own main" passes for the
>    wrong reason. A Phase 7 puzzle, not a Phase 9 one.
> 4. **Combat is carried by `CombatAdvisor`, not the evaluator.** `v0-blind` still scores 6/6 on
>    lethal *and* blocking. Those two categories are a regression net for `CombatAdvisor`'s seed
>    heuristics; a `BoardFeatures.kt` change will not move them. The one combat position the
>    evaluator owns — hold a blocker home rather than attack with everything — fails.

Authored against **`ScenarioTestBase`** (`rules-engine/src/testFixtures/.../support/ScenarioTestBase.kt`,
1562 L, already on `:ai`'s test classpath). Not scenario JSON — `manual-scenarios/` +
`DevScenarioController` exist to drive the *server* for manual UI inspection, and consuming that from
`:ai` would drag in game-server. The `.claude/skills/generate-scenario` skill can bootstrap authoring,
but the committed artifact is Kotlin.

Files: `ai/src/test/.../puzzles/{AiPuzzle,PuzzleRunner,PuzzleSuiteTest}.kt` + `categories/*.kt`.

- **Assert a predicate over the chosen action**, never exact equality — *"removal targets the 4/4
  flier, not the 1/1"*, *"attacks with at least the 3 evasive creatures"*. Exact-action assertions
  break on harmless tie-break changes and train you to ignore the suite.
- `PuzzleSuiteTest` asserts the failing-id set **equals** a committed `KNOWN_FAILURES` set. Flags
  regressions *and* unexpected fixes; keeps CI green without hiding anything.
- `PuzzleReport` (benchmark-gated) prints pass rate overall and per category — the number quoted
  alongside arena win rate.

**Exit:** ✅ 48 puzzles committed, ✅ per-category baseline in `docs/ai/baseline-metrics.md`,
✅ `just arena-puzzles` runs in ~15 s.

---

### Phase 2b — Puzzle suite, second pass · *4–6 d* — 🟡 **3 of 6 categories shipped 2026-07-29**

> **Shipped: `respond`, `activate`, `keywords` — the three that needed no framework change.**
> 18 positions in `ai/src/test/.../puzzles/categories/{StackResponse,ActivatedAbility,CombatKeyword}Puzzles.kt`,
> plus `advanceToStackResponse` in `PuzzlePositions.kt` and `PuzzleMove.shouldActivate`.
>
> **Suite 48 → 66. `production` scores 60/66 (91%)**, `v0-blind` 30/66 (45%) — the discrimination
> control holds at the new size, which was the exit criterion that mattered. The AI solves **16 of
> the 18** new positions.
>
> | Category | `production` | `v0-blind` |
> |---|---|---|
> | `respond` | 5/6 | 1/6 |
> | `activate` | 5/6 | 2/6 |
> | `keywords` | 6/6 | 5/6 |
>
> Three things the build changed about the plan:
>
> 1. **A latent harness bug was hiding behind the fact that no old puzzle had twin blockers.**
>    `PuzzleMove.blockAssignments` was a `Map<String, List<String>>` keyed by **card name**, so two
>    Grizzly Bears gang-blocking one attacker collapsed into a single entry and
>    `shouldBlockWithAtLeast` counted **1**. `keywords-03` (menace) reported a failure the AI had
>    not made — it finds the double block correctly. It is now a `List<Pair<…>>`. The engine's
>    menace validation (`BlockPhaseManager.validateMenaceRequirements:504`) was never at fault, and
>    `PuzzleRunner`'s legality gate had already proved as much by not rejecting the move — a report
>    of an illegal block that the engine accepts is a harness bug, every time.
> 2. **`keywords` discriminates barely at all — 6/6 against 5/6 blind — and that is the same
>    finding Phase 2 recorded about `lethal` and `blocking`.** Combat is carried by
>    `CombatAdvisor`'s seed heuristics, not by `BoardFeatures.kt`, so trample / menace / reach are
>    a regression net for *that* code. Only `keywords-06` (Murder vs an indestructible creature)
>    sits with the evaluator, and it passes — `chooseCommittedTargets`' simulation refinement does
>    overrule `heuristicTargetRank`'s +3.0 indestructible bonus in time.
> 3. **Both new failures are the same shape, and it is `instants-05`'s shape.** `respond-05`
>    (regenerate through a Wrath) and `activate-05` (firebreathe an unblocked attacker) both pay
>    mana *now* for an effect that only materializes at a later step, so the post-simulation state
>    is strictly worse than passing. **That was the point of building these before Phase 7:** the
>    rollout evaluator now has a four-puzzle signal on "cannot see past the current step" —
>    `instants-05`, `race-03`, `respond-05`, `activate-05` — where it had two, and two of the four
>    are non-combat, so `CombatAdvisor` cannot carry them.
>
> **Still to build:** `walker` (needs `ScenarioBuilder.withCountersOn` — a planeswalker placed by
> `withCardOnBattlefield` enters at **0 loyalty** and dies to state-based actions immediately),
> `lines` (needs `AiLinePuzzle`), `decisions` (needs `AiDecisionPuzzle`), `pod` (needs an N-seat
> `withPlayers`). Everything below is the plan as written.

**Do this before Phase 7, not after.** At 44/48 the suite has four points left, and *two* of them
(`sequencing-02`, `noncreature-02`) are the same Phase 9 constant — `CardAdvantage.cardValue(0) =
−3.0` — which no amount of rollout will move. So Phase 7's entire puzzle-side signal is
**`instants-05` and `race-03`: two positions**. Its stated exit criterion, "puzzle gains in
sequencing / race math / board-wipe timing", is arithmetically capped at **one** puzzle — those
categories sit at 5/6, 5/6 and 6/6, and the one sequencing miss is the Phase 9 constant. A lever
this expensive deserves more than a one-bit localizing signal.

> **It was done before Phase 7, and it earned it.** The three shipped categories gave Phase 7 a
> signal the 48 could not: the rollout closes `instants-05` and *loses* `respond-02` — a horizon
> effect nobody predicted — for a net 55/66, exactly `v0`'s score. On the old suite the same agent
> read as a clean +1. Both readings are true; only the second is useful.

The suite also can't see whole regions of the AI. Five structural gaps, each of which the 48
positions share by construction rather than by choice:

| # | Gap | Consequence |
|---|---|---|
| **G1** | Every puzzle probes `chooseAction` at a **clean priority window** | All 18 `PendingDecision` branches in `DecisionResponder.kt` (33 KB) are **completely unmeasured** — discard, tutor, damage assignment, modal choice, distribute, block ordering, mana-source selection |
| **G2** | Every puzzle scores **one action** | A *line* is inexpressible. "Kill the blocker, then attack" is two actions. This is the shape that would actually measure a rollout evaluator, because seeing past the current action is its whole claim |
| **G3** | Every puzzle is **1v1** | Phase 3's headline bug was "the AI systematically attacked the wrong player" in a pod, and pod win-share is the only thing that can see it — a signal that says *that*, never *what*. `withPlayers()` is hardcoded to two seats |
| **G4** | Nothing is ever **on the stack** | The AI is never asked to respond. Phase 4b deliberately shipped without the "real counterspell window" CRITICAL trigger; there is no puzzle that would tell us whether adding it helped |
| **G5** | No puzzle asserts on an **`ActivateAbility`** | `PuzzleMove.playedCard` already handles it (`AiPuzzle.kt:86`) and zero puzzles use it. Pingers, tappers, firebreathing, regeneration, loyalty abilities — all unmeasured |

Content-wise the 48 are also narrow: five permanents at most, no counters, no damage marked, one
aura, and combat keywords stop at flying / deathtouch / first strike / vigilance.

**36 new positions across six categories**, taking the suite to **84**. Three land against today's
framework; three need the extensions in "Framework work" below and are the reason this is 4–6 days
rather than 2.

#### 9. `STACK_RESPONSE` — "respond" · *no framework change*

The opponent has cast something and the AI holds priority. Closes G4.

| id | Position | Expect | What it catches |
|---|---|---|---|
| `respond-01` | P2 casts **Serra Angel**; AI has 2 Islands + **Counterspell** | Counter it | Does the AI respond at all |
| `respond-02` | P2 (7 lands, full grip) casts **Grizzly Bears**; AI has 2 Islands + **Counterspell** | Pass | Negative control — a category of "counter it" scores 100% for an agent that counters everything |
| `respond-03` | AI has three creatures; P2 casts **Wrath of God** | Counter it | The counter's value is *board*, not cards — the position where countering is most clearly right |
| `respond-04` | AI controls **Serra Angel**; P2 casts **Murder** targeting it | Counter it | Reading the **target of a spell on the stack**. Nothing in `:ai` does this today |
| `respond-05` | AI controls **Troll Ascetic** + 2 Forests; P2 casts **Wrath of God** | Activate `{1}{G}` regenerate | Regeneration as a *response*; also G5 |
| `respond-06` | P2 casts **Wrath of God**; AI holds **Negate** *and* **Essence Scatter**, 2 lands | Cast Negate | Picks the counter that is legal here rather than passing |

*Position helper:* `advanceToStackResponse(seat)` in `PuzzlePositions.kt` — `withActivePlayer(2)`,
`castSpell(2, …)`, then pass until the AI seat holds priority with a non-empty stack. Both halves
exist (`ScenarioTestBase.castSpell:525`, `passPriority:892`); this is ~15 lines alongside
`advanceToDeclaration` / `advanceToPriority`.

*Prediction:* `respond-01/03/04` plausible, `respond-02` likely passes for the wrong reason (the AI
rarely casts anything at instant speed on an opponent's turn), `respond-05` fails — nothing models a
regeneration shield.

#### 10. `ACTIVATED_ABILITIES` — "activate" · *no framework change*

Closes G5. Every card here is already implemented and verified: `Prodigal Sorcerer` (`{T}`: 1 damage
to any target), `Icy Manipulator` (`{1},{T}`: tap target artifact/creature/land), `Royal Assassin`
(`{T}`: destroy target tapped creature), `Shivan Dragon` (`{R}`: +1/+0).

| id | Position | Expect | What it catches |
|---|---|---|---|
| `activate-01` | AI: **Prodigal Sorcerer**. P2: **Llanowar Elves** + **Hill Giant** | Ping the Elves | Ability targeting through `heuristicTargetRank` — a path only `CastSpell` reaches today |
| `activate-02` | P2 at **1 life**, empty boards, AI has **Prodigal Sorcerer** | Ping their face | Lethal **through an ability**. The lethal category is attacks and burn spells only. Direct probe of the Phase 3 finding that a player target always ranks −5.0 |
| `activate-03` | AI: **Craw Wurm** + **Icy Manipulator** + 1 untapped land. P2: **Wall of Stone** (0/8) | Tap the Wall | Spend a resource *now* to unlock combat *later* — the one-ply blind spot in miniature |
| `activate-04` | AI: **Prodigal Sorcerer**. P2: **Hill Giant** only, both at 20 | Don't point it at the Giant | Negative control on toughness: 1 damage to a 3/3 does nothing |
| `activate-05` | **Shivan Dragon** unblocked, P2 at 7, two Mountains untapped | Activate firebreathing | Converting floating mana into damage. *Note:* only the first pump is visible to a single-action check — the honest version of this is `line-05` |
| `activate-06` | P2 attacked with **Craw Wurm** (now tapped) and kept **Hill Giant** home; AI has **Royal Assassin** | Assassinate the Wurm | The ability is only live against a *tapped* creature, and the window is mid-combat |

#### 11. `COMBAT_KEYWORDS` — "keywords" · *no framework change*

Combat past the four keywords the blocking category covers. Each position is built so the naive
reading picks the wrong side.

| id | Position | Expect | What it catches |
|---|---|---|---|
| `keywords-01` | **Fangren Hunter** (4/4 trample) attacks; AI at 12 with **Llanowar Elves** | No block | Chumping a trampler buys one point of life for a whole creature |
| `keywords-02` | Same attacker; AI has **Wall of Stone** (0/8 defender) | Block | The mirror: 8 toughness eats all 4, *nothing* tramples over. A blanket "don't block tramplers" rule fails this |
| `keywords-03` | **Goblin Trailblazer** (2/1 menace) attacks; AI at **2 life** with two **Grizzly Bears** | Gang-block with both | Menace makes a single block *illegal* — either find the double block or die |
| `keywords-04` | **Wind Drake** + **Trained Armodon** attack; AI at 8 with **Giant Spider** (2/4 reach) | Block the Drake | Reach can block a flier at all, and the Drake is the one the Spider eats for free |
| `keywords-05` | **Ambush Viper** (2/1 deathtouch) attacks; AI at 18 with **Serra Angel** | No block | Deathtouch means blocking trades a bomb for a 2/1 |
| `keywords-06` | AI holds **Murder**, 3 Swamps. P2: **Zetalpa, Primal Dawn** (indestructible) + **Craw Wurm** | Kill the Wurm | *Legal but useless.* `creatureValue` pays **+3.0 for indestructible** plus flying/double-strike/trample/vigilance, so Zetalpa is by far the highest-ranked target — the evaluator's own keyword table aims the spell at the creature it cannot kill |

*Prediction:* `keywords-06` is the interesting one. `heuristicTargetRank` will point at Zetalpa; the
question the puzzle answers is whether `chooseCommittedTargets`' simulation refinement is consulted
in time to overrule it.

#### 12. `PLANESWALKERS` — "walker" · *needs `withCountersOn`*

`permanentValue` prices a walker at `max(4.0, prior + loyalty × 0.8)` (`BoardFeatures.kt:143-146`) —
Phase 6 built it and **nothing measures it**. 25 planeswalkers are implemented; these use
`Ajani, Caller of the Pride` (loyalty 4), `Vivien Reid` (5) and `Karn, Scion of Urza` (5).

| id | Position | Expect | What it catches |
|---|---|---|---|
| `walker-01` | P2 at 20 with **Ajani** at 4 loyalty, no blockers; AI has **Hill Giant** | Attack Ajani | Is a walker a target at all |
| `walker-02` | P2 at **3** with **Karn** at 5, no blockers; AI has two **Hill Giants** | All at the face | Lethal beats value — the negative control |
| `walker-03` | AI controls **Ajani**; P2 attacks it with **Grizzly Bears**; AI has **Hill Giant** untapped | Block | Our own walker is worth defending |
| `walker-04` | AI controls **Vivien Reid**, precombat main, empty board | Activate a loyalty ability | Does the AI *ever* use a walker |
| `walker-05` | AI controls **Vivien Reid**; P2 controls **Air Elemental** | Activate the **−3** (destroy target creature with flying), not the +1 | Ability selection *inside* a walker — the closest thing to modal reasoning the action layer has |
| `walker-06` | P2 has **Karn** at 5 and **Grizzly Bears**; AI has **Serra Angel** (vigilance) | Attack Karn | Vigilance makes the attack free, so the only question left is the valuation |

*Framework:* `withCardOnBattlefield` adds no `CountersComponent`, so a planeswalker placed directly
enters at **0 loyalty and dies to state-based actions immediately**. Tests that need counters
hand-roll them today (`MilesMoralesScenarioTest.kt:41`). Add
`ScenarioBuilder.withCountersOn(name, type, count)` — useful well beyond this suite. `PuzzleMove`
also needs `attackTargets: Map<String, String>` (attacker → defender name); `DeclareAttackers`
already carries the defender and `attackerNames` throws it away (`AiPuzzle.kt:105`).

#### 13. `LINES` — multi-action · *needs `AiLinePuzzle`* · **this is the Phase 7 signal**

G2, and the reason to build all of this before Phase 7 rather than after. A line puzzle lets the AI
act until the turn ends and asserts over the **resulting state**, not the first move.

| id | Position | Assert |
|---|---|---|
| `line-01` | AI: **Craw Wurm**, 3 Swamps, **Murder**. P2 at 6 with **Wall of Stone** | P2 is dead. (Murder the Wall → attack for 6. A single-action check can only ask "did you cast Murder") |
| `line-02` | `sequencing-05`'s board — **Glorious Anthem** + two Bears, P2 at 6 | P2 is dead |
| `line-03` | `activate-03`'s board, P2 at 6 | P2 is dead — Icy the Wall, then swing |
| `line-04` | `sequencing-01`'s board — 3 Mountains, a Mountain and a **Hill Giant** in hand | The Giant is on the battlefield *and* the land was played |
| `line-05` | `activate-05`'s board — **Shivan Dragon**, P2 at 7, two Mountains | P2 is dead — both pumps, not one |
| `line-06` | AI: **Grizzly Bears**, 1 Forest, **Giant Growth**. P2: **Hill Giant** | Attacked *and* the Growth was spent on the block, not precombat |

`AiLinePuzzle` is a loop: `while (state.priorityPlayerId == aiId && !turnEnded) { process(ai.chooseAction(state)) }`
with the same 200-iteration guard `advanceUntil` already uses, then a predicate over the final
`GameState`. ~40 lines in `PuzzleRunner.kt`. **Every line here is currently unreachable by the
suite, and every one of them is exactly what a rollout evaluator claims to find.**

#### 14. `DECISIONS` — · *needs `AiDecisionPuzzle`* · **the largest unmeasured surface**

G1. Build the position, drive it to a `pendingDecision`, call the AI's `DecisionResponder`, assert
over the `DecisionResponse`. `PuzzleRunner` currently *rejects* any position with a pending decision
(`PuzzleRunner.kt:50`) — correctly, for action puzzles; this is the sibling runner.

| id | Decision | Position | Expect |
|---|---|---|---|
| `decision-01` | `SelectCardsDecision` | P2 resolves **Mind Rot**; AI hand is **Murder**, **Craw Wurm**, one land, with 2 lands in play | Pitch the uncastable Wurm, keep the removal |
| `decision-02` | `SearchLibraryDecision` | **Diabolic Tutor** resolving; library has **Wrath of God** and **Serra Angel**; P2 has three creatures | Fetch the Wrath |
| `decision-03` | `SearchLibraryDecision` | Same, but *we* have the board and P2 is empty | Fetch the Angel — the mirror |
| `decision-04` | `AssignDamageDecision` | **Fangren Hunter** (4/4 trample) blocked by **Grizzly Bears** | 2 to the blocker, 2 through. `respondDamageAssignment` is *eight lines* that forward `decision.defaultAssignments` unexamined (`DecisionResponder.kt:475`) |
| `decision-05` | `AssignDamageDecision` | 4/4 double-blocked by a 2/2 and a 3/3 | All 4 into the 3/3 — killing one beats splitting and killing neither |
| `decision-06` | `SelectManaSourcesDecision` | AI holds **Counterspell**, casts a 2-drop, controls **Island** + 2 **Plains** | Pay with Plains. `respondManaSelection` unconditionally auto-pays whenever the solver has a suggestion (`DecisionResponder.kt:523`) |

*Caveat on `decision-06`:* verify the prompt actually fires — the engine may auto-resolve the
payment without ever asking. If it does, the defect is `ManaSolver`'s source choice rather than the
AI's, the puzzle belongs to the engine suite instead, and **that is a finding worth recording**, not
a reason to drop the position.

#### 15. `POD` — multiplayer tactics · *needs N-seat `withPlayers`* · *optional, ship last*

G3. The pod arena reports win share and nothing else; these localize it. Six positions at `ffa3`
and `2hg`:

- **`pod-01`** — two opponents, one tapped out and one with blockers: attack the one who can't block.
- **`pod-02`** — the runaway leader is *across* the table, not next in turn order: removal points at
  them. This is the exact shape of the pre-Phase-3 bug; it should pass now, which makes it a
  regression net for `OpponentAggregate.THREAT`.
- **`pod-03`** — 2HG: block an attacker that is attacking our **teammate** (CR 810 — teammates
  defend as one side).
- **`pod-04`** — 2HG: **don't Wrath away our teammate's board.** `BoardPresence` folds teammates into
  `sides.mine`, so a sweeper killing three of theirs and three of ours must read as neutral.
- **`pod-05`** — one opponent at 4 and one at 20: swing at the one we can kill.
- **`pod-06`** — don't spend removal on the player who is already dead to someone else's board.

*Framework:* `withPlayers` builds exactly two seats (`ScenarioTestBase.kt:98`). An N-seat overload
plus a team assignment is the prerequisite; treat it as the expensive item and ship this category
after the other five if it is.

#### Framework work, itemized

| Item | Where | Size | Unlocks |
|---|---|---|---|
| `advanceToStackResponse(seat)` | `puzzles/PuzzlePositions.kt` | ~15 L | category 9 |
| `PuzzleMove.attackTargets` | `puzzles/AiPuzzle.kt:105` | ~5 L | `walker-01/02/06` |
| `ScenarioBuilder.withCountersOn` | `ScenarioTestBase.kt` | ~15 L | category 12; generally useful |
| `AiLinePuzzle` + runner | `puzzles/PuzzleRunner.kt` | ~40 L | category 13 |
| `AiDecisionPuzzle` + runner | `puzzles/PuzzleRunner.kt` | ~50 L | category 14 |
| N-seat `withPlayers` | `ScenarioTestBase.kt:98` | ~60 L | category 15 |

`PuzzleSuiteTest`'s shape invariants change with this: `byCategory(…).size shouldBe 6` becomes
`shouldBeGreaterThanOrEqual(6)` and `all.size shouldBe 48` is re-pinned. Keep the **`KNOWN_FAILURES`
set-equality gate exactly as it is** — it is the reason the suite stays green without hiding
anything, and 36 new positions will arrive with a large known-failure set that is the *point*, not a
problem.

**Exit:** 84 puzzles committed; a per-category baseline for the six new categories appended to
`docs/ai/baseline-metrics.md`; `just arena-puzzles` still under ~60 s (the line and decision runners
each play out more than one action, so this is not free); `v0-blind` still scores strictly lower on
the expanded suite — the discrimination control has to hold at the new size or the new positions are
coin flips.

---

### Phase 3 — Multiplayer evaluation · *1–2 d* — ✅ **DONE 2026-07-28**

> **Shipped.** `AiOpponent.kt` is now `Sides.kt`: `state.sidesFor(playerId)` returns the AI's own
> side (itself plus still-in teammates) and one entry per opposing team, and every feature folds a
> per-opponent score over it with `OpponentAggregate.THREAT` (board presence, threat assessment) or
> `.FIELD` (life, cards, tempo). Pod scoreboard: `just arena-pod <table> <a> <b> <games>` over
> `ffa3` / `ffa4` / `2hg`. Numbers and findings:
> [`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md#phase-3--multiplayer-baselines);
> how to read a pod report: [`docs/ai/measurement.md`](../docs/ai/measurement.md#the-pod-arena).
>
> **Baseline: `v0` beats a field of `v0-blind` 100% at all three tables** (150 / 120 / 120 games)
> against nulls of 33.3% / 25% / 50%. `production` vs a field of `v0` at `ffa3` is 31.7%,
> CI [29.3%, 33.7%] — **the card advisors are neutral in a pod too**, a third independent
> measurement agreeing with Phase 1's arena and Phase 2's puzzles.
>
> Four corrections the build produced:
>
> 1. **The diagnosis below was wrong, in the direction that made the bug sound smaller.**
>    `soleOpponent` was `getOpponents(playerId).firstOrNull()`, and `getOpponents` returns *every*
>    opponent — so it returned the **first opponent in turn order**, not null, and the evaluator did
>    not return 0.0. It scored a pod as a duel against one arbitrary neighbour. That is worse than
>    "no evaluation" in the way that matters: the runaway leader across the table was invisible, so
>    removal aimed at them scored 0.0 while the same spell aimed at the first opponent scored
>    normally, and the AI systematically attacked the wrong player.
> 2. **Two-Headed Giant was failing three ways at once, and the sharpest one is a stale component.**
>    A 2HG team's life lives on the team's canonical owner (`GameState.teamLifeOwnerOf`); the other
>    member's own `LifeTotalComponent` is never written again after setup. `LifeDifferential` read it
>    directly, so for half the table the life differential was **frozen at the starting 30 for the
>    whole game**. Everything now reads `state.lifeTotal()` and values a side per *life pool*.
> 3. **`GameState.turnNumber` stopped advancing after the first elimination.**
>    `TurnManager.startTurn` only incremented it for `turnOrder.first()`, and `turnOrder` keeps
>    eliminated players — so a pod played on for twenty more turns at "turn 16". The arena's wedge
>    detector and length cap both keyed on it and declared every healthy three-way endgame stuck.
>    **Fixed in the engine since**: `turnNumber` now counts player turns, so it advances on every
>    turn at any table size and the harness reads it directly again. The same freeze had reached
>    delayed triggers and every other `turnNumber + 1` reading of "next turn" — see
>    `backlog/multiplayer.md` for that half.
> 4. **No `AiProfile` flag, deliberately.** In 1v1 the new code is bit-identical by construction (one
>    opposing side of one player, short-circuited before the fold), which `FrozenBaselineTest` and the
>    unchanged 39/48 puzzle score both confirm. In multiplayer the old behaviour was a bug, not a
>    strategy — preserving it behind a switch so it can be A/B'd against itself buys a number and
>    costs a permanent dual path through `:ai`, `:gym` and `:gym-trainer`. The pod arena's control is
>    `v0-blind`, the same one the other two scoreboards use.
>
> Also found, and **not fixed** (a 1v1 bug, and Phase 6 owns the function): `heuristicTargetRank`
> derives `isOpponent` from `projected.getController(entityId)`, but `ProjectedState` only covers
> battlefield entities — so `getController` on a *player* returns null and an opponent player always
> ranks **−5.0**, exactly as our own face does. Quantified in the baseline doc.

**The original diagnosis, kept for the record:** all five features in `BoardFeatures.kt` open with
`state.soleOpponent(playerId) ?: return 0.0`. In FFA, Commander or 2HG the evaluator returns 0.0 for
every candidate — no evaluation at all. (`AiOpponent.kt:6-15` documents this as deliberate:
multiplayer pods launch without AI seats. That is a workaround, not a fix.)

Generalize to `opponentsOf(playerId)`: differentials become "me vs. strongest opponent"
(threat-focused) or "me vs. mean" (positional); 2HG treats a team as one entity via `teammatesOf`.
Add multiplayer games to the arena to verify.

**Exit:** ✅ every feature folds over all opposing sides, ✅ `just arena-pod` over three tables with
an exact-mirror / clean-game / discrimination harness in the always-on suite, ✅ pod baselines in
`docs/ai/baseline-metrics.md`, ✅ 1v1 provably unchanged (`FrozenBaselineTest` green, puzzles 39/48).

> **Sizing a pod run.** A pod game is 5–10× the wall clock of a duel — three agents deciding instead
> of two, over boards that keep growing because nobody is closing the game out. 300 pod games is
> ~10 minutes where 300 duels is ~70 seconds, so the head-to-head merge-gate table does not transfer.

---

### Phase 4 — Branching factor + budget · *5–8 d* — ✅ **DONE 2026-07-28**

> **Shipped.** `rules-engine/.../legalactions/MeaningfulActionFilter.kt` owns the auto-pass rules;
> `AutoPassManager` adapts the DTO and delegates. `ai/.../engine/budget/{DecisionBudget,BudgetPolicy}.kt`
> carries the four tiers, threaded through `AIPlayer` → `Strategist` → `CombatAdvisor` →
> `DecisionResponder` and switched by `AiProfile.useMeaningfulFilter` / `.budgetPolicy`. New
> scoreboard: `just arena-budget-scaling`. Numbers and findings:
> [`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md#phase-4--branching-factor--budget);
> how to read the ladder: [`docs/ai/measurement.md`](../docs/ai/measurement.md#the-budget-scaling-ladder).
>
> **Baselines: `v0-meaningful` 51.3% CI [49.8%, 52.7%] and `v0-phase4` 50.8% CI [49.4%, 52.2%]**
> over 1,000 paired games each — neutral, which is the result enabling infrastructure should
> produce. The exit criterion was ≥50% precisely because a filtered agent that *loses* is
> discarding a real option; it does not lose. **The budget-scaling ladder is monotone with every
> rung's lower CI bound above parity** (1000 vs 100: 55.7% [52.7%, 58.7%] · 3000 vs 1000: 54.0%
> [51.0%, 57.0%] · end to end: 55.3% [52.0%, 59.0%]) — the safety net is calibrated and passing
> before rollouts exist, which was the whole point of building it now.
>
> Five corrections the build produced:
>
> 1. **Phase 1's illegal-action finding was a targeting bug, not a filtering one — and it is now
>    fixed.** 889 of 945 rejections were `CastSpell: No valid targets available`, and the
>    meaningful-action filter turned out not to be the cause. `Strategist` abandoned target
>    selection for the *whole spell* whenever *any* requirement had no legal target, then submitted
>    an untargeted cast. Almost every case is an **optional** trailing slot — Conduct Electricity's
>    "up to one target creature token" with no token on the board makes the AI decline to target the
>    mandatory creature either. `fillableRequirements` fills what it can. Measured: **36 → 0** over
>    200 mirror games.
> 2. **`validTargets` cannot see a multi-requirement spell's second slot.** It only ever mirrors the
>    first requirement, so "targeted spell with no legal target" passed a spell whose second
>    *mandatory* slot was empty. `PriorityAction.hasUnfillableTargetRequirement` asks the real
>    question, and the client's auto-pass gets the fix too — it was stopping players on spells they
>    could not cast.
> 3. **A budget must be spent as work, not wall clock.** `SearchAllowances` converts a tier into a
>    count of simulations once; the millisecond figure is a hard safety stop only. A stopwatch would
>    have made every arena run irreproducible and `ArenaHarnessTest`'s "identical at 8 threads and
>    at 1" assertion flaky — and it is why the arena's 3.5-minute 1,000-game gate survives Phase 4b
>    intact, contrary to Phase 1's warning that shipping a budget would make the arena expensive.
> 4. **The plan's threading list was one module too long.** Every scan in `DecisionResponder` is
>    already bounded to ≤11 simulations by construction, at or below what even ROUTINE allows. The
>    budget is wired into the one place it can bind (the target pre-rank cut) and deliberately not
>    into the other twenty responders.
> 5. **Two CRITICAL triggers from the table are not implemented, on purpose.** "Sweeper castable"
>    and "a real counterspell window" both need to know what a card *does* — Phase 6's `CardIntent`.
>    Guessing from a mana cost would put the most expensive tier on the wrong windows.
>
> **`LEGACY_V0` is untouched.** Both halves are behind profile flags, including the target-filling
> bug fix — not because the old behaviour is defensible, but because quietly strengthening the
> permanent reference opponent would silently rebase every number published against it.
> `FrozenBaselineTest` would not have caught it: its frozen game is all-vanilla Portal, which has no
> multi-requirement spell.
>
> **The branching-factor exit criterion was dropped, as Phase 0 predicted.** "Down 30–50%" was
> written against an assumed ~8 candidates; the real figure is 1.75, and `filterMeaningful` has
> almost nothing left to cut. The delivered win is `canAutoPassWithoutEnumerating`: **40% of
> priority windows are now decided without calling the enumerator at all** (measured over 884 real
> windows by `AutoPassParityTest`, which prints the figure).

#### 4a. Port the meaningful-action filter down into the engine

`AutoPassManager.getMeaningfulActions` (`game-server/.../priority/AutoPassManager.kt:281`) is a
complete, Arena-style implementation — drops `PassPriority`, mana abilities without a sacrifice cost,
targeted spells with no legal targets, unaffordable casts/cycles/crews — but it lives in game-server
and consumes the server DTO `LegalActionInfo`, so `:ai` and `:gym` can't call it.

**Create** `rules-engine/.../legalactions/MeaningfulActionFilter.kt` over `LegalAction`, exposing:

- **`filterMeaningful(...)`** — shrinks the Strategist's candidate set. `Strategist.kt:57` currently
  filters only `affordable && !isManaAbility && != PassPriority`.
- **`shouldAutoPass(...)`** — **the bigger win**: lets a playout skip an entire priority window
  without scoring anything. A 2-turn rollout crosses ~20–30 windows; auto-passing the ~80% that are
  trivially skippable cuts playout length ~4–5×.

Port `shouldAutoPassOnMyTurn` / `shouldAutoPassOnOpponentTurn` and the `SPELL_CAST_ACTION_TYPES`
(`:76`) / `INSTANT_RESPONSE_ACTION_TYPES` (`:93`) constants with it. `AutoPassManager` then
**delegates** — one source of truth.

This is production UX code, so: **`AutoPassParityTest`** runs both the old inline logic and the new
shared filter over a corpus of arena-harvested states and asserts identical verdicts. Delete the
inline logic only once it's green.

Route `Strategist.kt:57` and `GameSimulator.kt:63` through the filter, behind
`AiProfile.useMeaningfulFilter` so V0 is preserved.

The crude version that used to live in `Searcher.canRespond` — "has an untapped land AND a non-empty
hand" — went with the file in Phase 0; there is nothing to supersede. Note `LegalAction.holdPriority`
(`LegalAction.kt:154`) is the engine's explicit "never auto-pass while this is available" flag;
honour it.

> **Re-scope this sub-phase against Phase 0's measurement.** The branching factor is **1.75
> candidates** on the 24% of windows that offer any — `filterMeaningful` has almost nothing left to
> cut, and the "branching factor down 30–50%" exit criterion below is not reachable because it was
> written against an assumed ~8. The win is entirely `shouldAutoPass`: 76.2% of windows already
> yield zero candidates and still pay `enumerate` (0.40 ms) first, which is ~148 ms per game of pure
> waste and is paid again on every rollout crossing. Scope 4a as "skip the enumeration".
>
> **As built,** that became a third entry point, `canAutoPassWithoutEnumerating`, which decides a
> window from the **state alone** — the other two still need the enumerated list. It fires on 40%
> of windows rather than 76.2%, because it declines every window whose verdict depends on what the
> player is holding, and `AutoPassParityTest` holds it to being a strict subset of the full verdict
> over a corpus of real windows.

#### 4b. `DecisionBudget`

`ai/.../engine/budget/DecisionBudget.kt` — `deadlineNanos`, `expired()`, `remainingMs()`, and a
tier assigned by `BudgetPolicy.tierFor(state, meaningfulActions, playerId)`:

| Tier | Budget | When |
|---|---|---|
| TRIVIAL | 0 ms | ≤1 legal action, or `shouldAutoPass` says yes |
| ROUTINE | 200 ms | Opponent's-turn priority with no immediate threat; upkeep/draw/end-step |
| NORMAL | 2,000 ms | Our main phase with meaningful actions; most decisions |
| CRITICAL | 5,000 ms | Combat declaration; either player in lethal range; sweeper castable or on the stack; a real counterspell window |

Thread through `AIPlayer` → `Strategist` → `CombatAdvisor` → `DecisionResponder`. Replace
`CombatAdvisor.kt:181`'s hardcoded `+ 1000` with the budget deadline, and `MAX_BLOCK_SIMULATIONS = 10`
(`:35`) with a budget-driven loop keeping 10 as a **floor**. Combat declaration is always CRITICAL,
so combat never gets *less* time than today.

**Anytime contract:** every consumer must have a valid answer after its first iteration. Enforced
structurally by Phase 7's sequential halving.

**Exit:** ~~branching factor down 30–50%~~ (unreachable at 1.75 candidates — replaced by "40% of
priority windows decided without enumerating"); ✅ `just arena v0 v0-meaningful 1000` = 51.3%,
CI [49.8%, 52.7%]; ✅ `ArenaBudgetScalingTest` running **and monotone**.

---

### Phase 5 — Simulation speed · *3–5 d* — ✅ **DONE 2026-07-28** — *was never a gate on Phase 7*

> **Shipped.** 5a is done and closes `engine-performance.md` Step 4; 5b was already dropped by
> Phase 0; **5c is adjudicated and stays dropped**, now on a fresh profile rather than an old one.
> `rules-engine/.../mechanics/mana/ManaStaticsIndex.kt` and
> `rules-engine/.../event/BattlefieldStaticsIndex.kt` own the walks. Numbers and findings:
> [`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md#phase-5a--the-on-battlefield-scans).
>
> **The targeted leaves are gone**, measured with async-profiler over 60 BLB games:
> `ManaSolver.findAvailableManaSources` **~59% → 3.1%** inclusive ·
> `getStaticGrantedManaAbilities` **3.5% self → 0.00%** ·
> `TriggerAbilityResolver.getWardTriggeredAbilities` **13.7% → 0.19%** ·
> `isWardSuppressed` → **0.00%** · `GameState.getBattlefield()` **19% → 0.28%**. The two new
> indexes together cost **0.71%** inclusive.
>
> Three corrections the build produced:
>
> 1. **An eagerly-built index is a hotspot of its own, and the first cut had one.** Giving
>    `getTriggeredAbilities` a *default argument* that builds the index looks free and is not: Kotlin
>    evaluates a default per call, and ~19 call sites sit inside per-entity loops, so
>    `BattlefieldStaticsIndex.build` came back at **5.2% inclusive** — about the size of the hotspot
>    the hoist had just removed. Threading it on `TriggerIndex` (which `detectTriggers` already
>    builds once per pass) took it to **0.13%**, and `detectTriggers` from 20.7% to 7.1%.
>    `ManaSolver` had the same trap and is fixed with a local `lazy(LazyThreadSafetyMode.NONE)`.
>    **Hoisting work out of an inner loop is only half the fix; the other half is paying for it once
>    per pass rather than once per call that might have needed it.**
> 2. **`PredicateEvaluator.matchesCardPredicate` is now the engine's top hotspot at 20.4% self** —
>    more than 3× the next entry. That is the next perf item, and it is a different shape of problem:
>    not a redundant scan, but the per-call cost of the predicate language itself.
> 3. **The benchmark delivers, but only against a same-day run.** Total engine CPU over 200 games
>    goes **1,051 s → 832 s (−21%)** and wall clock 133 s → 108 s, with `process` nearly halving
>    (287 s → 144 s — the trigger half) and `enumerate` down 10% (764 s → 688 s — the mana half).
>    Against its own *recorded* baseline it reads ~290 actions/sec/thread versus ~404, which is not
>    a regression: `GameState.turnNumber` counts player turns now rather than rounds, and the BLB
>    pool has roughly doubled since May, so the two runs describe different workloads. Compare
>    same-afternoon runs and let the profile say why.
>
> **Phase 0's headline conclusion stands unchanged:** simulation was already ~2× the rate the
> rollout budget needs, so none of this was blocking Phase 7. It is a standing engine win taken on
> its own merits.

> **Phase 0 measured this target as already met.** The budget below assumed ~8 candidates and a
> ~404 actions/sec/thread engine. Reality: **~3,400 `process()`/sec/thread** (as-played mix,
> ~2,400 at the candidate mix) and **1.75 candidates** per non-trivial window. A 2 s budget buys
> ~6,800 `process()` calls ≈ **60 rollouts per decision, ~35 per candidate** — an order of magnitude
> more than Phase 7's R = 3–4 × K = 1–2 needs. Steps 1–3 of `engine-performance.md`, which landed
> after that baseline, evidently did the work.

**The original budget, kept for the record:**

```
candidates after Phase 4 filter    ~8       determinizations   K = 1–2
rollouts per (candidate, world)     R = 3–4  actions/rollout   ~50 (post-auto-pass; ~200 before)
→ ~3,200 process() calls per decision ÷ 2.0 s  →  ~1,600 process()/sec/thread
```

With sequential halving (Phase 7) the effective candidate count drops to ~3, so
**1,500–2,000 `process()`/sec/thread was the target** — cleared today at ~3,400.

#### 5a. Finish `engine-performance.md` Step 4 — hoist the O(n²) battlefield scans

**This is the headline perf item, not a projection cache.** `ManaSolver.findAvailableManaSources`
was **59% inclusive** in the profile, and it is still quadratic:
`findAvailableManaSources` loops over candidate entities (`ManaSolver.kt:945`) and calls
`getStaticGrantedManaAbilities(entityId, state)` (`:1823`), which itself loops over
`state.getBattlefield()` — an O(n²) scan per enumerate, and enumerate runs at every priority step.

Hoist: compute the granted-mana-ability map **once** per `findAvailableManaSources` call and pass it
down. Same treatment for `TriggerAbilityResolver.isWardSuppressed` (`:662`), which still does
`state.getBattlefield().any { … }` from inside a per-entity path (`:496`) — precompute the
suppressor set once per detection pass.

`getBattlefield()`'s memoization (already landed) removed the *allocation* cost of these scans but
not the *iteration*. This is the remaining half.

> **As built, the hoist went wider than the two named scans, because they had siblings.**
> `getStaticGrantedManaAbilities` was one of *six* per-source battlefield walks inside
> `findAvailableManaSources` (the other five: the aura colour override — carried twice, once in the
> solver and once in `ManaAbilityEnumerator` — the `ReplaceLandManaColor` check, the aura bonus-mana
> scan and the source-tap bonus-mana scan), and `isWardSuppressed` was one of *four* inside
> `TriggerAbilityResolver` (the others: the battlefield-scope `GrantWard` scan and two
> attachment scans). Fixing one and leaving its siblings would have left the O(n²) exactly where it
> was, so each file got **one index instead of five one-off hoists**:
> `mechanics/mana/ManaStaticsIndex.kt` and `event/BattlefieldStaticsIndex.kt`.
>
> Both index the *rare* static they hunt for, so an ordinary board yields the `EMPTY` instance and
> the per-entity cost collapses to a lookup that finds nothing. Each bucket reproduces its original
> loop's collection rules exactly — including the two places where those rules disagreed with one
> another (face-down handling; `staticAbilities` vs `effectiveStaticAbilities`). Preserving a
> pre-existing inconsistency is deliberate: this is a hoist, not a rules change.
>
> Fold-ins that came for free: `EnumerationContext.manaStatics` shares one index across a whole
> enumeration pass, and `TriggerDetector.buildTriggerIndex`'s existing grant-provider pass merged
> into the new walk (it was scanning for a sibling of the same `GrantX` shape over the same entity
> set), so trigger detection now walks the battlefield once where it walked twice.

#### 5b. Do **not** build a projection cache — *confirmed by Phase 0*

An earlier draft of this plan proposed caching `StateProjector.project` across rollout states, on the
theory that `by lazy` per-instance never hits during a rollout. **The profile contradicts it:**
`StateProjector.project` is **7.4% inclusive** and explicitly flagged "already cached — *not* a
target." And critically, the profile was taken on `RandomActionBenchmark`, which visits each state
exactly once — so it *already* reflects the zero-cache-hit case. A cross-state projection cache would
buy ≤7%, not the 2–5× a rollout budget needs, and it would carry real silent-wrongness risk from a
fingerprint that misses a layer input. **Dropped.**

Phase 0 measured this independently: projection timed cold on a fresh `state.copy()` is **11.2–11.5%
of one `process()` call**. Same verdict, from a different measurement — the ceiling on a perfect
cache is ~12%.

#### 5c. Persistent collections for `entities` / `zones` — profile-gated ❌ **DROPPED 2026-07-28**

> **The gate was checked and it does not open.** In the post-5a profile the allocation cluster this
> targets is `Arena::grow` **1.37%** plus `posix_madvise` ~0.7% — about **2%** of the engine, against
> 4–6 days plus the serializer work below. `engine-performance.md` Step 5 says "only if `Arena::grow`
> is still hot"; it is not. Everything below stays accurate as a design should the number ever move.

Matches `engine-performance.md`'s own Step 5 ("only if `Arena::grow` is still hot"). `withEntity`
(`GameState.kt:312`) is `copy(entities = entities + (id to container))` — an **O(entities) map copy
per single component write**, at 125–250 entities.

Containment is better than it looks:
- All mutation funnels through `GameState.kt:296-440`. Only ~20 direct `copy(entities = …)` /
  `copy(zones = …)` sites exist elsewhere (`GameInitializer`, `StackResolver`, `MulliganHandler`,
  `ZoneTransitionService`, `LibraryAndZoneContinuationResumer`, `MoveCollectionExecutor`,
  `ShuffleLibraryExecutor`, `ScenarioBuilderService`), all of `x + (k to v)` form, which type-checks
  unchanged against `PersistentMap.plus`. Two (`ShuffleLibraryExecutor.kt:37`,
  `MoveCollectionExecutor.kt:551`) build a local map and need `.toPersistentMap()`.
- All ~127 read sites compile unchanged — `PersistentMap : Map`, `PersistentList : List`.

**What actually breaks is serialization.** `GameState` is `@Serializable` and round-trips through
`RedisGameRepository.kt:60`, `ScenarioController.kt:110`, `PublicReplayController.kt:77`. Fix with
`rules-engine/.../state/PersistentCollectionSerializers.kt` whose serializers **delegate to the
standard `MapSerializer` / `ListSerializer`** and call `.toPersistentMap()` / `.toPersistentList()`
on decode → **byte-identical wire format**, no migration for persisted Redis sessions or committed
replays. `ComponentContainerSerializer` is untouched (it serializes values, not the map). Add a
golden-JSON `GameStateSerializationFormatStabilityTest`.

Do `entities` first. `zones` values are 40–60-element lists and `removeFromZone` is `current - id` on
every draw; `PersistentList.removeAt` is still O(n), so the win is smaller — treat as a follow-on.

Expect 1.5–3×. Add `kotlinx.collections.immutable` to `gradle/libs.versions.toml`.

#### Explicitly out of scope

**Incrementalizing `StateProjector`.** 2+ weeks, and the failure mode is silent rules bugs across the
whole engine. The profile says it's 7.4%. Not worth it.

**Exit:** `SimulationThroughputBenchmark` ≥1,500 `process()`/sec/thread — **already true at ~3,400
before any Phase 5 work**, so the meaningful exit for 5a is the performance plan's own loop: the
targeted leaf shrank under the profiler and `just benchmark-random 200 BLB` improved against a
freshly measured baseline. `just test-rules` and `:game-server:test` green.

---

### Phase 6 — `CardIntent`: card knowledge that generalizes · *5–7 d* — ✅ **DONE 2026-07-28**

> **Shipped, with one exit criterion met loudly and one missed.**
> `ai/.../engine/knowledge/{CardIntent,CardIntentAnalyzer,EffectWalker,IntentCatalog,HoldPolicy}.kt`.
> Numbers and findings:
> [`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md#phase-6--cardintent).
>
> **Puzzles 39/48 → 44/48.** Non-creature valuation **2/6 → 5/6** (the exit criterion, met) and
> holding instants **3/6 → 5/6** (up, as required). **The arena is neutral**, 50.9% for `v0-intent`,
> CI [49.1%, 52.8%], and the `ffa3` pod agrees at 35.7% CI [32.3%, 39.0%] against a 33.3% null — so
> "arena lower CI bound above 50%" is **not** met. BLB sealed is mostly creatures, which is the same
> insensitivity Phases 1 and 2 already measured about the 42 card advisors; the merge argument is
> "clearly better tactically, demonstrably not a regression".
>
> Carried on an `AiProfile.useCardIntent` flag, on for `PRODUCTION` and the new `v0-intent` /
> `v0-phase4-intent` arena agents, off for `LEGACY_V0` — `FrozenBaselineTest` still passes.
>
> Four things the build changed about the plan:
>
> 1. **The removal half of the `HoldPolicy` was built, measured and removed.** A penalty on
>    "instant removal in our own main phase" large enough to change behaviour (−2.0) also vetoed
>    `noncreature-01` — an instant-speed Disenchant in our own main phase — which is the exact cast
>    this phase exists to enable. A combat trick outside combat provably does nothing; holding
>    removal is a *preference between futures*, and pricing it is a Phase 7 rollout question, not a
>    constant. The shipped policy asserts the first and is silent on the second.
>
>    **Reopened and answered, 2026-08-07** — `RemovalPatience`, behind
>    `AiProfile.holdRemovalForBetterTargets`. The diagnosis above is right about a *constant* and
>    wrong about the question: the mistake is not the window, it is the **target**, and "is this
>    creature worth a card?" is a comparison rather than a preference. The discount is what the
>    target falls short of a creature the removal's own mana value should trade with, so a 1/1 under
>    a Murder pays and a 3/3 does not — and the Disenchant that killed the constant is out of scope
>    by construction, since the term only reads *creature* targets. It closes `timing-01` and the new
>    `removal-07` on the live agent (79/87 → 81/87, strict subset of failures) and moves nothing on
>    `production`. **Promoted on the puzzle half; the arena came back null** — 100 games at 50.0%,
>    CI [50.0%, 50.0%], all 50 pairs 1-1-0 against 10/36/37 decisive pairs in the three prior runs,
>    which says the case arises rarely in sealed play rather than that the fix is wrong. Numbers,
>    the lethal veto, and a harness caveat (`production`-class games are not reproducible, so
>    action-stream divergence is not a usable instrument):
>    [`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md#promotion--production-candidate-patience-goes-live).
> 2. **A penalty cannot beat a mis-measurement.** `ThreatAssessment` reads a Giant Growth's +3/+3 as
>    a permanently faster clock and pays **+10.8** for it, so no defensible constant closes
>    `instants-01`. `TimingVerdict.NoWindow` — "whatever the simulation reports, this is not better
>    than passing", floored at the pass score — closes it and `instants-06` together. The underlying
>    flaw (`attackPotential` counts P/T that expires at cleanup, while `creatureValue` already
>    discounts it) is real, still open, and wants its own switch because it moves `LEGACY_V0`.
> 3. **`noncreature-02` misses by 0.40 points and is not a card-knowledge failure** — it is
>    `CardAdvantage.cardValue(0) = −3.0`, the same constant `sequencing-02` fails on. Arithmetic in
>    the baseline doc. Raising the anthem prior would pass it and would be tuning one guess to cancel
>    another.
> 4. **The two rating stores are not duplicates.** One is a curated 0–5 pick rating over 44 sets, the
>    other raw 17Lands win rates over one. `LimitedCardRater` now *chains* them behind a manifest
>    instead of consolidating: real data on 44 sets instead of 1, no new files.

Replaces 42 hand-written advisor entries with a structural analyzer covering every card in the
engine. **Highest strength-per-effort in the plan**, and it raises the leaf evaluator Phase 7 averages
over.

Files: `ai/.../engine/knowledge/{CardIntent,CardIntentAnalyzer,EffectWalker,IntentCatalog}.kt`.

**Reuse the existing precedent.** `LimitedCardRater.effectBonus` (`LimitedCardRater.kt:205`) +
`scoreEffect` (`:229`) **already walk `CardDefinition.script: CardScript`** — spellEffect,
triggeredAbilities (×0.8), activatedAbilities (×0.6), with `asConditional()` / `asMayDecide()`
unwrapping and a `when (effect)` over the SDK effect types. **Extract that traversal into
`EffectWalker`** and have both `LimitedCardRater` and `CardIntentAnalyzer` consume it. One walk, two
scorers — the elegance the `review-changes` skill would ask for.

```kotlin
data class CardIntent(
    val tags: Set<IntentTag>,      // REMOVAL, EXILE_REMOVAL, SWEEPER, DRAW, TUTOR, RAMP, ANTHEM,
                                   // PUMP, COMBAT_TRICK, COUNTERSPELL, LIFEGAIN, DISCARD,
                                   // RECURSION, TOKEN_MAKER, PROTECTION, TAPPER, EVASION_GRANT,
                                   // SACRIFICE_OUTLET
    val speed: Speed,              // SORCERY | INSTANT | STATIC | ACTIVATED
    val removalReach: Int?,        // damage / toughness it can answer
    val cardsDrawn: Int?,
    val affectsOpponent: Boolean,
    val repeatable: Boolean,       // activated / triggered on a permanent
    val staticPriorValue: Double,  // feeds permanentValue
)
```

Pure function of `CardDefinition` — memoize by card name in a `ConcurrentHashMap` scoped to the
`CardRegistry`. Cost paid once.

**Five consumers:**

- **(a) `BoardFeatures.permanentValue` (`:81-98`)** — the flat `0.5` becomes `intent.staticPriorValue`:
  repeatable removal artifact/enchantment 2.5–4.0 · anthem/lord `1.0 + 0.5 × creatures pumped`
  (state-dependent, cheap) · token generator `1.5 + tokens/turn` · mana rock 0.7 (≈ a land) ·
  uninterpretable stays 0.5 (unchanged fallback). Planeswalkers: replace the flat `4.0` with
  `loyalty × 0.8 + Σ ability intent`.
- **(b) Targeting** — `Strategist.heuristicTargetRank:289`'s `else -> 0.0` becomes
  `intent.staticPriorValue + 10.0` on the opponent's side. This alone makes the AI capable of
  Disenchanting correctly, which it currently cannot do at all.
- **(c) Timing** — generalize the ad-hoc `passScore - 1.5` (`Strategist.kt:87-91`) into an
  intent-driven `HoldPolicy`: penalize casting an INSTANT COMBAT_TRICK / REMOVAL / COUNTERSPELL in
  our own main phase with no forcing reason; bonus on the opponent's end step or after blockers.
  Targets the "holding instants" puzzle category directly.
- **(d)** Playout-policy softmax weights (Phase 7 step 4).
- **(e)** Determinization prior (Phase 8, unknown-decklist tier).

**17Lands prior — expand it.** `LimitedCardRater` already loads real 17Lands GIH win-rate data from
`rules-engine/src/main/resources/ratings/{SET}.json` and maps it to a 0–5 rating, but
`LimitedCardRater.kt:45` hardcodes `setCodes = listOf("BLB")`. Derive the set list from a manifest,
add JSON for the sets we actually play, and consolidate with the duplicate store at
`ai/src/main/resources/draftai/ratings/BLB.json` — two rating stores for the same data is a trap.

> **Caveat:** GIH-WR is a *limited-format, context-free* prior. Use it for static card quality (deck
> building, cards-in-hand valuation, determinization priors). **Never** as a positional evaluation
> term — it says nothing about the board.

**Do not delete the 42 advisors in this phase.** Add `CardIntent` underneath, then run
`just arena-gauntlet` over `{intent-only, advisors-only, both}` and retire the advisors that
`CardIntent` reproduces, keeping the ones encoding genuinely card-specific tactics. A principled
retirement criterion beats a judgment call.

> **As built:** the advisors are untouched and both live side by side —
> `Strategist.evaluate1Ply` applies the timing verdict *outside* the advisor override, so a card with
> both keeps both and an advisor still sees the pure board score as its `defaultScore`. The
> retirement gauntlet is deliberately left for a follow-up: with the arena reading neutral on
> `v0-intent` it cannot yet separate the three configurations, so retiring anything on it would be
> reading a coin flip.

**Exit:** puzzle "non-creature valuation" **2/6 → ≥5/6** (Phase 2's measured baseline, not the ~0%
this plan guessed) — ✅ **5/6**; "holding instants" up — ✅ **3/6 → 5/6**; arena lower CI bound above
50% — ❌ **not met**, 49.2% CI [47.2%, 51.4%], neutral.

> **Phase 2 sharpened the target.** All four non-creature failures are the AI declining to *cast*
> the Disenchant, not mis-targeting it: at flat `permanentValue = 0.5`, destroying an artifact is
> worth +0.75 weighted and costs −1.5 of card advantage, so passing wins. `staticPriorValue` has to
> clear the **cost of casting**, not merely outrank a sibling target. Consumer (b) — the
> `heuristicTargetRank` fix — is necessary but on its own changes nothing.

---

### Phase 7 — Rollout evaluator · *6–9 d* — ✅ **DONE 2026-07-29** — the primary lever

> **Shipped.** `ai/.../engine/rollout/` owns the whole stack: `CandidateEvaluator` (the seam),
> `WinProbability` (the scale conversion), `PlayoutEngine` + `PlayoutPolicy` +
> `FastDecisionResponder` (one cheap game forward) and `RolloutCandidateEvaluator` (sequential
> halving over a shared seed grid). `CombatSeed` and `TargetSelection` are the heuristic halves of
> `CombatAdvisor` and `Strategist`, extracted so a playout can use them without simulating. New
> doc: [`docs/ai/architecture.md`](../docs/ai/architecture.md). Numbers and findings:
> [`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md#phase-7--rollout-evaluator).
>
> **`just arena v0 v0-rollout 300` → 56.0%, CI [52.0%, 59.7%]** at the shipped 16 playouts, and
> 57.3% CI [53.0%, 61.7%] at 8 — clearing the exit criterion (≥53%, lower bound above parity).
> On Phase 2b's expanded 66-puzzle suite the rollout is **neutral — 55/66, exactly `v0`'s score —
> and the two moves cancel**: it closes **`instants-05`**, the Fog puzzle Phase 2 explicitly assigned
> to this phase, and it loses **`respond-02`** ("do not spend the only Counterspell on a 2/2 with
> seven lands still open"). The loss is a horizon effect and the honest price of the mechanism:
> countering shows a gain inside the two-turn horizon, the cost of not holding the card falls outside
> it. On top of Phases 4 and 6 it is a small net negative (56/66 against 58/66), so what ships is not
> "turn everything on" — the arena says the rollouts earn their place, the suite says where they do
> not.
>
> Also found, and **not fixed** (pre-existing, and an affordability bug rather than evaluator work):
> every agent that plays different lines from `v0` surfaces a `CastSpell: Not enough mana to cast
> this spell` rejection at ~0.05/game — `blb-advisors` 42 per 300 games, `v0-intent` 19,
> `v0-rollout` **13**. `v0` never reaches those states, which is why `ArenaHarnessTest`'s
> `v0`-mirror clean-games assertion stays green. Quantified in the baseline doc.
>
> Five corrections the build produced:
>
> 1. **Squashing the *absolute* board score makes the search report "certain loss" for every
>    candidate.** The plan says to squash and average; it does not say *what* to squash, and the
>    obvious reading fails because the evaluator has no calibrated zero. `ThreatAssessment` prices
>    "we can never kill them" with a 99-turn sentinel, so an ordinary turn-1 position where one side
>    has no creatures scores **−176** while a close board is single digits — and at any `SCALE` small
>    enough to separate real candidates, −176 and −156 both squash past the clamp to the same number.
>    Measured: the puzzle suite fell to 32/48, every failure "chose PassPriority". The fix is to
>    squash the **delta from the decision's root**, which is shared by every candidate, so the
>    arbitrary offset cancels and only the differences the Strategist compares survive.
> 2. **A pure rollout is *weaker* than the greedy AI it replaces, structurally rather than
>    statistically.** Passing in your own main phase does not end the turn — it advances a step — and
>    the playout policy then casts the very spell you just declined. Two turns downstream the lines
>    have converged, the mean cannot see the tempo difference, and the strict `best > pass` sends the
>    tie to passing (48/66 against `v0`'s 55/66; removal 6/6 → 2/6, non-creature 2/6 → 0/6).
>    `RolloutSettings.staticWeight` mixes the static leaf back in — the two estimators are blind to
>    different things — and it is its own control at 0.0 and 1.0.
> 3. **Puzzle positions had empty libraries** — invisible to a 1-ply agent, fatal to a searching one,
>    since a two-turn playout hits the draw step with nothing to draw and every line ends in a
>    decking race (CR 104.3c). `PuzzleRunner` stocks 30 basics per seat now; the existing baselines
>    are unchanged by it, which is the evidence it fixed the harness rather than the positions.
> 4. **Phase 7 is the first search whose cost the arena can feel, and it does not need the cost.**
>    At the ~60 playouts a 2 s tier affords, a game is ~70 s against `v0`'s ~0.07 s and the
>    1,000-game gate is hours — Phase 1 predicted exactly this ("the budget, not the game count, is
>    what makes an arena expensive") and Phase 4b escaped it. But the ladder says the money is
>    wasted: **strength rises from 4 to 8 playouts and then plateaus** (`v0` vs `v0-rollout-4` =
>    53.7% CI [49.3%, 57.3%]; vs `v0-rollout-8` = 57.3% CI [53.0%, 61.7%]; `-4` vs `-32` = 50.7% CI
>    [47.5%, 53.7%] over 400 games). That is saturation, not the risk register's failure mode of
>    strength *falling* with more search — the rollout term is bias-limited rather than
>    variance-limited, because it carries a quarter of the score, common random numbers already pair
>    it, and no amount of sampling reveals the tempo it cannot see. So `NORMAL_PLAYOUTS` ships at
>    **16**, not 64, and the feature is 4× cheaper than its own budget.
> 5. **The plan's one-candidate `score(...)` API cannot express sequential halving.** An evaluator
>    that sees one candidate cannot decide to spend 4× on it. `scoreAll` is the resolution, and it
>    defaults to `map(::score)` — so `StaticCandidateEvaluator` needs no override and `LEGACY_V0`
>    comes out bit-identical, which `FrozenBaselineTest` proves.
>
> **Not done, deliberately:** the `earlyCutoffMargin` flag exists and is off — it trades a real bias
> for speed and deserves its own A/B rather than shipping on by default. `RolloutSettings.determinizations`
> is present and pinned at 1: the seed grid is `(d, r)` so Phase 8 can add worlds without rewriting
> the common-random-number scheme, but with no `Determinizer` yet every world is the same world.

**Plug point is one line:** `Strategist.kt:160`'s
`evaluator.evaluate(result.state, result.state.projectedState, playerId)`.

**Create** `ai/.../engine/rollout/CandidateEvaluator.kt`:

```kotlin
interface CandidateEvaluator {
    fun score(root: GameState, afterAction: GameState, playerId: EntityId, budget: DecisionBudget): Double
}
```

`StaticCandidateEvaluator` = today's behaviour, what `LEGACY_V0` uses. `RolloutCandidateEvaluator` =
the new one. **The `CardAdvisor` override path is untouched** — advisors receive the rollout score as
`CastContext.defaultScore`, so per-card overrides keep working over a much better base.

#### Scores must become win probabilities

`CompositeBoardEvaluator` returns `Double.MAX_VALUE / 2` for a win (`BoardEvaluator.kt:28`) —
averaging that with anything is meaningless. Add `rollout/WinProbability.kt`:
`squash(s) = 1 / (1 + exp(-s / SCALE))`, terminal = 1.0 / 0.5 / 0.0. Average **in probability
space**; squash the pass score the same way. This also retires the unscaled magic number at
`Strategist.kt:87`. `SCALE` falls out of Phase 9's logistic fit for free.

#### The playout engine

`PlayoutEngine.kt` owns its **own** `ActionProcessor(EngineServices(registry), computeUndo = false)`,
its own `LegalActionEnumerator`, its own state. **Do not share the Strategist's `GameSimulator`** —
`isResolving` (`GameSimulator.kt:35`) is mutable instance state and `decisionResolver` is a mutable
`var`; sharing one instance corrupts the recursion guard.

`PlayoutPolicy.kt` — **hard rule: it must never call `Strategist`, `CombatAdvisor`'s local search,
`DecisionResponder`'s simulation paths, or `chooseCommittedTargets`.** Anything that simulates inside
a playout makes the playout quadratic. In order:

1. `shouldAutoPass` → `PassPriority`, zero cost. Expect ~80% of windows.
2. `DeclareAttackers` / `DeclareBlockers` → **seed only**. Extract `CombatAdvisor`'s existing
   heuristic seed phase (it already builds `seedMap` *before* optionally running
   `improveAttackViaLocalSearch`) into `ai/.../CombatSeed.kt`; `CombatAdvisor` calls it too, so
   there's one implementation. The deadline local search is skipped in playouts.
3. Land drop → always, if legal.
4. Otherwise → **weighted random** over filtered candidates: softmax (τ≈1.0) over a zero-simulation
   priority score (`CardIntent` priority + mana value + creature bonus). **Stochastic is essential** —
   a deterministic policy makes all R rollouts within a determinization identical, collapsing K·R to K.
5. Targets → `heuristicTargetRank` (`Strategist.kt:289`) only.

**Decisions inside playouts:** `GameSimulator.trivialResponseFor` (`:163`) is private — extract it
verbatim to a top-level `TrivialDecisions.kt` and have `GameSimulator` delegate. Add
`rollout/FastDecisionResponder.kt` with an O(1) rule per `PendingDecision` type (trivial response if
any; else first legal / minimum / default assignment / random-from-legal). No simulation, no
prompt-string matching.

#### Horizon and depth schedule

Default horizon: **end of the opponent's next turn** — stop when the state next reaches
`activePlayerId == playerId && step == UNTAP` after ≥2 turn transitions, or `gameOver`, or a 150-action
safety cap. Depth schedule: after the first pass, if `|best − second| < ε` and budget remains, extend
to two opponent turns **for survivors only** — the one good idea inside the dead
`Searcher.recommendDepth`, which was deleted in Phase 0. Reimplement the idea here; don't go
looking for the file.

#### Variance reduction — all four; they are the difference between working and not

1. **Common random numbers — the big one.** `GameState.rng` is state-resident (`GameState.kt:260`),
   so at the start of rollout *(d, r)* set `state.copy(rng = GameRng(mix(rootSeed, d, r)))` and use
   the **same (d, r) seed grid for every candidate**. Candidate comparisons become paired and
   between-candidate noise mostly cancels. Without this you need ~4× the rollouts.
2. **Shared determinizations** — the same K worlds for all candidates. Same principle.
3. **Early cutoff** on `gameOver`; optionally on `|squashed − 0.5| > 0.47` (small bias, real speedup —
   put it behind a flag and A/B it).
4. **Sequential halving over candidates.** 1 rollout each → drop the bottom half → double the
   per-candidate budget → repeat until one remains or the budget expires. ~30 LOC, 2–4× effective
   rollouts on the contenders, and **inherently anytime** — this is how the `DecisionBudget` gets
   consumed: `while (!budget.expired() && survivors.size > 1) { … }`.

| Tier | Rollout behaviour |
|---|---|
| TRIVIAL | Not called |
| ROUTINE | Static evaluator only, no rollouts |
| NORMAL | Sequential halving to 2,000 ms |
| CRITICAL | Sequential halving to 5,000 ms, deeper horizon |

**Exit:** arena ≥53% with lower CI bound above 50%; `ArenaBudgetScalingTest` **monotone in budget**;
puzzle gains in sequencing / race math / board-wipe timing; p95 latency ≤5 s.

---

### Phase 8 — Determinization (fair play) · *5–7 d* — ✅ **DONE 2026-07-29**

#### 8a. Extract the visibility oracle

**Create** `rules-engine/.../view/Visibility.kt` — a public object in the **same package** as
`ClientStateTransformer`, so no import churn. **Move** (don't copy) the private `isZoneVisibleTo`
(`ClientStateTransformer.kt:479`) and `isCardRevealedTo` (`:595`), plus the helpers they need
(`revealsOpponentHandsTo`, `hasActiveStaticAbility`, `hasLookAtFaceDownCreatures`).
`ClientStateTransformer` delegates.

That logic is non-trivial and correct — Mindslaver `actorFor`, 2HG `teammatesOf`, Seer's-Vision-style
reveal effects, sideboard privacy (CR 100.4 / 400.11a), conditional statics. Don't reimplement it.
`RevealedInHandTracker` (invoked at `ActionProcessor.kt:90`) maintains `RevealedToComponent` for the
"revealed into hand stays visible" rule.

**Regression net:** golden test asserting `ClientStateTransformer.transform` output is byte-identical
on a corpus of scenario states before/after.

#### 8b. The determinizer

`ai/.../engine/hidden/Determinizer.kt`:
`sample(state, viewerId, model: OpponentModel, rng: GameRng): GameState`

**Core design: permute identities, never entities.** This is what keeps everything else intact.

1. Collect hidden ids — opponent LIBRARY entities, plus HAND entities where
   `Visibility.isCardRevealedTo(state, id, viewerId)` is false.
2. **Pin** anything with `RevealedToComponent` for the viewer, referenced by `state.continuationStack`,
   targeted by something on the stack, or carrying unusual components (counters, face-down, attached).
3. Draw `|hidden|` `CardDefinition`s from the plausible pool.
4. **Rewrite each hidden entity's card identity in place** — same `EntityId`, same `OwnerComponent`,
   same zone slot, same zone ordering. Only `CardComponent` and card-derived components are replaced.
5. Shuffle library order using the passed `rng`.

Because entity ids and zone memberships are preserved, `continuationStack` references,
`RevealedToComponent`, `pendingDecision` and zone sizes are all structurally intact.
**`DeterminizerInvariantsTest`** enforces it: entity-id set, zone-key set, per-zone sizes, and the
full viewer-visible projection identical before/after.

**The riskiest bit** is rebuilding components from a different `CardDefinition`. Factor
`CardInstantiator.componentsFor(cardDef, ownerId, zone)` out of `GameInitializer`'s library-building
path so the determinizer uses the engine's own card-construction code rather than a hand-rolled copy.
Highest bug density in this phase.

#### `OpponentModel` — what pool do we draw from?

`setDeckList` is currently a no-op (`EngineAiPlayerController.kt:144-146`). Three tiers:

- **Known decklist** — wire `setDeckList` to store `Map<String, Int>`. Draw from `decklist − seen`,
  where `seen` = every card of that opponent currently visible (battlefield, graveyard, exile, stack,
  revealed-in-hand) plus a running ledger of cards played this game. **Available in the arena by
  construction**, and in any public-list format. Highest fidelity — tune here.
- **Unknown decklist (production default)** — pool from (i) cards the opponent has already revealed
  this game, upweighted 3–5× (people play 2–4 copies), and (ii) a format-legal pool
  (reuse `ConstructedDeckGenerator`'s `legalFormats` filter) restricted to the opponent's revealed
  colours and curve, weighted by the 17Lands / `LimitedCardRater` prior from Phase 6. A genuinely
  decent Bayesian-ish prior for very little code.
- **Identity permutation only (fallback)** — shuffle the *existing* hidden identities among the hidden
  entity ids. Costs nothing, needs no model; destroys knowledge of *which card is where* but not of
  *what is in the deck*. For unknown formats, Momir, Commander with an unknown list. **Document
  honestly as "cheating-lite."**

#### K and integration

**Start with K = 1.** In imperfect-information MTG most of the loss from cheating is "I know exactly
which card you're holding," not "I haven't averaged over enough worlds" — K=1 buys ~90% of the
fairness at 1/K the cost. Determinize **once per search**, at the root of
`RolloutCandidateEvaluator.score`, before the candidate loop (variance reduction #2). Raise K only if
the arena shows it beats spending the same wall clock on more rollouts. It probably won't — K
competes directly with R.

#### Expect a dip, and name it in advance

Turning determinization on **will make the AI weaker in the arena**, because today it cheats. Run
`just arena rollout-k1 rollout-k1-determinized 1000` and **record the dip as a deliverable** —
typically 2–6%. This is a fairness cost, not a bug.

**Exit — not "≥50% vs the cheating version":** the determinized agent still beats `LEGACY_V0` with
the lower CI bound above 50%; invariants test green; `ClientStateTransformer` golden test green; the
dip quantified in `docs/ai/`.

---

### Phase 9 — Texel-style evaluation tuning · *4–6 d*

Stop guessing the ~25 constants in `BoardFeatures.kt` and the 5 weights in
`AIPlayer.defaultEvaluator()` (`:179-187`).

**Weights as data. ✅ DONE 2026-07-31.** `ai/src/main/resources/ai/eval-weights.json`
(profileId → weight vector) +
`evaluation/EvalWeights.kt` with **today's values compiled in as the default**, so a missing or
malformed resource can never break the AI. `AiProfile.evalWeightsId` selects; the arena A/Bs two
weight sets as two agents, no recompile.

**Collection. ✅ DONE 2026-07-31.** `-DarenaEmitFeatures=<path>` appends JSONL
`{features, toMove, turn, gameId, result}` at every 8th quiet state (decorrelation). Rows are buffered
until the result is known and appended as one synchronized game batch, so parallel arena workers
cannot interleave JSON. `RawBoardFeatures` reads battlefield characteristics through projected state
and emits the unweighted Phase 9 schema; no fitted constants have entered the runtime evaluator yet.

**Fit ~30 raw features, not the 5 composites.** The composites *are* the hand-tuned aggregation we're
replacing; weighting them can't fix `lifeValue`'s hand-drawn piecewise curve or `creatureValue`'s
`power × 1.0 + toughness × 0.4`. Proposed: life (mine / theirs / diff / `min(life,7)` burn-range) ·
creature count, total power, total toughness, evasive count, untapped count (each differential) ·
permanent counts by type · planeswalker loyalty sum · hand sizes · lands and untapped lands ·
graveyard sizes · summoning-sick count · turn number · is-my-turn · library size · a few
`CardIntent`-derived counts (removal in hand, threats in play). Keep the 5 composites as a fallback
profile.

**Fit in Python — initial fitter + runtime profile wiring landed 2026-08-01; corpus, uncertainty
reporting, and candidate fit remain.** `scripts/tune_eval.py`, sklearn logistic regression with L2:
`P(win | position) = σ(w·x)`, label = the game's final result for the player to move. Gives
regularization paths, calibration curves and coefficient standard errors for free. Outputs
`eval-weights.json`, a calibration plot, **and the fitted `SCALE`** that Phase 7's
`WinProbability.squash` needs. Don't hand-roll gradient descent in Kotlin test code.

**Five overfitting guards:**
1. **Hold out by game, not by position** — positions within one game are massively correlated; a
   positional split gives a fantasy validation score.
2. **Hold out by set/format** — fit on {BLB, Portal, Onslaught}, validate on a set held out entirely.
   Report both.
3. **Validate by arena win rate, not log-loss.** A weight set with better log-loss that loses in the
   arena is **rejected**. Log-loss selects among candidates; the arena decides. This is the whole
   discipline.
4. **Diversify the data-generating agents** — collect from V0, the current best, and a
   high-temperature/ε-greedy agent. Pure self-play data collapses onto one policy's state
   distribution.
5. **Regularize and clamp** — L2 strength by held-out-by-game CV; cap coefficient magnitudes.

The puzzle suite is an independent third signal: if a tuned weight set improves log-loss *and* arena
but tanks a puzzle category, look hard before shipping.

> **Starter fit rejected 2026-08-01.** 12,548 decisive positions from 238 completed games
> (`v0`/`production`, BLB + POR training, LCI holdout) produced superficially healthy positional
> metrics—training log-loss 0.519 / accuracy 71.4%, held-out LCI 0.487 / 71.5%—but lost the BLB
> smoke arena **0–100** to `v0`. The vector learned policy correlations rather than action value:
> lands had negative coefficients while cards retained in hand were positive, so the agent declined
> development. The candidate was not promoted. Next collection must include a genuinely exploratory
> policy and the fit needs action-delta diagnostics before another arena run; held-out positional
> log-loss alone passed while play was catastrophically bad, exactly why the arena is the gate.

### Phase 9b–9g — Data-driven policy/value/search roadmap · research review 2026-08-01

The rejected starter fit changes the objective, not just the dataset size. Predicting the winner of
a position sampled from an existing policy is **not** the same problem as choosing the action that
improves a position. The next system learns from comparisons between legal actions produced by the
engine, uses search as its teacher, and keeps search at inference time.

#### Research verdict

There is no published method that is unconditionally best for Magic's combination of stochasticity,
hidden information, long horizons, changing card pools, structured legal actions, and multiplayer.
The strongest general results divide into four families:

| Family | Evidence | Fit here |
|---|---|---|
| **Guided search + policy/value learning** | [Expert Iteration](https://papers.nips.cc/paper_files/paper/2017/hash/d8e1344e27a5b08cdfd5d027d9b8d6de-Abstract.html) separates slow tree-search improvement from fast policy generalization; [AlphaZero](https://arxiv.org/abs/1712.01815) closes the same self-play loop with policy/value-guided MCTS | **Best immediate fit.** We already own exact dynamics, legal actions, simulation, determinizations, rollout budgets, and arena promotion. Search can label decisions more directly than final outcomes, while a learned policy/value makes later search stronger and cheaper |
| **Public-belief/game-theoretic search** | [ReBeL](https://arxiv.org/abs/2007.13544) combines self-play learning and search with convergence guarantees in two-player zero-sum imperfect-information games; [Student of Games](https://arxiv.org/abs/2112.03178) unifies guided search, learning, and game-theoretic reasoning across perfect- and imperfect-information games | **Best long-term duel architecture**, but not the first implementation. Exact public-belief enumeration over arbitrary Magic hands/libraries is prohibitive. Start with sampled viewer-consistent worlds, then add belief-aware recurrent state and continual re-solving once the learned policy/value works |
| **Regret minimization / equilibrium learning without search** | [Deep CFR](https://arxiv.org/abs/1811.00164) replaces tabular CFR abstractions with neural approximation in large poker; [NFSP](https://arxiv.org/abs/1603.01121) learns approximate equilibria from fictitious self-play; [DeepNash](https://arxiv.org/abs/2206.15378) reaches expert Stratego without search using regularized Nash dynamics | **A real alternative, not the current choice.** These optimize low exploitability in two-player zero-sum imperfect-information games, but require enormous traversal/self-play budgets and a stable information-state/action representation we do not yet have. They also leave the engine's exact simulator and working search stack underused. Revisit if determinization search proves systematically exploitable |
| **Offline RL / outcome-only fitting** | [Conservative Q-Learning](https://arxiv.org/abs/2006.04779) exists specifically because static logged data produces out-of-distribution value overestimation | **Rejected as the primary path.** We can interact with an exact simulator, so accepting offline RL's distribution-shift problem buys nothing. The 0–100 starter result is the local demonstration: good held-out outcome log-loss did not imply a usable policy |

Two further variants are deliberately not selected:

- **MuZero / learned dynamics.** [Sampled MuZero](https://arxiv.org/abs/2104.06303) is attractive for
  complex action spaces, but its learned dynamics solve a problem we do not have: Argentum already
  supplies exact, deterministic state transitions around explicitly modeled randomness. Learn policy
  and value; do not replace a correct rules engine with an approximate world model.
- **Pure policy-gradient self-play.** It can eventually work, as DeepNash demonstrates, but sparse
  game outcomes throw away the dense counterfactual supervision our simulator can generate. Search
  distillation is more sample-efficient for the resources and infrastructure available here.

**Decision:** build a **duel-first, multiplayer-capable imperfect-information Expert Iteration** loop:
exact legal-action generation + shared-determinization search produces policy/value targets; a fast
learned apprentice generalizes them; the apprentice guides the next search. Spend most collection,
tuning, and promotion compute on 1v1, where the objective and measurements are cleanest, but make every
shared representation and artifact support a variable number of players from its first version.
Evolve duel search toward Student-of-Games/ReBeL-style belief awareness only after the basic loop
demonstrates arena strength. This is an inference from the papers and this repository's constraints,
not a claim that one algorithm dominates every game.

The boundary is: **one learning pipeline, multiple search objectives**. Observation encoding, legal
candidate encoding, teacher-record format, replay buffer, policy/ranker, entity encoder, training
service, artifact format, and inference runtime are shared. Duel and multiplayer adapters may differ
in utility targets, opponent sampling, backup rule, exploration schedule, and promotion arena. Do not
fork a `MultiplayerAi` model or dataset pipeline unless a measured incompatibility makes sharing worse.

#### Deployment envelope — current-server class hardware

The production opponent must remain in the same operational class as the current AI. Training and
large-budget teacher search are offline jobs and must never be required by the game server. Runtime is
an immutable, pre-fitted artifact plus bounded search; loading a champion must not start Python, a GPU
runtime, a training service, or a separate model server.

Set explicit budgets before implementation and record them beside arena strength:

- profile the current production AI on the target server and use its p50/p95 decision latency, peak
  heap, allocation rate, and concurrent-game throughput as the baseline;
- the first learned champion must stay within **1.25x p95 latency**, **+128 MiB process memory**, and
  **1.25x allocation rate** at the same concurrency; treat these as provisional until the real server
  baseline is captured;
- model artifacts should initially remain below **25 MiB** and load once per process, shared by all
  games; per-game mutable model state is limited to small recurrent/belief summaries;
- every decision has a hard wall-clock/node budget and returns the best completed candidate when it
  expires; multiplayer may reduce per-candidate rollouts to preserve the same latency envelope;
- expose `LIGHT`, `NORMAL`, and `STRONG` compute profiles using the same weights. The server default is
  the strongest profile that passes concurrency soak, not the largest model available.

Prefer computation in this order: cached linear/GAM scoring, candidate pruning, batched evaluation of
the remaining legal actions, then a small number of guided rollouts. A learned prior is successful if
it lets search examine **fewer** nodes for equal strength. Do not combine the full current rollout
budget with additional expensive inference and call the result an upgrade.

#### 9b — Repair the data boundary first

Training data is invalid if the generating game was invalid. Before collecting another corpus:

- Fix or reject games containing illegal AI actions. The starter runs found insufficient-mana casts
  and unpayable additional-sacrifice costs; recovered games are useful bug reports, not labels.
- Diagnose the repeatable ONS arena wedge. Never train on the completion-biased prefix of a wedged run.
- Make `runId + gameId` globally unique; record set, format, deck hashes, seed, seat, profile, schema
  version, completion reason, and whether any recovery occurred.
- Record the acting player's **masked observation** plus the search's sampled worlds. Never serialize
  exact unseen identities into model inputs or labels.
- Represent participants as `self + unordered other players`, with stable per-player public features
  and optional team identity. Never bake `player 1/player 2`, exactly-one-opponent, or two-seat tensor
  shapes into the observation schema.
- Add a corpus validator that rejects duplicate games, mixed schemas, incomplete games, illegal-action
  recovery, non-finite features, and missing generator diversity.

Exit: at least three supported sets can each complete a 300-game collection with zero illegal actions,
zero exceptions, zero wedges, and deterministic row hashes across thread counts.

#### 9c — Collect decisions and candidates, not periodic positions

Add a `DecisionTrainingRecord` at every meaningful decision root:

```
run/game/decision identity
format, player count, acting seat, team ids
masked root observation
legal candidate descriptors
one-ply quiet state per candidate
candidate-minus-root feature delta
shared sampled-world ids and shared rollout seeds
static score, rollout mean/variance, terminal result per candidate
search allocation / visit count
chosen action
eventual game result
terminal placement and per-seat/team utility vector
```

The engine remains authoritative: the learner ranks only candidates emitted by
`LegalActionEnumerator`. Targets, modes, costs, and decisions are represented structurally; a model
never invents an action string.

The first training target is pairwise preference:

```
P(A better than B) = sigmoid(score(root, A) - score(root, B))
```

Use shared determinizations and common random numbers when comparing A and B. This labels the
**effect of acting**, unlike final-result regression, and reduces variance between candidates.

The record has one schema for duels and pods. In 1v1, the utility vector is constrained to zero-sum
`[u, -u]`. In free-for-all games it stores every player's placement/utility rather than prematurely
collapsing the result to `won = 0/1`. Candidate targets are always from the acting player's perspective;
the raw vector remains available so later work can learn threat assessment, kingmaking risk, and
opponent responses without recollecting games.

Exit: replaying every record reproduces its legal candidates and quiet-state digest; swapping player
perspective negates every antisymmetric feature delta; a free land-drop fixture is labeled above pass.

#### 9d — Build the search teacher and exploratory corpus

The teacher is the current rollout stack with a larger offline-only budget, not a new execution
engine. At each decision it searches a bounded candidate subset and stores the full normalized score
distribution, not only the argmax.

Collect from a reproducible mixture:

- current production/champion policy — realistic states;
- rollout teacher — stronger states and targets;
- temperature/epsilon exploratory policy — recovery and off-policy states;
- frozen older champions — prevents the corpus collapsing onto one policy and exposes
  non-transitive matchups.

Start a small multiplayer shadow stream as soon as the record validator passes. A default compute
allocation is **80% duel / 20% multiplayer**, adjustable from measured learning curves. Pod games use
randomized seat order and opponents sampled independently from the champion/league population; never
fill every opposing seat with the same latest policy exclusively. The shadow stream is not a pod
promotion gate yet—it prevents the shared encoder and policy from silently specializing to exactly
one opponent.

Exploration samples only engine-legal, meaningful actions. Prefer softmax over teacher scores, with a
small uniform tail; store the propensity so later training can reweight the sample. Iteratively run
the apprentice in the environment and ask the teacher to label the states it actually reaches—the
same distribution-correction principle as Expert Iteration, rather than one-shot behavior cloning.

Exit: every action family with meaningful arena frequency appears in train and validation; no single
generator supplies more than half the corpus; candidate score margins have enough near-ties to teach
real ranking rather than only obvious wins.

#### 9e — Apprentice v1: interpretable ranking and value

Train shared heads over one observation/action representation:

1. **Policy/ranker:** scores each engine-supplied legal candidate. Train on the teacher's normalized
   search distribution plus pairwise candidate comparisons.
2. **Value:** emits a variable-player utility/placement estimate from the viewer's masked observation.
   The duel adapter reads the acting player's zero-sum value; the pod adapter reads the acting player's
   expected utility and may also consume the per-opponent outputs for threat estimates. Train on a
   mixture of terminal result and teacher search value; balance by game phase so nearly decided late
   states do not dominate.

Use permutation-equivariant aggregation for opponents: the prediction must not change when two
opponent seat labels are swapped while their states are swapped with them. Keep player identity only
where rules require it (turn order, teams, attack direction, choices already made). This lets the same
weights run with two, three, or four players instead of padding a duel-specific network into a pod.

Start with regularized linear or generalized-additive models over candidate deltas. They are fast,
JSON-loadable, and auditable. Do not advance a model whose coefficient/invariance report fails:

- free land development cannot be worse than retaining the same land;
- gaining life with everything else fixed cannot hurt;
- adding friendly power/toughness cannot hurt;
- removing an opposing permanent cannot hurt;
- player-perspective swap negates value;
- permuting equivalent opponent seats permutes per-opponent outputs but leaves the acting player's
  policy/value unchanged;
- removing an already-eliminated opponent does not change remaining-player predictions beyond encoded
  turn-order consequences;
- terminal wins/losses dominate finitely;
- every fitted coefficient and inference result is finite.

Only after this baseline clears the arena should representation move to a small entity/set model:
encode visible cards/permanents as an unordered collection, share card/entity encoders, aggregate by
zone/controller, and score a structured action against the state embedding. Avoid card-name one-hots;
use reusable card structure and `CardIntent` so held-out cards and sets have a path to generalize.

The entity/set model is optional, not the assumed destination. Advance beyond the linear/GAM model
only when an equal-latency arena shows a material strength gain. If needed, cap it to a small CPU-only
network, export a dependency-light JVM representation, quantize weights when that measurably helps,
and compute the root embedding once per decision rather than once per candidate or rollout node.

Exit order: action-delta invariants → held-out games → held-out decks → held-out set → puzzle suite →
100-game smoke → 300-game directional arena. Positional accuracy/log-loss cannot waive an earlier gate.

#### 9f — Close the Expert Iteration loop

Once apprentice v1 is not weaker in duels:

1. use its policy head as a prior and candidate pre-ranker;
2. use its value head at rollout/search horizons;
3. search under a fixed simulation budget;
4. store the improved search policy and value targets;
5. retrain from a replay buffer containing new data plus stratified older data;
6. challenge the champion; promote only through the standing arena gates.

Measure policy-only, value-only, and combined agents separately. Search must remain anytime and
deterministic under a fixed seed. The budget ladder must stay monotone: if more search makes the
agent weaker, improve targets/leaf value rather than adding simulations.

Use a champion/league buffer rather than latest-vs-latest self-play only. If the gauntlet shows
cycling or rock-paper-scissors behavior, add a small opponent population inspired by policy-space
response methods instead of selecting solely by Elo.

Run the same loop on the multiplayer shadow stream with a format-specific search adapter:

- duel search uses zero-sum backups;
- team games use team utility and remain zero-sum when the format is two-team;
- free-for-all search uses acting-player expected placement/match utility, preserves every player's
  value estimate, and initially uses shallow opponent-policy rollouts rather than pretending the game
  is minimax;
- policy priors, leaf evaluator, candidate representation, sampled hidden worlds, and stored search
  distributions remain shared.

Multiplayer batches update the shared trunk and policy head, then use format/player-count conditioning
in the value/output adapter. During early training, cap their gradient contribution so a noisier pod
objective cannot erase duel strength; increase it only when joint-training ablations improve pods
without breaching the duel regression budget.

Keep pod inference within the duel latency envelope. Rank all legal actions cheaply, search only the
top bounded set plus tactically mandatory actions, reuse one root/opponent encoding, and allocate a
fixed total simulation budget across opponents rather than multiplying the duel budget by player
count. A pod agent that is stronger only with 3x compute is not evidence that the shared pipeline met
its goal.

Exit: new champion's lower paired CI exceeds 50% against both the previous champion and `v0` over
1,000 duel games each; no duel gauntlet matchup falls below 45%; multiplayer shadow metrics do not
regress beyond their provisional tolerance; puzzles do not regress unexpectedly; illegal
actions/exceptions are zero; inference fits the selected decision budget.

#### 9g — Hidden-information upgrade and multiplayer specialization

Do **not** claim equilibrium play from determinized MCTS. The first loop uses several shared
viewer-consistent worlds because it is tractable, but strategy fusion and non-locality remain.

For duels, incrementally approach the ReBeL/Student-of-Games shape:

- recurrent public-history encoder;
- explicit belief over opponent hand/deck hypotheses;
- opponent-range sampling conditioned on public actions, not only deck priors;
- belief-state value/policy targets;
- continual re-solving at public decision points;
- exploitability probes in small mechanically faithful subgames where an exact solver is possible.

Gate each addition against equal-compute determinization search. Exact public-belief enumeration is
out of scope unless profiling proves a bounded abstraction can support it.

Apply the same belief machinery to each opponent independently at first, with a shared opponent-range
encoder and permutation-equivariant aggregation. Later add joint hypotheses only if profiling shows
that correlated hidden information materially improves play. This keeps the common case tractable and
lets duel improvements transfer directly to pods.

Multiplayer is a different **objective**, not a different AI stack: it is general-sum, may involve
teams, and has no single duel-style Nash target. Once the duel champion is strong and shadow results
are stable, tune the multiplayer search adapter and increase pod data without cloning the model.

The target is controlled degradation rather than identical win percentages. Compare against the same
fixed opponent league and normalize by the format baseline:

- retain at least **90% of the shared model's duel-relative strength** when moving from duel-only to
  joint training;
- joint training must beat a duel-only checkpoint in pods at equal inference compute;
- a multiplayer-specialized head is allowed only if it improves pod score materially while sharing the
  trunk, policy/action encoder, artifact, and collection pipeline;
- maintain 2-, 3-, and 4-player pod matrices across seat, deck, player count, and opponent mixture;
- report placement distribution, match/pod win rate, survival, decision latency, and per-opponent
  exploitability probes—never infer multiplayer strength from duel win rate or compare a pod result
  naively against 50%.

Promote one **universal champion artifact** when both the duel gate and the appropriate pod regression
gate pass. Difficulty profiles and decision budgets may differ by format, but the model version should
not. If the universal artifact repeatedly misses either gate, first try player-count conditioning,
loss balancing, and a small format-specific value head; splitting the entire pipeline is the last
resort and requires an arena-backed decision record.

#### Data split and promotion protocol

- Split by **game**, then hold out deck seeds and at least one entire set. Never split positions from
  one game across folds.
- Maintain an untouched final test corpus that model selection never reads.
- Stratify every split by player count and format. Keep entire games/pods together, and reserve both
  duel-only and multiplayer-only final corpora so shared-training gains cannot hide transfer regressions.
- Report action top-1/top-k agreement, pairwise ranking loss, calibration, per-action-family metrics,
  and invariance failures. Let paired duel arena win rate decide duel strength; use fixed-composition,
  repeated-seat pod comparisons with bootstrap confidence intervals for multiplayer.
- Run the puzzle suite as the localizing signal, budget ladder as the search-health signal, duel arena
  as the strength gate, gauntlet as the non-transitivity guard, and pod arena for multiplayer only.
- Version observation schema, action schema, teacher, weights, code commit, sets, and deck generator in
  every artifact. A result without reproducible provenance is not a baseline.
- Run a target-server concurrency soak before promotion. Report strength versus latency as a Pareto
  curve; reject a candidate dominated by a cheaper checkpoint even when its unconstrained arena score
  is higher.

#### Recommended execution order

```
9b data integrity
 → capture target-server latency/memory/concurrency baseline
 → 9c variable-player decision/candidate-delta records
 → 9d duel teacher + 20% multiplayer shadow collection
 → 9e shared policy/trunk + variable-player value baseline
 → 9f duel-primary Expert Iteration + multiplayer search adapter
 → 9g shared belief encoder + multiplayer specialization
 → one universal champion gated in duel and pod arenas
```

The next concrete unit is **9b + the record skeleton of 9c**. It directly prevents another
high-accuracy/0–100 failure and produces data usable by both the cheap linear apprentice and the
long-term policy/value model. Its acceptance test now includes replayable 2-, 3-, and 4-player records,
even though the first strength-optimization milestone remains 1v1.

---

### Phase 10 — Optional

- **Difficulty levels** *(1–2 d)* — nearly free once `AiProfile` + `DecisionBudget` exist. EASY = V0
  at 100 ms with an ε-greedy blunder rate; NORMAL = rollouts at 500 ms; HARD = full budget + K=2.
  Ship as `AiProfile` presets. There is currently **no** difficulty concept anywhere in the repo.
- **Replay-mined puzzles** — `ReplayReconstructor.reconstructStateAt(replay, frame)`
  (`game-server/.../replay/`) returns a full unmasked `GameState` at any frame of any recorded human
  game, with `ReplayFidelity.EXACT` verification and card pinning. Mine positions where a strong
  human and the AI diverge and promote them into the suite. Best puzzle source available — but only
  after the hand-authored core exists, and only from `EXACT`-fidelity replays.
- **Gym alignment** — fix `GameEnvironment.playGame`'s no-agent fallback (`:314`) to actually use
  `AIPlayer` as its KDoc claims; add `seed` to `EnvConfig` and stop dropping it in
  `MultiEnvService.toGameConfig` (~3 lines, and it's why `gym-self-play-testing.md` says games aren't
  seedable); add an `AIPlayer`-backed `ActionSelector`. Makes `AlphaZeroSearch` arena-benchmarkable as
  just another `ArenaAgent`. Off the critical path.
- **Full information-set MCTS** — with `PlayoutEngine`, `Determinizer`, `MeaningfulActionFilter` and
  `DecisionBudget` in place this becomes a contained change, and `gym-trainer/.../AlphaZeroSearch.kt`
  already has a working PUCT loop over O(1) `fork()`. Its gap is `StructuredDecisionResolver`, which
  collapses 12 of 18 decision types into one forced *random* edge (`:264-294`, `:408`) — targets,
  damage assignment and mana-source selection are effectively unsearched. Deliberately deferred:
  prove rollouts work first.

---

## Recommended order

```
0̶ → 1̶ → 2̶ → 3̶ → 4̶ → 5̶ → 6̶ → 2b🟡 → 7̶ → 8 → 9 → 10
```

Phases 0–8 are done — all four scoreboards exist, the evaluator is no longer one-eyed at a pod
table, the budget ladder is calibrated and monotone before any rollout depends on it, the engine's
quadratic battlefield scans are gone (−21% engine CPU, and `engine-performance.md` is now fully
closed out), the leaf evaluator can see a permanent that has no power and toughness, and the primary
strength lever is in at **57.3%, CI [53.0%, 61.7%]** against `v0`. **Phase 5 never was on the
critical path** — simulation was already ~2× the speed the rollout budget needs — so it was taken as
a standing engine win, not a prerequisite.

Next up is **Phase 9, Texel-style evaluation tuning**. Phase 8 now samples one viewer-consistent
world before any candidate simulation, so search no longer plays twenty lines against exact hidden
hand identities and library order.

Phase 7 leaves three pieces of homework of its own. **`ThreatAssessment`'s 99-turn sentinel** is now
a measured hazard rather than a suspected one (correction 1 above): it puts ±176 of uncalibrated
offset into every absolute score, and Phase 9's fit should replace it rather than rescale it.
**`staticWeight` is a fifth hand-set constant** — the mixture is real, its value is a guess selected
on 48 puzzles, and it belongs in the same logistic fit. And the **rollout arena is expensive
enough** that Phase 8's "record the determinization dip as a deliverable" needs the playout ladder to
be affordable at all.

**Phase 2b is inserted before it, by the same argument that put Phase 2 before everything.** Phase 6
took the suite to 44/48, which leaves the plan's most expensive phase with a two-puzzle localizing
signal and no coverage at all of the shapes a rollout is supposed to be good at — multi-action
lines, mid-combat responses, decisions. Measuring Phase 7 with the suite as it stands means reading
the arena alone, which is exactly the position Phase 1 was built to get us out of.

Ranked by strength-per-effort, independent of ordering:

| Rank | Phase | Effort | Why |
|---|---|---|---|
| — | **6̶** CardIntent | 5–7 d | **Done.** Was ranked 1. Removed the flat-0.5 blindness to every artifact/enchantment/PW; puzzles 39/48 → 44/48, arena neutral. Its rollout-policy and determinization-prior consumers shipped in Phases 7 and 8. |
| — | **3̶** multiplayer eval | 1–2 d | **Done.** Was ranked 1: the evaluator scored a whole pod as a duel against one arbitrary neighbour, and read a stale life component in 2HG. |
| 1 | **9** Texel tuning | 4–6 d | Replaces ~25 guessed constants with fitted ones, and each later phase raises the stakes: two of the remaining puzzle failures are *one* constant (`CardAdvantage.cardValue(0) = −3.0`), Phase 7 added a fifth guess (`staticWeight`), and `ThreatAssessment`'s 99-turn sentinel is now a measured hazard rather than a suspected one. Cheap once the arena exists. |
| — | **7̶** rollout evaluator | 6–9 d | **Done.** The real lever, and it delivered against `v0`; the Fog puzzle Phase 2 assigned to it is closed. Two surprises worth carrying forward: squashing an *absolute* board score destroys the search, and a *pure* rollout is weaker than the greedy AI it replaces. |
| 3 | **2b**🟡 puzzle suite, second pass | 4–6 d | **3 of 6 categories shipped.** No direct strength — but it took the suite from 48 to 66 and gave Phase 7 a real localizing signal where 44/48 had left two puzzles. The three unshipped categories need framework changes. |
| 4 | **1̶ / 2̶** arena + puzzles | 7–10 d | No direct strength — but nothing above is *knowable* without them. Both done. |
| — | **4̶a** auto-pass filter | 3–5 d | **Done.** Demoted by Phase 0 and rescoped to "skip the enumeration": 40% of priority windows now never call the enumerator. Also closed Phase 1's 889-of-945 illegal-action finding, which turned out to be a targeting bug. |
| — | **4̶b** DecisionBudget | 2–3 d | **Done.** Enabling infrastructure, and it measures like it — neutral in the arena, monotone in the scaling ladder. |
| — | **8̶** determinization | 5–7 d | **Done.** Search samples a shared viewer-consistent world; the paired smoke found no detectable dip and retained a 55% point estimate against `v0`. |
| — | **5̶a** hoist O(n²) scans | 3–5 d | **Done.** Demoted by Phase 0 (not a rollout prerequisite), taken as a standing engine win anyway: `findAvailableManaSources` 59% → 3.1% inclusive, −21% engine CPU. Closed `engine-performance.md` Step 4. |
| — | **5̶c** persistent collections | 4–6 d | **Dropped**, gate checked. Post-5a profile puts the allocation cluster at ~2% (`Arena::grow` 1.37%). The top leaf is now `PredicateEvaluator.matchesCardPredicate` at 20.4% self. |
| — | projection incrementalization | 2+ wk | **Skip.** 7.4% in the profile, 11% measured cold, and already cached. |

Phases 0–2 are ~10 days of pure infrastructure before any strength lands. That is the correct trade:
without them every subsequent claim is unfalsifiable. Phase 0 already earned its keep by deleting
two phases' worth of assumed work.

---

## Risk register

| Risk | Detection | Mitigation |
|---|---|---|
| ~~**Search makes the AI slower AND weaker**~~ | `ArenaBudgetScalingTest` — strength must be monotone in budget | **Instrument built and calibrated in Phase 4, before rollouts exist, and it passes**: 55.7% / 54.0% / 55.3% up the ladder, every lower CI bound above parity. Still the standing gate for Phase 7 — non-monotone ⇒ rollouts are noise, and the fix is the leaf evaluator (6, 9), not more samples |
| **Determinization dip read as a regression** | Certain to happen | Named in advance; Phase 8's bar is "still beats V0", not "beats the cheating version" |
| **Overfitting to self-play** | Held-out set + gauntlet + puzzles | ≥3 collecting agents; hold out by game *and* by set; arena is the arbiter, not log-loss |
| **Non-transitive strength** | Full pairwise matrix, not just Elo | Must beat V0 *and* previous version; lose to no gauntlet member worse than 45% |
| ~~**Combat's 1 s cap fights the global budget**~~ | Blocking puzzle category; `DecisionBudgetTest` | **Closed in Phase 4b** — combat declaration is always CRITICAL and `MAX_BLOCK_SIMULATIONS = 10` is a floor on every tier, asserted directly, so combat can never search *less* than it did before the budget existed |
| ~~**`GameSimulator.isResolving` / `decisionResolver` thread-safety**~~ | Nondeterminism across arena reruns at the same seed | **Closed in Phase 7.** One `AIPlayer` per *seat* per game, never shared, and `PlayoutEngine` owns its own `ActionProcessor`, enumerator and `CombatAdvisor` as required. `ArenaHarnessTest` (identical at 8 threads and at 1) and `FrozenBaselineTest` are both green with rollouts on, which also proves the search reads no clock |
| **A rollout search quietly becomes irreproducible** | `ArenaHarnessTest`; `RolloutCandidateEvaluatorTest` asserts scores are a pure function of the position | Every playout seed derives from the root state and every allocation from `SearchAllowances.rolloutPlayouts`. `DecisionBudget.expired()` is a safety stop only — a search that spent wall clock would play a different game under load than idle |
| ~~**Averaging a `Double.MAX_VALUE / 2` win score**~~ | Certain, on the first won playout | **Closed in Phase 7** — `WinProbability` squashes to a probability, averages there, and converts back; a proven win is ±55 raw points, which dominates every board score finitely |
| **Squashing an *absolute* board score** | Every candidate scores identically at the clamp; puzzles collapse to "chose PassPriority" | Squash the **delta from the decision's root**, which is shared by all candidates. The absolute scale is uncalibrated: `ThreatAssessment`'s 99-turn sentinel puts an ordinary position at −176. Cost the phase 7 puzzle points before it was found |
| **A rollout mean cannot see tempo** | Removal and Disenchant puzzles fail by passing | Passing advances a step, not the turn, so the playout re-casts the declined spell and the lines converge. `RolloutSettings.staticWeight` mixes the static leaf back in; it is its own control at 0.0 and 1.0 |
| **A pod result is read against 50%** | Certain to happen — every other number in this plan is | The null is **1/teams**: 33% at `ffa3`, 25% at `ffa4`, 50% at `2hg`. `ArenaReport.podSummary` prints the null on the same line as the win share and states it in the verdict sentence |
| **A multiplayer harness trusts `GameState.turnNumber`** | Used to read every pod game as wedged after the first elimination | `turnNumber` was a round counter that only advanced for `turnOrder.first()`, who may be dead. **Closed**: it counts player turns now, so it is a sound clock at any table size (`backlog/multiplayer.md`) |
| **Persistent collections break persisted sessions / committed replays** | `GameStateSerializationFormatStabilityTest` golden JSON | Serializers delegate to standard `MapSerializer`/`ListSerializer` ⇒ byte-identical wire format |
| **`CardInstantiator` extraction produces malformed cards** | `DeterminizerInvariantsTest` + full engine suite | Reuse `GameInitializer`'s own construction path, don't hand-roll |
| ~~**Arena wall-clock makes the merge gate unaffordable**~~ | Measured in Phase 1, re-measured in Phase 4b | **Closed.** 1,000 games is still ~3.5 min *with* a budget, because `SearchAllowances` spends a tier as a count of simulations rather than as wall clock. The reduced-budget arena mode was never needed and was not built |
| ~~**`AiProfile.LEGACY_V0` silently drifts**~~ | `FrozenBaselineTest` golden action-stream hash | **Closed in Phase 1** — one fixed all-vanilla Portal game, SHA-256 over the action stream. All-vanilla so the hash tracks *AI* behaviour, not every card that ships |
| ~~**`respondBudgetModal` zero-cost infinite loop**~~ | Arena stuck-game detector | **Closed in Phase 0** — free modes are taken once; regression test committed |
| ~~**`Searcher.kt`'s `Double.MIN_VALUE/2` gets accidentally revived**~~ | — | **Closed in Phase 0** — file deleted rather than repaired |

---

## Verification

Per phase, in addition to the exit criteria above:

- **Build gates:** use the `verify` skill. Phases touching `rules-engine` (4a, 5a, 5c, 8a) need the
  full `:rules-engine:test` + `:game-server:test` run, not just `:ai:test`. Always via `just`, never
  raw `./gradlew` — parallel agents each spawn their own daemons and thrash the box.
- **Perf validation loop** (from `engine-performance.md`): after each perf step, `just test-rules` →
  `just benchmark-random 200 BLB` **against a same-session run of the pre-change tree**, never
  against a figure recorded months ago → re-profile with async-profiler and confirm the targeted
  leaf shrank *and that nothing new appeared*. Phase 5a's first cut passed the "targeted leaf
  shrank" half and failed the second, and only the benchmark caught it.
- **SDK docs:** no card-SDK surface changes are expected. If any phase adds an SDK primitive,
  `docs/card-sdk-language-reference.md` updates in the *same* change.
- **New docs:** `docs/ai/baseline-metrics.md` (Phase 0, appended per phase) ·
  `docs/ai/measurement.md` (Phase 1 — how to read an arena report; the promotion rule) ·
  `docs/ai/architecture.md` (profile / budget / evaluator seams, once Phase 7 lands).
- **End-to-end sanity:** after Phases 7 and 8, play a real game via `just server` and confirm decision
  latency feels right and the AI no longer plays around cards it shouldn't know about.
- **Standing regression set** after each merge: `just arena-puzzles` (seconds, from Phase 2) +
  `just arena <prev> <new> 1000` (the gate) + `just arena <new> v0 1000` (the compounding check).
  Both 1,000-game runs are ~3.5 min each today. Add `just arena-pod ffa3 <new> v0 300` (~10 min,
  from Phase 3) whenever the change touches evaluation — it is the only run that exercises more than
  one opponent.

---

## Cross-reference: `engine-performance.md` is now fully closed

Verified against the code on 2026-07-28:

| Step | Status |
|---|---|
| 1 — remove kotlin-reflect from component keys | **Done** |
| 2 — key components by `Class<*>` | **Done** — `ComponentContainer.kt:23` is `Map<Class<*>, Component>`, lookups at `:29/:45/:52` use `T::class.java` |
| 3 — memoize `getBattlefield()` | **Done** — `GameState.kt:808` returns a `by lazy cachedBattlefield` built in one pass |
| 4 — hoist battlefield scans in ward / trigger / mana detection | **Done** — this plan's Phase 5a; `ManaStaticsIndex` + `BattlefieldStaticsIndex` |
| 5 — reduce component-map copy churn | **Dropped** — this plan's Phase 5c; gate checked, `Arena::grow` is 1.37% |

The `~404 actions/sec/thread` figure in that doc is **pre-Steps-1–3 and not comparable to anything
measured since** — `GameState.turnNumber` counts player turns rather than rounds now, and the
implemented BLB pool has roughly doubled, so the benchmark describes a different workload. Both docs
now say so at the point where the number appears. Compare same-session runs and use the profile to
explain the delta.

**The next perf item is not in that document.** `PredicateEvaluator.matchesCardPredicate` is the top
leaf in the post-5a profile at **20.4% self**, more than 3× the next entry, reached from both the
enumerator and every filter match. It is a different shape of problem from Steps 1–5 — the per-call
cost of the predicate language rather than a redundant scan — and wants its own analysis.
