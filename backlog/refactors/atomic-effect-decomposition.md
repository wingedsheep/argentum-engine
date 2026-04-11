# Atomic Effect Decomposition

Audit of monolithic effects in `mtg-sdk` that could be decomposed into atomic pipeline chains (Gather/Select/Move) to improve engine extensibility and modularity.

## Progress

- [x] Delete dead `DealDamageExileOnDeathEffect` — removed data class, CardValidator branch, reference.md entry
- [x] Decompose `ShuffleGraveyardIntoLibraryEffect` — replaced with `EffectPatterns.shuffleGraveyardIntoLibrary()` (Gather → Move pipeline), deleted executor
- [x] Decompose `EachPlayerReturnsPermanentToHandEffect` — ForEachPlayer → Gather → Select → Move (executor added during unified draw replacement shield refactor)
- [ ] Wire `EachOpponentDiscardsEffect` to existing `EffectPatterns` pipeline (unblocked by unified draw replacement shield refactor)
- [ ] Decompose `ExileAndReplaceWithTokenEffect` — needs StoreControllerRef / "Data Bus" infrastructure
- [ ] Decompose `ExileUntilLeavesEffect` (Oblivion Ring) — `MoveToZone(exile, storeAs) + StoreEntityRefEffect`; engine-side executor is 77 lines and bundles zone change with linked-return bookkeeping
- [ ] Decompose `RevealUntilEffect` — engine executor is 136 lines that bakes Gather + Filter + Reveal together; should decompose into `Gather(top N) → Filter → Reveal → Move` via atomic primitives
- [ ] Decompose `PutCreatureFromHandSharingTypeWithTappedEffect` — needs context-aware filter; engine executor is 98 lines and should vanish once the filter exists
- [x] Decompose `PatriarchsBiddingEffect` — replaced with `EffectPatterns.patriarchsBidding()` (EachPlayerChoosesCreatureType → ForEachPlayer(Gather → Move) pipeline) + added `HasSubtypeInStoredList`/`HasSubtypeFromVariable` context-aware predicates. Also decomposed `ChooseCreatureTypeUntapEffect` → `EffectPatterns.chooseCreatureTypeUntap()` (ChooseOption → ForEachInGroup untap)
- [ ] Decompose `EachPlayerDiscardsOrLoseLifeEffect` — needs ChoiceGate primitive
- [ ] Decompose `EachPlayerMayDrawEffect` — needs SelectNumber + remainder arithmetic
- [ ] Decompose `DiscardAndChainCopyEffect` — needs OptionalCostEffect + MayCopySourceEffect (see Chain Gate below)

---

## Design Patterns for Decomposition

### Pattern 1: The "Data Bus" (Enhanced Variable Binding)

The biggest reason for monolithic effects is the need to "remember" an object from one step to use in the next. The existing `storeAs`/`storeSelected` mechanism handles collections, but we need richer data flow for entity-level references like "the controller of the creature we just exiled."

**Problem:** `ExileAndReplaceWithTokenEffect` exists because the system needs to know *who* the controller of the exiled creature was to give *them* the token.

**Solution: `ControllerOfStoredEntity` target reference**

Instead of a monolithic effect, chain atomic steps with a new target type that can dereference a stored entity's controller:

```kotlin
// Instead of a monolith, chain:
val effect = sequence(
    MoveToZoneEffect(target, Zone.EXILE, storeAs = "exiledEntity"),
    CreateTokenEffect(
        count = 1,
        stats = ...,
        // Reference the controller of the entity we just moved
        target = EffectTarget.ControllerOfStoredEntity("exiledEntity")
    )
)
```

**Implementation:** Add a `storeAs` parameter to `MoveToZoneEffect` (or a new `StoreEntityRefEffect` step) that saves entity metadata (controller, P/T, types) into the pipeline context before the zone change removes it. New `EffectTarget` variants can then dereference these stored refs.

### Pattern 2: The "Choice Gate" (Unless/Or Effects)

Many monolithic effects exist because they present a player with a forced choice between two outcomes. Rather than encoding each variant as its own effect, create a generic `ChoiceGateEffect`.

