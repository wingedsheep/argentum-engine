# Engine Performance — Hotspot Analysis & Improvement Plan

CPU profile of the rules engine's hot path (legal-action enumeration + action processing),
with a prioritized plan to cut total CPU. Generated May 2026 from a sampled profiling run.

**Status:** **Steps 1–4 shipped** (Step 4 on 2026-07-28). **Step 5 dropped** — its profile gate was
checked and does not open. Every item in this document is now closed; the next perf item is
`PredicateEvaluator.matchesCardPredicate`, the top leaf in the post-Step-4 profile at **20.4%
self**, which needs its own analysis rather than a line in this one.
Items below are ordered by impact ÷ risk; each carries its own status marker.

> ⚠️ **The profile and the May 2026 baseline below predate Steps 1–3.** The measured hotspot table
> and the `~404 actions/sec/thread` figure describe the engine *before* the component-keying and
> `getBattlefield()` fixes landed, so percentages for the fixed items are historical.
>
> **The random-action baseline has now been re-run** (2026-07-28, Step 4's own before/after) — see
> ["Baseline"](#baseline) below and
> [`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md#phase-5a--the-on-battlefield-scans).
> Read the 404 figure as *not comparable* rather than as a target: the BLB card pool has roughly
> doubled since May, and `GameState.turnNumber` now counts player turns rather than rounds, so the
> same game reports ~2× the turns. Only the same-session before/after pair says anything.
>
> Phase 0 of [`engine-ai-improvement.md`](engine-ai-improvement.md) measured
> `ActionProcessor.process` at **~3,400 calls/sec/thread** on the AI-driven workload. Different
> action mix, not directly comparable either — but it is why **Step 4 was never blocking the AI's
> rollout evaluator**, and why it shipped on its own merits rather than as a prerequisite.

## Methodology

Profiled the random-action throughput benchmark — pure engine work, no AI evaluation —
so the numbers reflect the engine itself, not search heuristics:

```bash
just benchmark-random 60 BLB        # 60 Bloomburrow sealed games, random legal actions
```

Sampling was done with **async-profiler** (itimer event, 2 ms interval) attached as a JVM
agent, collecting ~42,000 CPU samples across the 8 worker threads. The `.collapsed` stacks
were aggregated into self-time (leaf) and inclusive (any-frame) totals.

To reproduce the profile (the agent dylib ships inside JetBrains IDEs):

```bash
AGENT="/Applications/DataGrip.app/Contents/lib/async-profiler/libasyncProfiler.dylib"
./gradlew --stop
JAVA_TOOL_OPTIONS="-agentpath:$AGENT=start,event=itimer,interval=2ms,collapsed,file=/tmp/prof/engine-%p.collapsed" \
  ./gradlew --no-daemon :ai:test --tests "*.RandomActionBenchmark" \
  -Dbenchmark=true -DbenchmarkGames=60 -DbenchmarkSet=BLB
# The worker JVM's collapsed file (the one with the most com/wingedsheep frames) is the engine profile.
```

`--no-daemon` is required so the forked test-worker JVM inherits `JAVA_TOOL_OPTIONS` and loads
the agent; pick the per-PID collapsed file containing engine frames.

## Bottom line

The workload is dominated by `LegalActionEnumerator.enumerate` (~76% inclusive — expected, it
runs at every priority step). The cost inside it concentrates in a few structural problems that
are **mechanical to fix and card-agnostic**:

1. **Component lookup uses kotlin-reflect + String-keyed maps** on every single access.
2. **`GameState.getBattlefield()` re-filters and re-allocates** the battlefield list on every
   call and is never memoized (unlike `projectedState`, which is already `by lazy`).
3. **Ward / trigger / mana detection re-scan the whole battlefield repeatedly**, including one
   O(n²) inner scan.

State projection (`StateProjector.project`, 7.4%) is **already cached** per `GameState` via
`by lazy` and is *not* a problem — leave it alone.

## Measured hotspots

Inclusive CPU (method appears anywhere on the stack):

| Method | Inclusive % | Note |
|--------|-------------|------|
| `LegalActionEnumerator.enumerate` | ~76% | the workload itself; runs every priority step |
| `ManaSolver.findAvailableManaSources` | ~59% | scans whole battlefield repeatedly |
| `GameState.getBattlefield()` | **19%** | recomputed + 2–3 list allocations per call |
| `TriggerAbilityResolver.getWardTriggeredAbilities` | **13.7%** | full battlefield scan per event; O(n²) suppressor check |
| `TriggerDetector.detectTriggers` | 16.3% | per-event trigger scan |
| `StateProjector.project` | 7.4% | already cached — **not** a target |

Self-time CPU (leaf frame — where cycles are actually spent):

| Leaf | Self % | Attributable to |
|------|--------|-----------------|
| `java/util/HashMap.getNode` | 7.5% | String-keyed component map lookups |
| `ManaSolver.getStaticGrantedManaAbilities` | 3.5% | per-enumerate battlefield scan |
| `KClassImpl.getQualifiedName` (+ `SoftReference.get` 1.7%) | ~4% | reflective component keying |
| `String.equals` / `String.hashCode` | ~3% | String-keyed component map |
| `TriggerAbilityResolver.getWardTriggeredAbilities` | 2.3% | battlefield re-scan |
| `Arena::grow` / `zero_blocks` / `posix_madvise` | ~3% | allocation / GC churn |
| `__psynch_*` | ~6% | thread-pool lock contention (benchmark harness, not engine) |

## Root causes

### A. `ComponentContainer` keys every component by `T::class.qualifiedName`

`rules-engine/.../state/ComponentContainer.kt`:

```kotlin
inline fun <reified T : Component> get(): T? = components[T::class.qualifiedName] as? T
inline fun <reified T : Component> has(): Boolean = components.containsKey(T::class.qualifiedName)
inline fun <reified T : Component> with(component: T) =
    ComponentContainer(components + (T::class.qualifiedName!! to component))
```

`qualifiedName` is a **kotlin-reflect** call (`KClassImpl.getQualifiedName`, backed by a
`SoftReference` cache), and `components` is `Map<String, Component>`, so every access also pays
String `hashCode` + `equals`. This single design choice feeds the reflection cluster (~4%), most
of `HashMap.getNode` (7.5%), and the String hashing (~3%). Every component access in the engine —
and there are many per enumerate — goes through it.

### B. `getBattlefield()` is recomputed and allocates on every call (19% inclusive)

`rules-engine/.../state/GameState.kt`:

```kotlin
fun getBattlefield(): List<EntityId> =
    zones.filterKeys { it.zoneType == Zone.BATTLEFIELD }   // allocates a new map
         .values.flatten()                                  // allocates a new list
         .filter { entities[it]?.has<PhasedOutComponent>() != true }  // new list + reflective has<> per entity
```

Called in tight loops by ward detection (4 separate loops), `ManaSolver` (5+ loops), and
cast-permission checks. It is never memoized, even though `GameState` is immutable and
`projectedState` on the same class already demonstrates the `by lazy` pattern.

### C. Ward / trigger detection re-scan the battlefield, with an O(n²) inner scan

`TriggerAbilityResolver` iterates `state.getBattlefield()` in `getTriggeredAbilities`,
`getTriggeredAbilitiesWithProviders`, `getWardTriggeredAbilities`, and `isWardSuppressed` —
and `isWardSuppressed` does `getBattlefield().any { … }` **inside** a `getBattlefield()` loop,
making ward resolution quadratic in board size. Each of those `getBattlefield()` calls also pays
cost (B).

### D. Secondary: allocation churn & lock contention

`Arena::grow` / `zero_blocks` / `madvise` (~3%) is allocation pressure fed by the list/map copies
in (B) and by `with`/`without` rebuilding the component map on every mutation. `__psynch_*` (~6%)
is thread-pool lock contention from the benchmark's 8-way fan-out — a harness artifact, not engine
logic; ignore it for engine optimization.

## Improvement plan

Ordered by impact ÷ risk. Steps 1 and 3 are independent, individually shippable, and together
should be the bulk of the win.

### Step 1 — Remove reflection from component keys ✅ **DONE** *(quick · low risk · ~5–8%)*

> Shipped, and superseded by Step 2 — `ComponentContainer` no longer touches `qualifiedName` at all.

Replace `T::class.qualifiedName` → `T::class.java.name` in all six sites in `ComponentContainer`
(plus the two raw `::class.qualifiedName` lookups in `DamageUtils` / elsewhere). `Class.getName()`
is JVM-cached and avoids kotlin-reflect entirely. The map stays `Map<String, Component>`, so the
serialization shape is **unchanged**. Pure mechanical swap; kills the
`KClassImpl.getQualifiedName` / `SoftReference` cluster outright.

- **Risk:** very low. `qualifiedName` and `java.name` differ only for nested classes (`.` vs `$`),
  and the key is internal — never persisted as a public contract. Verify with `just test-rules`.

### Step 2 — Key components by `Class<*>` instead of `String` ✅ **DONE** *(medium · ~8–12% on top of Step 1)*

> Shipped. `ComponentContainer.kt:23` is `Map<Class<*>, Component>`; `get`/`has`/`with`/`without`
> (`:29`, `:45`, `:52`, `:59`) key on `T::class.java`. The required custom serializer exists —
> `@Serializable(with = ComponentContainerSerializer::class)` (`:21`), `ComponentContainerSerializer`
> (`:104`) — so the JSON wire format still uses class names and round-trips unchanged.

Change the internal map to `Map<Class<*>, Component>` (`T::class.java` as key). `Class` uses
identity hashCode — no string hashing at all — eliminating most of `HashMap.getNode` and the
remaining `String.equals` / `hashCode`. `T::class.java` compiles to a constant-pool class-literal
load (no reflection).

- **Requires** a custom `KSerializer<ComponentContainer>` that serializes keys via class name to
  preserve JSON round-trips (the wire format keeps using names; only the in-memory key changes).
- **Risk:** medium — touches serialization. Gate behind the serialization tests; confirm a
  save/load round-trip of a live `GameState`.

### Step 3 — Memoize `getBattlefield()` per `GameState` ✅ **DONE** *(quick · low risk · ~10–15%)*

> Shipped. `GameState.kt:808` returns a `by lazy cachedBattlefield` built in a single pass with one
> list allocation, replacing the `filterKeys` + `flatten` + `filter` chain. A body `val`, so it is
> not serialized. `allBattlefieldEntities()` (phased-out-inclusive) is unchanged as planned.
>
> **Note for Step 4:** this removed the *allocation* cost of the repeated battlefield scans but not
> the *iteration*. Step 4 is the remaining half, and it is now the larger one.

Make `getBattlefield()` a `by lazy val` mirroring `projectedState` — safe because `GameState` is
immutable, so the battlefield set is constant for the lifetime of a state instance. Precompute the
phased-out filter once rather than a reflective `has<PhasedOutComponent>()` per entity per call.

- **Risk:** low. The only correctness concern is that nothing mutates a `GameState` in place after
  construction — which the immutability invariant already guarantees. Keep `allBattlefieldEntities()`
  (the phased-out-inclusive variant) as-is.

### Step 4 — Hoist battlefield scans in ward / trigger / mana detection ✅ **DONE 2026-07-28** *(medium)*

> Shipped as Phase 5a of [`engine-ai-improvement.md`](engine-ai-improvement.md). Two new index
> types own the walk, and the nine per-entity scans that used to hunt for these statics are gone:
>
> - `rules-engine/.../mechanics/mana/ManaStaticsIndex.kt` — built once per
>   `findAvailableManaSources` / `calculateExplicitActivationBonusMana` call, and once per
>   enumeration pass on `EnumerationContext.manaStatics`. It replaces **six** per-source battlefield
>   walks: `getStaticGrantedManaAbilities`, `findEnchantedLandManaColorOverride` (twice — the solver
>   and `ManaAbilityEnumerator` each carried a copy), `landMatchesManaColorReplacement`,
>   `augmentWithAuraBonusMana`, `augmentWithSourceTapBonusMana`.
> - `rules-engine/.../event/BattlefieldStaticsIndex.kt` — built once per `detectTriggers` pass and
>   threaded into `getTriggeredAbilities` / `getTriggeredAbilitiesWithProviders`. It replaces the
>   battlefield-scope `GrantWard` scan, the `isWardSuppressed` scan, and the two attachment scans in
>   `getWardTriggeredAbilities` / `getAttachedGrantedTriggeredAbilities`.
>
> Both index the *rare* statics they are hunting for, so an ordinary board produces the `EMPTY`
> instance and the per-entity cost collapses to a lookup that finds nothing. Each bucket reproduces
> its original loop's collection rules exactly — including where those rules disagreed with each
> other about face-down permanents and about `staticAbilities` vs `effectiveStaticAbilities` — so
> this is a hoist, not a rules change.
>
> **Measured:** the fresh random-action baseline and the post-change numbers are in
> [`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md#phase-5a--the-on-battlefield-scans).
> Gates: `just test-rules`, `:game-server:test`, `:ai:test` green; six new unit tests pin what each
> index bucket collects (`ManaStaticsIndexTest`, `BattlefieldStaticsIndexTest`).

**The original plan, kept for the record:** compute the battlefield list once per `detectTriggers` /
`findAvailableManaSources` call and pass it down instead of re-calling `getBattlefield()`; fix the
O(n²) `isWardSuppressed` by precomputing the set of ward-suppressors once per detection pass.

What changed in execution: hoisting the *list* would have removed only the allocation, which Step 3
had already removed. The cost that remained was the **iteration**, so the fix had to hoist the
*result of the scan* — hence an index per concern rather than a shared battlefield list. And once
one scan in each file was indexed, leaving its four siblings scanning would have left the O(n²)
in place, so all of them moved.

### Step 5 (optional) — Reduce component-map copy churn ❌ **DROPPED 2026-07-28** *(gate checked, does not open)*

> The gate is "only if `Arena::grow` is still prominent after 1–4". It is not: in the post-Step-4
> profile `Arena::grow` is **1.37%** self and `posix_madvise` ~0.7% — about **2%** of the engine,
> against the 4–6 days plus serializer work [`engine-ai-improvement.md`](engine-ai-improvement.md)
> Phase 5c scopes. The mechanism below is still correctly described, and the
> `kotlinx.collections.immutable` migration would still work; there is just no longer a number
> behind it. Revisit only if a fresh profile puts allocation back near the top.
>
> **What is at the top instead:** `PredicateEvaluator.matchesCardPredicate` at **20.4% self**, more
> than 3× the next entry. That is the next perf item on this plan.

If `Arena::grow` is still prominent after 1–4, reduce the `map + (k to v)` / `map - k` rebuild
cost in `with` / `without` for hot single-component updates (e.g. a small persistent map or a
copy-on-write builder). Profile-gated — don't pre-optimize this.

## Validation loop

After **each** step:

1. `just test-rules` — correctness must be unchanged.
2. `just benchmark-random 200 BLB` — compare wall time / throughput against the baseline below.
3. Re-profile (commands above) — confirm the targeted leaf shrank and nothing regressed.

### Baseline

`just benchmark-random 200 BLB`, 8 threads. **Compare against a run from the same session on the
same machine** — see the warning below.

**Current (2026-07-28, post-Step-4):**

- Completed 200 / 200, 0 crashes
- Turns avg 54.8, actions avg 1,528 / game
- Engine CPU 832 s total — Enumerate 688 s (83%) / Process 144 s (17%)
- Wall time ~108 s; ~1.9 games/sec wall-clock

**Immediately before Step 4 (2026-07-28, same session):** engine CPU 1,051 s — Enumerate 764 s
(73%) / Process 287 s (27%); wall ~133 s. So Step 4 is **−21% engine CPU**, with `process` nearly
halving. Full three-point series, including the intermediate version that regressed 10%:
[`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md#phase-5a--the-on-battlefield-scans).

**Historical (May 2026, pre-Steps-1–3):**

- Turns avg 26.5, actions avg 1,569 / game
- Throughput ~404 actions/sec per thread
- Wall time ~98 s; ~2.0 games/sec wall-clock
- Time split: Enumerate ~57% / Process ~43%

> ⚠️ **The May figure is not a target and never was comparable.** `GameState.turnNumber` counts
> player turns rather than rounds since the multiplayer fix, so the same game now reports ~2× the
> turns; and the implemented BLB pool has roughly doubled, so sealed decks are richer and each
> priority window enumerates more. Two runs three months apart describe two different workloads.
> Per-game CPU on this box also spans 1 s to 25 s depending on what else is running, so a ±5%
> difference between two runs means nothing. **Use the profile to say why a number moved.**

### Related: AI-workload baseline (July 2026)

[`docs/ai/baseline-metrics.md`](../docs/ai/baseline-metrics.md) measures the same engine under real
AI games rather than random actions: `ActionProcessor.process` ~3,400/sec/thread, `StateProjector.project`
~47 µs cold (11% of one `process()`), and 6.36 legal actions per priority window. That confirms the
"leave projection alone" call above from a second angle — a perfect cross-state projection cache
caps out near 12%.
