# Build speed & memory

Written 2026-08-15, after diagnosing the recurring `OutOfMemoryError` on an 8-core / 32 GB developer
box. This documents what was wrong, what changed, and what is deliberately left for later.

## 1. What was actually OOMing

**The Kotlin compile daemon's FIR frontend — not the Gradle daemon, not the test JVMs.**

Every OOM in `~/.gradle/daemon/9.6.1/daemon-*.log` had the same shape: the error was reported through
`BasicCompilerServicesWithResultsFacadeServer` (the RMI channel *from* the Kotlin daemon back to
Gradle), and the stacks all rooted in the frontend resolver:

```
e: java.lang.OutOfMemoryError: GC overhead limit exceeded
	at org.jetbrains.kotlin.name.ClassId.asSingleFqName(ClassId.kt:74)
	at org.jetbrains.kotlin.fir.FirLookupTrackerComponentKt.recordCallableCandidateAsLookup(...)
	at org.jetbrains.kotlin.fir.resolve.calls.tower.DispatchReceiverMemberScopeTowerLevel...
	at org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer...
```

Two things follow from that stack:

- `GC overhead limit exceeded` is a **ParallelGC-specific** failure (98 % of time in GC recovering
  <2 % of heap). `-XX:+UseParallelGC` on the Kotlin daemon is deliberate, so an undersized heap fails
  fast instead of grinding — it was doing its job. The heap really was too small for the work.
- `FirLookupTrackerComponent.recordCallableCandidateAsLookup` is the **incremental-compilation lookup
  tracker**, which retains a `callableId → file` map sized roughly by (files × resolved call sites).
  On a source set of thousands of DSL-heavy files it is a first-class heap consumer, not overhead.

The OOMs fired during the *compile* step of a test run (`… :rules-engine:test --tests
*TomBertAndWilliamScenarioTest` → OOM), i.e. in `compileTestKotlin`, before a single test executed.

## 2. Why it started happening

Two compile units had outgrown a 4 GB frontend, and both grew with **every card PR**:

| Compile unit | Files | Lines |
|---|---:|---:|
| `rules-engine/src/test` (one `compileTestKotlin`) | 3,405 | 446,018 |
| — of which `…/engine/scenarios/**` | 3,260 | **424,335 (95 %)** |
| — the engine's own unit tests | ~145 | 21,683 |
| `mtg-sets/src/main` (one `compileKotlin`) | 12,183 | 567,584 |

The house rule "one card, one test file" is right for maintainability, but it meant a single Kotlin
compilation grew by a file per card, forever. Same for `mtg-sets`. Nothing here was a leak — it was
linear growth in a unit that was never split.

## 3. What changed

### Daemon heap, rebalanced

The Gradle daemon logged single-digit-percent use of its 4 GB while the compile daemon died. So:
Gradle daemon `-Xmx4g` → `-Xmx2g`, Kotlin compile daemon `-Xmx4g` → `-Xmx6g`. Same total footprint,
spent where the failures actually were.

Note that `~/.gradle/gradle.properties` overrides project properties, so a developer box with its own
`kotlin.daemon.jvmargs` keeps whatever it sets. The committed values are what CI uses.

### Concurrent compilations, bounded

`KotlinCompileThrottle` (in `buildSrc`) is a shared build service with `maxParallelUsages`, and every
`KotlinCompile` task takes a lease. Every compile task in the build is executed by the *same* Kotlin
daemon, so they share one heap: `org.gradle.workers.max` bounds Gradle worker leases, not compiler
memory, and with six workers six modules could be in the FIR frontend at once inside a single JVM.
Peak heap was the sum of six live sessions rather than the largest one. Default 2; raise with
`-PkotlinCompileParallelism=N`.

### The card corpus, split by release era

`:mtg-sets` was one 12,000-file Kotlin compilation. It is now:

- **`:mtg-sets:core`** — `CardDiscovery`, `TokenArtData` + `tokens.json`, and the setless
  `definitions/custom/` cards. The only things card definitions import from outside the SDK.
- **`:mtg-sets:<era>`** × 9 — the definitions, in fixed release-year ranges (`1993-1999`,
  `2000-2002`, `2003-2007`, `2008-2016`, `2017-2022`, `2023`, `2024`, `2025`, `2026`), each
  depending only on `:mtg-sdk` and `:mtg-sets:core` — never on another era.
- **`:mtg-sets`** — the aggregator: `MtgSetCatalog` (a classpath scan, so no compile dependency on
  any era), the Scryfall sync tasks, `PredefinedTokens`, the legality resources, and the corpus-wide
  tests. It `api`-re-exports core and every era, so **every existing `project(":mtg-sets")`
  dependency is unchanged** — nothing downstream had to move.

The eras were first chained oldest→newest with `api`, because a few sets referenced an older set
across an era boundary — every one of them a basic-land fallback (`basicLandsFallback = PortalSet`
or `OnslaughtSet`) in an `MtgSet` object. That chain made the corpus compile one era at a time, and a card added to an old era recompiled every era
after it. `MtgSet.basicLandsFallbackCode` now names the fallback set by code instead (resolved through
`MtgSetCatalog` by the `MtgSet.basicLandsFallback` extension in `:mtg-sets`), so no era needs another
at compile time: the eras compile in parallel, and a change recompiles only its own era. A new
cross-era reference is a compile error — refer to the other set by code.

