# Inline pipeline DSL — typed slot handles for card-file pipelines

_Drafted 2026-06-12, out of the PR #630 (Drop of Honey) review discussion. Status: §5 step 1
(builder core) + pilot migration implemented — `mtg-sdk/dsl/PipelineBuilder.kt`, entry point
`Effects.Pipeline { }`, reference in `card-sdk-language-reference.md` §5.5; `inv/cards/Lobotomy.kt`
migrated byte-identically. Steps 2–5 (new-card pilots, corpus migration, facade closure, emitter
renderer) remain open._

## 0. The problem

Two authoring rules collide on multi-step pipelines, and the corpus has quietly grown a third
state that neither rule describes:

- **"No single-use patterns"** says: don't extract a one-off composition into a named
  `*Patterns` entry; inline it until a second user shows up (exception: named MTG mechanics).
- **The facade boundary** (SDK review §2.3, `FacadeBoundaryTest`) says: card files construct
  effects through curated facades, never raw foundational types — that contract is what lets
  the SDK refactor the underlying shapes without touching ~3,500 card files.
- **Reality:** the facade test's forbidden list is narrow (`CompositeEffect(`,
  `MoveToZoneEffect(`, `ForEachInGroupEffect(`, raw `AdditionalCost.*`/`PayCost.*`). The
  pipeline *step* constructors are not on it, so **231 card files today inline raw
  `GatherCardsEffect(...)` / `SelectFromCollectionEffect(...)` / `MoveCollectionEffect(...)`
  chains** inside `Effects.Composite(...)`, hand-threading string slot keys
  (`storeAs = "hand"`, `from = "hand"`) between steps (e.g. `inv/cards/Lobotomy.kt`,
  `xln/cards/SunbirdsInvocation.kt`, `bro/cards/FlowOfKnowledge.kt`).

So a card author picking up a one-off pipeline card faces three bad options: add a named
single-use pattern (violates rule 1 — PR #630 did this, defensibly, because Porphyry Nodes is a
known second user), inline raw constructors with hand-written string keys (violates the spirit
of rule 2, and hand-threaded keys are exactly the error class `CardLinter`'s dataflow check
exists to catch — the Atmospheric Greenhouse silent no-op), or mis-shape the card to fit an
existing facade.

The original design intent of the atomic pipeline (architecture-principles §1.5) was that
one-off cards compose Gather → Select → Move **without new SDK surface per card**. This
proposal restores that intent behind the facade boundary instead of around it.

## 1. Proposal in one paragraph

Add a `pipeline { }` builder to the SDK DSL whose steps return **typed slot handles** instead
of taking hand-written string keys. The builder compiles to the exact same
`CompositeEffect` + step-effect JSON tree the engine already executes — zero engine changes,
zero new serialized types, zero change to the JSON/custom-card authoring path. Slot keys are
auto-generated deterministically (with an optional explicit `name =` override), so the
dangling-slot error class is impossible at the Kotlin authoring layer by construction, and the
mtgish emitter gets one canonical rendering for arbitrary pipeline trees instead of needing a
named bridge entry per pattern.

```kotlin
// Drop of Honey's upkeep effect, inline — no named GroupPatterns entry needed:
effect = Effects.Pipeline {
    val tied = gather(GameObjectFilter.Creature.hasLeastPowerAmongAllCreatures())
    val pick = chooseExactly(1, from = tied,
        prompt = "Choose a creature with the least power to destroy",
        useTargetingUI = true)
    destroy(pick, noRegenerate = true)
}
```

## 2. Design

### 2.1 Slot handles

One handle type per `CardLinter.Space` namespace, mirroring the linter's existing model of the
`EffectContext` stores:

| Handle | Backing store | Produced by (examples) | Consumed by (examples) |
|---|---|---|---|
| `CollectionSlot` | `storedCollections` | `gather`, `chooseExactly`, `filter`, `captureControllers` | `move`, `reveal`, `select`, `forEachCaptured` |
| `NumberSlot` | `storedNumbers` | `storeNumber` | `DynamicAmount.VariableReference` |
| `ChosenSlot` | `chosenValues` | `storeCardName`, `chooseOption`, `noteCreatureType` | `GameObjectFilter.namedFromVariable`, `CardPredicate.NameEqualsChosen` |
| `SubtypeGroupsSlot` | `storedSubtypeGroups` | `gatherSubtypes` | subtype-matching filters |

A handle is a thin value class wrapping the generated key string. Steps take handles as
inputs and return them as outputs; the **only** way to obtain one is from a step that produced
it, so a read-without-write cannot be expressed. Kotlin's lexical scoping then gives non-linear
dataflow for free — a handle consumed by two steps, or referenced inside a nested block, is
just a closure capture.