**Problem:** `EachPlayerDiscardsOrLoseLifeEffect`, "Unless you pay X" effects, and similar fork-in-the-road patterns all require custom executors.

**Solution: `ChoiceGateEffect`**

```kotlin
// Decomposed "Unless you pay 3 life, sacrifice this"
val unlessEffect = ChoiceGateEffect(
    choicePlayer = Player.You,
    optionA = ChoiceOption(
        label = "Pay 3 life",
        effect = LoseLifeEffect(3)
    ),
    optionB = ChoiceOption(
        label = "Sacrifice this",
        effect = SacrificeSelfEffect
    ),
    isOptional = false // One must be chosen
)
```

This replaces the need for `ConditionalOnDiscarded` and similar ad-hoc primitives — the choice itself is the atomic unit.

### Pattern 3: The "Chain Gate" (Optional Cost → Copy Source)

Chain spells (Chain of Vapor, Chain of Smog, etc.) follow a repeatable pattern: resolve an effect, then the affected player may sacrifice a resource to copy the spell onto a new target.

**Problem:** `DiscardAndChainCopyEffect` was listed as "not decomposable" because chain-copy requires stack manipulation.

**Solution: `OptionalCostEffect` + `MayCopySourceEffect`**

```kotlin
// Decomposed Chain of Vapor
val chainVapor = sequence(
    MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND),
    OptionalCostEffect(
        cost = SacrificeEffect(GameObjectFilter.Land),
        ifPaid = MayCopySourceEffect(newTargetFilter = TargetFilter.Permanent)
    )
)
```

The `MayCopySourceEffect` puts a copy of the spell on the stack with new targets chosen by the affected player. This is a reflexive trigger variant — the "chain" is just repeated reflexive copying.

### Pattern 4: Effect Decorators (Cross-Cutting Modifiers)

Instead of adding boolean flags like `noRegenerate: Boolean` to every destruction effect, use wrapper effects that modify the execution context.

**Problem:** Properties like "can't be regenerated", "exile instead if it would die", etc. are cross-cutting concerns that bloat individual effect signatures.

**Solution: `WithReplacementModifier` wrapper**

```kotlin
// "Destroy target creature. It can't be regenerated."
val effect = WithReplacementModifier(
    modifier = ReplacementModifier.PreventRegeneration,
    wrappedEffect = Effects.Destroy(EffectTarget.ContextTarget(0))
)
```

This keeps individual effects focused on their single primitive action while decorators handle edge-case modifiers.

---

## Details

### ~~Dead Code: `DealDamageExileOnDeathEffect`~~ (Done)

Had **no executor**, **no cards using it**, **no tests**. Carbonize already used the decomposed `MarkExileOnDeath then DealDamage` pattern. Deleted data class, CardValidator branch, and reference.md entry.

### ~~`ShuffleGraveyardIntoLibraryEffect`~~ (Done)

Replaced with `EffectPatterns.shuffleGraveyardIntoLibrary()` — a Gather(graveyard) → Move(library, Shuffled) pipeline. Deleted `ShuffleGraveyardIntoLibraryExecutor`. All Reminisce tests pass unchanged.

### ~~`EachPlayerReturnsPermanentToHandEffect`~~ (Done)

Added `EachPlayerReturnsPermanentToHandExecutor` that delegates to `EffectPatterns.eachPlayerReturnsPermanentToHand()` pipeline. Created during the unified draw replacement shield refactor, which replaced 5 specific `SerializableModification.ReplaceDrawWith*` variants with a single `ReplaceDrawWithEffect` that stores the replacement `Effect` directly and delegates to the effect execution pipeline at consumption time.

### `EachOpponentDiscardsEffect`
**File:** `mtg-sdk/.../scripting/effects/DrawingEffects.kt` | **Difficulty:** Medium

