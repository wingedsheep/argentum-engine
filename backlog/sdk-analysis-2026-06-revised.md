# mtg-sdk Improvement Plan — June 2026 (revised)

_Revision date: 2026-06-10. This is the filtered successor to
[sdk-analysis-2026-06.md](sdk-analysis-2026-06.md): it keeps only the items judged genuinely worth
doing **and** genuinely improvements to the SDK, re-sequenced. Items that were cut or split are
recorded in §7 with their rationale, so they don't get re-proposed without new evidence._

**Changes from the analysis doc:**

- **§1.1 (executor coverage) is DONE** (PR #606): a CI test + registry throw guarantees every
  `Effect` has an executor; new `StaticAbility`/`ReplacementEffect` subtypes are a compile error
  until classified. Removed from the plan, kept in the "fixed" ledger below.
- **§2.2 (zone-move `MoveSpec`) is split.** The engine-side single zone-move path is worthwhile but
  is engine work, independent of SDK encoding — tracked separately, not here. The SDK-side
  `MoveSpec` re-encoding is **dropped as premature** (§7.2). The exile/return one-off triage moves
  into the pipeline-branch harvest (§2.1 here).
- **§2.4 (event-filter collapse) is REJECTED**, not deferred — the conversion is not
  semantics-preserving because event filters evaluate against event snapshots/LKI, not live state
  (§7.1).
- **§4 (mtgish alignment) is trimmed** to the two things that are SDK work or protect SDK work: the
  co-evolution process rule, and the cast-time choice model (the one real SDK feature in it).

---

## 0. Snapshot

Hierarchy sizes as of 2026-06-09 (grep-verified): `Effect` 245 · `StaticAbility` 109 · `Condition`
65 · `CardPredicate` 72 · `ReplacementEffect` 34 · `DynamicAmount` 31. Authoring surface ~566 public
DSL entry points.

**Verified fixed** (carried from the analysis doc, plus what landed since): `GameObjectFilter` OR
semantics; `dynamicMaxCount` enumeration-time resolution; KeywordAbility flatten;
static single-vs-group unification; `ModifySpellCost`; `TurnTracker`; `ContextProperty`;
exhaustive `StatePredicate` evaluator; **effect-executor coverage guarantees (PR #606)**.

**What's healthy — keep doing it:** "data, not code" holds module-wide (zero deps beyond
kotlinx-serialization, no lambdas, no engine refs); the Gather→Select→Move pipeline and the
three-axis + `anyOf` filter core deliver; `Subtype` is the extensibility model to copy; the
`Patterns` index + thin facade delegation is clean; sealed `@Serializable` auto-registration and
`CompactJsonTransformer` are symmetric by construction; the hygiene-test pattern
(`SerializationPolymorphicRegistrationTest`, and now the executor-coverage test) demonstrably works
and several items below are "apply that pattern to another contract."

---

## 1. GUARANTEES — move failures from runtime to build time (do first)

These are cheap, card-migration-free, and they're what makes every collapse in §3 safe. Highest ROI
in the document.

### 1.1 Card linter — grow `CardValidator` into structural validation — [HIGH] — ✅ DONE

> Landed as `CardLinter` (mtg-sdk) + corpus gate `CardLintTest` (mtg-sets) with a burn-downable
> `lint-allowlist.txt`. Took the stale-proof JSON-tree route; abilities are detected structurally
> (no `type` discriminator + `effect` + trigger/cost/target member), and two registry-hygiene
> checks keep the dataflow vocabulary honest (the executor-coverage pattern). Checks: pipeline
> reads have in-scope writers (typo = error, cross-resolution = warning), orphan stores,
> `ContextTarget`/`BoundVariable` per owning ability (count-expanded slots; modes, reflexive
> triggers, delayed triggers, granted abilities all scoped), `ChoiceSlot` reads vs declarations,
> `SourceChosenModeIs` mode ids. First corpus run: 48 errors → triaged to **one real shipped bug**
> (Atmospheric Greenhouse's ETB was a silent no-op — `ContextTarget(0)` inside `ForEachInGroup`),
> the rest were linter/registry gaps, fixed; corpus is now 0-error with 13 review-level warnings.

**Problem.** `CardValidator` checks creature stats, target-index bounds, aura/equipment consistency,
planeswalker loyalty — and nothing else. Not validated: pipeline variable references
(`GatherCardsEffect(storeAs = "x")` … `MoveCollectionEffect(from = "y")` — a typo resolves to an
empty collection at runtime), `ChoiceSlot` references, `ContextTarget` indices inside granted or
nested abilities, `BoundVariable` names, kicker/mode target slicing. New cards' structural mistakes
surface as silent no-ops in playtesting.

**Solution.**
1. **Extract a generic effect-tree walker.** Generalize `CardValidator.collectIndicesRecursive`
   (`CardValidator.kt:169`) into `walkEffects(script, visit)` covering composite children, modes,
   gates, granted triggered/activated abilities, class levels, saga chapters, kicker effects, card
   faces. (Stale-proof alternative: serialize with the existing machinery and walk the JSON tree —
   any field holding an `Effect` is visited automatically; the snapshot test already produces this
   tree.)
2. **Dataflow checks on top:** every `from` collection name has a prior `storeAs` writer in the same
   tree (plus implicit `remainder`); orphan `storeAs` definitions flagged; `ContextTarget(i)` /
   `BoundVariable(name)` resolve against the *owning ability's* requirements; `ChoiceSlot`
   references have declarations; `StoredEntityTarget` writers exist (warning-level — cross-trigger
   flows are legal).
3. **Run corpus-wide at build time:** a `CardLintTest` beside `CardDefinitionSnapshotTest`
   (which already instantiates every registered card), zero-errors assertion, per-card allowlist
   for intentional exceptions. The `add-card` skill inherits the gate for free.

### 1.2 Strengthen the facade boundary from spot-check to guarantee — [HIGH]

**Problem.** The facade is the load-bearing abstraction that makes every §3 collapse safe (refactor
the data types, update only the facade) — but enforcement is a regex test covering five patterns;
the other ~240 effect constructors are constructible from cards today, and nothing verifies a new
SDK type even *gets* a facade entry.

**Solution.** Two cheap tests, seeded **before** the §3 collapses begin:
1. **Import-whitelist boundary** (replaces the regex blacklist in `FacadeBoundaryTest`). Card files
   may import `dsl.*` / `core.*` / `model.*`; any `scripting.*` import fails unless listed in a
   committed `facade-boundary-exceptions.txt` (seeded from current violations — debt becomes a
   visible, burn-downable file). ~20 lines, no AST. Keep the raw-`CompositeEffect(` regex as a
   second pass for types re-exported through dsl.
2. **Facade-coverage test** (in mtg-sdk's own suite): walk `Effect::class.sealedSubclasses` (and
   `Condition`, cost hierarchies), assert each concrete type's simple name appears as a constructor
   call under `dsl/`, or is on an explicit `ENGINE_INTERNAL` list. Source-scan, not reflection —
   facades return the supertype, so the text scan is the honest check.

### 1.3 Close the fail-open defaults — [MED] — ✅ DONE (PR #617)

**Problem & solution**, three small items:
1. `GameObjectFilter.and` silently discards the left controller predicate on conflict
   (`ObjectFilter.kt:600`) → `require` failure; with the §1.1 linter running corpus-wide, it fires
   at build time, not in a game.
2. `ControllerPredicate` has no combinators → add `And`/`Or`/`Not` + evaluator support (~30 lines);
   removes the only reason heterogeneous-controller filters need the `anyOf` escape hatch.
3. `SuccessCriterion.Auto` treats unrecognized effect shapes as success (`CompositeEffects.kt:317`)
   → throw at card-load for shapes it can't infer ("specify an explicit criterion"). Unknown ≠ ok.

---

## 2. STRUCTURAL DE-GENERATORS — remove what *mints* new one-offs

Fix these and the hierarchies stop re-bloating after each cleanup.

### 2.1 The missing pipeline primitive: branch on gathered properties — [HIGH] — ✅ DONE (PR #618)

**Problem.** Gather→Select→Move is strictly linear. Any card that says "reveal/look, **if it's a
land** do X **otherwise** Y" (Explore, "draw and reveal, discard unless…") cannot be expressed and
forces a bespoke effect type. This is the #1 generator of new one-off effects going forward.

**Solution.**
1. **`PartitionCollectionEffect(from, filter, storeMatching, storeRest)`** — splits a named
   collection by a `GameObjectFilter`; deterministic, no continuation, one ~40-line executor.
2. **Collection-gated branches:** `Gate.WhenCondition` + `Conditions.CollectionContainsMatch`
   already express "run this only if `landPart` is non-empty" — verify the gate evaluator can see
   pipeline collections in `EffectContext` (if not, pass `storedCollections` into condition
   evaluation — §1.1's dataflow view wants the same thing).
3. **Worked example — Explore** becomes pure composition:

   ```kotlin
   Effects.Composite(
       GatherCardsEffect(CardSource.TopOfLibrary(1), storeAs = "revealed"),
       PartitionCollectionEffect("revealed", Filters.Land, storeMatching = "land", storeRest = "nonland"),
       MoveCollectionEffect(from = "land", destination = ToZone(Zone.HAND)),
       Effects.AddCounters(PLUS_ONE_PLUS_ONE, 1, target = Self),
       GatedEffect(Gate.WhenCondition(CollectionContainsMatch("nonland")),
           SelectFromCollectionEffect("nonland", ChooseUpTo(1), storeSelected = "toGrave"),
           MoveCollectionEffect(from = "toGrave", destination = ToZone(Zone.GRAVEYARD)))
   )
   ```
4. **Then harvest** the bespoke types whose only reason to exist was the missing branch:
   `DrawRevealDiscardUnlessEffect`, the predicate-carrying exile-repeat variants, and — folded in
   from the old §2.2 — the composable members of the specialized exile/return family
   (`ExileUntilLeavesEffect` = move + linked return; `ForceReturnOwnPermanentEffect` = select +
   move). Keep what's genuinely atomic (`MarkExileOnDeathEffect` is a replacement marker). Each
   deletion is a small PR with a snapshot diff.

### 2.2 Get set-specific mechanics off the core `CardBuilder` — [HIGH] — ✅ DONE

> Landed: the 12 set-mechanic helpers (`leyline`, `flurry`, `mobilize` ×2, `firebending`, `sneak`,
> `decayed`, `vividEtb`, `vividCostReduction`, `impending`, `renew`, `craft`, `station`) moved off
> `CardBuilder` into one-file-per-mechanic `CardBuilder` extensions under `mtg-sdk/.../dsl/mechanics/`,
> bodies verbatim. They stay in package `com.wingedsheep.sdk.dsl` (call syntax unchanged); the five
> builder collections they compose onto were widened `private` → `internal`. Evergreen `prowess()` /
> `rampage(n)` stay on the core builder. Call sites only needed the matching `import
> com.wingedsheep.sdk.dsl.<mechanic>` — one mechanical sweep across the corpus (~99 files). Behavior is
> byte-identical: `CardDefinitionSnapshotTest` passes with no re-bless. `StationDslTest` moved to
> `dsl/mechanics/`. Reference doc §11 gains the placement rule. The mtgish emitter renders these as
> source-text strings and is unaffected (it has no SDK dependency).

**Problem.** `CardBuilder` has accumulated 12+ mechanic-specific methods (`leyline()`, `flurry()`,
`mobilize()`, `firebending()`, `station()`, …) on the core class (`CardBuilder.kt:311-860`) —
mechanics from six sets interleave in the one file every card author reads, violating the project's
own "set-specific mechanics live in set-specific files" rule.

**Solution.** Kotlin extension functions make this nearly free:
1. `dsl/mechanics/`, one file per mechanic (`StationDsl.kt` holding `fun CardBuilder.station(...)`)
   — bodies move verbatim since they only call public builder APIs; widen the rare private field to
   `internal` where needed.
2. Call sites don't change (extensions resolve identically); only imports do — one mechanical sweep.
   DSL tests move with their mechanic.
3. **Decision rule, enforced in `add-feature` review:** evergreen/multi-set parameterized keywords
   (rampage, kicker) may live on the core builder; set mechanics get an extension file (default
   `dsl/mechanics/`, so the mtgish emitter can keep importing from the SDK).
4. End state: `CardBuilder.kt` shrinks toward the universal blocks, and "where do I add my
   mechanic's sugar" has a one-word answer.

### 2.3 Condition hierarchy: stop the one-off accumulation — [MED] — ✅ DONE (PR #649)

> Landed as `EntityMatches(entity: EffectTarget, filter)` (mtg-sdk) — the four near-clones
> (`SourceMatches`, `EnchantedPermanentMatches`, `TargetMatchesFilter`,
> `TriggeringSpellMatchesFilter`) are deleted and their `Conditions.*` facades now desugar to it,
> so card sources are unchanged; the five cards importing the raw types moved to the facade. The
> `ConditionEvaluator` dispatches one `EntityMatches` branch on the entity role to the
> already-correct matching strategy: live predicate match for `Self` / enchanted-or-equipped
> (dual-mode), chosen-target match for `ContextTarget` (resolution-only, player ⇒ false), and the
> LKI cast-record match for `TriggeringEntity` (resolution-only) — preserving every prior behavior
> (snapshot re-bless is the per-card review artifact). Parts 2 & 3 are forward-looking rules now
> enforced by docs + the `add-feature` checklist: tracker-shaped checks route through `Compare` +
> a tracked `DynamicAmount` (the LIFE_GAINED precedent), and set-mechanic conditions are quarantined
> in mechanic-named files. The `CardLinter` now validates the `ContextTarget` index that
> `TargetMatchesFilter` previously hid in a raw `targetIndex` field.

**Problem.** 65 `Condition` subtypes, growing ~3-5 per set. Two anti-patterns: four near-clones of
"entity X matches filter" (`SourceMatches`, `EnchantedPermanentMatches`, `TargetMatchesFilter`,
`TriggeringSpellMatchesFilter`) differing only in *which* entity; and hyper-specific trackers that
should be `Compare` over a tracked amount.

**Solution.**
1. **One `EntityMatches(entity: EffectTarget, filter: GameObjectFilter)`** — the `EffectTarget`
   vocabulary already names every entity role and `resolveTarget` already resolves them. The four
   clones become facade constants, then get deleted. Evaluator: resolve entity →
   `matchesWithProjection` — one code path, projection handled once.
2. **Tracker-shaped conditions route through `Compare` + `TurnTracking`** (the LIFE_GAINED
   precedent). When the tracker doesn't exist, add the *tracker enum value* (data), not a condition
   class.
3. **Set-mechanic conditions are allowed but quarantined** in mechanic-named files; the
   `add-feature` checklist gains this placement question explicitly.

---

## 3. COLLAPSES — one encoding per concept, one family per PR

> **Shared migration recipe** (stated once): (1) introduce the parameterized type + facade entries;
> (2) repoint the *existing facade signatures* at the new type so most card sources don't change;
> (3) mechanically migrate the residue (deprecate with `ReplaceWith`, fix, delete); (4) re-bless the
> card snapshot golden and review the per-card JSON diff — that diff *is* the behavior-preservation
> review artifact; (5) update the mtgish bridge/emitter **in the same PR** and re-run
> `coverage-verify` + `EmitterGoldenTest` (§6). Scenario tests stay untouched — they assert
> behavior, not encoding.

### 3.1 Near-duplicate effect families — [HIGH]

Verified sampling puts ~25-35% of the 245 effect types in near-duplicate families differing only by
a parameter the SDK already knows how to model. In order of attack:

| Family | Today | Target shape |
|---|---|---|
| **Counters** (~12 types) | one type per verb×scope | `CounterOp(op: Add/Remove/RemoveAll/Move/Double, type, amount, target, from, distribute)` |
| **Control change** (5) | one type per "who gets it"; inconsistent `duration` | `ChangeControl(newController: Player, target, duration)` — the `Player` AST already expresses ActivePlayer/TargetPlayer/… |
| **Evasion grants** (~6) | fixed-vs-chosen color and "except by" baked into names | `GrantEvasion(exception: GameObjectFilter?, duration)` — chosen color via the existing `HasChosenColor` predicate |
| **Sacrifice** (3) | scope baked into type | one type over `EffectTarget`/filter |
| **Numeric `CardPredicate`s** (13) | one class per property×operator | `NumericPredicate(property, op, value)` — `EntityNumericProperty` already exists; old names become facade functions |
| **Targeting OR-types** | bespoke unions (`TargetCreatureOrPlayer`, …) | delete; `TargetObject(filter = A or B)` |

Family-specific notes:
- **Counters first** — most members, least semantic risk, engine already routes counter mutation
  through one service. `ProliferateEffect` stays (named mechanic, own rules text);
  `AddCountersToCollectionEffect` folds into the pipeline's `addCounterType`.
- **Control change** — `ExchangeControlEffect` stays separate (genuinely different shape: two-way
  swap). Normalize `duration` across all paths.
- **Evasion** — `CantAttack`/`CantBlock` group effects are a separate mini-family (restriction, not
  evasion): same recipe, different PR.
- **Review rule, enforced in `add-feature`:** a new `Effect` subtype must include one sentence in
  the PR description proving it cannot be a parameter of an existing family.

### 3.2 One cost language — [HIGH] — ✅ DONE

> Landed across multiple PRs: `CostAtom` extracted for `PayCost` + `AdditionalCost` (commit
> 87b71bf2e), then `AbilityCost` folded onto `AbilityCost.Atom(CostAtom)` (this PR). All three cost
> contexts now carry one shared `CostAtom` vocabulary; the `Costs.*` facades are unchanged.
> Counter-removal, X-variable, and named-mechanic costs stay as context-specific wrapper members by
> design.

**Problem.** Three parallel cost hierarchies — `AbilityCost`, `AdditionalCost` (591 lines),
`PayCost` — share ~70% of their constructors. Each new payable thing (Blight, waterbend fodder, …)
gets implemented per-hierarchy or arbitrarily lands in one. The engine-side `CostPaymentService`
unification already proved payment *execution* can be shared; the SDK-side declaration is the
remaining duplication.

**Solution.**
1. **Extract `CostAtom`** — one sealed hierarchy holding the shared vocabulary, each variant
   parameterized the way the best current version is:

   ```kotlin
   @Serializable sealed interface CostAtom {
       data class Mana(val cost: ManaCost) : CostAtom
       data object Tap : CostAtom
       data class TapPermanents(val count: Int, val filter: GameObjectFilter) : CostAtom
       data class Sacrifice(val filter: GameObjectFilter, val count: DynamicAmount = Fixed(1)) : CostAtom
       data class Discard(val count: DynamicAmount, val filter: GameObjectFilter? = null, val random: Boolean = false) : CostAtom
       data class ExileFrom(val zone: Zone, val filter: GameObjectFilter, val count: DynamicAmount) : CostAtom
       data class PayLife(val amount: DynamicAmount) : CostAtom
       data class RemoveCounters(val type: CounterType, val amount: DynamicAmount, val from: EffectTarget = Self) : CostAtom
       // deliberately NOT: context-specific oddities (BlightVariable, Echo timing) — those stay on wrappers
   }
   ```
2. **Wrappers become thin contexts** — each gains `atoms: List<CostAtom>` plus its genuinely
   context-specific extras (`AdditionalCost` keeps optionality/kicker linkage; `AbilityCost` keeps
   activation-speed implications). Old subtypes become deprecated aliases constructing atoms.
3. **Migration order: smallest first** — `PayCost` (~10 subtypes, fewest call sites,
   `CostPaymentService` already executes it), then `AdditionalCost`, then `AbilityCost`. One wrapper
   per PR; `Costs.*` signatures stay identical until the final deprecation sweep.
4. **Engine:** `CostPaymentService` gets one `payAtom(atom, ...)` dispatcher; the three payment
   paths delegate and shrink to context-specific residue. The executor-coverage test (PR #606
   pattern) extends to `CostAtom` automatically.
5. **Done =** a new payable thing is one `CostAtom` variant + one payment branch, available in all
   three contexts on day one.

---

## 4. CAST-TIME CHOICES — the one big new model (proper `add-feature` project)

**Problem.** Choosing values at cast/activation time (X, color, creature type, life paid) and
*inheriting* the choice into later effects is handled by per-mechanic ingredients that don't form
one model: `ChoiceSlot`, `CastTimeCapture`, `CastChoice(slot)`, `DynamicAmount.CastX` + durable
`CastChoicesComponent` (PR #510), `xManaRestriction`, `castTimeCreatureTypeChoice` — each with its
own read path. It is simultaneously a genuine SDK gap and the mtgish emitter's single biggest
AUTOGEN blocker (the tooling's creator-note explicitly asks for it).

**Solution.** Unify under one declared-choices model, generalizing the CastX precedent — which
already solved the hard part (durable storage + permanent→stack→LKI read fallback):
1. **One declaration:** `CardScript.castChoices: List<ChoiceSlot>` — a slot declares its kind
   (`Numeric(X)`, `Color`, `CreatureType`, `Mode`, …), when it's made (cast vs. resolution, per
   Scryfall rulings), and its prompt.
2. **One storage:** `CastChoicesComponent` widened from `Int` to a small value union — holds all
   slot values, survives onto the permanent, LKI-readable. Exactly CastX's semantics.
3. **One read path:** `DynamicAmount.CastChoice(slot)` / `ChosenValue(slot)` predicates replace the
   per-mechanic readers (`CastX` stays as the X-slot alias); `castTimeCreatureTypeChoice` and
   `xManaRestriction` become slot declarations.
4. **Payoff is triple:** per-mechanic engine plumbing converges; the §1.1 linter validates slot
   references statically; and the emitter's standing scaffold class becomes renderable — mtgish is
   already a declare-and-reference language (`ChooseAColor` … `TheChosenColor`,
   `Trigger_ValueXOfThatSpell`), so the mapping is mechanical once the SDK offers a uniform target.
   Promote the relevant envelopes to gated bridge capabilities first, so
   `just coverage` quantifies how many cards the feature unlocks per set **before engine work
   starts**.

Illustrative end-state (DSL names settle during `add-feature`):

```kotlin
// Story Circle — today: permanent SCAFFOLD (entry choice + TheChosenColor read)
castChoices { color("color") }
activatedAbility {
    cost = Costs.Mana("{W}")
    effect = Effects.PreventNextDamage(
        from = Filters.Source.hasChosenColor("color"),
        to = EffectTarget.Controller,
    )
}

// Phyrexian Processor — declaration is itself a payment; the read crosses into an activated ability
castChoices { payLife("lifePaid") }
// token PT = DynamicAmount.CastChoice("lifePaid")
```

**Verification path:** scenario test for the *first* card of each shape (X-counters, chosen-color
read, declared life payment) before trusting batch output; newly-AUTO cards added to the committed
fixture slice; calibrated sets stay 0-mismatch in `coverage-verify`.

**Sequencing:** after §1 — the linter and boundary tests are what make a cross-cutting model change
like this safe to land.

---

## 5. REGISTRATION & VOCABULARY HYGIENE — small, real, anytime

### 5.1 De-risk new-set registration — [MED] — ✅ DONE (PR #620)

Three silent failure modes, three fixes:
1. **Discover sets the way cards are discovered.** `CardDiscovery` already uses ClassGraph; add
   `findSets()` and make `MtgSetCatalog.all` a lazily-built view over it — the hand-maintained
   import block + list (two places to forget) disappear.
2. **Zero cards = hard error.** `check(cards.isNotEmpty())` in the set-loading path with a
   "typo in package name?" message — converts the worst silent failure (set ships hollow) into an
   immediate, named error.
3. **Stamp `setCode` centrally for basics** in the same registry-load pass that stamps cards; delete
   the per-set `.copy(setCode = code)` boilerplate. Plus a tiny `MtgSetCatalogTest` (codes unique,
   dates parseable, basics non-empty or fallback set).

### 5.2 Keep enum-locked vocabularies honest — [LOW]

`Keyword`/`CounterType`/`AbilityFlag` stay enums — exhaustive matching engine-side is worth the
edit cost; don't chase `Subtype`-style stringly-typing here. But:
1. **Delete the parallel `Counters` string object** (`CounterType.kt:57-135`); callers route through
   the enum + the existing `resolveCounterType`. One vocabulary, one sync point.
2. **A unit test asserting every `Keyword` entry round-trips through `parseFromOracleText`** or is
   on an explicit `NOT_PARSED` list — "add enum entry, forget the parser" stops being tribal
   knowledge.

### 5.3 Export the plumbing-effect set from the SDK — [LOW]

Fidelity scoring filters structural-only nodes via a hardcoded set in the tooling
(`Fidelity.kt:22-25`). Move the classification to the source of truth — a marker interface
(`StructuralEffect : Effect`) or a `PLUMBING_EFFECT_TYPES` constant next to the serializer config —
so §2.1's `PartitionCollectionEffect` lands pre-classified.

---

## 6. TOOLING CO-EVOLUTION — the rules that protect every change above

Kept from the analysis doc's §4 because they're cheap and they de-risk the SDK work; the tooling
itself stays a downstream consumer.

1. **Same-PR rule:** an SDK PR that renames/collapses a type the bridge or emitter references
   updates both dictionaries in the same PR, with `EmitterGoldenTest` re-blessed and
   `just coverage-verify --set POR` green (POR stays 0-mismatch). Enforced via this doc + the
   `add-feature` skill. The §3 collapses *reduce* tooling surface when done this way (one cost
   vocabulary to render instead of three; per-variant dispatch becomes parameter mapping).
2. **Validate the emitter's SDK-name strings mechanically:** a hygiene test in `:mtgish-tooling`
   scanning handler sources for `Effects.X` / `Patterns.X` / raw-constructor string literals and
   verifying each against the SDK (facade members via reflection, type names via the bridge's
   existing `@SerialName` Registry scan). Build it to share the enumeration helper with the §1.2
   facade-coverage test — they're two views of the same contract. After this, an SDK rename breaks
   the tooling build immediately and by name, not at the next golden regen.
3. **Emit facades only:** audit the handlers once — each raw-constructor render either switches to
   the existing facade, gets a facade added (the §1.2 coverage test lists exactly which types lack
   one), or is demoted to decline→SCAFFOLD (the established "decline, don't widen" principle). End
   state: emitted drafts import only `dsl/`, identical to human-authored cards, and collapses stop
   touching emitter internals beyond argument mapping.

---

## 7. REJECTED / DEFERRED — and why

Recorded so these aren't re-proposed without new evidence.

### 7.1 Event-filter vocabulary collapse (analysis §2.4) — REJECTED

The proposal: convert `RecipientFilter`/`SourceFilter`/etc. cases to `GameObjectFilter`, delegate
evaluation to `PredicateEvaluator`, then dissolve the wrappers so `EventPattern` holds raw
`GameObjectFilter`. Rejected because the conversion is **not semantics-preserving**:

- Event filters evaluate against the **event payload**, which is frequently a snapshot of an object
  that no longer exists in that form — dies/leaves triggers match a creature already in the
  graveyard (the `ZoneChangeEvent` `lastKnownCardDefinitionId` LKI fallback exists precisely for
  this), damage triggers can reference a source that has left the battlefield,
  controller-at-event-time can differ from controller-now. `PredicateEvaluator` is built for live
  projected battlefield state. "Convert-then-delegate" therefore requires a full LKI/snapshot
  evaluation mode for `PredicateEvaluator` — an `add-feature`-sized engine project hiding inside a
  "retire a vocabulary" line item.
- The cited evidence ("`Matching` already wraps `GameObjectFilter`, proving the bridge works") only
  proves it works *for the live-entity cases it's currently used on*. Forcing all ~25 cases —
  including the LKI-sensitive ones — through that path is exactly the fail-open pattern §1.3
  closes elsewhere.
- Risk/reward is poor: the parallel vocabulary is ugly but small and **stable** (~25 cases that
  rarely change), while trigger matching is the most behavior-sensitive code in the engine, with
  only partial scenario coverage of edge timings. Dissolving the wrappers (step 3) would also delete
  the seam where event-specific semantics live, making the change hard to walk back.

**What survives instead:** nothing SDK-side for now. If emitter burden ever justifies it, the cheap
moves are (a) triaging only the genuinely redundant small hierarchies (`ControllerFilter`, maybe
`SpellCastPredicate`) case-by-case, or (b) sharing predicate logic *inside* the event evaluator for
live-entity cases without changing the SDK shape. Either requires first confirming, in source, how
the evaluator consumes event-snapshot fields.

### 7.2 Zone-move `MoveSpec` re-encoding (analysis §2.2, SDK half) — DEFERRED as premature

The section conflated two layers. The valuable half is **engine-side**: one `ZoneMoveService` as the
only code path that emits `ZoneChangeEvent` and consults replacement effects, so interception (Rest
in Peace, token cleanup, exile-instead) is right in one place instead of N. That needs **no SDK
encoding change** — both executors can delegate to it with their current fields — and should be
tracked as engine work, not in this doc.

The SDK half (nesting a `MoveSpec` value into `MoveToZoneEffect`/`MoveCollectionEffect`, unifying
`byDestruction` vs `MoveType`) churns the serialized form of every card that moves anything, forces
a corpus-wide re-bless, and touches both emitter render paths — for an encoding-aesthetics gain.
Revisit only after the engine service exists and §2.1's harvest has shrunk the specialized
exile/return family, when the residual duplication (if any) is visible.

### 7.3 Sealed-union refits (analysis §2.5) — DEFERRED, opportunistic

Both are real warts, neither is urgent, both need serialization-migration care — do them after the
§1 guarantees exist, as between-sets filler:
- **`TargetCount` sealed union** replacing the five interacting `count`/`minCount`/`optional`/
  `unlimited`/`dynamicMaxCount` knobs — worth doing (the magic-count and doc-comment-invariant bugs
  were real); keep old properties as derived accessors during migration; custom serializer reads
  legacy flat fields for one release.
- **Face-model unification** on `cardFaces` + `layout` with `backFace` as a derived `val` — do it
  when a DFC-heavy set next forces face work, not speculatively.
- **Boolean event flags → `requires`**: no migration; the §1.1 linter warns on *new* uses of the
  deprecated flags so the set monotonically shrinks. (This part lands with the linter.)

---

## 8. Sequencing

1. **Guarantees (§1)** — card linter, facade boundary + coverage tests, fail-open closes; plus the
   tooling name-validation test (§6.2). Days of work, no card migrations; everything after becomes
   dramatically safer.
2. **De-generators (§2)** — partition primitive + harvest, mechanics off the builder,
   `EntityMatches`. These stop the hierarchies re-growing while the collapses proceed.
3. **Collapses (§3)** — one family per PR, snapshot re-bless as the review artifact, bridge/emitter
   in the same PR. Start with counters; cost atoms last (biggest).
4. **Cast-choice unification (§4)** — proper `add-feature` project, after the guarantees.
5. **Hygiene (§5) and §7.3 refits** — anytime; ideal between-sets filler.

### What this buys, per goal

- **Correct:** the SDK↔content and SDK↔tooling contracts join the SDK↔engine contract (PR #606) as
  machine-checked; fail-open paths close; new-card structural bugs die in CI.
- **Elegant:** one encoding per concept — counters, control, evasion, costs, cast-time choices —
  via the proven facade/snapshot machinery, without betting the trigger system on a risky
  vocabulary migration.
- **Extensible:** a new set touches set-local files only; a new mechanic is a composition + maybe a
  tracker enum value; and every encoding the SDK simplifies makes the auto-gen tooling render more
  of the next set for free.
