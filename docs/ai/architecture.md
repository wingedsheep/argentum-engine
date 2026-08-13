# Engine AI — Architecture

The seams the engine AI is built from, and which phase of
[`backlog/engine-ai-improvement.md`](../../backlog/engine-ai-improvement.md) put each one there.

Read this before changing how the AI *decides*. For how to measure a change, see
[`measurement.md`](measurement.md); for the numbers, [`baseline-metrics.md`](baseline-metrics.md).

---

## The one-paragraph version

An `AIPlayer` is a `GameSimulator` ("what happens if I do X?"), a `BoardEvaluator` ("how good is
this?"), a `Strategist` ("which action?") and a `DecisionResponder` ("how do I answer this
prompt?"). Everything a phase adds hangs off an **`AiProfile`** — a named, reproducible
configuration that the arena treats as an agent. `AiProfile.LEGACY_V0` pins every feature off and
reproduces the original greedy 1-ply AI by construction; it is the permanent reference opponent and
must never be "improved".

---

## The four seams

| Seam | Question it answers | Added by |
|---|---|---|
| `AiProfile` | which version of the AI is this? | Phase 1 |
| `MeaningfulActionFilter` | is this window worth thinking about? | Phase 4a |
| `DecisionBudget` / `BudgetPolicy` | how hard should this decision be searched? | Phase 4b |
| `CandidateEvaluator` | how is a candidate's resulting position scored? | Phase 7 |

Plus `IntentCatalog` (Phase 6), which is card knowledge rather than a control-flow seam: it feeds
`BoardPresence.permanentValue`, `TargetSelection.rank`, `HoldPolicy` and the rollout policy's
priors.

Multi-face cards make "what does this card do?" and "what is this permanent doing?" different
questions, so the catalog answers them separately and callers must pick deliberately. A card in
hand or on the stack is `forName` — every face unioned, because casting the Adventure half is a
real option. A permanent on the battlefield is `forPermanent`, which reads only what is in force
there: a Room (CR 709.5) contributes one reading per *unlocked* door, and everything else drops the
spell faces of an Adventure / Omen / modal DFC, which resolve away and never stand on the board.
Reaching for `forName` on a battlefield permanent is the bug both of those exist to prevent.

Phase 9 adds a second leaf implementation behind the existing `BoardEvaluator` seam. Composite
profiles load from `eval-weights.json`; fitted raw-feature profiles load from
`raw-eval-weights.json`. `AiProfile.evalWeightsId` selects either kind, and malformed raw profiles
fall back to the compiled composite default. The raw evaluator extracts projected battlefield facts
once per evaluation and applies the fitted linear vector; it is internal AI behavior with no SDK,
server, or client contract.

---

## A decision, end to end

```
AIPlayer.chooseAction(state)
 ├─ MeaningfulActionFilter.canAutoPassWithoutEnumerating  → PassPriority, no enumeration  (Phase 4a)
 └─ Strategist.chooseAction
     ├─ combat declaration?  → CombatAdvisor (seed from CombatSeed, then local search)
     ├─ candidates           ← MeaningfulActionFilter.filterMeaningful                    (Phase 4a)
     │   └─ one candidate per plausible {X}  ← XCostSelection
     ├─ budget               ← BudgetPolicy.budgetFor(state, player, candidates)          (Phase 4b)
     ├─ pass 1: simulate each candidate (and the pass) to its quiet state
     │           └─ drop leaves that repeat a position we already acted from  ← StateProgress
     │               (re-aiming an inert one by simulation first  ← Strategist.materialize)
     ├─ pass 2: CandidateEvaluator.scoreAll(root, leaves, player, budget)                 (Phase 7)
     ├─ pass 3: HoldPolicy timing delta + CardAdvisor override, in raw evaluator units    (Phase 6)
     └─ best > pass ? commit targets (refined by simulation) : pass
```

The three passes are the shape Phase 7 needed. The evaluator sees **every** candidate at once, which
is what lets it *allocate* effort rather than spend a fixed amount per candidate — sequential
halving is impossible with a one-candidate-at-a-time API. For `StaticCandidateEvaluator` the batch
is `map(::score)`, so `LEGACY_V0` is bit-identical; `FrozenBaselineTest` is what proves it.

---

## Choosing `{X}` (`XCostSelection`)

X is announced as part of casting (CR 601.2b), so the enumerator — which runs *before* the cast —
cannot pick it. It hands over `maxAffordableX`, `minX`, and a deliberately permissive
`validTargets`, and leaves the choice to whoever is casting. A human makes it in the client's
X-selection phase; the AI makes it here, by expanding the action into one candidate per X worth
simulating and letting the ordinary scoring passes decide.

Not doing this is not a mild inefficiency. An `ActivateAbility` with no `xValue` at least reaches
the engine's own choose-X pause, but a `CastSpell` has no such pause: X binds to 0 as the spell goes
on the stack, so an unexpanded Mind Spring draws nothing and an unexpanded Day of Black Sun destroys
nothing. Every X spell in a deck is dead weight until it is expanded.

Which values are worth a simulation depends on whether X gates the targets:

- **X gates target legality** ("mana value X or less", "mana value X", "power X" — Blue Sun's
  Twilight, Repeal, Spell Blast, Ent-Draught Basin): the candidates are the values that make some
  currently-legal target legal, biggest first. Sweeping the affordable range would mostly produce X
  values nothing matches. For the "or less" form the candidate is the target's own mana value — a
  larger X hits the same permanent for more mana, so it is dominated.
- **X is free of the targets** (Fireball, Genesis Wave, "up to X target creatures"): more X is more
  effect, so the top `MAX_X_CANDIDATES` affordable values are the interesting ones. X=0 is never a
  candidate — it is the enumerator's own default.

This reads four `xConstrains*` flags off the enumerated action, so a gate the enumerator does not
surface is a gate that silently does not exist: an action missing its flag falls into the sweep and
casts for the wrong X. `CastSpellEnumerator` and `CastFromZoneEnumerator` populate all four for the
same reason `ActivatedAbilityEnumerator` does — the client's X-selection phase reads exactly the
same flags.

Each candidate then carries a target list **narrowed to that X**, mirroring `applyXFilters` /
`resolveMaxByX` in the client's `pipelinePhases.ts`. The enumerator is permissive on purpose and
whoever binds X owes the narrowing; without it the AI would pick a target the server then rejects.
Mana values come out of the projection, not the printed cost — a face-down permanent has none
(CR 708.2a), which is what the engine's own filter applies. An X that empties a mandatory target
slot is dropped rather than offered, and an action with no legal X at all is dropped entirely —
submitting the bare action would cast it at X=0. The `MAX_X_CANDIDATES` cap is applied to what
survives narrowing, so an uncastable value costs a filter rather than a simulation slot.

Two callers cannot afford the simulation and take a single X instead: the Strategist's
one-legal-action shortcut binds the best-looking candidate (`bindBestX`), and `PlayoutPolicy`
*samples* one (`sampleX`) — a playout that always picked the largest affordable X would make all R
playouts of that line identical, which is the one thing a rollout policy may not do.

Two shapes keep their own rule. A **targeted activated ability** is never pre-bound: submitted bare
it reaches the engine's choose-X pause, which `DecisionResponder` answers by simulating each value.
The **Momir avatar** keeps its own candidate rule (`Strategist.momirXCandidates`): its X is a
format-strategy question — skip the weak early activations, then aim at the 8-drop band — not a
"more is better" sweep.

---

## Going in circles (`StateProgress`)

A leaf score cannot tell that it is being handed the same position over and over, and the comparison
it makes is not even between comparable positions: passing carries the game forward into whatever
was about to happen, while a free ability that resolves back onto the same board stops right where
it is. When what is about to happen is bad, *doing nothing* can outscore passing — and then it does
so again from the identical position it just produced. That is how the AI came to activate Aphetto
Alchemist ({T}: untap target artifact or creature, aimed at itself) eleven times in a row.

`StateProgress.digest` reduces a `GameState` to the position a player could point at — zone
contents, everything true of the objects and players in them, turn and step — and is deliberately
blind to "it happened" memories such as activation counts, which change on every activation whether
or not anything else did. It names what it *excludes* rather than what it reads, so a `GameState`
field added later counts by default: a field missed by a read-list would make a real action look
inert, and an action that looks inert is refused forever.

`Strategist` drops any candidate whose leaf digest matches the position it is acting from, or one of
the last 32 positions it has acted from. Target refinement ranks an inert targeting below every real
one, so an ability is only dropped when *no* target does anything — and because that refinement is
the first thing a sub-`NORMAL` budget gives up, `Strategist.materialize` buys it back for exactly
the candidates the cheap pick made inert. Without that, the quiet opponent's-turn windows this guard
exists for would be the ones where it fires on an ability that had a productive target. All of it
follows CR 732.3: a player whose actions have reached the same game state again must make a
different choice.

The guard is **not** behind an `AiProfile` flag, which is the one thing this codebase normally
insists on — see `StateProgress`'s KDoc for why an abandoned game has no strength worth freezing,
and why `FrozenBaselineTest` staying green is not evidence either way.

---

## Hidden information (Phase 8)

Rollout profiles may enable determinization. Before any candidate is simulated, `Strategist` asks
`Determinizer` for one world consistent with the acting player's view and uses that same root for
every candidate. Sampling before the one-ply simulation matters because pending decisions reached
by that simulation can inspect hidden zones too. Sharing the world is essential: independently
sampling candidates would make hidden-information variance look like move quality.

`Visibility` in `rules-engine/view` is the common oracle for both client masking and AI
determinization. The determinizer rewrites identities, never entities: entity IDs, zone membership,
pending decisions, targets and continuations remain intact. Individually revealed cards and cards
carrying runtime state are pinned. With a known decklist it samples from `decklist − seen`;
otherwise it permutes the existing hidden multiset. That fallback removes exact hand and
library-order knowledge, but remains “cheating-lite” because it knows which cards exist unseen.

Phase 8 starts with one shared determinization per search. More worlds compete directly with rollout
count, so raising K requires an arena result showing it buys more strength at the same wall-clock
budget.

---

## Scores are in raw evaluator units, everywhere above the leaf

The pass comparison, the hold policy's `±1.5`, and a `CardAdvisor` returning `defaultScore + 2.0`
are all written in `BoardEvaluator` units. So `CandidateEvaluator.score` returns those units too,
whatever it did internally. `RolloutCandidateEvaluator` averages in probability space — it must, see
below — and converts back at its own boundary. New mechanism underneath the old vocabulary.

---

## The rollout evaluator (Phase 7)

```
RolloutCandidateEvaluator          allocates the budget, owns the seed grid
  └─ PlayoutEngine (Playouts)      plays one short game forward
      ├─ PlayoutPolicy             what a player does at a priority window — never simulates
      │   ├─ CombatSeed            the heuristic half of CombatAdvisor
      │   └─ TargetSelection       the heuristic half of the Strategist's target pick
      └─ FastDecisionResponder     O(1) answer per PendingDecision type
```

**`PlayoutEngine` owns its own `ActionProcessor` and `LegalActionEnumerator`.** Not tidiness:
`GameSimulator.isResolving` is a mutable recursion guard and `decisionResolver` is a mutable `var`,
so a playout sharing one would corrupt the guard of the simulation that spawned it.

**Nothing inside a playout may simulate.** A playout that simulates is quadratic, and cheapness is
the entire reason a rollout mean beats one static evaluation. That is why `CombatSeed` and
`TargetSelection` exist as extractions rather than as calls into `CombatAdvisor` and `Strategist`.

**The policy must be stochastic.** A deterministic policy makes all R playouts of a candidate
identical, collapsing R samples into one.

### Three things that are easy to get wrong

**1. Averaging must happen in probability space.** `CompositeBoardEvaluator` returns
`Double.MAX_VALUE / 2` for a won game, so the mean of "one win and three even boards" overflows to a
number that beats everything. In probability space those four samples are `0.625`, which is what
"one line in four wins outright" means. `WinProbability` is the conversion.

**2. Squash the *delta from the decision's root*, never the absolute score.** The evaluator has no
calibrated zero: `ThreatAssessment` prices "we can never kill them" with a 99-turn sentinel, so an
ordinary position where one side has no creatures scores around −176 while a close board is single
digits. Squashing absolutely pins every candidate to the same clamped extreme and the search
reports *certain loss for every line* — measured, on the puzzle suite, before the subtraction
existed. Because the baseline is the decision's root it is identical for every candidate, so the
offset cancels and only the differences the Strategist compares survive.

**3. A pure rollout is weaker than the greedy AI it replaces.** Passing in your own main phase does
not end the turn, it advances a step — and the playout policy then casts the very spell you just
declined. Two turns downstream the two lines have converged, the rollout mean cannot see the tempo
difference, and the tie goes to passing (48/66 against `v0`'s 55/66).
`RolloutSettings.staticWeight` mixes the static leaf back in; the two estimators are blind to
different things and the mixture recovers both.

**4. The horizon has a price, and it shows up as impatience.** `respond-02` — "do not spend the only
Counterspell on a 2/2 with seven lands still open" — is a puzzle `v0` solves and the rollout does
not. Countering shows a concrete gain inside the two-turn horizon; the cost of not having the card
later falls outside it. A longer horizon is not the fix. Knowing what a card is *for* is, which is
`CardIntent`/`HoldPolicy` territory — `production`, which has both, keeps it.

### Reproducibility

`ArenaHarnessTest` asserts identical outcomes at 8 threads and at 1, and `FrozenBaselineTest` hashes
`LEGACY_V0`'s action stream. Both survive Phase 7 because **nothing in the search reads a clock or a
counter**: every playout seed derives from the root state, and every allocation from
`SearchAllowances.rolloutPlayouts`. `DecisionBudget.expired()` is consulted only as the hard safety
stop it was designed to be. A rollout search that spent wall clock would produce a different move
under load than idle, and every arena rerun would be a different game.

---

## Where the cost is

A rollout decision costs *N* playouts × ~40 engine actions, so at the ~60 a 2 s tier affords it is
~2,400 `process()` calls and a rollout arena game is ~1,000× a `v0` game (~70 s against ~0.07 s).

**It does not need them.** The ladder (`v0-rollout-4/8/16/32`, via `RolloutBudgetPolicy`) measured
strength rising from 4 to 8 playouts and then flat: 8× more buys nothing (4-vs-32 is 50.7%, CI
[47.5%, 53.7%] over 400 games). The rollout term is bias-limited, not variance-limited — it carries
a quarter of each score, common random numbers already pair its comparisons, and no amount of
sampling reveals the tempo it cannot see. `SearchAllowances.NORMAL_PLAYOUTS` therefore ships at
**16** rather than 64.

That ladder is also Phase 7's safety net, the analogue of `ArenaBudgetScalingTest` one level down:
strength must never *fall* with more playouts, or the search is generating noise rather than signal.
Saturation is fine; inversion is the alarm.

---

## Watching a decision happen (local testing mode)

Everything above is invisible from inside a game: `Strategist` computes a
`List<Pair<LegalAction, Double>>`, takes the max, returns the winner, and the numbers go out of
scope. When the AI makes a move that looks wrong, "wrong" is all you can observe.

`AiInsightSink` (in `ai/insight/`) is the one place those numbers escape. `Strategist` publishes the
options it scored, `CombatAdvisor` publishes the attack/block plans its local search simulated, and
each record carries the `GameState` the decision was made from:

```
Strategist.chooseAction ──┐
                          ├── AiInsightSink.record(state, AiDecisionInsight)
CombatAdvisor local search ┘
```

Three properties are load-bearing:

- **It is the real decision, not a re-run.** The sink is fed from the same locals the choice was made
  from, so a captured ranking cannot disagree with the move — which is exactly what a
  recompute-for-display would eventually do, and only under the conditions you were investigating.
- **It is free when off.** The sink is null in production and every recording site is behind a null
  check. No extra simulation, no extra scoring; `AiProfile` is untouched.
- **The dropped candidates are in it too.** An action that failed materialization, walked back into a
  position already acted from (`StateProgress`), or fell off the end of the budget is recorded with
  the reason. Without them the panel silently implies the AI never considered a line it in fact
  discarded — the most misleading thing a debugging view can do.

Scores are raw evaluator units (see above), so an option is reported as an **advantage over the
baseline** — passing priority, or declaring no attackers — which is the threshold it actually had to
clear. `AdjustedScore.note` says when a hold-policy verdict or a `CardAdvisor` override, rather than
the board score, is what decided it.

The game server records into `AiInsightService` and serves `/api/dev/ai-insight/{playerId}`, mounted
only under `game.ai.insight-enabled` (on in `application-local.yml`). The web client's `AiInsightPanel`
browses it live and exports `(board state, ratings)` bundles; the `state` in an export is a full
`GameState`, so a position that produced a bad rating also re-opens through
`POST /api/scenarios/from-state`. The export is unmasked by design — which is why the flag is separate
from `game.dev-endpoints.enabled` and off by default.

### Stepping and overriding

Reading a ranking after the fact answers "what did it think?". **Step mode** answers the more useful
question, "what if it had done something else?".

Each `AiActionOption` carries the concrete `GameAction` the AI would submit for it — already
materialized, already simulated — so an option is not merely a label but a *playable* move. With step
mode on, `AiActionGate` holds the AI between choosing and submitting, and the panel can hand the
engine any option's action in place of the AI's own. The line then plays out for real rather than
being argued about.

Three constraints shape it:

- **The gate runs after the decision is recorded**, so a held game is showing a finished ranking, not
  a blank panel.
- **Only submittable options are offered.** A candidate the processor already rejected is still
  listed (the AI did consider it) but carries no action, so the panel can never offer a move the
  engine would refuse. `AiInsightCaptureTest` pins this by submitting every offered option through a
  real `ActionProcessor`.
- **It can never wedge a game.** Every path that isn't an explicit human choice — timeout, no
  recorded decision, seat mismatch, step mode toggled off, history cleared — falls back to the AI's
  own pick. Step mode is also keyed by *seat* and gated on a recent poll: a closed tab reads as
  "nobody is watching" and the game plays on at full speed.

An override is stamped on the recorded decision and travels into the export as `humanOverride`, which
is the pair actually worth training on: the position, what the AI ranked highest, and what a human
played instead.
