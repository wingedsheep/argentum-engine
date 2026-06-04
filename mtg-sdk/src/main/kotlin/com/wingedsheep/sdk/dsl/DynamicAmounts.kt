package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.TurnTracker

/**
 * Convenience factory for common DynamicAmount expressions.
 *
 * These build on the generic AggregateBattlefield/Count/math primitives
 * without adding to the sealed hierarchy.
 *
 * Usage:
 * ```kotlin
 * DynamicAmounts.battlefield(Player.You).count()
 * DynamicAmounts.battlefield(Player.You).maxManaValue()
 * DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
 * DynamicAmounts.creaturesYouControl()
 * DynamicAmounts.landsYouControl()
 * ```
 */
object DynamicAmounts {

    // =========================================================================
    // Fluent battlefield query builder
    // =========================================================================

    /**
     * Start a fluent query over battlefield permanents.
     *
     * ```kotlin
     * DynamicAmounts.battlefield(Player.You).count()
     * DynamicAmounts.battlefield(Player.You).maxManaValue()
     * DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
     * DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower()
     * ```
     */
    fun battlefield(player: Player, filter: GameObjectFilter = GameObjectFilter.Any) =
        BattlefieldQuery(player, filter)

    class BattlefieldQuery(private val player: Player, private val filter: GameObjectFilter) {
        fun count(): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter)

