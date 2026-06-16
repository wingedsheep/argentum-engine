package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.WasCast as WasCastCondition
import com.wingedsheep.sdk.scripting.conditions.NoManaSpentToCast as NoManaSpentToCastCondition
import com.wingedsheep.sdk.scripting.conditions.NoManaSpentToCastEntered as NoManaSpentToCastEnteredCondition
import com.wingedsheep.sdk.scripting.conditions.WasCastFromHand as WasCastFromHandCondition
import com.wingedsheep.sdk.scripting.conditions.WasCastFromZone as WasCastFromZoneCondition
import com.wingedsheep.sdk.scripting.conditions.WasKicked as WasKickedCondition
import com.wingedsheep.sdk.scripting.conditions.BlightWasPaid as BlightWasPaidCondition
import com.wingedsheep.sdk.scripting.conditions.SneakCostWasPaid as SneakCostWasPaidCondition
import com.wingedsheep.sdk.scripting.conditions.CastChoiceMade as CastChoiceMadeCondition
import com.wingedsheep.sdk.scripting.conditions.CastChoiceIs as CastChoiceIsCondition
import com.wingedsheep.sdk.scripting.conditions.CastTimeFlagSet as CastTimeFlagSetCondition
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.conditions.SourceIsRingBearer as SourceIsRingBearerCondition
import com.wingedsheep.sdk.scripting.conditions.YouChoseOtherCreatureAsRingBearer as YouChoseOtherCreatureAsRingBearerCondition
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn as IsYourTurnCondition
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn as IsNotYourTurnCondition
import com.wingedsheep.sdk.scripting.conditions.IsInPhase as IsInPhaseCondition
import com.wingedsheep.sdk.scripting.conditions.PlayerAttackedWithCreaturesThisTurn
import com.wingedsheep.sdk.scripting.conditions.PlayerCastSpellsThisTurn
import com.wingedsheep.sdk.scripting.conditions.PlayerCommittedCrimeThisTurn
import com.wingedsheep.sdk.scripting.conditions.PlayerHasCitysBlessing
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.scripting.conditions.Condition as ConditionInterface

/**
 * Facade object providing convenient access to Condition types.
 *
 * Usage:
 * ```kotlin
 * Conditions.OpponentControlsMoreLands
 * Conditions.LifeAtMost(5)
 * Conditions.ControlCreature
 * ```
 */
object Conditions {

    // =========================================================================
    // The Ring (CR 701.54)
    // =========================================================================

    /**
     * If the source permanent is your Ring-bearer (CR 701.54e).
     */
    val SourceIsRingBearer: ConditionInterface = SourceIsRingBearerCondition

    /**
     * If you chose a creature other than this as your Ring-bearer (CR 701.54a). Intervening-if
     * for `Triggers.RingTemptsYou` payoffs that fire only when the player picked someone else.
     */
    val YouChoseOtherCreatureAsRingBearer: ConditionInterface = YouChoseOtherCreatureAsRingBearerCondition

    /**
     * If you put a counter on this creature this turn (Secrets of Strixhaven — Fractal
     * Tender). True while the source permanent carries the per-turn "received counters"
     * marker, which the counter-placement path stamps and cleanup clears each turn.
     */
    val SourceReceivedCounterThisTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SourceReceivedCounterThisTurn

    // =========================================================================
    // Battlefield Conditions (via Exists / Compare)
    // =========================================================================

    /**
     * Generic numeric comparison of two [DynamicAmount]s with a [ComparisonOperator] — the
     * facade entry point for any "if amount X (</≤/=/≠/>/≥) amount Y" intervening-if or static
     * condition. Composes the underlying [Compare] condition.
     *
     * Example (Taii Wakeen, Perfect Shot intervening-if — "damage equal to that creature's
     * toughness"):
     * ```
     * Conditions.CompareAmounts(
     *     DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
     *     ComparisonOperator.EQ,
     *     DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_RECIPIENT_TOUGHNESS),
     * )
     * ```
     */
    fun CompareAmounts(
        left: DynamicAmount,
        operator: ComparisonOperator,
        right: DynamicAmount,
    ): ConditionInterface = Compare(left, operator, right)

