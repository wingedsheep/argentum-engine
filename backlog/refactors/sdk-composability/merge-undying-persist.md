# Merge Undying and Persist into Single Type

## Problem

`UndyingEffect` and `PersistEffect` are two separate replacement effects that follow the exact same pattern: "when this creature dies, if it had no [counter type] counters, return it with a [counter type] counter." The only difference is the counter type (+1/+1 vs -1/-1).

Neither is currently used by any card in mtg-sets.

## Target State

```kotlin
data class ReturnWithCounterOnDeath(
    val counterType: CounterTypeFilter,  // PlusOnePlusOne or MinusOneMinusOne
    val absenceCheck: CounterTypeFilter, // must NOT have this counter type to trigger
    override val appliesTo: GameEvent = ZoneChangeEvent(...)
) : ReplacementEffect
```

Or even simpler, since Undying and Persist always check absence of the same counter they add:

```kotlin
data class ReturnWithCounterOnDeath(
    val counterType: CounterTypeFilter,
    override val appliesTo: GameEvent = ZoneChangeEvent(...)
) : ReplacementEffect
```

- Undying = `ReturnWithCounterOnDeath(PlusOnePlusOne)`
- Persist = `ReturnWithCounterOnDeath(MinusOneMinusOne)`

DSL facade can still expose `Effects.Undying()` and `Effects.Persist()` for readability.

## Files

- **SDK:** `ReplacementEffect.kt` (replace 2 classes with 1)
- **Engine:** replacement effect handler (merge 2 branches into 1)
- **Sets:** none currently
