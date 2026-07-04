package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.AttachmentKind
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

        /**
         * The number of distinct values of [property] (power / toughness / mana value) among the
         * matched permanents — e.g. `distinctValues(POWER)` for "the number of different powers
         * among creatures you control" (Selvala, Eager Trailblazer). Two permanents sharing a
         * value count once.
         */
        fun distinctValues(property: CardNumericProperty): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, Aggregation.DISTINCT_VALUES, property)

        /**
         * The number of differently named matched permanents — e.g. `distinctNames()` over
         * `GameObjectFilter.Land` for "the number of differently named lands you control"
         * (Emil, Vastlands Roamer). Two permanents sharing a name count once.
         */
        fun distinctNames(): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, Aggregation.DISTINCT_NAMES)
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

    /**
     * The number of distinct colors of mana spent to cast the source spell (0–5).
     * Backs the Converge ability word and the Sunburst counter rule. Colorless is not a
     * color, so it never contributes.
     */
    fun colorsOfManaSpent(): DynamicAmount = DynamicAmount.DistinctColorsManaSpent

    /**
     * The number of distinct colors of mana spent to cast the spell that fired this trigger
     * (0–5). The triggering-spell analogue of [colorsOfManaSpent] (which reads the resolving
     * object's own cast, i.e. Converge). Used by "Whenever you cast an instant or sorcery spell,
     * … for each color of mana spent to cast that spell" payoffs on a separate permanent
     * (Magmablood Archaic).
     */
    fun colorsSpentOnTriggeringSpell(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.COLORS_SPENT_ON_TRIGGERING_SPELL)

    /**
     * Your devotion to [colors] (CR 700.5) — the number of mana symbols of those colors among
     * the mana costs of permanents you control. Pass one color for "devotion to red", or several
     * for a colored combination ("devotion to white and black"). See [DynamicAmount.DevotionTo].
     */
    fun devotionTo(vararg colors: Color): DynamicAmount =
        DynamicAmount.DevotionTo(colors.toList())

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

    /**
     * The number of unlocked doors among Rooms [player] controls (CR 709.5). A Room with both
     * doors unlocked counts as two. Used for Misty Salon's X/X token and Rampaging Soulrager's
     * "two or more unlocked doors" gate (via [Conditions]).
     */
    fun unlockedDoors(player: Player = Player.You): DynamicAmount =
        DynamicAmount.UnlockedDoors(player)

    /**
     * The number of distinct printed names among unlocked door faces of Rooms [player] controls.
     * The per-face analogue of [differentlyNamedLandsYouControl] — distinct over door faces, not
     * whole Room entities. Feeds Promising Stairs' "eight or more different names" alt-win.
     */
    fun distinctUnlockedDoorNames(player: Player = Player.You): DynamicAmount =
        DynamicAmount.UnlockedDoors(player, distinctNames = true)

    fun attackingCreaturesYouControl(): DynamicAmount =
        battlefield(Player.You, GameObjectFilter.Creature.attacking()).count()

    fun creaturesWithSubtype(subtype: Subtype): DynamicAmount =
        battlefield(Player.Each, GameObjectFilter.Creature.withSubtype(subtype)).count()

    fun landsWithSubtype(subtype: Subtype): DynamicAmount =
        battlefield(Player.You, GameObjectFilter.Land.withSubtype(subtype)).count()

    // =========================================================================
    // Equipment counting (convenience shortcuts)
    // =========================================================================

    /**
     * The number of Equipment [player] controls — "for each Equipment you control"
     * (Adelbert Steiner, Barret Wallace). Equipment is an artifact subtype (CR 301.5c),
     * so this counts permanents whose projected subtypes include Equipment, picking up
     * permanents turned into Equipment by a continuous effect as well.
     */
    fun equipmentYouControl(player: Player = Player.You): DynamicAmount =
        battlefield(player, GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT)).count()

    /**
     * The number of equipped creatures [player] controls — creatures with at least one
     * Equipment attached (CR 301.5). Reads the attachment state, so a creature equipped
     * by several Equipment still counts once. Used by "for each equipped creature you
     * control" payoffs.
     */
    fun equippedCreaturesYouControl(player: Player = Player.You): DynamicAmount =
        battlefield(player, GameObjectFilter.Creature.equipped()).count()

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

    /**
     * Number of distinct card types among the cards in the named pipeline collections (union,
     * de-duplicated by card type). For "draw a card for each card type among cards discarded this
     * way" style payoffs spread over several discard/gather collections — see
     * [DynamicAmount.DistinctCardTypesInCollections].
     */
    fun distinctCardTypesIn(vararg collections: String): DynamicAmount =
        DynamicAmount.DistinctCardTypesInCollections(collections.toList())

    /**
     * Total mana value of every card in a named pipeline collection (e.g. the cards just milled
     * into a "milled" collection). Reads each card by entity id, so it stays correct after the
     * collection has moved zones. See [DynamicAmount.ManaValueSumOfCollection].
     */
    fun manaValueSumOf(collectionName: String): DynamicAmount =
        DynamicAmount.ManaValueSumOfCollection(collectionName)

    // =========================================================================
    // Graveyard counting
    // =========================================================================

    fun cardsInYourGraveyard(): DynamicAmount =
        DynamicAmount.Count(Player.You, Zone.GRAVEYARD)

    fun creatureCardsInYourGraveyard(): DynamicAmount =
        DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature)

    // =========================================================================
    // Hand counting
    // =========================================================================

    /** The number of cards in your hand (e.g. Stingerback Terror's "-1/-1 for each card in your hand"). */
    fun cardsInYourHand(): DynamicAmount =
        DynamicAmount.Count(Player.You, Zone.HAND)

    // =========================================================================
    // Opponent-relative counting
    // =========================================================================

    fun creaturesAttackingYou(multiplier: Int = 1): DynamicAmount {
        val base = battlefield(Player.EachOpponent, GameObjectFilter.Creature.attacking()).count()
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

    /**
     * The number of [filter] counters the source had the moment its self-exile / self-sacrifice
     * cost wiped them — "for each verse counter on this" / "if it had seven or more counters on it"
     * read as last-known information (CR 112.7a). See
     * [DynamicAmount.LastKnownSourceCounters] (Lost Isle Calling).
     */
    fun lastKnownSourceCounters(
        filter: com.wingedsheep.sdk.scripting.events.CounterTypeFilter
    ): DynamicAmount = DynamicAmount.LastKnownSourceCounters(filter)

    // =========================================================================
    // Spell-cast trigger values
    // =========================================================================

    /**
     * "X" — the value chosen for `{X}` on the spell that fired a spell-cast trigger (CR 601.2b).
     * Read inside the payoff of a `youCastSpell(requires = setOf(SpellCastPredicate.HasXInCost))`
     * trigger; e.g. Geometer's Arthropod "look at the top X cards of your library." `0` outside a
     * spell-cast trigger or when the spell had no {X}.
     */
    fun xValueOfTriggeringSpell(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.X_VALUE_OF_TRIGGERING_SPELL)

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

    /**
     * Damage dealt to [player] so far this turn by artifact sources (Reverse Polarity).
     * Combat and non-combat artifact damage both count; prevented damage does not.
     */
    fun damageReceivedFromArtifactsThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.DAMAGE_RECEIVED_FROM_ARTIFACTS)

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
     * "The number of [other] [subtype]s that entered the battlefield under [player]'s control
     * this turn" (Geralf, the Fleshwright — "each other Zombie that entered the battlefield under
     * your control this turn"). Counts entries even after the permanent has left or changed type.
     * Set [excludeTriggeringEntity] for "each *other*" — drops the permanent whose entry triggered
     * the ability (and, for simultaneous entries, lets each entrant see the others per the
     * 2024-04-12 ruling).
     */
    fun subtypeEnteredUnderControlThisTurn(
        subtype: com.wingedsheep.sdk.core.Subtype,
        player: Player = Player.You,
        excludeTriggeringEntity: Boolean = false
    ): DynamicAmount =
        DynamicAmount.SubtypeEnteredUnderControlThisTurn(player, subtype, excludeTriggeringEntity)

    /**
     * "The number of times [player] descended this turn" (CR 700.11) — count of
     * nontoken permanent cards put into [player]'s graveyard from any zone this turn.
     * Used by the descend N / fathomless descent ability words.
     */
    fun descendedThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.DESCENDED)

    /**
     * "The number of permanents [player] sacrificed this turn" (controller-scoped, any permanent
     * type). Reads the per-player `PermanentsSacrificedThisTurnComponent`, distinct from the
     * game-wide cost-reduction counter. Used by Sawblade Skinripper ("deals that much damage").
     */
    fun permanentsSacrificedThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.PERMANENTS_SACRIFICED)

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
     *
     * Pass [fromZone] to count only spells cast from that zone (e.g. `Zone.HAND`), matched
     * independently of [filter].
     */
    fun spellsCastThisTurn(
        player: Player = Player.You,
        filter: GameObjectFilter = GameObjectFilter.Any,
        excludeSelf: Boolean = false,
        fromZone: Zone? = null
    ): DynamicAmount =
        DynamicAmount.SpellsCastThisTurn(player, filter, excludeSelf, fromZone)

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
        DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.AttachmentCount())

    /** Number of Equipment attached to the source (Shagrat, Loot Bearer's amass amount). */
    fun equipmentAttachedToSelf(): DynamicAmount =
        DynamicAmount.EntityProperty(
            EntityReference.Source,
            EntityNumericProperty.AttachmentCount(AttachmentKind.EQUIPMENT)
        )

    fun numberOfBlockers(): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.BlockerCount)

    fun triggeringManaValue(): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.ManaValue)

    fun triggeringPower(): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Power)

    fun triggeringToughness(): DynamicAmount =
        DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Toughness)

    /**
     * Number of distinct creatures that crewed or saddled this permanent this turn (source-
     * relative; includes contributors that have since left the battlefield). See
     * [DynamicAmount.CreaturesThatCrewedOrSaddledThisTurn]. Used by "for each creature that
     * crewed it this turn" (Luxurious Locomotive).
     */
    fun creaturesThatCrewedOrSaddledThisTurn(): DynamicAmount =
        DynamicAmount.CreaturesThatCrewedOrSaddledThisTurn

    /**
     * Number of permanents sacrificed by the current resolving effect ("this way"), read from the
     * effect context's `sacrificedPermanents`. See [DynamicAmount.PermanentsSacrificedThisWay]. Used
     * by "Create a Food token for each creature sacrificed this way" (Voracious Fell Beast).
     */
    fun permanentsSacrificedThisWay(): DynamicAmount =
        DynamicAmount.PermanentsSacrificedThisWay
}
