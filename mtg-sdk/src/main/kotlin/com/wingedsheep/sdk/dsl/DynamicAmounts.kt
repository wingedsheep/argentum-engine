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
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.CounterType
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
    // Constants, X, and the number of cards in a zone
    // =========================================================================

    /**
     * A constant. Most facades that take an amount also take an `Int` directly — `DrawCards(2)`,
     * `CardSource.TopOfLibrary(3)`, `x + 1` — so this is for the slots that only take a
     * [DynamicAmount] (a stat bonus beside a dynamic one, a `Conditional` branch beside a dynamic one).
     */
    fun fixed(amount: Int): DynamicAmount = DynamicAmount.Fixed(amount)

    /** X — the value chosen for X as this spell was cast or this ability was activated. */
    fun xValue(): DynamicAmount = DynamicAmount.XValue

    /**
     * The X paid when this permanent was cast, read after it has resolved — "enters with X +1/+1
     * counters", "when this enters, … X" ([DynamicAmount.CastX]).
     */
    fun castX(): DynamicAmount = DynamicAmount.CastX

    /** A number the source locked in as it was cast (a chosen number, a blight amount, …). */
    fun castChoice(slot: com.wingedsheep.sdk.scripting.ChoiceSlot): DynamicAmount = DynamicAmount.CastChoice(slot)

    /**
     * A number an earlier effect of this resolution stored under [name] (a fight's excess damage,
     * a clash's mana values, a guess). Inside `Effects.Pipeline { }` read the step's `NumberSlot`
     * instead.
     */
    fun storedNumber(name: String): DynamicAmount = DynamicAmount.VariableReference(name)

    /** The number of cards [player] has in [zone] matching [filter]. */
    fun count(player: Player, zone: Zone, filter: GameObjectFilter = GameObjectFilter.Any): DynamicAmount =
        DynamicAmount.Count(player, zone, filter)

    // =========================================================================
    // Arithmetic — see DynamicAmountOperators.kt for `+ - * /` and unary minus
    // =========================================================================

    /** [ifTrue] when [condition] holds as the amount is read, otherwise [ifFalse]. */
    fun conditional(
        condition: com.wingedsheep.sdk.scripting.conditions.Condition,
        ifTrue: DynamicAmount,
        ifFalse: DynamicAmount
    ): DynamicAmount = DynamicAmount.Conditional(condition, ifTrue, ifFalse)

    /** [conditional] between two constants ("two if …, otherwise one"). */
    fun conditional(
        condition: com.wingedsheep.sdk.scripting.conditions.Condition,
        ifTrue: Int,
        ifFalse: Int
    ): DynamicAmount = conditional(condition, fixed(ifTrue), fixed(ifFalse))

    /** The greater of [a] and [b]. */
    fun max(a: DynamicAmount, b: DynamicAmount): DynamicAmount = DynamicAmount.Max(a, b)

    /** The lesser of [a] and [b]. */
    fun min(a: DynamicAmount, b: DynamicAmount): DynamicAmount = DynamicAmount.Min(a, b)

    /** [amount], or 0 when it would be negative — for differences that can't go below zero. */
    fun nonNegative(amount: DynamicAmount): DynamicAmount = DynamicAmount.IfPositive(amount)

    /** [base] raised to the [exponent] ("2^X" — Mathemagics). */
    fun pow(base: Int, exponent: DynamicAmount): DynamicAmount = DynamicAmount.Power(base, exponent)

    // =========================================================================
    // Life, players, and mana
    // =========================================================================

    /** [player]'s life total. */
    fun lifeTotal(player: Player): DynamicAmount = DynamicAmount.LifeTotal(player)

    /** Your life total. */
    fun yourLifeTotal(): DynamicAmount = DynamicAmount.YourLifeTotal

    /** The number of players in [scope] ("the number of opponents you have"). */
    fun playerCount(scope: Player = Player.EachOpponent): DynamicAmount = DynamicAmount.PlayerCount(scope)

    /** The number of players in [scope] for whom [condition] holds. */
    fun countPlayersWith(scope: Player, condition: com.wingedsheep.sdk.scripting.conditions.Condition): DynamicAmount =
        DynamicAmount.CountPlayersWith(scope, condition)

    /** The greatest value of [inner] (evaluated as each of [players]) — "the most life among players". */
    fun greatestAmongPlayers(inner: DynamicAmount, players: Player = Player.Each): DynamicAmount =
        DynamicAmount.GreatestAmongPlayers(players, inner)

    /** The total mana spent to cast this spell. */
    fun totalManaSpent(): DynamicAmount = DynamicAmount.TotalManaSpent

    /** The amount of [color] mana spent on X. */
    fun manaSpentOnX(color: Color): DynamicAmount = DynamicAmount.ManaSpentOnX(color)

    /** The amount of mana produced by a [subtype] source (a Cave) spent to cast this. */
    fun manaSpentFromSubtype(subtype: Subtype): DynamicAmount = DynamicAmount.ManaSpentFromSubtype(subtype)

    /** The amount of unspent mana in [player]'s pool. */
    fun unspentMana(player: Player): DynamicAmount = DynamicAmount.UnspentMana(player)

    /** The greatest number of creatures [player] controls that share a creature type. */
    fun largestSharedCreatureTypeCount(player: Player = Player.You): DynamicAmount =
        DynamicAmount.LargestSharedCreatureTypeCount(player)

    /** Craft's exiled materials: their total power / total mana value / number of colors. */
    fun craftedMaterialsTotalPower(): DynamicAmount = DynamicAmount.CraftedMaterialsTotalPower
    fun craftedMaterialsTotalManaValue(): DynamicAmount = DynamicAmount.CraftedMaterialsTotalManaValue
    fun craftedMaterialsColorCount(): DynamicAmount = DynamicAmount.CraftedMaterialsColorCount

    // =========================================================================
    // Numbers the triggering event or the resolution carries
    // =========================================================================
    //
    // Each reads one [ContextPropertyKey] off the effect context; the key's own description is the
    // printed phrase ("the damage dealt", "the life gained", …).

    /** The damage dealt by the triggering event ("that much damage"). */
    fun triggerDamageAmount(): DynamicAmount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)

    /** The excess damage (beyond lethal) the triggering event dealt. */
    fun triggerExcessDamageAmount(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_EXCESS_DAMAGE_AMOUNT)

    /** The toughness of the creature the triggering damage was dealt to. */
    fun triggerRecipientToughness(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_RECIPIENT_TOUGHNESS)

    /** The life gained by the triggering event. */
    fun triggerLifeGained(): DynamicAmount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_LIFE_GAINED)

    /** The life lost by the triggering event. */
    fun triggerLifeLost(): DynamicAmount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_LIFE_LOST)

    /** The number of cards discarded by the triggering event. */
    fun triggerDiscardCount(): DynamicAmount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DISCARD_COUNT)

    /** The number the triggering scry looked at. */
    fun triggerScryCount(): DynamicAmount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_SCRY_COUNT)

    /** The number of counters the triggering event put on ("that many"). */
    fun triggerCountersPlaced(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_COUNTERS_PLACED_AMOUNT)

    /** The number of counters the triggering event removed. */
    fun triggerCountersRemoved(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_COUNTERS_REMOVED_AMOUNT)

    /** The discover value of the triggering discover. */
    fun triggerDiscoverValue(): DynamicAmount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DISCOVER_VALUE)

    /** The mana value of the triggering spell. */
    fun triggeringSpellManaValue(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGERING_SPELL_MANA_VALUE)

    /** The amount of mana spent to cast the triggering spell. */
    fun manaSpentOnTriggeringSpell(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL)

    /** The number of times a mode was chosen for the triggering spell. */
    fun modesChosenOnTriggeringSpell(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.MODES_CHOSEN_ON_TRIGGERING_SPELL)

    /** The number of +1/+1 counters the source had when it left the battlefield. */
    fun lastKnownPlusOneCounters(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT)

    /** The number of counters (of any kind) the source had when it left the battlefield. */
    fun lastKnownCounterCount(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.LAST_KNOWN_TOTAL_COUNTER_COUNT)

    /** The number of cards exiled with the source (its linked exile). */
    fun linkedExileCardCount(): DynamicAmount = DynamicAmount.ContextProperty(ContextPropertyKey.LINKED_EXILE_CARD_COUNT)

    /** The number of card types among the cards exiled with the source. */
    fun linkedExileDistinctCardTypeCount(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.LINKED_EXILE_DISTINCT_CARD_TYPE_COUNT)

    /** The number of targets this spell or ability has. */
    fun targetCount(): DynamicAmount = DynamicAmount.ContextProperty(ContextPropertyKey.TARGET_COUNT)

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
    fun battlefield(player: Player, filter: GameObjectFilter = GameObjectFilter.Any, excludeSelf: Boolean = false) =
        BattlefieldQuery(player, filter, excludeSelf)

    /**
     * An aggregate over the battlefield permanents [player] controls that match [filter] —
     * [excludeSelf] leaves the source out ("the number of *other* creatures you control").
     */
    class BattlefieldQuery(
        private val player: Player,
        private val filter: GameObjectFilter,
        private val excludeSelf: Boolean = false
    ) {
        private fun aggregate(aggregation: Aggregation, property: CardNumericProperty? = null): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, aggregation, property, excludeSelf)

        fun count(): DynamicAmount = aggregate(Aggregation.COUNT)

        fun maxManaValue(): DynamicAmount = aggregate(Aggregation.MAX, CardNumericProperty.MANA_VALUE)

        fun maxPower(): DynamicAmount = aggregate(Aggregation.MAX, CardNumericProperty.POWER)

        fun maxToughness(): DynamicAmount = aggregate(Aggregation.MAX, CardNumericProperty.TOUGHNESS)

        fun minToughness(): DynamicAmount = aggregate(Aggregation.MIN, CardNumericProperty.TOUGHNESS)

        fun sumPower(): DynamicAmount = aggregate(Aggregation.SUM, CardNumericProperty.POWER)

        fun sumToughness(): DynamicAmount = aggregate(Aggregation.SUM, CardNumericProperty.TOUGHNESS)

        fun sumManaValue(): DynamicAmount = aggregate(Aggregation.SUM, CardNumericProperty.MANA_VALUE)

        /** The number of colors among the matched permanents. */
        fun distinctColors(): DynamicAmount = aggregate(Aggregation.DISTINCT_COLORS)

        /** The number of card types among the matched permanents. */
        fun distinctTypes(): DynamicAmount = aggregate(Aggregation.DISTINCT_TYPES)

        /** The total number of [counterType] counters on the matched permanents. */
        fun totalCounters(counterType: CounterType): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, Aggregation.SUM, excludeSelf = excludeSelf, counterType = counterType)

        /**
         * The number of distinct values of [property] (power / toughness / mana value) among the
         * matched permanents — e.g. `distinctValues(POWER)` for "the number of different powers
         * among creatures you control" (Selvala, Eager Trailblazer). Two permanents sharing a
         * value count once.
         */
        fun distinctValues(property: CardNumericProperty): DynamicAmount =
            aggregate(Aggregation.DISTINCT_VALUES, property)

        /**
         * The number of differently named matched permanents — e.g. `distinctNames()` over
         * `GameObjectFilter.Land` for "the number of differently named lands you control"
         * (Emil, Vastlands Roamer). Two permanents sharing a name count once.
         */
        fun distinctNames(): DynamicAmount = aggregate(Aggregation.DISTINCT_NAMES)
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

        /** The number of card types among the matched cards ("card types among cards in your graveyard"). */
        fun distinctTypes(): DynamicAmount =
            DynamicAmount.AggregateZone(player, zone, filter, Aggregation.DISTINCT_TYPES)
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
     * The number of different *color pairs* among the permanents [player] controls that are
     * exactly two colors (CR 105.2c) — Niv-Mizzet, Guildpact's X. Maxes out at 10.
     *
     * A permanent contributes only if it is exactly two colors; mono-colored, three-or-more
     * colored, and colorless permanents contribute nothing, and two permanents of the same pair
     * count once. Reads colors via projected state, so recolor effects apply.
     */
    fun colorPairsAmongPermanents(
        player: Player = Player.You,
        filter: GameObjectFilter = GameObjectFilter.Permanent
    ): DynamicAmount =
        DynamicAmount.AggregateBattlefield(player, filter, Aggregation.DISTINCT_COLOR_PAIRS)

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

    /**
     * Legendary creatures you control — the Kamigawa channel lands' "costs {1} less to activate
     * for each legendary creature you control", and the same count the legendary-matters payoffs
     * read. Counts creatures only: a legendary land or artifact does not qualify.
     */
    fun legendaryCreaturesYouControl(): DynamicAmount =
        battlefield(Player.You, GameObjectFilter.Creature.legendary()).count()

    fun allCreatures(): DynamicAmount =
        battlefield(Player.Each, GameObjectFilter.Creature).count()

    /**
     * "The greatest number of [filter] a player controls" — [DynamicAmount.GreatestAmongPlayers]
     * around a per-player battlefield count, which is the whole of Investigator's Journal's
     * enters-with amount.
     *
     * Note the difference from [allCreatures] and every other `battlefield(Player.Each, …)` count:
     * those give the table's **total**, because the underlying aggregate flattens all players'
     * permanents into one list before counting. This one keeps the per-player boundary and takes
     * the largest, which is what "a player controls" means in Oracle's superlative wording.
     *
     * @param players [Player.Each] for "a player", [Player.EachOpponent] for "an opponent".
     */
    fun greatestControlledBySinglePlayer(
        filter: GameObjectFilter = GameObjectFilter.Any,
        players: Player = Player.Each,
    ): DynamicAmount = DynamicAmount.GreatestAmongPlayers(
        players = players,
        inner = battlefield(Player.You, filter).count(),
    )

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
     * The number of planeswalker types (CR 205.3j) among planeswalkers [player] controls — two
     * Jaces count once. Reads projected subtypes. Tam, the Possibility: "Proliferate X times,
     * where X is the number of planeswalker types among planeswalkers you control."
     */
    fun planeswalkerTypes(player: Player = Player.You): DynamicAmount =
        DynamicAmount.AggregateBattlefield(
            player = player,
            filter = GameObjectFilter.Planeswalker,
            aggregation = Aggregation.DISTINCT_PLANESWALKER_SUBTYPES
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

    /**
     * The number of *permanents* with [subtype] on the battlefield — what a **bare** tribal noun
     * counts.
     *
     * "…where X is the number of **Slivers** on the battlefield" counts every Sliver permanent,
     * while "the number of **Sliver creatures**" counts only the creatures. Oracle spells the two
     * differently and means two different things, so they are two facades rather than one:
     * [creaturesWithSubtype] is the adjectival form's counterpart. Reach for this one whenever the
     * printed text is the bare noun.
     */
    fun permanentsWithSubtype(subtype: Subtype): DynamicAmount =
        battlefield(Player.Each, GameObjectFilter.Permanent.withSubtype(subtype)).count()

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

    fun cardsInYourGraveyard(player: Player = Player.You): DynamicAmount =
        DynamicAmount.Count(player, Zone.GRAVEYARD)

    /**
     * Creature cards in [player]'s graveyard. Defaults to [Player.You] ("creature cards in your
     * graveyard"); pass [Player.Each] for the "in **all** graveyards" wording — Undergrowth
     * Scavenger's entry counter count.
     */
    fun creatureCardsInYourGraveyard(player: Player = Player.You): DynamicAmount =
        DynamicAmount.Count(player, Zone.GRAVEYARD, GameObjectFilter.Creature)

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
     * The number of [filter] counters the source had as it last existed on the battlefield
     * (CR 113.7a / 608.2h) — either wiped by its own self-exile / self-sacrifice cost ("for each
     * verse counter on this" / "if it had seven or more counters on it", Lost Isle Calling) or
     * captured when it left the battlefield, for a dies/leaves trigger ("if it had a revival
     * counter on it", Nine-Lives Familiar). See [DynamicAmount.LastKnownSourceCounters]; use
     * [countersOnSelf] for a permanent that is still on the battlefield.
     */
    fun lastKnownSourceCounters(counterType: CounterType?): DynamicAmount =
        DynamicAmount.LastKnownSourceCounters(counterType)

    /**
     * The total damage dealt to the source this turn, captured as last-known information when it
     * left the battlefield — "where X is the amount of damage dealt to it this turn" (Tangled
     * Colony). See [DynamicAmount.LastKnownDamageDealtToSource].
     */
    fun lastKnownDamageDealtToSource(): DynamicAmount = DynamicAmount.LastKnownDamageDealtToSource

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
    // Death-batch trigger values
    // =========================================================================

    /**
     * "The total power of those creatures" — the summed last-known power of the creatures that
     * died in the batch that fired a `OneOrMoreCreaturesYouControlDie` trigger (CR 603.2c). Only
     * the deaths matching the trigger's filter count. Captured at detection time (the graveyard
     * card's printed power would drop counters/buffs), so it survives to resolution. `0` outside a
     * creatures-died batch trigger. Used by The Skullspore Nexus — "create a … token with base
     * power and toughness each equal to the total power of those creatures."
     */
    fun diedBatchTotalPower(): DynamicAmount =
        DynamicAmount.ContextProperty(ContextPropertyKey.DIED_BATCH_TOTAL_POWER)

    // =========================================================================
    // Prevention-reaction values
    // =========================================================================

    /**
     * "That much" / "that many" — the amount of damage a prevention shield just prevented, readable
     * inside the shield's `onPrevented` follow-up (New Way Forward, Deflecting Palm). See
     * [Effects.PreventDamage]'s `onPrevented`.
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

    /** The number of cards [player] has drawn this turn. */
    fun cardsDrawnThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.CARDS_DRAWN)

    /** The damage [player] has been dealt this turn. */
    fun damageReceivedThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.DAMAGE_RECEIVED)

    /** The number of creatures that left the battlefield under [player]'s control this turn. */
    fun creaturesLeftBattlefieldThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.CREATURES_LEFT_BATTLEFIELD)

    /** The number of different bending kinds [player] has performed this turn (Avatar). */
    fun distinctBendsThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.DISTINCT_BENDS)

    /**
     * Artifacts put into a graveyard from the battlefield this turn. Defaults to [Player.Each] —
     * the **game-wide** count, which is the only reading printed so far ("the number of artifacts
     * that were put into graveyards from the battlefield this turn", Anzrag's Rampage). Pass
     * [Player.You] for the controller-scoped slice.
     */
    fun artifactsDiedThisTurn(player: Player = Player.Each): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.ARTIFACTS_DIED)

    fun opponentsWhoLostLifeThisTurn(): DynamicAmount =
        DynamicAmount.TurnTracking(Player.You, TurnTracker.OPPONENTS_WHO_LOST_LIFE)

    fun opponentCreaturesExiledThisTurn(): DynamicAmount =
        DynamicAmount.TurnTracking(Player.You, TurnTracker.OPPONENT_CREATURES_EXILED)

    fun lifeGainedThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.LIFE_GAINED)

    /**
     * "The amount of life [player] lost this turn" (Rowan, Scion of War) — damage taken,
     * life-loss effects and life paid as a cost. Life gained never nets against it.
     */
    fun lifeLostThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.LIFE_LOST_AMOUNT)

    /**
     * "The number of lands that entered the battlefield under [player]'s control this turn"
     * (Bioengineered Future). Counts every land ETB under the player — land drops, Lander
     * search, Cultivate-style "put a land onto the battlefield" effects — not just land
     * drops. Reads the per-player permanent-entry log populated by
     * `PermanentEntryTracker`.
     */
    fun landsEnteredUnderControlThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.LANDS_ENTERED_UNDER_CONTROL)

    /**
     * "The number of nonland permanents that entered the battlefield under [player]'s control this
     * turn" — the complement of [landsEnteredUnderControlThisTurn] over the same per-player entry
     * log. Tokens count; a land creature does not. The threshold form is the Celebration ability
     * word (`Conditions.Celebration`); this is the raw count for "for each …" scaling.
     */
    fun nonlandPermanentsEnteredUnderControlThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.NONLAND_PERMANENTS_ENTERED)

    /**
     * "The number of [subtype]s that died this turn" — game-wide by default ([Player.Each]).
     * See [DynamicAmount.CreaturesWithSubtypeDiedThisTurn]. Used by the Zubera cycle
     * ("for each Zubera that died this turn").
     */
    fun creaturesWithSubtypeDiedThisTurn(
        subtype: com.wingedsheep.sdk.core.Subtype,
        player: Player = Player.Each
    ): DynamicAmount = DynamicAmount.CreaturesWithSubtypeDiedThisTurn(subtype, player)

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
        DynamicAmount.SubtypeEnteredUnderControlThisTurn(player, setOf(subtype), excludeTriggeringEntity)

    /**
     * "The number of As and/or Bs that entered the battlefield under [player]'s control this turn"
     * (Cloudspire Coordinator — "Mounts and/or Vehicles"). Any-of over [subtypes], so a permanent
     * carrying several of them still counts once; summing per-subtype amounts would not.
     */
    fun subtypesEnteredUnderControlThisTurn(
        subtypes: Set<com.wingedsheep.sdk.core.Subtype>,
        player: Player = Player.You,
        excludeTriggeringEntity: Boolean = false
    ): DynamicAmount =
        DynamicAmount.SubtypeEnteredUnderControlThisTurn(player, subtypes, excludeTriggeringEntity)

    /**
     * "The number of times [player] descended this turn" (CR 700.11) — count of
     * nontoken permanent cards put into [player]'s graveyard from any zone this turn.
     * Used by the descend N / fathomless descent ability words.
     */
    fun descendedThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.DESCENDED)

    /**
     * "The number of cards [player] has discarded this turn" (CR 701.8). Reads the per-player
     * `CardsDiscardedThisTurnComponent`; every discard site (cost, effect, cycling, hand-size
     * cleanup) feeds it. Used by Green Goblin, Revenant ("draw a card for each card you've
     * discarded this turn").
     */
    fun cardsDiscardedThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.CARDS_DISCARDED)

    /**
     * "The number of cards that were put into [player]'s graveyard from their library this turn"
     * (Cruel Calculations, with `Player.ContextPlayer(0)` for "target player"). Mill, surveil and
     * any other library → graveyard move all count; see
     * [TurnTracker.CARDS_PUT_INTO_GRAVEYARD_FROM_LIBRARY].
     */
    fun cardsPutIntoGraveyardFromLibraryThisTurn(player: Player = Player.You): DynamicAmount =
        DynamicAmount.TurnTracking(player, TurnTracker.CARDS_PUT_INTO_GRAVEYARD_FROM_LIBRARY)

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
     *
     * Pass [beforeTriggeringSpell] for the storm-style "each other spell you've cast **before it**
     * this turn" clause: only casts recorded ahead of the triggering spell count, so neither the
     * triggering spell nor anything cast in response to the trigger is included.
     *
     * ```kotlin
     * // Thousand-Year Storm: "for each other instant and sorcery spell you've cast before it"
     * DynamicAmounts.spellsCastThisTurn(
     *     filter = GameObjectFilter.InstantOrSorcery, beforeTriggeringSpell = true)
     * ```
     */
    fun spellsCastThisTurn(
        player: Player = Player.You,
        filter: GameObjectFilter = GameObjectFilter.Any,
        excludeSelf: Boolean = false,
        fromZone: Zone? = null,
        beforeTriggeringSpell: Boolean = false,
        countDistinctCardTypes: Boolean = false
    ): DynamicAmount =
        DynamicAmount.SpellsCastThisTurn(
            player, filter, excludeSelf, fromZone, countDistinctCardTypes, beforeTriggeringSpell
        )

    /** The total number of spells cast during the immediately preceding turn. */
    fun spellsCastLastTurn(): DynamicAmount = DynamicAmount.SpellsCastLastTurn

    /** The starting life total of a player (20 in standard, 40 in commander). */
    fun startingLifeTotal(player: Player = Player.You): DynamicAmount =
        DynamicAmount.StartingLifeTotal(player)

    /**
     * A player's speed, 0–4 (Aetherdrift, CR 702.179) — "where X is your speed". A player with no
     * speed reads as 0 (CR 702.179f), so this never needs a guard.
     */
    fun speed(player: Player = Player.You): DynamicAmount = DynamicAmount.Speed(player)

    /**
     * How many counters of [counterType] a player currently has (CR 122.1 — counters placed on a
     * player rather than a permanent). Poison, energy, and rad counters all live here.
     */
    fun playerCounterCount(counterType: CounterType, player: Player = Player.You): DynamicAmount =
        DynamicAmount.PlayerCounterCount(counterType, player)

    /**
     * A player's current energy counter total (CR 107.14) — "where X is the number of energy
     * counters you have" (Longtusk Cub, Electrostatic Pummeler).
     */
    fun energyCount(player: Player = Player.You): DynamicAmount =
        DynamicAmount.PlayerCounterCount(com.wingedsheep.sdk.core.CounterType.ENERGY, player)

    // =========================================================================
    // Entity property shortcuts (composable entity + property)
    // =========================================================================

    /** [property] of [entity] — the general form behind [powerOf], [manaValueOf], [countersOn], …. */
    fun propertyOf(entity: EffectTarget.SingleEntity, property: EntityNumericProperty): DynamicAmount =
        DynamicAmount.EntityProperty(entity, property)

    /** [entity]'s power (a target handle, `EffectTarget.TriggeringEntity`, `IterationEntity`, …). */
    fun powerOf(entity: EffectTarget.SingleEntity): DynamicAmount = propertyOf(entity, EntityNumericProperty.Power)

    /** [entity]'s toughness. */
    fun toughnessOf(entity: EffectTarget.SingleEntity): DynamicAmount = propertyOf(entity, EntityNumericProperty.Toughness)

    /** [entity]'s mana value. */
    fun manaValueOf(entity: EffectTarget.SingleEntity): DynamicAmount = propertyOf(entity, EntityNumericProperty.ManaValue)

    /** The amount of mana spent to cast [entity] (a spell) — reduced by cost reductions, unlike its mana value. */
    fun manaSpentToCast(entity: EffectTarget.SingleEntity): DynamicAmount = propertyOf(entity, EntityNumericProperty.ManaSpent)

    /** The number of [type] counters on [entity] (`null` = counters of any kind). */
    fun countersOn(entity: EffectTarget.SingleEntity, type: CounterType?): DynamicAmount =
        propertyOf(entity, EntityNumericProperty.CounterCount(type))

    fun sourcePower(): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.Self, EntityNumericProperty.Power)

    fun sourceToughness(): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.Self, EntityNumericProperty.Toughness)

    /**
     * The source's own mana value — the ninth cell of the source/target/triggering grid the two
     * neighbouring blocks fill for power and toughness, and the only one that had no name.
     * "{2}, {T}, Sacrifice this artifact: You gain life equal to its mana value."
     */
    fun sourceManaValue(): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.Self, EntityNumericProperty.ManaValue)

    /** Power of the creature the source Aura/Equipment is attached to. */
    fun enchantedCreaturePower(): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.EnchantedCreature, EntityNumericProperty.Power)

    fun targetPower(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.ContextTarget(index), EntityNumericProperty.Power)

    fun targetToughness(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.ContextTarget(index), EntityNumericProperty.Toughness)

    fun targetManaValue(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.ContextTarget(index), EntityNumericProperty.ManaValue)

    fun targetManaSpent(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.ContextTarget(index), EntityNumericProperty.ManaSpent)

    /** Number of distinct colors of the indexed cast-time target ("for each color of the creature it targets"). */
    fun targetColorCount(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.ContextTarget(index), EntityNumericProperty.ColorCount)

    /** Number of distinct colors of any referenced entity. */
    fun colorCountOf(entity: EffectTarget.SingleEntity): DynamicAmount =
        DynamicAmount.EntityProperty(entity, EntityNumericProperty.ColorCount)

    /**
     * The number of mana symbols of [colors] in the referenced entity's printed mana cost — the
     * per-object pip count ("with one or more blue mana symbols in its mana cost … create that
     * many", Namor the Sub-Mariner), *not* devotion. Use [devotionTo] for the whole-battlefield
     * count of CR 700.5. Hybrid and Phyrexian pips count for their colour(s) (CR 107.4e/f).
     */
    fun coloredManaSymbolsOf(entity: EffectTarget.SingleEntity, vararg colors: Color): DynamicAmount =
        DynamicAmount.EntityProperty(entity, EntityNumericProperty.ColoredManaSymbolCount(colors.toList()))

    fun sacrificedPower(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.SacrificedAsCost(index), EntityNumericProperty.Power)

    fun sacrificedToughness(index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.SacrificedAsCost(index), EntityNumericProperty.Toughness)

    fun countersOnSelf(type: CounterType?): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.Self, EntityNumericProperty.CounterCount(type))

    fun countersOnTarget(type: CounterType?, index: Int = 0): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.ContextTarget(index), EntityNumericProperty.CounterCount(type))

    /**
     * Number of counters (of [type]; defaults to counters of every kind) on the triggering
     * permanent — "X is the number of counters on it" for an ANY-bound triggered ability such as
     * Spider-Man Noir's "whenever a creature you control attacks alone."
     */
    fun countersOnTriggering(type: CounterType? = null): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.TriggeringEntity, EntityNumericProperty.CounterCount(type))

    fun attachmentsOnSelf(): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.Self, EntityNumericProperty.AttachmentCount())

    /**
     * Number of attachments of [kind] on the creature the source is attached to — the *enchanted*
     * creature for an Aura, the *equipped* creature for an Equipment; both read the same attachment
     * link. Defaults to [AttachmentKind.ANY]: every Aura and Equipment (With Great Power…:
     * "enchanted creature gets +2/+2 for each Aura and Equipment attached to it"). Pass
     * [AttachmentKind.EQUIPMENT] for the Equipment-only count an Equipment buffing its own host by
     * that host's Equipment needs (Golem-Skin Gauntlets: "equipped creature gets +1/+0 for each
     * Equipment attached to it" — which includes the Gauntlets themselves).
     *
     * Distinct from [attachmentsOnSelf], which counts attachments on the source itself.
     */
    fun attachmentsOnEnchantedCreature(kind: AttachmentKind = AttachmentKind.ANY): DynamicAmount =
        DynamicAmount.EntityProperty(
            EffectTarget.EnchantedCreature,
            EntityNumericProperty.AttachmentCount(kind)
        )

    /** Number of Equipment attached to the source (Shagrat, Loot Bearer's amass amount). */
    fun equipmentAttachedToSelf(): DynamicAmount =
        DynamicAmount.EntityProperty(
            EffectTarget.Self,
            EntityNumericProperty.AttachmentCount(AttachmentKind.EQUIPMENT)
        )

    fun numberOfBlockers(): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.TriggeringEntity, EntityNumericProperty.BlockerCount)

    fun triggeringManaValue(): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.TriggeringEntity, EntityNumericProperty.ManaValue)

    fun triggeringPower(): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.TriggeringEntity, EntityNumericProperty.Power)

    fun triggeringToughness(): DynamicAmount =
        DynamicAmount.EntityProperty(EffectTarget.TriggeringEntity, EntityNumericProperty.Toughness)

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

    /**
     * Total power of the permanents sacrificed by the current resolving effect ("their total
     * power"), read from the same `sacrificedPermanents` snapshots as
     * [permanentsSacrificedThisWay]. See [DynamicAmount.TotalPowerSacrificedThisWay]. Used by
     * "exile the top X cards of your library, where X is their total power" (Kylox, Visionary
     * Inventor).
     */
    fun totalPowerSacrificedThisWay(): DynamicAmount =
        DynamicAmount.TotalPowerSacrificedThisWay

    /**
     * "That many" — the number of repetitions a
     * [com.wingedsheep.sdk.scripting.effects.PayManaCostRepeatedlyEffect] was paid, read back out
     * of the resolution pipeline. Pair it with the matching `storeCountAs` when the effect uses a
     * non-default name.
     *
     * Hawkeye, Master Marksman: "you may pay {1} up to three times. When you do, choose up to
     * **that many** —" is `dynamicChooseCount = DynamicAmounts.timesPaid()` on the reflexive
     * trigger's modal.
     */
    fun timesPaid(
        storeCountAs: String =
            com.wingedsheep.sdk.scripting.effects.PayManaCostRepeatedlyEffect.TIMES_PAID
    ): DynamicAmount = DynamicAmount.VariableReference(storeCountAs)

    /** How many distinct entities are in the pipeline [collection] (typed form). */
    fun distinctEntitiesIn(collection: CollectionSlot): DynamicAmount = distinctEntitiesIn(listOf(collection))

    /** How many distinct entities appear across the pipeline [collections] (typed form). */
    fun distinctEntitiesIn(collections: List<CollectionSlot>): DynamicAmount =
        DynamicAmount.DistinctEntitiesInCollections(collections.map { it.key })

    /** How many distinct card types are among the cards in the pipeline [collection] (typed form). */
    fun distinctCardTypesIn(collection: CollectionSlot): DynamicAmount = distinctCardTypesIn(listOf(collection))

    /** How many distinct card types appear across the pipeline [collections] (typed form). */
    fun distinctCardTypesIn(collections: List<CollectionSlot>): DynamicAmount =
        DynamicAmount.DistinctCardTypesInCollections(collections.map { it.key })

    /** The total mana value of the cards in [collection]. */
    fun manaValueSumOf(collection: CollectionSlot): DynamicAmount = manaValueSumOf(collection.key)

    /** The mana value of the (first) card in [collection]. */
    fun manaValueOf(collection: CollectionSlot): DynamicAmount = DynamicAmount.StoredCardManaValue(collection.key)

    /** The mana value of the (first) card an effect outside a pipeline stored under [collectionName]. */
    fun manaValueOf(collectionName: String): DynamicAmount = DynamicAmount.StoredCardManaValue(collectionName)
}