`EffectPatterns.eachOpponentDiscards()` already exists for the basic case but isn't wired to `Effects.EachOpponentDiscards()`. The draw-replacement system blocker is now resolved (unified `ReplaceDrawWithEffect` delegates to the pipeline, so replacing the concrete effect type is safe). Syphon Mind variant needs count tracking across ForEachPlayer iterations.

### `ExileAndReplaceWithTokenEffect`
**File:** `mtg-sdk/.../scripting/effects/RemovalEffects.kt` | **Difficulty:** Medium | **Pattern:** Data Bus

Exile a creature, then its controller creates a token (Crib Swap-style). Token must be created under the **exiled creature's controller**, but exile removes the `ControllerComponent`.

**Decomposed with Data Bus:**
```kotlin
val effect = sequence(
    MoveToZoneEffect(target, Zone.EXILE, storeAs = "exiledEntity"),
    CreateTokenEffect(
        count = 1,
        stats = ...,
        target = EffectTarget.ControllerOfStoredEntity("exiledEntity")
    )
)
```

**Prerequisite:** Implement `storeAs` on zone-move effects and `ControllerOfStoredEntity` target type.

### `ExileUntilLeavesEffect` (Oblivion Ring)
**File:** `mtg-sdk/.../scripting/effects/RemovalEffects.kt` | **Difficulty:** Medium | **Pattern:** Data Bus

Exile a permanent and link it to the source, so a later LeavesBattlefield trigger can return it. The
engine-side executor (`rules-engine/.../handlers/effects/removal/ExileUntilLeavesExecutor.kt`, 77
lines) bundles two conceptually independent things: (a) moving the target to exile, and (b) writing
a `LinkedExileComponent` onto the source so a separately-defined trigger can later find the exiled
card. Today the effect is *one node* that does both, which means a card like Banishing Light (same
mechanic, different flavor) can't reuse the primitive without sharing the exact linked-return
plumbing.

**Decomposed with Data Bus:**
```kotlin
// Enters-the-battlefield ability — exile and remember
val etb = sequence(
    MoveToZoneEffect(
        target = EffectTarget.ContextTarget(0),
        zone = Zone.EXILE,
        storeAs = "oblivionRingTarget" // stores entity ref on source
    )
)

// Leaves-the-battlefield trigger — return what we stored
val ltb = MoveToZoneEffect(
    target = EffectTarget.StoredEntityTarget("oblivionRingTarget"),
    zone = Zone.BATTLEFIELD
)
```

**Prerequisite:** `storeAs` on `MoveToZoneEffect` (see Data Bus pattern above, shared with
`ExileAndReplaceWithTokenEffect`). Once that lands, delete `ExileUntilLeavesEffect`,
`ExileUntilLeavesExecutor`, and `LinkedExileComponent` — everything flows through
`StoredEntityTarget` and the normal `MoveToZoneEffect` executor.

**Engine-side cleanup:** After the SDK change, also audit `ReturnLinkedExileEffect` and the
handful of card-side LeavesBattlefield triggers in `mtg-sets/` that currently reference
`LinkedExileComponent` directly. They should all route through the standard stored-ref lookup.

### `RevealUntilEffect`
**File:** `mtg-sdk/.../scripting/effects/LibraryEffects.kt` | **Difficulty:** Medium

"Reveal cards from your library until you find one matching X." Current engine executor
(`rules-engine/.../handlers/effects/library/RevealUntilExecutor.kt`, 136 lines) walks the library
one card at a time, checks the match filter, optionally checks `chosenCreatureType`, and emits a
`CardsRevealedEvent` itself — all in one method. Subsequent `MoveCollectionEffect` steps do the
actual zone change.

The existing shape is already pipeline-ish (it writes into `storeMatch` / `storeRevealed` named
collections), but the **gather** and **filter** steps are fused and the **reveal event emission**
is baked in. Three cards that all say "reveal until X" have to share this executor even though each
differs only in the filter.

**Target shape:**
```kotlin
val effect = sequence(
    GatherCardsEffect(
        source = CardSource.LibraryStreaming(playerRef = Player.You),
        filter = effect.matchFilter, // arbitrary GameObjectFilter
        stopOnFirstMatch = true,
        storeAs = "revealed",
        storeMatchAs = "match"
    ),
    RevealCollectionEffect(from = "revealed") // emits CardsRevealedEvent
)
```

