# Lorwyn Eclipsed AI training and promotion plan

**Set:** Lorwyn Eclipsed (`ECL`)  
**Status:** Ready for implementation after the Phase 9b/9c training-record work is present in the
working tree  
**Parent roadmap:** [`engine-ai-improvement.md`](engine-ai-improvement.md), Phases 9b–9f  
**Training-data contract:** [`../docs/ai/training-data.md`](../docs/ai/training-data.md)

## Objective

Produce an AI profile that is materially stronger in Lorwyn Eclipsed Limited at equal inference
compute, without changing engine legality, replacing the existing production AI, or regressing the
universal profile silently.

The first deliverable is an **ECL-specialized apprentice selected explicitly for ECL games**. It should
reuse the shared observation, structured-action, evaluator, and search pipeline. Set conditioning or a
small ECL coefficient overlay is allowed; an ECL-only execution engine, action generator, or collection
format is not.

## Boundary

The engine remains authoritative. The learner may rank only fully specified actions emitted by
`LegalActionEnumerator`; it never generates action strings, targets, modes, costs, combat assignments,
or decision responses. Training and collection are offline. Production inference loads one small,
immutable artifact and must continue to work when no trained artifact is installed.

This plan trains gameplay, not drafting or deck construction. `Draftsim` ratings and the ECL entries in
`SetArchetypes` may generate varied, plausible decks, but draft-pick and deck-build model training are
separate projects.

## Existing assets to preserve and reuse

- Set code `ECL` and its card definitions under
  `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/ecl/`.
- ECL Draftsim ratings at `ai/src/main/resources/draftai/ratings/ECL.json`.
- Seven ECL Limited archetypes in `SetArchetypes`: Kithkin, Merfolk, Faeries, Boggarts, Elementals,
  Elves, and Changelings.
- `DecisionTrainingRecord`, `DecisionRecordFactory`, `DecisionRecordReplayer`, and
  `TrainingCorpusValidator` under `ai.training`.
- The current static evaluator, rollout stack, determinizer, decision budgets, arena, pod arena, puzzle
  suite, and production/v0 agents.
- The measured production baseline in `docs/ai/training-data.md`.

Do not rewrite these seams merely to make a training script convenient. Extend them generally where a
real gap exists.

## Success criteria

An ECL apprentice is promotable only when all of the following hold:

1. **Data integrity:** every accepted game completes with no illegal action, fallback recovery,
   exception, wedge, or incomplete result.
2. **Replay:** every retained decision reproduces the masked root digest, exact legal candidate set,
   and each candidate quiet-state digest.
3. **Privacy:** no opponent hand or library identity appears in observations, targets, sampled-world
   labels, logs, or fitted artifacts unless it was publicly revealed to the acting player.
4. **Determinism:** identical run/game seeds produce identical ordered record hashes at one and many
   collection threads.
5. **ECL strength:** over at least 300 paired seat-swapped ECL games, the apprentice's lower paired 95%
   confidence bound is above 50% versus the current production ECL profile.
6. **Regression:** no fixed non-ECL matchup in the regression matrix falls below 45%, unless the model
   is shipped as an explicitly ECL-only overlay that cannot be selected elsewhere.
7. **Puzzles:** no unexplained regression in the existing puzzle suite; add ECL-specific puzzles for
   failures found during collection and arena evaluation.
8. **Runtime:** at the same concurrency and decision profile, p95 latency is at most 1.25× baseline,
   allocation rate is at most 1.25× baseline, process memory is at most 128 MiB above baseline, and the
   artifact is below 25 MiB.
9. **Safety:** illegal actions and engine exceptions remain zero in promotion arenas.

The current provisional limits from the measured baseline are p95 ≤ 10.941 ms and allocation rate
≤ 508.3 MiB/s. Re-run the benchmark on the target deployment host before final promotion.

## Required deliverables

- Reproducible ECL collection command and run manifest.
- Fully specified decision/candidate collector integrated at meaningful decision roots.
- Search-teacher labeller using shared determinizations and common rollout seeds.
- Corpus validation and replay command with machine-readable report.
- Immutable train/validation/test split manifest grouped by whole game.
- Training script for the interpretable apprentice, with locked dependencies.
- Finite, versioned, JSON-loadable JVM artifact.
- ECL arena agent/profile plus an equal-compute control.
- Metric report containing data composition, invariants, offline metrics, puzzles, arenas, and runtime.
- Focused tests for every new collector, validator, encoder, loader, and profile-selection behavior.

