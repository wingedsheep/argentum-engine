# Replace ChainAction and ChainCopyCost with Generic Effect and PayCost

## Problem

`ChainCopyEffect` uses two closed sealed types that duplicate existing systems:

- **`ChainAction`** — 5 variants (Destroy, BounceToHand, DealDamage, Discard, PreventAllDamageDealt). Each is a hardcoded action that already exists as a generic `Effect` (DestroyEffect, ReturnToHandEffect, DealDamageEffect, DiscardEffect, PreventDamageEffect). The executor has a separate method per action.

- **`ChainCopyCost`** — 3 variants (NoCost, SacrificeALand, DiscardACard). These duplicate the existing `PayCost` system.

A hypothetical new chain card with a different primary action (e.g., exile, tap, gain life) or different copy cost (e.g., pay 2 life) would require adding new enum variants + executor methods.

## Target State

Replace `ChainAction` with `Effect` and `ChainCopyCost` with `PayCost?`:

```kotlin
data class ChainCopyEffect(
    val action: Effect,              // was: ChainAction
    val target: EffectTarget,
    val targetFilter: TargetFilter? = null,
    val copyRecipient: CopyRecipient,
    val copyCost: PayCost? = null,   // was: ChainCopyCost (null = no cost)
    val copyTargetRequirement: TargetRequirement,
    val spellName: String
) : Effect
```

Card definitions stay the same — the `Effects.kt` facade methods already hide the construction. But now any combination is possible without code changes.

## Approach

1. **Change `action: ChainAction` to `action: Effect`** in `ChainCopyEffect`. Change `copyCost: ChainCopyCost` to `copyCost: PayCost?`.

2. **Refactor `ChainCopyExecutor.execute()`** — instead of a `when` over `ChainAction`, delegate the primary action to the standard `EffectExecutorRegistry`. The executor would:
   - Execute `effect.action` via the registry (passing the resolved target through context)
   - Determine the copy recipient from the target
   - Then proceed to `offerChainCopy()` as before

3. **Refactor cost checking/payment** in `offerChainCopy()` and `ChainCopyCostContinuation` — use the existing cost payment infrastructure from `PayCost` instead of hardcoded land sacrifice / card discard logic.

4. **Update `Effects.kt` facade** — the 5 helper methods construct the new shape:
   ```kotlin
   fun BounceAndChainCopy(target, targetFilter, spellName) = ChainCopyEffect(
       action = Effects.ReturnToHand(target),
       copyCost = PayCost.Sacrifice(GameObjectFilter.Land, count = 1),
       ...
   )
   ```

5. **Delete `ChainAction` and `ChainCopyCost`** sealed interfaces.

6. **Keep `CopyRecipient`** — this is genuinely a chain-specific concept (who gets offered the copy). It can't be derived from the primary effect alone.

## Complexity Note

The tricky part is that the current executor interleaves "execute primary action" with "determine who gets the copy offer." For example, `executeDestroy` resolves the target, gets its controller, *then* destroys, *then* offers the copy to that controller. With a generic `Effect`, the executor needs to:
1. Resolve the target and determine the copy recipient *before* executing
2. Execute the generic effect
3. Then offer the copy

This sequencing already works for all 5 cases — the target is resolved from `effect.target` and the recipient is determined by `CopyRecipient` + the target's controller/owner. The primary action execution is the only part that changes.

## Files

- **SDK:** `ChainCopyEffects.kt` (delete `ChainAction`, `ChainCopyCost`; change field types), `Effects.kt` (update facade methods)
- **Engine:** `ChainCopyExecutor.kt` (replace 5 `execute*` methods with generic delegation), `ChainSpellContinuationResumer.kt` (update cost payment), `ChainContinuations.kt` (continuations reference `ChainCopyEffect` which changes shape), `Serialization.kt` (remove polymorphic registrations for deleted types)
- **Sets:** No changes needed — card defs use `Effects.*` facades