    /**
     * If an opponent controls more lands than you.
     */
    val OpponentControlsMoreLands: ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.EachOpponent, GameObjectFilter.Land),
            ComparisonOperator.GT,
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land)
        )

    /**
     * If an opponent controls more creatures than you.
     */
    val OpponentControlsMoreCreatures: ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.EachOpponent, GameObjectFilter.Creature),
            ComparisonOperator.GT,
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature)
        )

    /**
     * If you control more creatures than opponent.
     * Used for CantAttackUnless / CantBlockUnless (e.g. Goblin Goon).
     */
    val ControlMoreCreatures: ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature),
            ComparisonOperator.GT,
            DynamicAmount.AggregateBattlefield(Player.EachOpponent, GameObjectFilter.Creature)
        )

    /**
     * If opponent controls a land of a specific subtype.
     * Used for CantAttackUnless (e.g. Deep-Sea Serpent, Slipstream Eel).
     */
    fun OpponentControlsLandType(landType: String): ConditionInterface =
        Exists(Player.EachOpponent, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype(landType))

    /**
     * If an opponent controls a creature.
     */
    val OpponentControlsCreature: ConditionInterface =
        Exists(Player.EachOpponent, Zone.BATTLEFIELD, GameObjectFilter.Creature)

    /**
     * If you control a creature.
     */
    val ControlCreature: ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature)

    /**
     * If there are no creatures anywhere on the battlefield (either player). Global scope —
     * `Player.Each` checks every player's battlefield, negated. Used by Drop of Honey's
     * "when there are no creatures on the battlefield, sacrifice this enchantment".
     */
    val NoCreaturesOnBattlefield: ConditionInterface =
        Exists(Player.Each, Zone.BATTLEFIELD, GameObjectFilter.Creature, negate = true)

    /**
     * If you control at least one permanent matching [filter].
     * General-purpose battlefield existence check — pass any [GameObjectFilter]
     * (e.g. `GameObjectFilter.Creature.copy(statePredicates = listOf(StatePredicate.HasAnyCounter))`
     * for "a creature with a counter on it").
     */
    fun YouControl(filter: GameObjectFilter, negate: Boolean = false): ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, filter, negate = negate)

    /**
     * If you control an enchantment.
     */
    val ControlEnchantment: ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Enchantment)

    /**
     * If you control an artifact.
     */
    val ControlArtifact: ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Artifact)

    /**
     * If you control a legendary creature or planeswalker.
     * Used as the cast restriction for legendary sorceries.
     */
    val ControlLegendaryCreatureOrPlaneswalker: ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.CreatureOrPlaneswalker.legendary())

    /**
     * If you control N or more lands.
     */
    fun ControlLandsAtLeast(count: Int): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * Domain threshold: if [count] or more basic land types are among lands you control.
     * Reads via projected state, so type-changed lands and dual lands count.
     */
    fun BasicLandTypesAtLeast(count: Int): ConditionInterface =
        Compare(
            DynamicAmounts.domain(Player.You),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If you control N or more permanents (any type).
     * Used as the intervening-if for Ascend triggers (10+ permanents → city's blessing).
     */
    fun ControlPermanentsAtLeast(count: Int): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Any),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If you control N or more creatures.
     */
    fun ControlCreaturesAtLeast(count: Int): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If you control [count] or more permanents matching [filter].
     *
     * The general-purpose filtered-count form of [ControlCreaturesAtLeast] /
     * [ControlLandsAtLeast] — pass any [GameObjectFilter] (e.g.
     * `GameObjectFilter.Creature.attacking()` for "three or more attacking creatures",
     * Stormbeacon Blade). [YouControl] checks mere existence; this counts the group.
     */
    fun YouControlAtLeast(count: Int, filter: GameObjectFilter): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, filter),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If [count] or more different kinds of counters are among permanents you control matching
     * [filter] (default: creatures). Counts distinct counter kinds across the whole group — a
     * +1/+1 and a finality counter on two creatures is two kinds; the same kind on several
     * permanents counts once. Used for Hundred-Battle Veteran ("three or more different kinds of
     * counters among creatures you control").
     */
    fun DifferentCounterKindsAtLeast(
        count: Int,
        filter: GameObjectFilter = GameObjectFilter.Creature
    ): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, filter, Aggregation.DISTINCT_COUNTER_TYPES),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If you control a creature with a specific keyword.
     */
    fun ControlCreatureWithKeyword(keyword: Keyword): ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.withKeyword(keyword))

    /**
     * If you control a creature of a specific type.
     */
    fun ControlCreatureOfType(subtype: Subtype): ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.withSubtype(subtype))

    /**
     * If a player controls more creatures of the given subtype than each other player.
     */
    fun APlayerControlsMostOfSubtype(subtype: Subtype): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.APlayerControlsMostOfSubtype(subtype)

    /**
     * If the target creature's power is at most the given dynamic amount.
     * Used for cards like Unified Strike.
     */
    fun TargetPowerAtMost(amount: DynamicAmount, targetIndex: Int = 0): ConditionInterface =
        Compare(DynamicAmount.EntityProperty(EntityReference.Target(targetIndex), EntityNumericProperty.Power), ComparisonOperator.LTE, amount)

    /**
     * If the target spell's mana value is at most the given dynamic amount.
     * Used for conditional counterspells like Dispersal Shield.
     */
    fun TargetSpellManaValueAtMost(amount: DynamicAmount, targetIndex: Int = 0): ConditionInterface =
        Compare(DynamicAmount.EntityProperty(EntityReference.Target(targetIndex), EntityNumericProperty.ManaValue), ComparisonOperator.LTE, amount)

    /**
     * If the target permanent has at least one counter of the given type.
     * Used for cards like Bring Low: "If that creature has a +1/+1 counter on it"
     */
    fun TargetHasCounter(counterType: CounterTypeFilter, targetIndex: Int = 0): ConditionInterface =
        Compare(DynamicAmount.EntityProperty(EntityReference.Target(targetIndex), EntityNumericProperty.CounterCount(counterType)), ComparisonOperator.GTE, DynamicAmount.Fixed(1))

    /**
     * If the chosen target at [targetIndex] matches a GameObjectFilter. Resolution-only; a player
     * target never matches a game-object filter (use [TargetIsPlayer] for that). Used for cards
     * like Blessing of Belzenlok: "If it's legendary, it also gains lifelink."
     */
    fun TargetMatchesFilter(filter: GameObjectFilter, targetIndex: Int = 0): ConditionInterface =
        EntityMatches(EffectTarget.ContextTarget(targetIndex), filter)

    /**
     * If the context target at [targetIndex] is a player (not a permanent/spell/card).
     * Used for "any target" effects with a player-only follow-up — e.g. Sonic Shrieker's
     * "If a player is dealt damage this way, they discard a card."
     */
    fun TargetIsPlayer(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetIsPlayer(targetIndex)

    /**
     * If the context target at [targetIndex] is a tapped battlefield permanent. Branch on a
     * target's tapped state at resolution — e.g. Shackle Slinger's "If it's tapped, put a stun
     * counter on it. Otherwise, tap it."
     */
    fun TargetIsTapped(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetIsTapped(targetIndex)

    /**
     * If the context target at [targetIndex] is this permanent (the ability's source). Wrap in
     * [Not] for "another"/"a different permanent" wordings — e.g. Arid Archway's "If another
     * Desert was returned this way".
     */
    fun TargetIsSource(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetIsSource(targetIndex)

    /**
     * If the target shares a color with the most common color among all permanents
     * (or a color tied for most common). Used by Tsabo's Assassin.
     */
    fun TargetSharesMostCommonColor(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetSharesMostCommonColor(targetIndex)

    /**
     * "If excess damage was dealt this way" — true post-damage when the target creature's
     * marked damage strictly exceeds its (projected) toughness. Chain after `DealDamage`
     * in a composite to fire a payoff on lethal-exceeding damage. Used by Orbital Plunge:
     * `Composite(DealDamage(6, t), Conditional(IfTargetTookExcessDamage(), CreateLander()))`.
     *
     * Semantics caveat: the condition reads marked-damage > toughness on the target as it
     * stands when the chain reaches this step, regardless of which preceding effect dealt
     * the damage. CompositeEffect resolves sub-effects sequentially without an interleaved
     * SBA pass and without firing other triggered abilities mid-chain, so for a "deal N
     * to a target, then check" pipeline the only marked damage in play is the damage just
     * dealt — making this read equivalent to "did the source effect deal excess to the
     * target". A future card that deals damage in multiple steps within the same composite
     * (or chains past SBA somehow) would see cumulative marked damage instead, so prefer
     * a different condition for those shapes.
     */
    fun IfTargetTookExcessDamage(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetMarkedDamageExceedsToughness(targetIndex)

    /**
     * If another permanent with the same name as the target is on the battlefield.
     * The target permanent itself is excluded from the comparison. Used by Winnow.
     */
    fun AnotherPermanentWithSameNameAsTarget(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.AnotherPermanentWithSameNameAsTarget(targetIndex)

    /**
     * If [color] is the most common color among all permanents on the battlefield, or is tied
     * for most common. Board-derived, so it works as a `ConditionalStaticAbility` gate. Used by
     * the Invasion djinn cycle (Goham/Halam/Ruham/Sulam/Zanam).
     */
    fun ColorIsMostCommon(color: Color): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.ColorIsMostCommon(color)

    /**
     * If the creature enchanted by the source Aura is legendary.
     */
    fun EnchantedCreatureIsLegendary(): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureIsLegendary

    /**
     * If the permanent enchanted by the source Aura matches [filter] (color, type, etc.).
     * General-purpose; e.g. `EnchantedPermanentMatches(GameObjectFilter.Permanent.anyColorOf(Color.RED, Color.GREEN))`
     * for Essence Leak's "as long as enchanted permanent is red or green".
     */
    fun EnchantedPermanentMatches(filter: com.wingedsheep.sdk.scripting.GameObjectFilter): ConditionInterface =
        EntityMatches(EffectTarget.EnchantedPermanent, filter)

    // =========================================================================
    // Life Total Conditions (via Compare)
    // =========================================================================

    /**
     * If your life total is N or less.
     */
    fun LifeAtMost(threshold: Int): ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.LTE, DynamicAmount.Fixed(threshold))

    /**
     * If *some* player in the game has [threshold] or less life. Existential — true
     * as soon as any player (you or any opponent, including in multiplayer) matches.
     *
     * Used by cards like Razortrap Gorge ("enters tapped unless a player has 13 or
     * less life"). Distinct from [LifeAtMost], which is `Player.You` only.
     */
    fun APlayerLifeAtMost(threshold: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.APlayerLifeAtMost(threshold)

    /**
     * If your life total is N or more.
     */
    fun LifeAtLeast(threshold: Int): ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.GTE, DynamicAmount.Fixed(threshold))

    /**
     * If the controller has taken at most [threshold] turns so far — i.e. it's
     * one of their first [threshold] turns of the game. The counter increments at
     * turn start (so during their first turn it reads 1).
     *
     * Used by Starting Town: "enters tapped unless it's your first, second, or
     * third turn of the game" — pass `threshold = 3`.
     */
    fun ControllerTurnsTakenAtMost(threshold: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.ControllerTurnsTakenAtMost(threshold)

    /**
     * If you have more life than an opponent.
     */
    val MoreLifeThanOpponent: ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.GT, DynamicAmount.LifeTotal(Player.EachOpponent))

    /**
     * If you have less life than an opponent.
     */
    val LessLifeThanOpponent: ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.LT, DynamicAmount.LifeTotal(Player.EachOpponent))

    // =========================================================================
    // Hand Conditions (via Compare / Exists)
    // =========================================================================

    /**
     * If you have no cards in hand.
     */
    val EmptyHand: ConditionInterface =
        Exists(Player.You, Zone.HAND, negate = true)

    /**
     * If you have N or more cards in hand.
     */
    fun CardsInHandAtLeast(count: Int): ConditionInterface =
        Compare(DynamicAmount.Count(Player.You, Zone.HAND), ComparisonOperator.GTE, DynamicAmount.Fixed(count))

    /**
     * If you have N or fewer cards in hand.
     */
    fun CardsInHandAtMost(count: Int): ConditionInterface =
        Compare(DynamicAmount.Count(Player.You, Zone.HAND), ComparisonOperator.LTE, DynamicAmount.Fixed(count))

    /**
     * If an opponent has N or fewer cards in hand.
     */
    fun OpponentCardsInHandAtMost(count: Int): ConditionInterface =
        Compare(DynamicAmount.Count(Player.EachOpponent, Zone.HAND), ComparisonOperator.LTE, DynamicAmount.Fixed(count))

    // =========================================================================
    // Graveyard Conditions (via Compare / Exists)
    // =========================================================================

    /**
     * If there are N or more creature cards in your graveyard.
     */
    fun CreatureCardsInGraveyardAtLeast(count: Int): ConditionInterface =
        Compare(
            DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If there are N or more cards in your graveyard.
     */
    fun CardsInGraveyardAtLeast(count: Int): ConditionInterface =
        Compare(DynamicAmount.Count(Player.You, Zone.GRAVEYARD), ComparisonOperator.GTE, DynamicAmount.Fixed(count))

    /**
     * If there is a card of a specific subtype in your graveyard.
     */
    fun GraveyardContainsSubtype(subtype: Subtype): ConditionInterface =
        Exists(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.withSubtype(subtype))

    // =========================================================================
    // Source Conditions
    // =========================================================================

    /**
     * If you cast this permanent (from any zone). False if it was put onto the
     * battlefield by another effect (reanimation, tokens, "put onto the battlefield").
     * Used for ETB triggers gated on "if you cast it" (e.g., Sunderflock).
     */
    val WasCast: ConditionInterface =
        WasCastCondition

    /**
     * "if it wasn't cast or no mana was spent to cast it" — the free-cast payoff clause.
     * True when no mana at all was spent to put the source onto the battlefield (it wasn't
     * cast, or it was cast for free / for {0}); false if any mana was spent, including mana
     * for additional costs or cost increases on an otherwise-free cast. Used for the OTJ
     * free-cast payoffs (Freestrider Commando, Satoru, the Infiltrator). Compose
     * `All(WasCast, NoManaSpentToCast)` for the narrower "cast, but for free" sense.
     */
    val NoManaSpentToCast: ConditionInterface =
        NoManaSpentToCastCondition

    /**
     * "if none of them were cast or no mana was spent to cast them" — the batch-enters variant of
     * [NoManaSpentToCast]. True iff **every** permanent a batch trigger captured (the
     * `Triggers.OneOrMorePermanentsEnter` batch, exposed at resolution as the `trigger.captured`
     * collection) had no mana spent to cast it; an empty capture is vacuously true. Use as a
     * resolution-time [com.wingedsheep.sdk.dsl.Effects] `ConditionalEffect` gate on the payoff —
     * Satoru, the Infiltrator.
     */
    val NoManaSpentToCastEntered: ConditionInterface =
        NoManaSpentToCastEnteredCondition

    /**
     * If this permanent was cast from your hand.
     * Used for Phage the Untouchable.
     */
    val WasCastFromHand: ConditionInterface =
        WasCastFromHandCondition

    /**
     * If this spell was cast from the specified zone.
     * Used for flashback spells and other zone-dependent effects.
     */
    fun WasCastFromZone(zone: Zone): ConditionInterface =
        WasCastFromZoneCondition(zone)

    /**
     * If this spell was cast from a graveyard.
     * Used for flashback bonus effects.
     */
    val WasCastFromGraveyard: ConditionInterface =
        WasCastFromZoneCondition(Zone.GRAVEYARD)

    /**
     * If this spell was kicked.
     * Used for kicker spells like Shivan Fire.
     */
    val WasKicked: ConditionInterface =
        WasKickedCondition

    /**
     * If this spell's sneak cost was paid (CR 702.190 — [com.wingedsheep.sdk.scripting.KeywordAbility.Sneak]).
     * Used for riders like Leonardo, Leader in Blue and The Last Ronin's Technique whose
     * effect changes when the spell was cast for its sneak cost.
     */
    val SneakCostWasPaid: ConditionInterface =
        SneakCostWasPaidCondition

    /**
     * If this spell's blight additional cost was paid (`AdditionalCost.BlightOrPay`).
     * Used for cards like Cinder Strike whose effect changes when the optional
     * Blight path was chosen during casting.
     */
    val BlightWasPaid: ConditionInterface =
        BlightWasPaidCondition

    /**
     * If a value was locked in for [slot] when the source was cast / as it entered
     * ("a color was chosen", "this spell was kicked"). Reads the durable cast-choices bag.
     */
    fun CastChoiceMade(slot: com.wingedsheep.sdk.scripting.ChoiceSlot): ConditionInterface =
        CastChoiceMadeCondition(slot)

    /**
     * If the value locked in for [slot] equals [value] (text compare; color uses the enum name).
     * The generic slot reader, e.g. `CastChoiceIs(ChoiceSlot.MODE, "Khans")`.
     */
    fun CastChoiceIs(slot: com.wingedsheep.sdk.scripting.ChoiceSlot, value: String): ConditionInterface =
        CastChoiceIsCondition(slot, value)

    /**
     * If the named cast-time capture [flag] was true *as the source spell was cast* (CR 601.2i).
     * Pairs with the `captureAtCast(flag, condition)` spell DSL: the engine freezes the cast-time
     * answer onto the spell, and this reads it back at resolution regardless of later board changes.
     * Used by Steer Clear ("deals 4 damage instead if you controlled a Mount as you cast this spell").
     */
    fun CapturedAtCast(flag: String): ConditionInterface =
        CastTimeFlagSetCondition(flag)

    /**
     * If specific colored mana was spent to cast this spell.
     * Used for Lorwyn Incarnation cycle (Catharsis, Deceit, etc.)
     * Example: ManaSpentToCastIncludes(requiredWhite = 2) checks if {W}{W} was spent.
     */
    fun ManaSpentToCastIncludes(
        requiredWhite: Int = 0,
        requiredBlue: Int = 0,
        requiredBlack: Int = 0,
        requiredRed: Int = 0,
        requiredGreen: Int = 0
    ): ConditionInterface = com.wingedsheep.sdk.scripting.conditions.ManaSpentToCastIncludes(
        requiredWhite = requiredWhite,
        requiredBlue = requiredBlue,
        requiredBlack = requiredBlack,
        requiredRed = requiredRed,
        requiredGreen = requiredGreen
    )

    /**
     * The unified "an entity matches a filter" primitive: the [entity] (named via the shared
     * [EffectTarget] vocabulary) matches [filter]. Subsumes the older near-clones — `SourceMatches`,
     * `EnchantedPermanentMatches`, `TargetMatchesFilter`, `TriggeringSpellMatchesFilter` — which are
     * now the named [SourceMatches] / [EnchantedPermanentMatches] / [TargetMatchesFilter] /
     * [TriggeringSpellMatches] helpers below. Prefer those helpers for the common roles; reach for
     * `EntityMatches` directly when you need a role they don't name (e.g. the equipped creature).
     *
     * `Self` and the enchanted/equipped attachment roles evaluate in both resolution and
     * projection; `ContextTarget` and `TriggeringEntity` are resolution-only.
     */
    fun EntityMatches(entity: EffectTarget, filter: GameObjectFilter): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.EntityMatches(entity, filter)

    /**
     * If the source permanent matches [filter]. The general source-state primitive behind the
     * `SourceIs*` / `SourceHas*` helpers; `EntityMatches(EffectTarget.Self, filter)`. Works in both
     * resolution and static-ability projection.
     */
    fun SourceMatches(filter: GameObjectFilter): ConditionInterface =
        EntityMatches(EffectTarget.Self, filter)

    /** If this creature is attacking. */
    val SourceIsAttacking: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.attacking())

    /** If this creature is blocking. */
    val SourceIsBlocking: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.blocking())

    /** If this permanent is tapped. */
    val SourceIsTapped: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.tapped())

    /** If this permanent is untapped. */
    val SourceIsUntapped: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.untapped())

    /** If this creature has dealt damage at least once since entering the battlefield. */
    val SourceHasDealtDamage: ConditionInterface =
        SourceMatches(
            com.wingedsheep.sdk.scripting.GameObjectFilter.Any
                .copy(statePredicates = listOf(StatePredicate.HasDealtDamage))
        )

    /** If this creature has dealt combat damage to a player (Saboteur-style payoffs). */
    val SourceHasDealtCombatDamageToPlayer: ConditionInterface =
        SourceMatches(
            com.wingedsheep.sdk.scripting.GameObjectFilter.Any
                .copy(statePredicates = listOf(StatePredicate.HasDealtCombatDamageToPlayer))
        )

    /** If this permanent entered the battlefield this turn. */
    val SourceEnteredThisTurn: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.enteredThisTurn())

    /**
     * If this permanent is saddled (CR 702.171b). Gates Mount payoffs on "while saddled" /
     * "as long as it's saddled" — evaluates identically at resolution and during projection.
     */
    val SourceIsSaddled: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.saddled())

    /**
     * If this creature was declared as an attacker at least once during the current turn.
     * Used by intervening-if triggers like Erg Raiders' "if this creature didn't attack this
     * turn, deal 2 damage to you" (negate via [com.wingedsheep.sdk.scripting.conditions.NotCondition]).
     */
    val SourceAttackedThisTurn: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.attackedThisTurn())

    /**
     * As long as this creature is a specific subtype.
     * Used for conditional static abilities like "has defender as long as it's a Wall."
     */
    fun SourceHasSubtype(subtype: Subtype): ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.withSubtype(subtype))

    /**
     * As long as this creature has a specific keyword.
     * Used for conditional effects like "If this creature has flying, it gets +1/+1."
     */
    fun SourceHasKeyword(keyword: Keyword): ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.withKeyword(keyword))

    /**
     * While this creature has a counter of the given type on it.
     * Used for intervening-if triggers like Moonshadow.
     */
    fun SourceHasCounter(counterType: CounterTypeFilter): ConditionInterface {
        val predicate: StatePredicate = when (counterType) {
            is CounterTypeFilter.Any -> StatePredicate.HasAnyCounter
            is CounterTypeFilter.PlusOnePlusOne -> StatePredicate.HasCounter("PLUS_ONE_PLUS_ONE")
            is CounterTypeFilter.MinusOneMinusOne -> StatePredicate.HasCounter("MINUS_ONE_MINUS_ONE")
            is CounterTypeFilter.Loyalty -> StatePredicate.HasCounter("LOYALTY")
            is CounterTypeFilter.Named -> StatePredicate.HasCounter(
                counterType.name.uppercase().replace(' ', '_')
            )
        }
        return SourceMatches(
            com.wingedsheep.sdk.scripting.GameObjectFilter.Any
                .copy(statePredicates = listOf(predicate))
        )
    }

    /**
     * While this permanent has [count] or more counters of [counterType] on it.
     *
     * The threshold form of [SourceHasCounter] (which only checks for one). This is the gate
     * behind a Station card's `{N+}` symbol (CR 721.2a — "As long as this permanent has N or more
     * charge counters on it, it has [abilities]"): use it as the `condition` of a
     * `staticAbility { }` row, or wrapped in `ActivationRestriction.OnlyIfCondition(...)` for a
     * threshold-gated activated ability. Generic over counter type, so it also serves any other
     * "N+ counters of a kind" gate. Reads the source's counters live, so it tracks counters added
     * or removed after the permanent entered.
     */
    fun SourceCounterCountAtLeast(counterType: String, count: Int): ConditionInterface =
        Compare(
            DynamicAmount.EntityProperty(
                EntityReference.Source,
                EntityNumericProperty.CounterCount(CounterTypeFilter.Named(counterType))
            ),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If a permanent with the given subtype was sacrificed as part of the cost.
     * Used for cards like Thallid Omnivore: "If a Saproling was sacrificed this way, you gain 2 life."
     */
    fun SacrificedHadSubtype(subtype: String): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SacrificedPermanentHadSubtype(subtype)

    /**
     * If at least one permanent sacrificed as part of the cost was legendary at the
     * moment of sacrifice. Used by LTR cards like Nasty End and Gríma Wormtongue.
     */
    val SacrificedWasLegendary: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SacrificedPermanentWasLegendary

    /**
     * If at least one permanent sacrificed "this way" was controlled by the source's
     * controller at the moment of sacrifice. Used to gate the personal half of a
     * symmetric edict (Rise of the Witch-king, LTR).
     */
    val YouSacrificedThisWay: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.YouSacrificedPermanentThisWay

    // =========================================================================
    // Turn Conditions
    //
    // These all compose `Compare(DynamicAmount.TurnTracking(player, key), op, Fixed(n))`
    // around the canonical [com.wingedsheep.sdk.scripting.values.TurnTracker] enum. Counts
    // and accumulators live on per-player components in the engine; the DSL just wraps the
    // comparison with friendlier names.
    // =========================================================================

    private fun trackerAtLeast(
        tracker: com.wingedsheep.sdk.scripting.values.TurnTracker,
        atLeast: Int = 1,
        player: Player = Player.You
    ): ConditionInterface =
        Compare(
            DynamicAmount.TurnTracking(player, tracker),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(atLeast)
        )

    /**
     * If you gained life this turn.
     * Used for Lunar Convocation.
     */
    val YouGainedLifeThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.LIFE_GAINED)

    /**
     * As long as you attacked with [atLeast] or more creatures matching [filter] this turn.
     * Used for cards like Deepway Navigator: "as long as you attacked with three or more
     * Merfolk this turn".
     */
    fun YouAttackedWithCreaturesThisTurn(
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter,
        atLeast: Int
    ): ConditionInterface =
        PlayerAttackedWithCreaturesThisTurn(Player.You, filter, atLeast)

    /**
     * As long as you've cast [atLeast] or more spells matching [filter] this turn.
     * Counts every spell cast — countered, fizzled, or still on the stack all count.
     * Defaults to any spell, matching the typical "you've cast two or more spells
     * this turn" pattern (Brightspear Zealot, Illvoi Infiltrator).
     *
     * Pass [fromZone] to restrict to spells cast from that zone, independently of [filter].
     * Negate with [not] for the Prairie Dog cycle's "you haven't cast a spell from your hand
     * this turn": `not(YouCastSpellsThisTurn(1, fromZone = Zone.HAND))`.
     */
    fun YouCastSpellsThisTurn(
        atLeast: Int,
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter = com.wingedsheep.sdk.scripting.GameObjectFilter.Any,
        fromZone: com.wingedsheep.sdk.core.Zone? = null
    ): ConditionInterface =
        PlayerCastSpellsThisTurn(Player.You, filter, atLeast, fromZone)

    /**
     * If you've committed a crime this turn (CR Outlaws of Thunder Junction). A crime is committed
     * when you cast a spell, activate an ability, or put a triggered ability on the stack that
     * targets an opponent, anything an opponent controls, and/or a card in an opponent's graveyard.
     *
     * Turn-scoped tracker — stays true for the rest of the turn once any crime is committed. Used by
     * Seize the Secrets ("This spell costs {1} less to cast if you've committed a crime this turn").
     */
    val YouCommittedCrimeThisTurn: ConditionInterface =
        PlayerCommittedCrimeThisTurn(Player.You)

    /**
     * If this is the first spell you've cast this turn that mana from a Treasure was
     * spent to cast. Used by Rain of Riches ("The first spell you cast each turn that
     * mana from a Treasure was spent to cast has cascade").
     *
     * Reads the controller's `CastSpellRecord` history and is true only when exactly
     * one record this turn carries `paidWithTreasureMana = true` and that record is
     * the most recent one (i.e., this spell).
     */
    val IsFirstSpellPaidWithTreasureManaCastThisTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.IsFirstSpellPaidWithTreasureManaCastThisTurn

    /**
     * As long as you've lost life this turn.
     * Used for Essence Channeler.
     */
    val YouLostLifeThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.LIFE_LOST)

    /**
     * If you gained or lost life this turn.
     * Used for Star Charter and similar Bloomburrow cards.
     */
    val YouGainedOrLostLifeThisTurn: ConditionInterface =
        AnyCondition(listOf(YouGainedLifeThisTurn, YouLostLifeThisTurn))

    /**
     * If you gained and lost life this turn.
     * Used for Lunar Convocation's second ability.
     */
    val YouGainedAndLostLifeThisTurn: ConditionInterface =
        AllConditions(listOf(YouGainedLifeThisTurn, YouLostLifeThisTurn))

    /**
     * If you attacked this turn (you declared at least one attacker).
     * Used for Mardu Skullhunter, Mardu Hordechief, Wingmate Roc, Arrow Storm, etc.
     */
    val YouAttackedThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.PLAYER_ATTACKED)

    /**
     * If you were dealt combat damage this turn.
     */
    val YouWereDealtCombatDamageThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.DEALT_COMBAT_DAMAGE)

    /**
     * If you've played a land this turn.
     * Used for cards like Rock Jockey ("can't cast unless no land was played").
     */
    val PlayedLandThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.LANDS_PLAYED)

    /**
     * Void: "if a nonland permanent left the battlefield this turn or a spell was warped this turn".
     * Backs the Void ability word from Edge of Eternities. Tokens count as nonland permanents;
     * lands do not. A warped spell satisfies the condition even if it was countered.
     */
    val Void: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.VoidCondition

    /**
     * If an opponent lost life this turn (from any source).
     * Used for cards like Hired Claw: "Activate only if an opponent lost life this turn"
     */
    val OpponentLostLifeThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.LIFE_LOST, player = Player.EachOpponent)

    // =========================================================================
    // Candidate-player target restrictions (CR 115)
    // =========================================================================
    // These read [Player.Candidate] — "the player being considered as a target" — so they only
    // belong inside `TargetPlayer.restriction` / `TargetOpponent.restriction`. The engine binds
    // each candidate player to `Player.Candidate` while enumerating and (per CR 608.2b)
    // re-validating targets. Used in a normal resolution/projection condition slot, the candidate
    // is unbound and the condition is false.

    /**
     * Candidate-target restriction: the player being targeted lost life this turn.
     * Backs "target player who lost life this turn" (Rix Maadi Guildmage).
     */
    fun candidateLostLifeThisTurn(): ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.LIFE_LOST, player = Player.Candidate)

    /**
     * Candidate-target restriction: the player being targeted has [n] or less life.
     * Backs "target player with N or less life". The restriction is re-checked at resolution
     * (CR 608.2b), so a player who gains above the threshold after being targeted is removed.
     */
    fun candidateLifeAtMost(n: Int): ConditionInterface =
        Compare(
            DynamicAmount.LifeTotal(Player.Candidate),
            ComparisonOperator.LTE,
            DynamicAmount.Fixed(n)
        )

    /**
     * If N or more cards left your graveyard this turn.
     */
    fun CardsLeftGraveyardThisTurn(count: Int): ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.CARDS_LEFT_GRAVEYARD, atLeast = count)

    /**
     * If you've sacrificed a Food this turn.
     */
    val SacrificedFoodThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.FOOD_SACRIFICED)

    /**
     * If you descended this turn (CR 700.11) — i.e. at least one nontoken permanent
     * card was put into your graveyard from any zone this turn. Tokens do not count;
     * non-permanent cards (instants, sorceries) do not count.
     *
     * Pass [atLeast] > 1 for the descend N / fathomless descent ability words
     * ("if you descended four or more times this turn").
     *
     * Used by the descend gate on cards like Ruin-Lurker Bat ("At the beginning of
     * your end step, if you descended this turn, scry 1").
     */
    fun YouDescendedThisTurn(atLeast: Int = 1): ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.DESCENDED, atLeast = atLeast)

    /**
     * If a permanent of the given card type entered the battlefield under the given player's
     * control this turn. The permanent need not still be on the battlefield, still be of that
     * type, or still be under that player's control — only the entry event matters.
     *
     * Used for Mechan Shieldmate (EOE): "As long as an artifact entered the battlefield under
     * your control this turn ..."
     */
    fun PermanentTypeEnteredBattlefieldThisTurn(
        cardType: CardType,
        player: Player = Player.You
    ): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.PermanentTypeEnteredBattlefieldThisTurn(cardType, player)

    /**
     * Shortcut: if an artifact entered the battlefield under your control this turn.
     */
    val ArtifactEnteredBattlefieldThisTurn: ConditionInterface =
        PermanentTypeEnteredBattlefieldThisTurn(CardType.ARTIFACT)

    /**
     * If you put a counter on a creature this turn.
     * Used for Lasting Tarfire.
     */
    val PutCounterOnCreatureThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.COUNTERS_PUT_ON_CREATURE)

    /**
     * Intervening-if: "if a creature died this turn" (global — any controller).
     * Used for cards like Scorpion, Seething Striker.
     */
    val CreatureDiedThisTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition

    /**
     * Intervening-if: "if a creature died under your control this turn" (scoped to the
     * source's controller). Used for Barrensteppe Siege (Mardu).
     */
    val ControlledCreatureDiedThisTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.ControlledCreatureDiedThisTurnCondition

    /**
     * Intervening-if: "if a permanent you controlled left the battlefield this turn".
     * Per-player (scoped to the source's controller), counts every permanent type
     * including lands and tokens — broader than [CreatureDiedThisTurn]/[ControlledCreatureDiedThisTurn].
     * Used by Shortcut to Mushrooms (LTR).
     */
    val YouHadPermanentLeaveBattlefieldThisTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.PermanentLeftBattlefieldThisTurn(
            com.wingedsheep.sdk.scripting.references.Player.You
        )

    /**
     * If this is the Nth time this ability has resolved this turn.
     * Used for cards like Harvestrite Host.
     */
    fun SourceAbilityResolvedNTimes(count: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SourceAbilityResolvedNTimesThisTurn(count)

    /**
     * Internal: gate on the plot mechanic's may-cast permission. True when the source
     * card is currently plotted and was plotted on a prior turn (CR 718.2). Cards
     * never reference this directly — the engine's plot handler wires it up.
     */
    val SourcePlottedOnPriorTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SourcePlottedOnPriorTurn

    /**
     * If it's your turn.
     */
    val IsYourTurn: ConditionInterface =
        IsYourTurnCondition

    /**
     * If it's not your turn.
     */
    val IsNotYourTurn: ConditionInterface =
        IsNotYourTurnCondition

    /**
     * If the current phase matches any of the listed phases.
     * When `yoursOnly = true` (default), also requires that it's the controller's turn.
     */
    fun IsInPhase(vararg phases: Phase, yoursOnly: Boolean = true): ConditionInterface =
        IsInPhaseCondition(phases.toList(), yoursOnly)

    /**
     * If it's your main phase (either precombat or postcombat main, on your turn).
     * Used for cards like Dose of Dawnglow.
     */
    val IsYourMainPhase: ConditionInterface =
        IsInPhaseCondition(listOf(Phase.PRECOMBAT_MAIN, Phase.POSTCOMBAT_MAIN), yoursOnly = true)

    /**
     * If you have the city's blessing (CR 702.131 / 700.5).
     *
     * Granted by Ascend triggers once the controller controls 10+ permanents on
     * ETB; once granted, never lost for the rest of the game.
     */
    val YouHaveCitysBlessing: ConditionInterface =
        PlayerHasCitysBlessing(Player.You)

    // =========================================================================
    // Trigger Entity Conditions
    // =========================================================================

    /**
     * If the triggering entity was historic (legendary, artifact, or Saga).
     * Used for Curator's Ward's "if it was historic" intervening-if condition.
     */
    val TriggeringEntityWasHistoric: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWasHistoric

    /**
     * If you cast the triggering entity (the entering permanent), as opposed to it being put
     * onto the battlefield by another effect. Sibling of [WasCast] for triggers whose source is
     * a separate permanent (e.g. "whenever a creature you control enters, if you cast it" on
     * The Sibsig Ceremony).
     */
    val TriggeringEntityWasCast: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWasCast

    /**
     * If the triggering entity entered or was cast from a graveyard.
     * Used by Twilight Diviner: "if they entered or were cast from a graveyard".
     */
    val TriggeringEntityEnteredOrWasCastFromGraveyard: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityEnteredOrWasCastFromGraveyard

    /**
     * If the triggering entity had a -1/-1 counter on it when it left the battlefield.
     * Used as an intervening-if condition on dies/leaves triggers (e.g., Retched Wretch).
     */
    val TriggeringEntityHadMinusOneMinusOneCounter: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityHadMinusOneMinusOneCounter

    /**
     * If the triggering entity had at least one counter of any kind on it when it left
     * the battlefield. Used as an intervening-if condition on dies/leaves triggers, e.g.
     * Host of the Hereafter: "Whenever this creature or another creature you control dies,
     * if it had counters on it, ...".
     */
    val TriggeringEntityHadCounters: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityHadCounters

    /**
     * If the triggering entity was NOT put onto the battlefield by this source's ability.
     * Used to break ETB-trigger loops on cards like Kodama of the East Tree:
     * "if it wasn't put onto the battlefield with this ability". Pair with
     * `MoveCollectionEffect.markEnteredViaSourceAbility = true` on the move that
     * tags the entering permanent.
     */
    val TriggeringEntityWasNotPutByThisSource: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWasNotPutByThisSource

    /**
     * If the triggering spell or ability has exactly one target.
     * Reads the triggering entity's TargetsComponent (counts unique chosen targets).
     * Used by cards like Spinerock Tyrant.
     */
    val TriggeringSpellHasSingleTarget: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringSpellHasSingleTarget

    /**
     * If the spell that triggered this ability matches [filter].
     * General intervening-if guard for "whenever you cast a spell, if it's a/an X ..." cards.
     */
    fun TriggeringSpellMatches(filter: com.wingedsheep.sdk.scripting.GameObjectFilter): ConditionInterface =
        EntityMatches(EffectTarget.TriggeringEntity, filter)

    /**
     * If the spell that triggered this ability is the first spell matching [filter] you've cast
     * this turn. True iff the triggering spell matches [filter] and no second matching spell has
     * been cast yet. Composed from [TriggeringSpellMatches] + the [YouCastSpellsThisTurn] count
     * primitive (no bespoke counting logic). Used by Alania, Divergent Storm.
     */
    fun YouCastFirstSpellOfTypeThisTurn(
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter
    ): ConditionInterface =
        All(
            TriggeringSpellMatches(filter),
            Not(YouCastSpellsThisTurn(atLeast = 2, filter = filter))
        )

    // =========================================================================
    // Collection Conditions (pipeline-based)
    // =========================================================================

    /**
     * If a card in the named pipeline collection matches the given filter.
     * Used for "if you did X this way" patterns (e.g., "if you returned a Squirrel card").
     */
    fun CollectionContainsMatch(collection: String, filter: GameObjectFilter = GameObjectFilter.Any): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch(collection, filter)

    // =========================================================================
    // Composite Conditions
    // =========================================================================

    /**
     * All conditions must be true (AND).
     */
    fun All(vararg conditions: ConditionInterface): ConditionInterface =
        AllConditions(conditions.toList())

    /**
     * Any condition must be true (OR).
     */
    fun Any(vararg conditions: ConditionInterface): ConditionInterface =
        AnyCondition(conditions.toList())

    /**
     * Condition must NOT be true.
     */
    fun Not(condition: ConditionInterface): ConditionInterface =
        NotCondition(condition)
}
