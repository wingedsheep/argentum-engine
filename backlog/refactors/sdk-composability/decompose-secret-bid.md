# Decompose SecretBidEffect

## Problem

`SecretBidEffect` is a monolithic effect used only by Menacing Ogre. It hardcodes the entire "each player secretly bids life, highest bidder loses that life, controller may gain counters" flow in one sealed class + one executor.

## Current Shape

```kotlin
data class SecretBidEffect(
    val counterType: String = "+1/+1",
    val counterCount: Int = 2
) : Effect
```

The executor (`SecretBidExecutor`) handles sequential number decisions for each player, comparison, life loss, and counter placement.

## Target State

This is hard to decompose into existing atoms because:
- It requires **simultaneous secret choices** (each player picks a number without seeing others')
- The comparison logic ("highest bidder") is unique
- The "tied = controller wins" rule is card-specific

Realistically, this should become a **generic secret-bid primitive** rather than decomposing into simpler effects:

```kotlin
data class SecretBidEffect(
    val bidders: Player = Player.Each,
    val loserEffect: Effect,       // what happens to the highest bidder (e.g., lose life equal to bid)
    val winnerEffect: Effect?,     // what happens if controller wins (e.g., gain counters)
    val tieBreaker: TieBreaker = TieBreaker.CONTROLLER_WINS
) : Effect
```

This is low priority — only one card uses it and the mechanic is inherently complex. The current monolithic approach is acceptable until more bid cards are needed.

## Files

- **SDK:** `SecretBidEffects.kt`
- **Engine:** `SecretBidExecutor.kt`, `SecretBidContinuation` (in continuations)
- **Sets:** `MenacingOgre.kt`