## Phase A — Audit ECL readiness before collecting labels

### A1. Verify set completeness and card behavior

1. Resolve `ECL` through `MtgSetCatalog`; record canonical card, reprint, token, and basic-land counts.
2. Run the existing set/card snapshot and AI suites through `just`; never invoke raw Gradle.
3. Run at least 300 unlabelled production-vs-production ECL games with fixed seeds and paired seats.
4. Report completion, illegal-action histogram, exceptions, draw reasons, turn/action distributions,
   and action-family frequencies.
5. Localize every illegal action or wedge to the engine, enumerator, or AI. Fix through `add-card` for a
   single card and `add-feature` for shared behavior. Add a focused regression test before continuing.
6. Re-run the same 300 seeds. The gate is zero illegal actions, exceptions, recoveries, and wedges.

Do not retain labels from failed or recovered runs. The entire completion-biased prefix of a failed
collection run is quarantined as diagnostics.

### A2. Establish the ECL performance baseline

Run the production benchmark on ECL after warm-up and record:

- decision latency p50/p95 and mean;
- bytes allocated per decision and allocation rate;
- peak used heap and process memory;
- concurrent games/second at the deployment concurrency;
- decisions, legal actions, and meaningful candidates per game;
- quiet-state simulations/second;
- completion and error counts.

Append an ECL subsection to `docs/ai/training-data.md`. All later candidate measurements must use the
same seeds, hardware, concurrency, and compute profile.

### Exit A

- [ ] 300/300 paired ECL baseline games are clean.
- [ ] ECL runtime baseline is committed.
- [ ] No known ECL engine or action-enumeration defect can contaminate collection.

## Phase B — Complete the replayable decision collector

The Phase 9c skeleton must become a collection path, not merely a serializable data class.

### B1. Define meaningful roots

Capture a root only when the acting player has a real choice:

- at least two semantically distinct, affordable actions after removing automatic bookkeeping;
- combat declarations with multiple legal plans;
- target, mode, X, payment, ordering, or other pending decisions with multiple legal responses;
- optional actions where pass/decline is a meaningful candidate.

Do not flood the corpus with forced priority passes, unique mandatory decisions, deterministic trigger
ordering, or equivalent mana-source permutations. Record skipped-root counts by reason so filtering is
auditable.

### B2. Expand fully specified candidates

For every legal root action, enumerate the complete structural choice:

- selected targets and target groups;
- modes and their per-mode targets;
- X values and variable target counts;
- alternative and additional costs;
- mana/payment choices only where strategically distinct;
- attacker-to-defender maps and blocker assignments;
- all `PendingDecision` response shapes.

Use shared engine enumeration primitives. Do not invent an ECL candidate expander beside the production
enumerator. If the engine cannot enumerate a decision structurally, add a reusable engine/AI capability
and test it across a non-ECL example too.

Each expanded candidate must process legally from the root without fallback. Illegal candidates reject
the game and produce a diagnostic record.

### B3. Mask observations and sampled worlds

- Root and candidate observations are from the acting player's perspective.
- Own hand identities and public objects are visible; opponent private zones expose counts only.
- Revealed information remains visible exactly while the engine says it is known.
- Determinized worlds may contain sampled hidden identities internally, but the persisted model input
  and label contain only a stable sampled-world id and viewer-consistent derived values.
- Opponents remain a permutation-equivariant `others` collection; seat/order features are separate and
  included only when rules require them.

### B4. Persist a complete game manifest

Store `runId`, `gameId`, ECL set/format, ordered deck hashes, seed, seat assignment, team assignment,
agent profiles, generator, schema version, completion reason, exception, recovery flag, initial-state
digest, and the replay action log or a durable reference to it. A record without enough information to
reconstruct its root is invalid.

Write atomically: stage one game's records, validate the terminal outcome, then publish the manifest and
records together. A process crash must not leave a half-accepted game.

### B5. Replay and determinism tests

Add tests that prove:

- JSON round-trip and schema rejection;
- 2-, 3-, and 4-player observation/replay compatibility;
- opponent hidden-card identities cannot change persisted observations;
- legal candidate and quiet-state digests replay exactly;
- candidate ordering and record hashes agree across thread counts;
- duplicate `runId/gameId` and decision ids are rejected;
- incomplete, recovered, exceptional, mixed-schema, non-finite, and single-generator corpora fail;
- a targeted/modal/X/combat/pending-decision fixture records fully specified candidates.

### Exit B