### 2.2 Key generation — deterministic, with an escape hatch

Auto-keys are positional per builder instance: `"<verb><stepIndex>"` (`gathered0`,
`selected1`, `matching3`). Properties:

- **Rename-stable:** renaming the Kotlin `val` doesn't touch the JSON, so snapshot goldens
  don't churn on a source-level refactor.
- **Reorder-sensitive:** reordering steps changes keys — acceptable, because reordering
  changes the serialized tree anyway.
- **Override:** every producing step takes an optional `name: String? = null`. Two uses:
  readable goldens for gnarly cards, and **churn-free migration** (§5) — an existing inline
  card migrated with `name = "hand"` etc. serializes byte-identically to today.

Duplicate explicit names within one builder are a build-time `require` failure.

### 2.3 Step vocabulary

The builder exposes one verb per existing pipeline step type. Initial set (covers every step
the current 231 inline cards use):

| Builder verb | Serializes to | Slots |
|---|---|---|
| `gather(source/filter)` | `GatherCardsEffect` | → `CollectionSlot` |
| `gatherUntilMatch(...)` | `GatherUntilMatchEffect` | → `CollectionSlot`s |
| `chooseExactly(n, from)` / `chooseUpTo(n, from)` / `selectAll(from)` | `SelectFromCollectionEffect` | slot → selected (+ optional `remainder`) |
| `filter(from, filter)` | `FilterCollectionEffect` | slot → matching (+ optional `rest`) |
| `move(from, destination)` / sugar `destroy`, `exile`, `toHand`, `toLibraryTop`… | `MoveCollectionEffect` | slot → |
| `reveal(from)` | `RevealCollectionEffect` | slot → |
| `captureControllers(from)` | `CaptureControllersEffect` | slot → `CollectionSlot` (parallel list) |
| `forEachCaptured(slot) { … }` | `ForEachCapturedControllerEffect` | slot → nested scope |
| `gatherSubtypes(from)` | `GatherSubtypesEffect` | slot → `SubtypeGroupsSlot` |
| `storeCardName(from)` | `StoreCardNameEffect` | slot → `ChosenSlot` |
| `storeNumber(amount)` | `StoreNumberEffect` | → `NumberSlot` |
| `choosePile(a, b, chooser)` | `ChoosePileEffect` | 2 slots → chosen + other |
| `ifNotEmpty(slot, filter?) { … } orElse { … }` | `ConditionalOnCollectionEffect` | slot → branch scopes |
| `whenMatches(slot, filter)` | `Condition` = `CollectionContainsMatch` | slot → condition |
| `run(effect)` | any other `Effect`, verbatim | — |

`run(...)` keeps the builder open: non-pipeline effects (a `ShuffleLibraryEffect`, a
`DealDamage`) interleave without the builder needing a verb for everything. The vocabulary
grows with **step types** (one verb per new `Effect`, reusable corpus-wide), never with cards.

Optional secondary outputs (`storeRemainder`, `storeNonMatching`) are only serialized when the
card actually requests the handle — `chooseExactly(...)` returns the selected slot;
`chooseExactlySplit(...)` (or a `withRemainder()` variant) returns both. This keeps emitted
JSON free of never-read writes, which the linter flags as warnings today.

### 2.4 Worked example — non-linear dataflow (Lobotomy, today vs proposed)

Today (`inv/cards/Lobotomy.kt`): 7 raw constructors inside `Effects.Composite`, five
hand-written keys (`"hand"`, `"chosen"`, `"chosenName"`, `"toExile"`), `from`/`storeAs`
threaded by string equality. Proposed:

```kotlin
effect = Effects.Pipeline {
    run(RevealHandEffect(player))
    val hand = gather(CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)), name = "hand")
    val chosen = chooseExactly(1, from = hand,
        filter = GameObjectFilter(cardPredicates = listOf(CardPredicate.Not(CardPredicate.IsBasicLand))),
        prompt = "Choose a card other than a basic land card",
        alwaysPrompt = true, showAllCards = true)
    val chosenName = storeCardName(chosen)
    val toExile = gather(CardSource.FromMultipleZones(
        zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
        player = Player.ContextPlayer(0),
        filter = GameObjectFilter.Any.namedFromVariable(chosenName)))
    exile(toExile, owner = Player.ContextPlayer(0))
    run(ShuffleLibraryEffect(target = EffectTarget.ContextTarget(0)))
}
```

