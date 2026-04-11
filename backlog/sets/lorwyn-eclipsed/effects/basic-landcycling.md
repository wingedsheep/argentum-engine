# Basic Landcycling

**Status:** Likely a thin extension of existing `Typecycling`.
**Cards affected in Lorwyn Eclipsed:** 2 (`Stratosoarer`, `Kulrath Zealot`). Low volume but the
set's only cycling variant, so blocking these two cards is cheap.
**Priority:** Low-medium.

## Rules text

> Basic landcycling {2} *({2}, Discard this card: Search your library for a basic land card,
> reveal it, put it into your hand, then shuffle.)*

The only difference from vanilla cycling is the "search library for a basic land card" part. We
already have `KeywordAbility.Typecycling(subtype, cost)` in
`mtg-sdk/.../scripting/KeywordAbility.kt` and the engine has a `TypecycleCardHandler`. The only
mismatch: Typecycling is parameterized by a single creature/land **subtype** (Slivercycling,
Wizardcycling, Mountaincycling). "Basic landcycling" searches for *any card with a basic land
type* — Plains, Island, Swamp, Mountain, or Forest — and must also tolerate "basic" as a modifier.

## Implementation plan

### 1. SDK — decide: extend Typecycling or add BasicLandcycling?

**Option A (preferred): extend Typecycling with a `GameObjectFilter` instead of a raw subtype.**

```kotlin
@SerialName("Typecycling")
@Serializable
data class Typecycling(
    val filter: GameObjectFilter,
    val cost: ManaCost,
    val displayName: String  // for rendering: "Swampcycling", "Basic landcycling", etc.
) : KeywordAbility
```

This is a breaking change for existing Typecycling cards. Do a one-shot migration: old
`Typecycling("Sliver", cost)` becomes `Typecycling(GameObjectFilter.CardsWithSubtype("Sliver"), cost, "Slivercycling")`.
Basic landcycling becomes
`Typecycling(GameObjectFilter.Card.withSupertype("Basic").andAny(BasicLandSubtypes), cost, "Basic landcycling")`.

**Option B: add a sibling `BasicLandcycling(cost)` keyword.**

Less general but minimal blast radius. Only pick this if the refactor in Option A turns out to
touch more than a handful of files.

Either way, expose `basicLandcycling(cost)` in the DSL.

### 2. Engine — `TypecycleCardHandler`
If Option A: replace the subtype string comparison inside the handler's library search with a
filter match using the shared predicate evaluator. All existing Slivercycling / Mountaincycling
code paths collapse into the same filter-based path.

If Option B: add a parallel `BasicLandcycleCardHandler` that reuses the search/shuffle continuation
infrastructure but filters by "has any basic land type".

### 3. Cards

```kotlin
card("Stratosoarer") {
    manaCost = "{5}{U}"
    typeLine = "Creature — Elemental Bird"
    power = 4; toughness = 3
    keywords(Keyword.FLYING)
    basicLandcycling("{2}")  // Option A DSL
}
```

### 4. Tests

- Scenario: discard `Stratosoarer`, pay `{2}`, search library for a Plains → put into hand,
  shuffle, draw nothing extra. Cycling trigger (`CardCycledEvent`) fires so cards like Astral
  Slide still work.
- Scenario: no basic land in library → search fails to find, card still cycled, shuffle
  happens. Library count decremented? (No — cycling-via-typecycling does not cause a "draw a
  card"; it's a search instead. Verify the existing handler matches this rule.)
- Regression: Slivercycling / Mountaincycling cards still work post-migration.

## Dependencies

- None new. Builds entirely on existing cycling infrastructure.
- Not blocked by any other ECL effect doc.
