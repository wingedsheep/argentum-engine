package com.wingedsheep.sdk.dsl

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
import com.wingedsheep.sdk.scripting.conditions.WasCastFromHand as WasCastFromHandCondition
import com.wingedsheep.sdk.scripting.conditions.WasCastFromZone as WasCastFromZoneCondition
import com.wingedsheep.sdk.scripting.conditions.WasKicked as WasKickedCondition
import com.wingedsheep.sdk.scripting.conditions.BlightWasPaid as BlightWasPaidCondition
import com.wingedsheep.sdk.scripting.conditions.SourceIsAttacking as SourceIsAttackingCondition
import com.wingedsheep.sdk.scripting.conditions.SourceIsBlocking as SourceIsBlockingCondition
import com.wingedsheep.sdk.scripting.conditions.SourceIsTapped as SourceIsTappedCondition
import com.wingedsheep.sdk.scripting.conditions.SourceIsUntapped as SourceIsUntappedCondition
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn as IsYourTurnCondition
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn as IsNotYourTurnCondition
import com.wingedsheep.sdk.scripting.conditions.IsInPhase as IsInPhaseCondition
import com.wingedsheep.sdk.scripting.references.Player
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
    // Battlefield Conditions (via Exists / Compare)
    // =========================================================================

    /**
     * If an opponent controls more lands than you.
     */
    val OpponentControlsMoreLands: ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.Opponent, GameObjectFilter.Land),
            ComparisonOperator.GT,
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land)
        )

    /**
     * If an opponent controls more creatures than you.
     */
    val OpponentControlsMoreCreatures: ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.Opponent, GameObjectFilter.Creature),
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
            DynamicAmount.AggregateBattlefield(Player.Opponent, GameObjectFilter.Creature)
        )

    /**
     * If opponent controls a land of a specific subtype.
     * Used for CantAttackUnless (e.g. Deep-Sea Serpent, Slipstream Eel).
     */
    fun OpponentControlsLandType(landType: String): ConditionInterface =
        Exists(Player.Opponent, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype(landType))

    /**
     * If an opponent controls a creature.
     */
    val OpponentControlsCreature: ConditionInterface =
        Exists(Player.Opponent, Zone.BATTLEFIELD, GameObjectFilter.Creature)

    /**
     * If you control a creature.
     */
    val ControlCreature: ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature)

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
     * If you control N or more creatures.
     */
    fun ControlCreaturesAtLeast(count: Int): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature),
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
     * If the target matches a GameObjectFilter.
     * Used for cards like Blessing of Belzenlok: "If it's legendary, it also gains lifelink."
     */
    fun TargetMatchesFilter(filter: GameObjectFilter, targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetMatchesFilter(filter, targetIndex)

    /**
     * If the creature enchanted by the source Aura is legendary.
     */
    fun EnchantedCreatureIsLegendary(): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureIsLegendary

    // =========================================================================
    // Life Total Conditions (via Compare)
    // =========================================================================

    /**
     * If your life total is N or less.
     */
    fun LifeAtMost(threshold: Int): ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.LTE, DynamicAmount.Fixed(threshold))

    /**
     * If your life total is N or more.
     */
    fun LifeAtLeast(threshold: Int): ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.GTE, DynamicAmount.Fixed(threshold))

    /**
     * If you have more life than an opponent.
     */
    val MoreLifeThanOpponent: ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.GT, DynamicAmount.LifeTotal(Player.Opponent))

    /**
     * If you have less life than an opponent.
     */
    val LessLifeThanOpponent: ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.LT, DynamicAmount.LifeTotal(Player.Opponent))

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
        Compare(DynamicAmount.Count(Player.Opponent, Zone.HAND), ComparisonOperator.LTE, DynamicAmount.Fixed(count))

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
     * If this spell's blight additional cost was paid (`AdditionalCost.BlightOrPay`).
     * Used for cards like Cinder Strike whose effect changes when the optional
     * Blight path was chosen during casting.
     */
    val BlightWasPaid: ConditionInterface =
        BlightWasPaidCondition

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
     * If this creature is attacking.
     */
    val SourceIsAttacking: ConditionInterface =
        SourceIsAttackingCondition

    /**
     * If this creature is blocking.
     */
    val SourceIsBlocking: ConditionInterface =
        SourceIsBlockingCondition

    /**
     * If this permanent is tapped.
     */
    val SourceIsTapped: ConditionInterface =
        SourceIsTappedCondition

    /**
     * If this permanent is untapped.
     */
    val SourceIsUntapped: ConditionInterface =
        SourceIsUntappedCondition

    /**
     * As long as this creature is a specific subtype.
     * Used for conditional static abilities like "has defender as long as it's a Wall."
     */
    fun SourceHasSubtype(subtype: Subtype): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SourceHasSubtype(subtype)

    /**
     * As long as this creature has a specific keyword.
     * Used for conditional effects like "If this creature has flying, it gets +1/+1."
     */
    fun SourceHasKeyword(keyword: Keyword): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SourceHasKeyword(keyword)

    /**
     * While this creature has a counter of the given type on it.
     * Used for intervening-if triggers like Moonshadow.
     */
    fun SourceHasCounter(counterType: CounterTypeFilter): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SourceHasCounter(counterType)

    /**
     * If a permanent with the given subtype was sacrificed as part of the cost.
     * Used for cards like Thallid Omnivore: "If a Saproling was sacrificed this way, you gain 2 life."
     */
    fun SacrificedHadSubtype(subtype: String): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SacrificedPermanentHadSubtype(subtype)

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
        com.wingedsheep.sdk.scripting.conditions.YouAttackedWithCreaturesThisTurn(filter, atLeast)

    /**
     * As long as you've cast [atLeast] or more spells matching [filter] this turn.
     * Counts every spell cast — countered, fizzled, or still on the stack all count.
     * Defaults to any spell, matching the typical "you've cast two or more spells
     * this turn" pattern (Brightspear Zealot, Illvoi Infiltrator).
     */
    fun YouCastSpellsThisTurn(
        atLeast: Int,
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter = com.wingedsheep.sdk.scripting.GameObjectFilter.Any
    ): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.YouCastSpellsThisTurn(filter, atLeast)

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
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.LIFE_LOST, player = Player.Opponent)

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
     * If this is the Nth time this ability has resolved this turn.
     * Used for cards like Harvestrite Host.
     */
    fun SourceAbilityResolvedNTimes(count: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SourceAbilityResolvedNTimesThisTurn(count)

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