- [ ] Every accepted ECL record is replayable.
- [ ] Fully specified candidates cover every action family seen in the ECL baseline.
- [ ] Single-thread and concurrent collection produce identical ordered hashes.
- [ ] No production AI code allocates training objects unless collection is explicitly enabled.

## Phase C — Build the ECL search teacher

The teacher is the current rollout/search stack with a larger offline-only budget. It is not a second
rules engine.

### C1. Candidate evaluation

At each retained root:

1. Compute the masked root observation once.
2. Use the same sampled determinizations for every candidate.
3. Use common rollout seeds for candidate comparisons.
4. Simulate each fully specified candidate to its one-ply quiet state.
5. Store root and quiet features plus `candidate - root` deltas.
6. Store static score, rollout mean, rollout variance, terminal result, visit count, and allocation.
7. Store the complete normalized score/visit distribution, not only the argmax.
8. Enforce a hard offline node/time budget and retain the best completed evaluation if it expires.

Terminal wins and losses dominate finitely. A failed simulation rejects the containing game.

### C2. Required teacher sanity fixtures

- A free legal land drop ranks above passing.
- A terminal winning action ranks above every non-winning alternative.
- Obvious profitable removal ranks above passing.
- A legal ECL tribal payoff sees all relevant creature types, including Changelings.
- Shared worlds and seeds are byte-identical across candidates.
- Reversing duel perspective negates antisymmetric deltas and utilities.
- Permuting equivalent opponents leaves the acting-player score unchanged.

### Exit C

- [ ] Teacher labels are deterministic at a fixed seed and budget.
- [ ] All candidates receive at least one completed evaluation.
- [ ] Candidate distributions include useful near-ties rather than only obvious decisions.
- [ ] Teacher sanity fixtures pass.

## Phase D — Collect the ECL corpus

### D1. Generator mixture

Use at least four reproducible generators:

1. **Production/champion:** realistic state distribution.
2. **Offline rollout teacher:** stronger trajectories.
3. **Exploratory:** softmax over teacher scores plus a small uniform legal-action tail; store action
   propensity for later reweighting.
4. **Frozen older profile (`v0` or prior champion):** broader and potentially adversarial states.

No generator may supply more than half of retained decisions. Random exploration samples only legal,
meaningful actions and never bypasses target/payment/decision validation.

### D2. Deck and matchup coverage

Build a deterministic deck bank covering:

- all seven encoded ECL archetypes;
- each archetype mirror;
- every archetype against every other archetype in both seats;
- multicolor/Changeling crossover decks;
- weak, medium, and strong deck-quality bands;
- multiple mana curves and interaction densities;
- deck seeds reserved before collection for train, validation, and final test.

Do not let one heuristic sealed builder define the entire state distribution. Keep exact deck hashes
and archetype labels in the manifest.

### D3. Initial corpus size

Use staged collection rather than committing immediately to a huge run:

| Stage | Clean games | Purpose |
|---|---:|---|
| Smoke | 100 | Find schema, replay, masking, and action-expansion defects |
| Pilot | 1,000 | Inspect distributions, generator balance, archetype/action coverage, label margins |
| Apprentice v1 | 5,000 | Train the first interpretable ECL model |
| Expansion | +5,000 increments | Add only when learning curves or coverage justify it |

Begin a 20% multiplayer shadow stream only after duel replay validation is clean. Multiplayer rows use
the same schema and are reported separately; they do not gate the first ECL duel specialization.

### D4. Immutable splits

Create and commit a split manifest before fitting:

- split by whole game, never by decision;
- keep paired seat-swapped games in the same split;
- hold out deck seeds and complete decklists;
- hold out at least one archetype pairing from model selection;
- keep an untouched final ECL test corpus;
- retain a separate cross-set regression corpus from at least BLB, POR, and one mechanically different
  set;
- stratify by generator, archetype, action family, game phase, and player count.

Recommended initial proportions are 70% train, 15% validation, and 15% untouched test.

### D5. Collection report

Report games and decisions by generator, archetype matchup, seat, action family, turn bucket, candidate
count, player count, completion reason, and score-margin bucket. Include replay pass rate, row-hash
determinism, illegal actions, exceptions, wedges, and rejected-game reasons.

### Exit D

- [ ] At least 5,000 clean ECL games are available for apprentice v1.
- [ ] Every meaningful ECL action family occurs in train and validation.
- [ ] Every archetype and seat is represented; no generator exceeds 50%.
- [ ] Replay, masking, finiteness, and deterministic-hash validation pass 100%.
- [ ] Final test data has never been read by training or hyperparameter selection.

