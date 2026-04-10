# Bug: Layer 7 controller-dependent filters not re-resolved after Layer 2

## Summary

When a control-changing effect (Layer 2) steals a creature, Layer 7 P/T effects with
controller-dependent filters (like `OtherCreaturesYouControl`) still use the **original**
controller, not the projected one. This means a lord's "other creatures you control get +1/+1"
won't pump a stolen creature.

## Root cause

In `StateProjector.project()`, after applying Layer 2 (control-changing) effects, only
**non-L7** controller-dependent filters are re-resolved (lines 137-148):

```kotlin
val postControlEffects = sortedEffects
    .filter { it.layer != Layer.CONTROL && it.layer != Layer.POWER_TOUGHNESS }
    //                                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //                                     L7 is excluded from re-resolution
    .map { effect ->
        if (effect.affectsFilter != null && filterResolver.isControllerDependentFilter(effect.affectsFilter)) {
            effect.copy(affectedEntities = filterResolver.resolveAffectedEntities(...))
        } else { effect }
    }
```

Layer 7 effects are re-resolved separately (lines 154-160), but only for **subtype-dependent**
filters — not controller-dependent ones:

```kotlin
val resolvedLayer7Effects = sortedEffects.map { effect ->
    if (effect.layer == Layer.POWER_TOUGHNESS
        && effect.affectsFilter != null
        && filterResolver.isSubtypeDependentFilter(effect.affectsFilter)) { // <-- only subtypes
        ...
    }
}
```

## Reproduction

Player A has a Bear Lord ("other creatures you control get +1/+1").
Player A steals Player B's Grizzly Bears with a control-changing aura.

**Expected:** Bears is 3/3 (lord pumps it — it's now "your" creature).
**Actual:** Bears is 2/2 (lord doesn't see it as "yours" in L7).

A test for this exists in `LayerSystemTest.kt` as a comment under the
`L2 → L7c` section documenting the limitation.

## Fix

In the L7 re-resolution block, also re-resolve controller-dependent filters:

```kotlin
val resolvedLayer7Effects = sortedEffects.map { effect ->
    if (effect.layer == Layer.POWER_TOUGHNESS && effect.affectsFilter != null &&
        (filterResolver.isSubtypeDependentFilter(effect.affectsFilter) ||
         filterResolver.isControllerDependentFilter(effect.affectsFilter))) {
        effect.copy(affectedEntities = filterResolver.resolveAffectedEntities(
            state, effect.sourceId, effect.affectsFilter, projectedValues))
    } else {
        effect
    }
}
```

After the fix, uncomment the `L2 → L7c` test in `LayerSystemTest.kt` and verify it passes.