This requires a new `CardSource.LibraryStreaming` (walks library top-down, stops when a filter
match is found) and a standalone `RevealCollectionEffect` primitive. Once both exist, delete
`RevealUntilEffect`, `RevealUntilExecutor`, and the hardcoded `matchChosenCreatureType` branch
(replaced by a composable `SharesCreatureTypeWith(contextKey)` filter, which is the same prerequisite
as `PutCreatureFromHandSharingTypeWithTappedEffect` below).

### `PutCreatureFromHandSharingTypeWithTappedEffect`
**File:** `mtg-sdk/.../scripting/effects/LibraryEffects.kt` | **Difficulty:** Medium

Needs a context-aware `GameObjectFilter` like `SharesCreatureTypeWith(contextKey)`.

Engine-side the executor
(`rules-engine/.../handlers/effects/library/PutCreatureFromHandSharingTypeExecutor.kt`, 98 lines)
pulls subtypes off `context.tappedPermanents`, filters the hand, builds a `SelectCardsDecision`,
and hands the rest to `PutFromHandContinuation`. All of that is boilerplate wrapping one thing the
engine can't express yet: **a filter that references a previously-stored entity's subtypes**.

**Target shape:**
```kotlin
val effect = sequence(
    GatherCardsEffect(
        source = CardSource.Hand(playerRef = Player.You),
        filter = GameObjectFilter.Creature.and(
            SharesSubtypeWith(contextKey = "tappedPermanents")
        ),
        storeAs = "matches"
    ),
    SelectFromCollectionEffect(from = "matches", selection = SelectionMode.ChooseUpTo(1), storeAs = "chosen"),
    MoveCollectionEffect(from = "chosen", destination = CardDestination.ToZone(Zone.BATTLEFIELD))
)
```

**Prerequisite:** `SharesSubtypeWith(contextKey)` predicate (reads subtypes of the entities stored
under `contextKey` in `EffectContext`). Shared with `RevealUntilEffect`'s
`matchChosenCreatureType` branch above. Once the predicate lands, delete both the SDK effect and
`PutCreatureFromHandSharingTypeExecutor`.

### `HarshMercyEffect` / `PatriarchsBiddingEffect`
**File:** `mtg-sdk/.../scripting/effects/RemovalEffects.kt` | **Difficulty:** Hard

Both require multi-player type choice aggregation — each player chooses a creature type, then a single filter uses the union of all choices. Needs an "aggregate choices" primitive.

### `EachPlayerDiscardsOrLoseLifeEffect`
**File:** `mtg-sdk/.../scripting/effects/DrawingEffects.kt` | **Difficulty:** Hard | **Pattern:** Choice Gate

Each player discards, then loses life if the discarded card wasn't a creature.

**Decomposed with Choice Gate:**
```kotlin
ForEachPlayer(Player.Each) -> [
    ChoiceGateEffect(
        choicePlayer = Player.Current,
        optionA = ChoiceOption("Discard a creature card", DiscardEffect(filter = GameObjectFilter.Creature)),
        optionB = ChoiceOption("Lose 2 life", LoseLifeEffect(2)),
        isOptional = false
    )
]
```

Note: This reframes the mechanic from "discard then check" to "choose: discard creature or lose life," which is functionally equivalent for the cards that use it.

### `EachPlayerMayDrawEffect`
**File:** `mtg-sdk/.../scripting/effects/DrawingEffects.kt` | **Difficulty:** Hard

Each player may draw up to N cards, gaining life for each not drawn. Needs `SelectNumber` + remainder arithmetic.

### `DiscardAndChainCopyEffect`
**File:** `mtg-sdk/.../scripting/effects/DrawingEffects.kt` | **Difficulty:** Medium | **Pattern:** Chain Gate

Previously listed as "not decomposable," but can be decomposed using the Chain Gate pattern:

