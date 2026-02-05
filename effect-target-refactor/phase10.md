# Phase 10 — Migrate Cards to `MoveToZoneEffect`

## Goal

Replace all usages of the six old zone-moving effect types with `MoveToZoneEffect` across card definitions, test cards, and scenario tests. Also migrate `TargetCardInGraveyard` to `TargetObject` where applicable.

## Migration Table

| Old Effect | New Form | Card Count |
|-----------|----------|-----------|
| `ReturnFromGraveyardEffect(target, HAND)` | `MoveToZoneEffect(target, Zone.Hand)` | ~10 |
| `ReturnFromGraveyardEffect(target, BATTLEFIELD)` | `MoveToZoneEffect(target, Zone.Battlefield)` | ~2 |
| `ReturnToHandEffect(target)` | `MoveToZoneEffect(target, Zone.Hand)` | ~9 |
| `PutOnTopOfLibraryEffect(target)` | `MoveToZoneEffect(target, Zone.Library, ZonePlacement.Top)` | ~4 |
| `ShuffleIntoLibraryEffect(target)` | `MoveToZoneEffect(target, Zone.Library, ZonePlacement.Shuffled)` | ~2 |
| `DestroyEffect(target)` | `MoveToZoneEffect(target, Zone.Graveyard, byDestruction = true)` | ~32 |
| `ExileEffect(target)` | `MoveToZoneEffect(target, Zone.Exile)` | 0 |

## Steps

### 10a — Migrate `ReturnFromGraveyardEffect` (~10 cards)

Cards: Gravedigger, RaiseDead, BreathOfLife, ElvenCache, DejaVu, + others

```kotlin
// Before
effect = ReturnFromGraveyardEffect(destination = SearchDestination.HAND)

// After
effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Hand)
```

For BreathOfLife (destination = BATTLEFIELD):
```kotlin
effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Battlefield)
```

### 10b — Migrate `ReturnToHandEffect` (~9 cards)

Cards: Man-o'-War, Rescue, and other bounce effects.

```kotlin
// Before
effect = ReturnToHandEffect(EffectTarget.ContextTarget(0))

// After
effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Hand)
```

For self-bounce (e.g., Endless Cockroaches):
```kotlin
// Before
effect = ReturnToHandEffect(EffectTarget.Self)

// After
effect = MoveToZoneEffect(EffectTarget.Self, Zone.Hand)
```

### 10c — Migrate `PutOnTopOfLibraryEffect` (~4 cards)

```kotlin
// Before
effect = PutOnTopOfLibraryEffect(EffectTarget.ContextTarget(0))

// After
effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Library, ZonePlacement.Top)
```

### 10d — Migrate `ShuffleIntoLibraryEffect` (~2 cards)

```kotlin
// Before
effect = ShuffleIntoLibraryEffect(EffectTarget.Self)

// After
effect = MoveToZoneEffect(EffectTarget.Self, Zone.Library, ZonePlacement.Shuffled)
```

### 10e — Migrate `DestroyEffect` (~32 cards)

Largest batch. Mechanical replacement:

```kotlin
// Before
effect = DestroyEffect(EffectTarget.ContextTarget(0))

// After
effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Graveyard, byDestruction = true)
```

### 10f — Verify `ExileEffect` (0 cards)

No cards use `ExileEffect` directly. Just verify no references remain.

### 10g — Migrate `TargetCardInGraveyard` → `TargetObject`

For cards that target graveyard cards, replace:

```kotlin
// Before
target = TargetCardInGraveyard(filter = TargetFilter.CreatureInYourGraveyard)

// After
target = TargetObject(filter = TargetFilter.CreatureInYourGraveyard)
```

### 10h — Update TestCards.kt and scenario tests

- Update `TestCards.kt` to use `MoveToZoneEffect`
- Update scenario tests if assertions reference old effect types
- Verify all existing scenario tests still pass

## Execution Strategy

Migrate in sub-batches by effect type (10a → 10b → ... → 10e), running tests after each batch. The `DestroyEffect` batch (10e) is the largest and should be done carefully, possibly split by set.

## Risk

High — touches ~57 card files. Mechanical but large surface area. Run full test suite after each sub-batch.