## Phase E — Train the interpretable ECL apprentice

### E1. First model

Start with a regularized linear or generalized-additive candidate ranker over reusable structural
features and candidate deltas. Do not begin with card-name one-hots or a neural entity model.

Train pairwise preferences:

```text
P(A better than B) = sigmoid(score(root, A) - score(root, B))
```

Use the teacher's normalized candidate distribution as an additional policy target. Weight examples by
stored propensity where exploration changes the sampling distribution. Balance game phases and action
families so late forced wins and common pass decisions do not dominate.

### E2. Set specialization experiments

Fit and compare three artifacts using identical data splits and feature code:

1. **Shared/unconditioned:** one universal coefficient vector.
2. **Shared + set feature:** common vector with ECL/format conditioning.
3. **Shared + ECL overlay:** common vector plus a small regularized ECL residual vector.

The overlay must use the same features and loader as the shared model. Prefer the simplest artifact that
wins the equal-compute arena. Do not ship an ECL-specific head merely because it has better training
loss.

Card-name one-hots are prohibited. Card structure, public state, action structure, and `CardIntent` are
the generalization path. If an ECL mechanic is systematically invisible, add a reusable structural
feature and prove it on another applicable set/card where possible.

### E3. Required invariants

Reject an artifact if any invariant fails:

- all coefficients, features, probabilities, and predictions are finite;
- free land development cannot be worse than retaining the same land;
- gaining life with everything else fixed cannot hurt;
- adding friendly power/toughness cannot hurt;
- removing an opposing permanent cannot hurt;
- terminal wins/losses dominate finitely;
- duel perspective swap negates value and antisymmetric deltas;
- equivalent opponent permutation leaves acting-player policy/value unchanged;
- removing an eliminated opponent changes only encoded turn-order consequences;
- ECL conditioning has no effect when the profile is selected for a non-ECL game.

### E4. Offline evaluation

Report by split, generator, archetype, action family, candidate-count bucket, turn/game phase, and score
margin:

- pairwise ranking loss and accuracy;
- top-1 and top-k teacher agreement;
- cross-entropy against the teacher distribution;
- calibration;
- invariance failures;
- coefficient/feature report;
- prediction latency and allocation.

Offline metrics nominate an arena candidate; they never authorize promotion.

### Exit E

- [ ] Artifact and training run are reproducible from committed manifests/configuration.
- [ ] All invariants pass.
- [ ] Validation improves over the current static evaluator on ECL.
- [ ] Held-out decks and archetype pairing improve, not only seen matchups.
- [ ] Artifact is finite, JSON-loadable, below 25 MiB, and dependency-light on the JVM.

## Phase F — Integrate without replacing the production AI

### F1. Artifact loader

- Load once per process and share immutably across games.
- Validate schema, feature names/count, set code, model version, and finiteness.
- Fail closed to the existing evaluator/profile when the artifact is absent, malformed, mismatched, or
  non-finite.
- Do not start Python, a GPU runtime, a model server, or a training service.
- Cache the root encoding once per decision; score candidates in a batch or allocation-light loop.

### F2. Profile selection

Add explicit arena names for the shared apprentice, the ECL-conditioned apprentice, and the ECL overlay.
Game/server selection may choose the promoted ECL profile only when the set/format is ECL. Existing
`production`, `current`, and `v0` profiles remain unchanged for controls and rollback.

Use the same `LIGHT`, `NORMAL`, and `STRONG` compute profiles and weights. Difficulty changes the search
budget, not the learned artifact.

### F3. Search integration order

Evaluate separately before combining:

1. apprentice as candidate ranker/prior only;
2. apprentice as leaf/value evaluator only;
3. ranker plus value;
4. ranker/value plus the existing bounded rollout search.

Search remains anytime and deterministic under a fixed seed. More budget must not reduce strength. If
it does, repair targets or leaf evaluation rather than adding simulations.

### Exit F

- [ ] Existing production AI behavior is unchanged unless the ECL profile is explicitly selected.
- [ ] Missing/bad artifacts fall back safely.
- [ ] Loader and inference tests cover schema mismatch and non-finite values.
- [ ] Equal-compute benchmark stays inside the runtime envelope.

## Phase G — Evaluation and promotion ladder

Run gates in order. Failure at an earlier gate stops promotion.

### G1. Local correctness