```kotlin
val effect = sequence(
    DiscardEffect(count = 2),
    OptionalCostEffect(
        cost = SacrificeEffect(GameObjectFilter.Land),
        ifPaid = MayCopySourceEffect(newTargetFilter = TargetFilter.Player)
    )
)
```

**Prerequisite:** Implement `OptionalCostEffect` and `MayCopySourceEffect` primitives.

---

## Decomposition Quick Reference

| Monolithic Effect | Pattern | Decomposed Chain |
|---|---|---|
| `ExileAndReplaceWithTokenEffect` | Data Bus | `MoveToZone(storeAs)` → `CreateToken(ControllerOfStoredEntity)` |
| `ExileUntilLeavesEffect` | Data Bus | `MoveToZone(exile, storeAs)` + LTB `MoveToZone(battlefield, StoredEntityTarget)` |
| `RevealUntilEffect` | Existing Pipeline | `Gather(LibraryStreaming, filter, stopOnFirstMatch)` → `Reveal` |
| `PutCreatureFromHandSharingTypeWithTappedEffect` | Existing Pipeline | `Gather(hand, SharesSubtypeWith(ctxKey))` → `Select` → `Move(battlefield)` |
| `EachPlayerDiscardsOrLoseLifeEffect` | Choice Gate | `ForEachPlayer` → `ChoiceGate(Discard \| LoseLife)` |
| `DiscardAndChainCopyEffect` | Chain Gate | `Discard` → `OptionalCost(Sacrifice → CopySource)` |
| `EachOpponentDiscardsEffect` | Existing Pipeline | Wire to `EffectPatterns.eachOpponentDiscards()` |
| `EachPlayerReturnsPermanentToHandEffect` | Existing Pipeline | `ForEachPlayer` → `Gather` → `Select` → `Move` |

---

## Atomic Effect Checklist

A new `Effect` class should only be created if it performs a **unique primitive action** on the game state:

- **Primitive (OK as standalone effect):** Moving a card between zones, changing a numerical value, adding/removing a component, creating a token, dealing damage.
- **Non-Primitive (should be Composite/Conditional/Iterator):** Doing A then B, doing A if B is true, doing A for every B. These should always use `CompositeEffect`, `ConditionalEffect`, `ForEachPlayer`/`ForEachTarget`, or `ChoiceGateEffect`.

---

## Not Decomposable (Leave Monolithic)

| Effect | File | Reason |
|--------|------|--------|
| `ReadTheRunesEffect` | DrawingEffects.kt | Iterative discard-or-sacrifice loop per card drawn |
| `TradeSecretsEffect` | DrawingEffects.kt | Unbounded repeat loop |

---

## New Primitives Needed (Ordered by Unblock Count)

| Primitive | Unblocks | Description |
|---|---|---|
| `ChoiceGateEffect` | `EachPlayerDiscardsOrLoseLife`, "Unless" effects | Binary choice between two effect branches |
| `storeAs` on zone-move + `StoredEntityTarget` / `ControllerOfStoredEntity` | `ExileAndReplaceWithTokenEffect`, `ExileUntilLeavesEffect` | Save entity metadata before zone change; dereference later for return-exile and same-controller token creation |
| `OptionalCostEffect` + `MayCopySourceEffect` | `DiscardAndChainCopyEffect`, Chain spells | Pay optional cost to copy spell with new targets |
| `SharesCreatureTypeWith(contextKey)` / `SharesSubtypeWith(contextKey)` filter | `PutCreatureFromHandSharingType...`, `RevealUntilEffect.matchChosenCreatureType` branch | Context-aware creature type matching against previously-stored entities |
| `CardSource.LibraryStreaming(filter, stopOnFirstMatch)` + `RevealCollectionEffect` | `RevealUntilEffect` | Walk library top-down with filter, emit reveal as a separate primitive |
| `AggregateChoices` primitive | `HarshMercyEffect`, `PatriarchsBiddingEffect` | Multi-player type choice aggregation |
| `SelectNumber` + remainder arithmetic | `EachPlayerMayDrawEffect` | Choose a number, compute remainder for secondary effect |