        fun maxManaValue(): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, Aggregation.MAX, CardNumericProperty.MANA_VALUE)

        fun maxPower(): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, Aggregation.MAX, CardNumericProperty.POWER)

        fun maxToughness(): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, Aggregation.MAX, CardNumericProperty.TOUGHNESS)

        fun minToughness(): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, Aggregation.MIN, CardNumericProperty.TOUGHNESS)

        fun sumPower(): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, Aggregation.SUM, CardNumericProperty.POWER)
    }

    // =========================================================================
    // Fluent zone query builder (non-battlefield zones)
    // =========================================================================

    /**
     * Start a fluent query over cards in a non-battlefield zone (graveyard, hand, library, exile).
     *
     * ```kotlin
     * DynamicAmounts.zone(Player.You, Zone.GRAVEYARD).maxManaValue()
     * DynamicAmounts.zone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature).count()
     * ```
     */
    fun zone(player: Player, zone: Zone, filter: GameObjectFilter = GameObjectFilter.Any) =
        ZoneQuery(player, zone, filter)

    class ZoneQuery(private val player: Player, private val zone: Zone, private val filter: GameObjectFilter) {
        fun count(): DynamicAmount =
            DynamicAmount.AggregateZone(player, zone, filter)

        fun maxManaValue(): DynamicAmount =
            DynamicAmount.AggregateZone(player, zone, filter, Aggregation.MAX, CardNumericProperty.MANA_VALUE)

        fun maxPower(): DynamicAmount =
            DynamicAmount.AggregateZone(player, zone, filter, Aggregation.MAX, CardNumericProperty.POWER)

        fun maxToughness(): DynamicAmount =
            DynamicAmount.AggregateZone(player, zone, filter, Aggregation.MAX, CardNumericProperty.TOUGHNESS)

        fun sumManaValue(): DynamicAmount =
            DynamicAmount.AggregateZone(player, zone, filter, Aggregation.SUM, CardNumericProperty.MANA_VALUE)
    }

    // =========================================================================
    // Battlefield counting (convenience shortcuts)
    // =========================================================================

    /**
     * The number of distinct colors among permanents [player] controls.
     * Used for Lorwyn Eclipsed's Vivid mechanic (effect-scaling half).
     * Maxes out at 5. Reads colors via projected state, so recolor effects apply.
     */
    fun colorsAmongPermanents(
        player: Player = Player.You,
        filter: GameObjectFilter = GameObjectFilter.Permanent
    ): DynamicAmount =
        DynamicAmount.AggregateBattlefield(player, filter, Aggregation.DISTINCT_COLORS)

    fun creaturesYouControl(): DynamicAmount =
        battlefield(Player.You, GameObjectFilter.Creature).count()

    fun allCreatures(): DynamicAmount =
        battlefield(Player.Each, GameObjectFilter.Creature).count()

    fun landsYouControl(): DynamicAmount =
        battlefield(Player.You, GameObjectFilter.Land).count()

    /**
     * Domain — the number of basic land types (Plains, Island, Swamp, Mountain, Forest)
     * among lands [player] controls. Capped at 5 by the size of the basic-subtype set.
     * Reads subtypes via projected state, so type-changed lands and dual lands count.
     */
    fun domain(player: Player = Player.You): DynamicAmount =
        DynamicAmount.AggregateBattlefield(
            player = player,
            filter = GameObjectFilter.Land,
            aggregation = Aggregation.DISTINCT_BASIC_LAND_SUBTYPES
        )

    /**
     * Number of differently named lands [player] controls.
     * Used for cards like All-Fates Scroll: counts each land you control once,
     * but only if its English name isn't shared with another already-counted land.
     */
    fun differentlyNamedLandsYouControl(player: Player = Player.You): DynamicAmount =
        DynamicAmount.AggregateBattlefield(player, GameObjectFilter.Land, Aggregation.DISTINCT_NAMES)

    fun attackingCreaturesYouControl(): DynamicAmount =
        battlefield(Player.You, GameObjectFilter.Creature.attacking()).count()

    fun creaturesWithSubtype(subtype: Subtype): DynamicAmount =
        battlefield(Player.Each, GameObjectFilter.Creature.withSubtype(subtype)).count()

    fun landsWithSubtype(subtype: Subtype): DynamicAmount =
        battlefield(Player.You, GameObjectFilter.Land.withSubtype(subtype)).count()

    // =========================================================================
    // "Other" counting (subtract 1 for self)
    // =========================================================================

    fun otherCreaturesYouControl(): DynamicAmount =
        DynamicAmount.Subtract(
            battlefield(Player.You, GameObjectFilter.Creature).count(),
            DynamicAmount.Fixed(1)
        )

    fun otherCreaturesWithSubtypeYouControl(subtype: Subtype): DynamicAmount =
        DynamicAmount.Subtract(
            battlefield(Player.You, GameObjectFilter.Creature.withSubtype(subtype)).count(),
            DynamicAmount.Fixed(1)
        )

    /**
     * Number of distinct entities across the named pipeline collections (union, de-duplicated
     * by entity id). For "you affected N *different* objects" payoffs spread over several
     * resolution-time selections — see [DynamicAmount.DistinctEntitiesInCollections].
     */
    fun distinctEntitiesIn(vararg collections: String): DynamicAmount =
        DynamicAmount.DistinctEntitiesInCollections(collections.toList())

    // =========================================================================
    // Graveyard counting
    // =========================================================================

    fun cardsInYourGraveyard(): DynamicAmount =
        DynamicAmount.Count(Player.You, Zone.GRAVEYARD)

    fun creatureCardsInYourGraveyard(): DynamicAmount =
        DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature)

    // =========================================================================
    // Opponent-relative counting
    // =========================================================================

    fun creaturesAttackingYou(multiplier: Int = 1): DynamicAmount {
        val base = battlefield(Player.Opponent, GameObjectFilter.Creature.attacking()).count()
        return if (multiplier == 1) base else DynamicAmount.Multiply(base, multiplier)
    }

    fun landsOfTypeTargetOpponentControls(landType: String, multiplier: Int = 1): DynamicAmount {
        val base = battlefield(Player.TargetOpponent, GameObjectFilter.Land.withSubtype(landType)).count()
        return if (multiplier == 1) base else DynamicAmount.Multiply(base, multiplier)
    }

    fun creaturesOfColorTargetOpponentControls(color: Color, multiplier: Int = 1): DynamicAmount {
        val base = battlefield(Player.TargetOpponent, GameObjectFilter.Creature.withColor(color)).count()
        return if (multiplier == 1) base else DynamicAmount.Multiply(base, multiplier)
    }

    fun tappedCreaturesTargetOpponentControls(): DynamicAmount =
        battlefield(Player.TargetOpponent, GameObjectFilter.Creature.tapped()).count()

    fun handSizeDifferenceFromTargetOpponent(): DynamicAmount =
        DynamicAmount.IfPositive(
            DynamicAmount.Subtract(
                DynamicAmount.Count(Player.TargetOpponent, Zone.HAND),
                DynamicAmount.Count(Player.You, Zone.HAND)
            )
        )

    // =========================================================================
    // Additional cost values
    // =========================================================================

    fun additionalCostExiledCount(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.ADDITIONAL_COST_EXILED_COUNT)

    // =========================================================================
    // Prevention-reaction values
    // =========================================================================

    /**
     * "That much" / "that many" — the amount of damage a prevention shield just prevented, readable
     * inside the shield's `onPrevented` follow-up (New Way Forward, Deflecting Palm). See
     * [Effects.PreventNextDamageFromChosenSource].
     */
    fun preventedDamage(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.PREVENTED_DAMAGE_AMOUNT)

    // =========================================================================
    // Target-based player values
    // =========================================================================

    fun damageDealtToTargetPlayerThisTurn(targetIndex: Int = 0): DynamicAmount =
        DynamicAmount.TurnTracking(Player.ContextPlayer(targetIndex), TurnTracker.DAMAGE_RECEIVED)

    // =========================================================================
    // Turn-based tracking
    // =========================================================================

    fun nonTokenCreaturesDiedThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.NONTOKEN_CREATURES_DIED)

    fun creaturesDiedThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.CREATURES_DIED)

    fun opponentsWhoLostLifeThisTurn(): DynamicAmount =
        DynamicAmount.TurnTracking(Player.You, TurnTracker.OPPONENTS_WHO_LOST_LIFE)

    fun opponentCreaturesExiledThisTurn(): DynamicAmount =
        DynamicAmount.TurnTracking(Player.You, TurnTracker.OPPONENT_CREATURES_EXILED)

    fun lifeGainedThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.LIFE_GAINED)

    /**
     * "The number of lands that entered the battlefield under [player]'s control this turn"
     * (Bioengineered Future). Counts every land ETB under the player — land drops, Lander
     * search, Cultivate-style "put a land onto the battlefield" effects — not just land
     * drops. Reads the per-player [LandsEnteredUnderControlThisTurnComponent] populated by
     * `PermanentEntryTracker`.
     */
    fun landsEnteredUnderControlThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.LANDS_ENTERED_UNDER_CONTROL)

    /**
     * "The number of times [player] descended this turn" (CR 700.11) — count of
     * nontoken permanent cards put into [player]'s graveyard from any zone this turn.
     * Used by the descend N / fathomless descent ability words.
     */
    fun descendedThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.DESCENDED)

    /**
     * "The number of [filter] spells [player] has cast this turn", optionally excluding the
     * resolving spell itself. Reads the per-player cast history, so the triggering spell counts
     * unless [excludeSelf].
     *
     * ```kotlin
     * // "other spells you've cast this turn" (Thunder Salvo's variable half)
     * DynamicAmounts.spellsCastThisTurn(excludeSelf = true)
     * // "noncreature spells they've cast this turn" (Magebane Lizard)
     * DynamicAmounts.spellsCastThisTurn(Player.TriggeringPlayer, GameObjectFilter.Noncreature)
     * ```
     */
    fun spellsCastThisTurn(
        player: Player = Player.You,
        filter: GameObjectFilter = GameObjectFilter.Any,
        excludeSelf: Boolean = false
    ): DynamicAmount =
        DynamicAmount.SpellsCastThisTurn(player, filter, excludeSelf)

    /** The starting life total of a player (20 in standard, 40 in commander). */
    fun startingLifeTotal(player: Player = Player.You): DynamicAmount =
        DynamicAmount.StartingLifeTotal(player)

    // =========================================================================
    // Entity property shortcuts (composable entity + property)
    // =========================================================================

    fun sourcePower(): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Power)

    fun sourceToughness(): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Toughness)

    /** Power of the creature the source Aura/Equipment is attached to. */
    fun enchantedCreaturePower(): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.EnchantedCreature, EntityNumericProperty.Power)

    fun targetPower(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Target(index), EntityNumericProperty.Power)

    fun targetToughness(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Target(index), EntityNumericProperty.Toughness)

    fun targetManaValue(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Target(index), EntityNumericProperty.ManaValue)

    fun targetManaSpent(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Target(index), EntityNumericProperty.ManaSpent)

    /** Number of distinct colors of the indexed cast-time target ("for each color of the creature it targets"). */
    fun targetColorCount(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Target(index), EntityNumericProperty.ColorCount)

    /** Number of distinct colors of any referenced entity. */
    fun colorCountOf(entity: EntityReference): DynamicAmount =
        DynamicAmount.EntityProperty(entity, EntityNumericProperty.ColorCount)

    fun sacrificedPower(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Sacrificed(index), EntityNumericProperty.Power)

    fun sacrificedToughness(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Sacrificed(index), EntityNumericProperty.Toughness)

    fun countersOnSelf(type: CounterTypeFilter): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.CounterCount(type))

    fun countersOnTarget(type: CounterTypeFilter, index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Target(index), EntityNumericProperty.CounterCount(type))

    fun attachmentsOnSelf(): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.AttachmentCount)

    fun numberOfBlockers(): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.BlockerCount)

    fun triggeringManaValue(): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.ManaValue)

    fun triggeringPower(): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Power)

    fun triggeringToughness(): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Toughness)
}