1. Training-data unit and replay tests.
2. Full `just test-ai`.
3. Existing puzzle suite.
4. New ECL puzzle pack covering tribal sequencing, Changeling interactions, flash/tempo, Wither combat,
   Blight/sacrifice decisions, Evoke, Convoke/go-wide development, graveyard use, and removal timing.

### G2. Offline and runtime gates

1. Untouched ECL final-corpus report.
2. Cross-set final-corpus report.
3. Baseline and candidate runtime benchmark on the same machine/seeds/concurrency.
4. Artifact size/load-time and fallback checks.

### G3. Arena ladder

All comparisons are paired, seat-swapped, fixed-seed, and equal-compute:

1. 100-game ECL smoke versus current production.
2. 300-game ECL directional arena versus current production.
3. 300-game ECL arena versus `v0`.
4. Budget ladder confirming monotone strength.
5. ECL archetype matrix with repeated seats, not only aggregate win rate.
6. At least 100 games per fixed non-ECL regression matchup (BLB, POR, and one other set).
7. If directional results pass, run the roadmap's larger merge/promotion gate.

Report paired confidence intervals, draws/incomplete games, seat effect, archetype matchup, action
families, latency, allocation, memory, illegal actions, exceptions, and wedges. Do not promote from
aggregate win rate alone if one major ECL archetype regresses badly.

### Promotion decision

- Promote as the **ECL profile** when ECL strength and runtime gates pass and selection is strictly
  limited to ECL.
- Promote into the **universal profile** only when cross-set arenas also pass the universal roadmap
  gates.
- Reject the artifact if it improves log-loss but loses the arena, repeats the earlier land-development
  pathology, or requires more inference compute for its strength.

Keep the prior artifact and profile selectable for immediate rollback.

## Phase H — Iterative Expert Iteration loop

After apprentice v1 is not weaker in ECL:

1. use its policy score as the search prior/candidate pre-ranker;
2. use its value at rollout horizons;
3. search under a fixed simulation budget;
4. store improved policy/value targets;
5. collect states reached by the apprentice and relabel them with the teacher;
6. retrain from a replay buffer with stratified older ECL and cross-set data;
7. challenge the current ECL champion and promote only through Phase G.

Maintain a champion/older-policy mixture so collection does not collapse onto the latest policy. Track
learning curves by corpus size; stop adding data when marginal strength does not justify collection and
label cost.

## Suggested implementation units

Keep each unit reviewable and independently tested:

1. ECL clean-run audit and baseline report.
2. Fully specified candidate expansion and replay fixtures.
3. Atomic game-manifest writer and validator CLI.
4. Search-teacher labeller with common-random-number tests.
5. ECL deck bank, split manifest, and collector command.
6. Corpus reporting and coverage gates.
7. Linear/GAM training script and invariant report.
8. JVM artifact loader and safe fallback.
9. ECL arena profiles and runtime comparison.
10. ECL puzzle pack and promotion report.

Do not combine engine bug repair, corpus collection, model fitting, and promotion into one unreviewable
change. A collector run is not evidence of model strength, and a fitted artifact is not a promotion.

## Verification guidance

Use the project skills and gates:

- Engine/AI capability work: `add-feature`.
- Single-card defects uncovered by ECL runs: `add-card`, one card and one scenario test file at a time.
- Build/test interpretation: `verify`.
- Heavy Gradle work: `just` recipes only.

Minimum code-change gate is `just test-ai`; run `just test` when candidate expansion or replay changes
engine behavior or shared legal-action enumeration. Run the relevant server suite only if profile
selection enters server orchestration. Update `docs/card-sdk-language-reference.md` in the same change
only when a real SDK primitive changes; this training plan alone introduces no SDK vocabulary.

If an unrelated working-tree change causes a failure, report it and stop. Never revert, stash, or
discard another agent's work.

## Final handoff checklist

- [ ] Phase A clean-run and ECL baseline gates pass.
- [ ] Collector records meaningful, fully specified engine actions.
- [ ] Masking and replay pass for every retained record.
- [ ] Corpus has generator, archetype, seat, action-family, phase, and deck diversity.
- [ ] Splits are immutable and grouped by game.
- [ ] Apprentice artifacts are reproducible, finite, small, and JVM-loadable.
- [ ] Invariants pass before arenas begin.
- [ ] ECL puzzles and equal-compute arenas pass.
- [ ] Runtime and safety remain inside budget.
- [ ] Cross-set behavior is measured and the selected promotion scope is explicit.
- [ ] Existing production and rollback profiles remain available.
- [ ] Results, commands, seeds, manifests, artifacts, and rejection reasons are documented.