The `chosenName` handle flowing into `namedFromVariable(...)` is the cross-namespace case: the
filter factory gains an overload taking `ChosenSlot` alongside the existing `String` one (the
string overload stays — it's the JSON/custom-card surface).

### 2.5 Branch-on-gathered (PR #618 idiom)

```kotlin
effect = Effects.Pipeline {
    val drawn = gather(CardSource.TopOfLibrary(1))
    reveal(drawn)
    ifNotEmpty(drawn, filter = GameObjectFilter.Creature) {
        toHand(drawn)
    } orElse {
        move(drawn, CardDestination.ToZone(Zone.GRAVEYARD))
    }
}
```

Branch bodies are nested builder scopes; handles from the outer scope are visible inside
(matching the engine's actual `EffectContext` scoping, which `CardLinter` already models —
nested *abilities* start fresh scopes, branches don't).

## 3. What does NOT change

- **Engine:** nothing. The builder emits the existing `CompositeEffect` tree.
- **JSON contract / custom cards on the fly:** nothing. Raw step types stay `@Serializable`
  with string keys; a scenario-builder or player-authored card keeps targeting the AST
  directly. `FacadeBoundaryTest` is a source lint on `mtg-sets` only.
- **`CardLinter`:** stays exactly as is — it lints the serialized tree, so it remains the
  backstop for the JSON path and for anything the builder can't statically prevent
  (cross-trigger flows, `Self`-vs-`ContextTarget` inside `ForEach`).
- **Named patterns for real mechanics:** Scry, Mill, FactOrFiction, SearchLibrary etc. stay in
  `*Patterns`. The "no single-use patterns" rule is *rescoped*, not repealed: named entries
  are for named MTG mechanics and shapes with a demonstrated second user; one-offs go inline
  via `pipeline { }`.
- **`add-feature` for new step semantics:** a card needing a genuinely new step type (a new
  capture kind, a new decision shape) still adds the `Effect` + executor first. The builder
  only composes the existing vocabulary.

## 4. mtgish alignment

The real verification gate (`coverage-verify`) diffs serialized capability trees, which are
authoring-surface-independent — no change there. The win is in the **emitter**: today,
rendering a multi-step card requires either a named-pattern bridge entry or declining to
SCAFFOLD. With the builder there is one mechanical rendering for *any* pipeline tree
(`CompositeEffect` of known step types → `pipeline { }` text), which raises the renders-whole
ceiling on unseen sets without per-pattern emitter handlers, and keeps the
"decline → SCAFFOLD, don't widen" principle intact for steps the emitter doesn't know.

## 5. Migration & enforcement

Phased so each step is independently shippable and the goldens stay quiet:

1. **Builder core** (`mtg-sdk/dsl/PipelineBuilder.kt`, entry point `Effects.Pipeline { }`):
   handles, deterministic keys, the §2.3 verb set, `require`-on-duplicate-names. Unit tests
   assert byte-identical serialization against hand-built `CompositeEffect` trees for the
   worked examples. Update `docs/card-sdk-language-reference.md` in the same change (hard
   rule).
2. **Pilot:** author the next 2–3 one-off pipeline cards with it; fold PR #630's
   `destroyLeastPowerCreature` question into practice (that one *keeps* its named pattern —
   Porphyry Nodes is the second user).
3. **Mechanical migration of the 231 inline cards**, per set, using explicit
   `name = "<existing key>"` everywhere → serialized JSON is unchanged, snapshot goldens are
   untouched, review is pure source diff. (Fan-out friendly: edit-only agents, no Gradle.)
4. **Close the boundary:** add the raw step constructors (`GatherCardsEffect(`,
   `SelectFromCollectionEffect(`, `MoveCollectionEffect(`, `FilterCollectionEffect(`, …) to
   `FacadeBoundaryTest`'s forbidden list with hint "use Effects.Pipeline { }". Rescope the
   "no single-use patterns" guidance in the review-changes skill + memory.
5. **Emitter renderer** (independent, optional): generic pipeline-tree → `pipeline { }` text
   in the mtgish emitter; verify with `coverage-verify` (POR must stay 0-mismatch).

Steps 1–2 deliver the authoring win on their own; 3–4 are hygiene that can trail; 5 is a
separate mtgish work item.

## 6. Open questions

- **Entry point naming:** `Effects.Pipeline { }` (foundational facade, proposed here) vs
  `Patterns.pipeline { }` (it composes, but isn't itself a named composition). Mild preference
  for `Effects.Pipeline` since it replaces `Effects.Composite` as the sequencing primitive in
  card files.
- **Auto-key spelling:** `gathered0`/`selected1` (verb-prefixed, proposed) vs bare `slot0` —
  verb-prefixed keeps goldens self-describing at near-zero cost.
- **Should `Effects.Composite` eventually be banned in cards too**, once every sequencing use
  is a pipeline? Probably yes, but decide after step 3 shows what's left.
- **Cross-namespace handle overloads** (filter factories accepting `ChosenSlot`/`NumberSlot`):
  add lazily, per call site that migration actually hits, rather than up front.
