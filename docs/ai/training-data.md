# AI training-data boundary

## Lorwyn Eclipsed apprentice workflow

The ECL pipeline now has committed contracts for meaningful-root filtering, atomic whole-game corpus
persistence, deterministic whole-game splits, a finite linear-artifact format, and set-scoped profile
selection. Collect a smoke corpus with `just collect-ecl-training <corpus.json> 100`; incomplete,
recovered, exceptional, or illegal-action games are quarantined rather than appended, and a
machine-readable sibling report summarizes generator, set, action-family, phase, player-count, and
candidate-count coverage. Fit an artifact with `just train-ecl-apprentice <examples.json> <artifact.json>`, install it
as `ecl-apprentice.json` under a directory passed through
`-Dargentum.ai.apprentice.dir=<directory>`, then run `just arena-ecl-smoke <directory>` followed by
`just arena-ecl-directional <directory>`. Missing, malformed, wrong-set, wrong-feature, and non-finite artifacts
fall back to the existing production evaluator. The ECL profile selector also falls back to production
when asked to select an ECL-restricted artifact for another set.

These commands implement the reproducible fitting and evaluation path; they do not claim promotion.
Promotion still requires the clean 5,000-game corpus, untouched split, paired confidence interval,
runtime envelope, puzzle gates, and safety checks in `backlog/ecl-ai-training-plan.md`.

Phase 9 training records are offline artifacts. Nothing in the production `AIPlayer` constructs a
record, runs a validator, or loads a training dependency. Collection code lives under `ai.training`
and must be enabled explicitly by a collection harness.

## Production baseline (2026-08-01)

Measured with `just benchmark-throughput 20 BLB` on the target 8-core Apple M1 Pro after 16 discarded
warm-up games. Both seats used the production AI. The benchmark completed 20/20 games with no engine
exception.

| Budget metric | Baseline |
|---|---:|
| Decision latency p50 | 0.186 ms |
| Decision latency p95 | 8.753 ms |
| Mean decision latency | 2.0 ms |
| Allocated per decision | 812.6 KiB |
| Allocation rate while choosing | 406.6 MiB/s |
| Peak used JVM heap | 344.0 MiB |
| Concurrent measured throughput | 4.66 games/s on 8 worker threads |
| Quiet-state simulations | 1,448/s/thread |

The games/s value includes the benchmark's candidate probes and is therefore conservative; use the
same harness and concurrency when comparing a learned champion. Allocation is measured with the JDK
thread-allocation counter around `AIPlayer.chooseAction`; heap is sampled after decisions. The Phase 9
provisional gates are therefore p95 ≤ 10.941 ms, allocation rate ≤ 508.3 MiB/s, and peak heap no more
than 128 MiB above this process baseline.

## Integrity contract

Every game has a `TrainingGameMetadata` manifest. `runId/gameId` is globally unique and carries set,
format, ordered deck hashes, seed, seat profiles, schema version, generator, completion reason,
exception, and illegal-action recovery status. `TrainingCorpusValidator` rejects duplicate games or
decisions, mixed schemas, incomplete/recovered/exceptional games, orphan records, non-finite numeric
targets, and a corpus without generator diversity. A recovered arena game remains a useful bug report,
but cannot become a label.

`MaskedObservation` contains the acting player's visible hand and public zones. Other players expose
only public zones and private-zone counts. Opponents are represented as a canonical, semantically
unordered collection, so the same schema covers two, three, and four players; team identity remains an
optional public feature.

## Decision records and replay

`DecisionRecordFactory` captures quiet decision roots only and asks `LegalActionEnumerator` for the
candidates. Each candidate retains polymorphic `GameAction` JSON, a one-ply quiet masked observation,
and its digest. The remaining Phase 9c teacher fields—feature delta, common worlds and random numbers,
static/rollout statistics, visits/allocation, chosen action, eventual result, placement, and the full
per-seat utility vector—are present in schema version 1 and may be filled by later offline search.

The game manifest and its action log reconstruct the root; `DecisionRecordReplayer` then verifies the
masked root digest, the exact engine-emitted candidate set, and every candidate quiet-state digest.
For duels, a populated utility vector is required to be zero-sum. Multiplayer retains all seat values
rather than reducing a pod to a win boolean.
