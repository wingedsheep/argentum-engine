---
name: verify
description: Run the right build/test/lint gates for a change to the Argentum Engine and interpret the results — which suite to run, how to re-bless card snapshots, what a pre-existing failure means. Use before committing engine, SDK, card, or client changes, or when a build/test run needs interpreting.
argument-hint: [cards | engine | client | e2e]
---

# Verify a change

## Always go through `just`, never raw `./gradlew`

Multiple agents work in this repo in parallel, each worktree a separate Gradle root. Raw `./gradlew`
spawns its own 4g daemon + 4g Kotlin daemon + 2g test JVM and grabs every core; three concurrent runs
thrash hard enough that tests cross the 300s hang-watchdog and *every* agent times out.

The `just` recipes route through `scripts/gradle-locked`, a machine-global lock
(`~/.cache/argentum/gradle.lock`, 30m timeout via `GRADLE_LOCK_TIMEOUT`) that serializes heavy runs
across all worktrees. Others queue instead of competing.

Verify once at the end rather than after each edit — each run costs a lock slot the other agents are
waiting on.

## Which gate for which change

| Change | Command |
|---|---|
| Card using only existing effects | `just build` |
| Card or feature with new engine behavior | `just test` |
| Engine-only | `just test-rules` |
| Server/DTO/masking | `just test-server` |
| AI advisors / draft heuristics | `just test-ai` |
| One class (rules-engine) | `just test-class <ClassName>` |
| Card snapshots after an intentional change | `just rebless-cards` |
| Web client | `cd web-client && npm run typecheck` |
| Visual / UX mechanic | `just e2e-test <pattern>` |
| Registry / executor / evaluator signature changed | the full module suite, not one class |

Benchmarks (`*Benchmark` in `:ai`) are too slow for routine validation — run `*Test` classes.

## Expected: a card-snapshot diff

`CardDefinitionSnapshotTest` (in `mtg-sets`) pins every registered card's compiled JSON tree against a
committed golden per set. Any new card or SDK change fails it by design — that's the reviewable diff,
not a bug. Re-bless and commit the new golden:

```bash
just rebless-cards
```

Then read `git diff mtg-sets/src/test/resources/snapshots/cards/<SET>.json`. It should show **only**
your card's tree added. **If an unrelated card also moved, you changed shared SDK behavior** — stop and
investigate before committing. That signal is the whole point of the snapshot net.

## Card lint

`CardLintTest` (in `mtg-sets`) runs `CardLinter` over every registered card: pipeline-variable reads
need writers in scope, `ContextTarget` / `BoundVariable` must resolve against the owning ability's
requirements, choice-slot reads need declarations.

A new SDK type that reads or writes a named pipeline variable must be classified in
`CardLinter.dataflowFields` — the hygiene check fails otherwise. See
[`card-sdk-language-reference.md`](../../../docs/card-sdk-language-reference.md) §21.

## Consistency checks (fast, no Gradle lock)

```bash
just check-card-printing "<Card Name>"   # canonical CardDefinition sits in the earliest real printing's set
just check-backlog                       # backlog/sets/*/cards.md headers match actual [x] counts
just fix-backlog                         # rewrite those headers
scripts/card-status --set BLB            # implemented vs missing (--list / --cards for detail)
```

## When something fails

**Fix only failures your change caused.** A pre-existing failure — or one in code you didn't touch — is
likely another agent's in-flight work. Report it to the user and stop. Do not revert, stash, discard, or
work around it (`AGENTS.md` → Hard rules).

To tell the two apart: `git diff --stat origin/main...HEAD` and check whether the failing test's subject
appears in your diff. If it doesn't, it isn't yours.

## Tests belong where the truth is

The **engine is the source of truth**, so card and mechanic behavior is proven in `rules-engine`
(`com.wingedsheep.engine.scenarios`) — ~2400 scenario tests live there.

`game-server` tests cover only genuine game-server concerns: state masking, DTO transformation, and
session/tournament orchestration. Never write a `game-server` scenario test to prove engine behavior.

Two setup styles, both in `rules-engine/src/testFixtures`, both backed by the real `ActionProcessor`:

- `ScenarioTestBase` — fluent static board (`scenario().withCardOnBattlefield(...).build()`) plus
  name-based action and decision sugar.
- `GameTestDriver` — live game with real turns, priority, and mana.

Both register the full `MtgSetCatalog`, test-only cards (`TestCards.all`), and predefined tokens, so any
printed card is available by name. For a card that doesn't exist yet, define one inline with
`CardDefinition.creature(...)` and `cardRegistry.register(...)` in `init { }`.