Boundaries are **fixed**: a new release year appends a module; no set ever moves between them.

### The scenario suite, split to mirror it

`mtg-sets/<era>/tests` (`:mtg-sets:<era>:tests`) mirrors the card modules one for one, as a child
module of the era it tests, so a set's cards and its tests are one directory apart. A test is
attributed to a set by the
card definitions it imports (compile-time truth, 43 %) or by its file name matching a card definition
(the one-file-per-card convention, 47 %); the remaining 10 % — 330 files, 70,579 lines — turned out
to be engine tests rather than card tests, and went back into `:rules-engine`'s own suite where they
belong.

These modules depend on the `:mtg-sets` **aggregator**, not on a single era, so every scenario still
sees the whole catalog exactly as before: `ScenarioTestBase` builds its registry from
`MtgSetCatalog`, and constraining that would have been a silent behavioural change. The era decides
only *where a file lives*.

(The tests are a *separate* module per era, not the era module's own test source set: `ScenarioTestBase`
and `TestCards` live in `:rules-engine`'s test fixtures and import `MtgSetCatalog`, so a test inside
`:mtg-sets:2024` itself would need `testFixtures(:rules-engine)` → `:mtg-sets` → `:mtg-sets:2024`. That
is a dependency cycle. A sibling `tests` module is the same colocation without it — Gradle parent/child
is naming, not a dependency, so `:mtg-sets:2024:tests` can depend on the aggregator above it.)

There is no project that owns all nine test modules, so **`:mtg-sets:scenarioTest`** fans out to every
`:mtg-sets:<era>:tests:test` — that is the one task path for the whole card suite, used by
`just test-rules` and CI. It is deliberately not wired into `check`: `:mtg-sets:check` is the
whole-corpus gate (snapshots, lint, facade boundary) and has to stay runnable without the card suite.

### Resulting compile units

| Unit | Before | After (largest) |
|---|---:|---:|
| Card definitions | 567,584 | 103,668 (`:mtg-sets:2025`) |
| Scenario tests | 424,335 | 72,928 (`mtg-sets/2024/tests`) |
| `rules-engine` tests | 446,018 | 92,262 (its own tests + the 330 returned engine tests) |

### Disk and daemon hygiene

- `just prune-worktrees` — reclaims `build/` and `.gradle/` from worktrees nobody is working in
  (~15 GB across 20 live worktrees when measured; 2.5 GB at a 7-day threshold). Dry run by default.
- `just kill-daemons` — reaps idle Kotlin and Gradle daemons. `org.gradle.daemon.idletimeout`
  governs only the *Gradle* daemon; the Kotlin compile daemon is a separate 6 GB JVM with its own
  lifetime, and a few hours-old idle ones are what push the box into swap and make the next compile
  OOM. Dry run by default.
- Both refuse to act while a `gradle-locked` semaphore slot is held, skip anything a running JVM
  references, and never touch `kotlin-lsp` — so they cannot disturb another agent mid-build.
- `just install-gradle-init` — installs `gradle/init.d/argentum-cache-retention.init.gradle.kts`,
  which bounds Gradle user home cache retention (the local build cache had reached 19 GB with nothing
  ever expiring it). This has to be an init script: Gradle rejects cache-retention settings from a
  project's `settings.gradle.kts` as "modified from an unsafe location", because they govern the
  shared user home rather than one build. Installing it is therefore a machine-level opt-in.

### Visibility

`kotlin.build.report.output=file` is on, so per-task compile time, GC and memory land in
`<module>/build/reports/kotlin-build/`. A source set growing back to the point where it needs its own
module should be visible before it OOMs again.

## 4. Finding things after the split

Nobody should have to remember which year a set shipped:

```bash
just where MRD                              # -> :mtg-sets:2003-2007 and mtg-sets/2003-2007/tests
just where "Myr Incubator"                  # -> the card file, its module, and its test path
just test-class MyrIncubatorScenarioTest    # finds the file, runs only its module's test task
just test-scenarios 2024                    # one era's scenarios
just test-rules                             # engine tests + every card scenario (as before)
```

`scripts/set_dirs.py` (`definitions_roots`, `iter_card_files`, `root_for_set`) is the shared helper
for tooling; `SetSourceRoots` is the equivalent for the corpus-wide Kotlin tests, and
`DEFINITIONS_ROOTS` / `definitionsRootFor` for `:mtgish-tooling`. All of them discover the modules by
globbing, so a new era module needs no change to any of them.

## 5. Deliberately not done

- **No measurement pass first.** The plan called for capturing per-unit peak heap before tuning; the
  work was authorised without it. The heap numbers above are therefore *reasoned from the failure
  mode*, not measured, and `kotlin.build.report` is now on precisely so the next tuning round has
  real numbers.
- **`forkEvery` on the scenario modules.** With ~3,000 test classes previously in one JVM, per-class
  state leakage is plausible; now that the suite is split across nine JVMs it is worth revisiting,
  but changing fork behaviour alongside a move this large would confuse any regression it caused.
- **A source-set size budget in CI** (fail if any Kotlin source set exceeds ~120k lines), which would
  make the next split deliberate rather than an incident.
