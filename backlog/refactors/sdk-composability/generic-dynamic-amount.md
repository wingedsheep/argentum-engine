# Replace Card-Specific DynamicAmount Variants with Generic Primitives

## Problem

`DynamicAmount` has ~10 sealed class variants that each read a single turn-tracking counter from a player component. Every new card that cares about "how many X happened this turn" requires a new SDK variant + a new engine component + evaluator branch. These should be expressible as data.

## Two Categories

### Category A: Turn-tracking counters (player-scoped)

These all follow the pattern "read a per-player counter from a turn-tracking component":

| Current variant | What it reads | Component |
|---|---|---|
| `CreaturesDiedThisTurn(player)` | creatures died under player's control | `CreaturesDiedThisTurnComponent.count` |
| `NonTokenCreaturesDiedThisTurn(player)` | nontoken creatures died | `NonTokenCreaturesDiedThisTurnComponent.count` |
| `OpponentCreaturesExiledThisTurn` | creatures exiled from opponents | `OpponentCreaturesExiledThisTurnComponent.count` |
| `OpponentsWhoLostLifeThisTurn` | count of opponents who lost life | `LifeLostThisTurnComponent` (presence check) |
| `DamageDealtToTargetPlayerThisTurn` | total damage received | `DamageReceivedThisTurnComponent.amount` |

**Proposed:** A single `TurnTracker(player, tracker)` where `tracker` is an enum of trackable stats:

```kotlin
enum class TurnTracker {
    CREATURES_DIED,
    NONTOKEN_CREATURES_DIED,
    OPPONENT_CREATURES_EXILED,
    DAMAGE_RECEIVED,
    // future: SPELLS_CAST, LANDS_PLAYED, LIFE_GAINED, etc.
}

data class TurnTracking(
    val player: Player,
    val tracker: TurnTracker
) : DynamicAmount
```

This still requires adding an enum value per tracked stat, but the SDK type + evaluator dispatch is one branch instead of N. The engine components can stay as-is — only the DynamicAmount layer is unified.

`OpponentsWhoLostLifeThisTurn` is slightly different (counts *players* matching a condition, not a counter). It could be `CountPlayers(condition = PlayerLostLifeThisTurn)` or stay as a special `TurnTracker` value.

### Category B: Context-dependent lookups

These read from the current entity/trigger context rather than player components:

| Current variant | What it does | Can it be generic? |
|---|---|---|
| `CountCreaturesOfSourceChosenType` | Count creatures matching source's chosen type | Yes — `AggregateBattlefield` with a `ChosenSubtype` filter predicate |
| `CreaturesSharingTypeWithTriggeringEntity` | Count creatures sharing a type with trigger entity | Yes — `AggregateBattlefield` with a `SharesTypeWith(entity)` filter predicate |
| `LastKnownCounterCount` | Counter count at time of death | Already covered by `EntityProperty(TriggeringEntity, CounterCount)` — but needs last-known-info support |
| `CardTypesInAllGraveyards` | Distinct card types across all graveyards | Unique aggregation — `AggregateZone` with `DISTINCT_TYPES` aggregation |
| `CardTypesInLinkedExile` | Distinct card types in linked exile | Needs linked-exile scope — keep or add to `AggregateZone` |
| `ColorsAmongPermanentsYouControl` | Distinct colors among your permanents | Unique aggregation — `AggregateBattlefield` with `DISTINCT_COLORS` aggregation |

### Category C: Trigger context values

| Current variant | Replacement |
|---|---|
| `TriggerDamageAmount` | Keep — reads from trigger event payload, not a component |
| `TriggerLifeGainAmount` | Keep — same mechanism |
| `TriggerLifeLossAmount` | Keep — same mechanism |
| `AdditionalCostExiledCount` | Keep — reads from cost payment context |

These are fine as-is. They read from the `EffectContext` trigger/cost payload, not from persistent state. Could merge into `TriggerContextValue(property)` for tidiness but low priority.

## Approach

### Phase 1: Unify turn-tracking counters
1. Add `TurnTracking(player, tracker)` to `DynamicAmount` with a `TurnTracker` enum.
2. Add a single evaluator branch in `DynamicAmountEvaluator` that maps `TurnTracker` → component read.
3. Migrate card definitions to use `TurnTracking(...)`.
4. Delete the old variants.

### Phase 2: Add filter predicates for context-dependent lookups
1. Add `CardPredicate.HasChosenSubtype` (resolves the source's `ChosenCreatureTypeComponent` dynamically).
2. Add `CardPredicate.SharesCreatureTypeWith(entity: EntityReference)`.
3. Replace `CountCreaturesOfSourceChosenType` → `AggregateBattlefield(Player.You, Creature.withChosenSubtype())`.
4. Replace `CreaturesSharingTypeWithTriggeringEntity` → `AggregateBattlefield(Player.You, Creature.sharingTypeWith(EntityReference.TriggeringEntity))`.
5. Delete the old variants.

### Phase 3 (optional): Distinct-type aggregations
1. Add `Aggregation.DISTINCT_TYPES` and `Aggregation.DISTINCT_COLORS` to the `Aggregation` enum.
2. Replace `CardTypesInAllGraveyards` → `AggregateZone(Player.Each, Zone.GRAVEYARD, aggregation = DISTINCT_TYPES)`.
3. Replace `ColorsAmongPermanentsYouControl` → `AggregateBattlefield(Player.You, aggregation = DISTINCT_COLORS)`.
4. `CardTypesInLinkedExile` needs a `Zone.LINKED_EXILE` concept or a dedicated scope — lower priority.

## Files

- **SDK:** `DynamicAmount.kt` (add `TurnTracking`, delete old variants), `GameObjectFilter.kt` (new predicates in Phase 2)
- **Engine:** `DynamicAmountEvaluator.kt` (replace N branches with 1), `PredicateEvaluator.kt` (new predicate evaluation), `ManaAbilityEnumerator.kt` (update `CountCreaturesOfSourceChosenType` check)
- **Sets:** Any card defs using the old variants (Caller of the Claw, Season of Loss, Gev, Vren, Mana Echoes, Three Tree City, etc.)

## Notes

- Phase 1 is the biggest win — it's purely mechanical and makes the pattern extensible. Future turn-tracking (spells cast, lands played, life gained) becomes an enum value instead of a new sealed class + component + evaluator branch.
- Phase 2 requires the engine's `PredicateEvaluator` to resolve entity references during filter matching, which is a slightly deeper change. The `ChosenSubtype` predicate also needs access to the source entity's components during evaluation.
- The `TurnTracker` enum is still a closed set requiring code changes, but it's a single enum shared across all DynamicAmount uses, rather than N separate sealed classes each needing their own evaluator logic.
