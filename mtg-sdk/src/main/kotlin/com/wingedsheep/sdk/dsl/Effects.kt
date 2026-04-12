package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfColorAmongEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.AddCardTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddSubtypeEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AnimateLandEffect
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.SetBasePowerEffect

import com.wingedsheep.sdk.scripting.effects.ChooseColorAndGrantProtectionToGroupEffect
import com.wingedsheep.sdk.scripting.effects.ChooseColorAndGrantProtectionToTargetEffect
import com.wingedsheep.sdk.scripting.effects.CantAttackGroupEffect
import com.wingedsheep.sdk.scripting.effects.CantAttackOrBlockTargetEffect
import com.wingedsheep.sdk.scripting.effects.CantBlockGroupEffect
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GrantDamageBonusEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.EachPlayerReturnsPermanentToHandEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.GrantFreeCastTargetFromExileEffect
import com.wingedsheep.sdk.scripting.effects.FightEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.ForceReturnOwnPermanentEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.effects.ExchangeControlEffect
import com.wingedsheep.sdk.scripting.effects.ExchangeLifeAndPowerEffect
import com.wingedsheep.sdk.scripting.effects.GainControlByMostOfSubtypeEffect
import com.wingedsheep.sdk.scripting.effects.GiftGivenEffect
import com.wingedsheep.sdk.scripting.effects.GrantSpellKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.GrantExileOnLeaveEffect
import com.wingedsheep.sdk.scripting.effects.GrantHexproofEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordToAttackersBlockedByEffect
import com.wingedsheep.sdk.scripting.effects.RemoveAllAbilitiesEffect
import com.wingedsheep.sdk.scripting.effects.RemoveKeywordEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.SetLifeTotalEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.PutOnTopOrBottomOfLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ExileFromTopRepeatingEffect
import com.wingedsheep.sdk.scripting.effects.ExileOpponentsGraveyardsEffect
import com.wingedsheep.sdk.scripting.effects.ExileUntilLeavesEffect
import com.wingedsheep.sdk.scripting.effects.CreateGlobalTriggeredAbilityWithDurationEffect
import com.wingedsheep.sdk.scripting.effects.CreatePermanentGlobalTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.ReturnCreaturesPutInGraveyardThisTurnEffect
import com.wingedsheep.sdk.scripting.effects.ReturnOneFromLinkedExileEffect
import com.wingedsheep.sdk.scripting.effects.ReturnSelfToBattlefieldAttachedEffect
import com.wingedsheep.sdk.scripting.effects.DrawUpToEffect
import com.wingedsheep.sdk.scripting.effects.ReadTheRunesEffect
import com.wingedsheep.sdk.scripting.effects.RemoveFromCombatEffect
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.effects.RepeatWhileEffect
import com.wingedsheep.sdk.scripting.effects.ReplaceNextDrawWithEffect
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.effects.SeparatePermanentsIntoPilesEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfEquippedCreatureEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfSourceEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.CounterCondition
import com.wingedsheep.sdk.scripting.effects.CounterDestination
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.effects.CounterTarget
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource
import com.wingedsheep.sdk.scripting.effects.ChangeSpellTargetEffect
import com.wingedsheep.sdk.scripting.effects.ChangeTargetEffect
import com.wingedsheep.sdk.scripting.effects.ReselectTargetRandomlyEffect
import com.wingedsheep.sdk.scripting.effects.CopyEachSpellCastEffect
import com.wingedsheep.sdk.scripting.effects.CopyNextSpellCastEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.AddCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.LoseAllCreatureTypesEffect
import com.wingedsheep.sdk.scripting.effects.SetCreatureSubtypesEffect
import com.wingedsheep.sdk.scripting.effects.DestroyAllEquipmentOnTargetEffect
import com.wingedsheep.sdk.scripting.effects.ForceExileMultiZoneEffect
import com.wingedsheep.sdk.scripting.effects.LoseGameEffect
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.PreventionScope
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.effects.SkipNextTurnEffect
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * Facade object providing convenient factory methods for creating atomic Effects.
 *
 * For composite effect patterns (search, scry, mill, discard, etc.),
 * use [EffectPatterns] directly.
 *
 * Usage:
 * ```kotlin
 * Effects.DealDamage(3, EffectTarget.ContextTarget(0))
 * Effects.DrawCards(2)
 * Effects.GainLife(5)
 * Effects.Composite(effect1, effect2)
 * ```
 */
object Effects {

    // =========================================================================
    // Damage Effects
    // =========================================================================

    /**
     * Deal damage to a target.
     * No default — every damage effect must explicitly declare its target.
     */
    fun DealDamage(amount: Int, target: EffectTarget): Effect =
        DealDamageEffect(amount, target)

    /**
     * Deal dynamic damage to a target.
     * Used for effects like "deal damage equal to the number of lands you control".
     */
    fun DealDamage(amount: DynamicAmount, target: EffectTarget): Effect =
        DealDamageEffect(amount, target)

    /**
     * Deal X damage to a target, where X is the spell's X value.
     * Used for X spells like Blaze.
     */
    fun DealXDamage(target: EffectTarget): Effect =
        DealDamageEffect(DynamicAmount.XValue, target)

    /**
     * Two creatures fight — each deals damage equal to its power to the other.
     */
    fun Fight(target1: EffectTarget, target2: EffectTarget): Effect =
        FightEffect(target1, target2)

    // =========================================================================
    // Life Effects
    // =========================================================================

    /**
     * Gain life. Default target is the controller.
     */
    fun GainLife(amount: Int, target: EffectTarget = EffectTarget.Controller): Effect =
        GainLifeEffect(amount, target)

    /**
     * Gain life with a dynamic amount. Default target is the controller.
     */
    fun GainLife(amount: DynamicAmount, target: EffectTarget = EffectTarget.Controller): Effect =
        GainLifeEffect(amount, target)

    /**
     * Lose life. Default target is target opponent.
     */
    fun LoseLife(amount: Int, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
        LoseLifeEffect(amount, target)

    /**
     * Lose dynamic life amount. Default target is target opponent.
     */
    fun LoseLife(amount: DynamicAmount, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
        LoseLifeEffect(amount, target)

    /**
     * Target player loses the game.
     */
    fun LoseGame(target: EffectTarget = EffectTarget.Controller, message: String? = null): Effect =
        LoseGameEffect(target, message)

    /**
     * Force a player to exile from multiple zones (battlefield, hand, graveyard).
     * Used for Lich's Mastery: "for each 1 life you lost, exile a permanent you control
     * or a card from your hand or graveyard."
     */
    fun ForceExileMultiZone(count: DynamicAmount, target: EffectTarget = EffectTarget.Controller): Effect =
        ForceExileMultiZoneEffect(count, target)

    /**
     * Set a player's life total to a fixed amount.
     * Per Rule 118.5, the player gains or loses the necessary amount of life.
     */
    fun SetLifeTotal(amount: Int, target: EffectTarget = EffectTarget.Controller): Effect =
        SetLifeTotalEffect(amount, target)

    /**
     * Set a player's life total to a dynamic amount.
     */
    fun SetLifeTotal(amount: DynamicAmount, target: EffectTarget = EffectTarget.Controller): Effect =
        SetLifeTotalEffect(amount, target)

    /**
     * Exchange a player's life total with a creature's power.
     * "{4}: Exchange your life total with this creature's power."
     */
    fun ExchangeLifeAndPower(target: EffectTarget = EffectTarget.Self): Effect =
        ExchangeLifeAndPowerEffect(target)

    /**
     * Lose half your life, rounded up.
     * Composes as LoseLifeEffect with Divide(LifeTotal, 2).
     *
     * @param roundUp Whether to round up (true) or down (false)
     * @param target Who loses life (default: Controller)
     * @param lifePlayer Which player's life total to halve (default: Player.You for controller)
     */
    fun LoseHalfLife(
        roundUp: Boolean = true,
        target: EffectTarget = EffectTarget.Controller,
        lifePlayer: Player = Player.You
    ): Effect = LoseLifeEffect(
        amount = DynamicAmount.Divide(
            DynamicAmount.LifeTotal(lifePlayer),
            DynamicAmount.Fixed(2),
            roundUp = roundUp
        ),
        target = target
    )

    // =========================================================================
    // Card Drawing Effects
    // =========================================================================

    /**
     * Draw cards. Default target is the controller.
     */
    fun DrawCards(count: Int, target: EffectTarget = EffectTarget.Controller): Effect =
        DrawCardsEffect(count, target)

    /**
     * Draw a dynamic number of cards. Default target is the controller.
     */
    fun DrawCards(count: DynamicAmount, target: EffectTarget = EffectTarget.Controller): Effect =
        DrawCardsEffect(count, target)

    /**
     * Draw up to N cards. The player chooses how many (0 to maxCards).
     */
    fun DrawUpTo(maxCards: Int, target: EffectTarget = EffectTarget.Controller): Effect =
        DrawUpToEffect(maxCards, target)

    /**
     * Draw X cards, then for each card drawn, discard a card unless you sacrifice a permanent.
     * Used for Read the Runes.
     */
    fun ReadTheRunes(): Effect = ReadTheRunesEffect

    /**
     * Each opponent discards N cards.
     * Delegates to the EffectPatterns pipeline: ForEachPlayer(EachOpponent) → Gather → Select → Move.
     */
    fun EachOpponentDiscards(count: Int = 1): Effect = EffectPatterns.eachOpponentDiscards(count)

    /**
     * Each player returns a permanent they control to its owner's hand.
     * Used as a replacement in Words of Wind:
     * ReplaceNextDraw(Effects.EachPlayerReturnPermanentToHand())
     */
    fun EachPlayerReturnPermanentToHand(): Effect = EachPlayerReturnsPermanentToHandEffect

    /**
     * Replace the next draw this turn with the given effect instead.
     * Used by the "Words of" enchantment cycle.
     */
    fun ReplaceNextDraw(effect: Effect): Effect = ReplaceNextDrawWithEffect(effect)

    // =========================================================================
    // Zone Movement Effects
    // =========================================================================

    /**
     * Destroy a target.
     */
    fun Destroy(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.GRAVEYARD, byDestruction = true)

    /**
     * Destroy all permanents matching a filter using the pipeline.
     * Correctly handles indestructible and regeneration via MoveType.Destroy.
     *
     * @param filter Which permanents to destroy
     * @param noRegenerate If true, destroyed permanents can't be regenerated
     * @param storeDestroyedAs If set, stores actually-destroyed IDs for follow-up effects
     *   (count available as DynamicAmount.VariableReference("{key}_count"))
     */
    fun DestroyAll(
        filter: GameObjectFilter,
        noRegenerate: Boolean = false,
        storeDestroyedAs: String? = null
    ): Effect = EffectPatterns.destroyAllPipeline(filter, noRegenerate, storeDestroyedAs)

    /**
     * Destroy all permanents matching [filter] and all permanents attached to them.
     * Used by End Hostilities-style board wipes.
     */
    fun DestroyAllAndAttached(
        filter: GameObjectFilter,
        noRegenerate: Boolean = false
    ): Effect = EffectPatterns.destroyAllAndAttachedPipeline(filter, noRegenerate)

    /**
     * Destroy all creatures sharing a creature type with the sacrificed creature.
     * Requires a creature sacrificed as additional cost.
     */
    fun DestroyAllSharingTypeWithSacrificed(
        noRegenerate: Boolean = false
    ): Effect = EffectPatterns.destroyAllSharingTypeWithSacrificed(noRegenerate)

    /**
     * Destroy all Equipment attached to the target permanent.
     */
    fun DestroyAllEquipmentOnTarget(target: EffectTarget): Effect =
        DestroyAllEquipmentOnTargetEffect(target)

    /**
     * Exile a target.
     */
    fun Exile(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.EXILE)

    /**
     * Exile all cards in each opponent's graveyard.
     */
    fun ExileOpponentsGraveyards(): Effect = ExileOpponentsGraveyardsEffect

    /**
     * Exile a target until this permanent leaves the battlefield.
     * Exiles the target and links it to the source permanent via LinkedExileComponent.
     * Used with a LeavesBattlefield trigger + ReturnLinkedExile() for the return.
     */
    fun ExileUntilLeaves(target: EffectTarget): Effect =
        ExileUntilLeavesEffect(target)

    /**
     * Exile all permanents matching a filter that the controller controls and link
     * them to the source permanent. Used for Day of the Dragons-style effects.
     * The count is available as DynamicAmount.VariableReference("{storeAs}_count").
     */
    fun ExileGroupAndLink(filter: GroupFilter, storeAs: String = "linked_exile"): Effect =
        EffectPatterns.exileGroupAndLink(filter, storeAs)

    /**
     * Return all cards linked to the source permanent (via LinkedExileComponent)
     * to the battlefield under the controller's control.
     */
    fun ReturnLinkedExile(): Effect = EffectPatterns.returnLinkedExile()

    /**
     * Return all cards linked to the source permanent (via LinkedExileComponent)
     * to the battlefield under their owners' control.
     */
    fun ReturnLinkedExileUnderOwnersControl(): Effect = EffectPatterns.returnLinkedExile(underOwnersControl = true)

    /**
     * Return all cards linked to the source permanent (via LinkedExileComponent)
     * to their owner's hand.
     */
    fun ReturnLinkedExileToHand(): Effect = ExilePatterns.returnLinkedExileToHand()

    /**
     * Return one card from the source's linked exile to the battlefield.
     * The active player chooses one of their owned exiled cards.
     */
    fun ReturnOneFromLinkedExile(): Effect = ReturnOneFromLinkedExileEffect

    /**
     * Return to hand all creature cards in a player's graveyard that were put there this turn.
     * Used by Garna, the Bloodflame and similar effects.
     */
    fun ReturnCreaturesPutInGraveyardThisTurn(player: Player = Player.You): Effect =
        ReturnCreaturesPutInGraveyardThisTurnEffect(player)

    /**
     * Create a global triggered ability that lasts permanently.
     * Used for recurring triggers from non-permanent sources.
     */
    fun CreatePermanentGlobalTriggeredAbility(ability: TriggeredAbility): Effect =
        CreatePermanentGlobalTriggeredAbilityEffect(ability)

    /**
     * Create a global triggered ability with a specified duration.
     * Used for temporary triggered abilities like "Until the end of your next turn, whenever..."
     */
    fun CreateGlobalTriggeredAbilityWithDuration(ability: TriggeredAbility, duration: Duration): Effect =
        CreateGlobalTriggeredAbilityWithDurationEffect(ability, duration)

    /**
     * Return to hand.
     */
    fun ReturnToHand(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.HAND)

    /**
     * Put on top of library.
     */
    fun PutOnTopOfLibrary(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.LIBRARY, ZonePlacement.Top)

    /**
     * Owner chooses to put target on top or bottom of their library.
     */
    fun PutOnTopOrBottomOfLibrary(target: EffectTarget): Effect =
        PutOnTopOrBottomOfLibraryEffect(target)

    /**
     * Shuffle into library.
     */
    fun ShuffleIntoLibrary(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.LIBRARY, ZonePlacement.Shuffled)

    /**
     * Put into owner's library at a specific position from the top.
     * @param positionFromTop 0-indexed: 0 = top, 1 = second, 2 = third from top, etc.
     */
    fun PutIntoLibraryNthFromTop(target: EffectTarget, positionFromTop: Int): Effect =
        MoveToZoneEffect(target, Zone.LIBRARY, positionFromTop = positionFromTop)

    /**
     * Grant "may play from exile" permission to all cards in a named collection.
     * Does NOT waive mana cost — pair with [GrantPlayWithoutPayingCost] for free play.
     */
    fun GrantMayPlayFromExile(from: String, untilEndOfNextTurn: Boolean = false): Effect =
        GrantMayPlayFromExileEffect(from, untilEndOfNextTurn)

    /**
     * Grant "play without paying mana cost" permission to all cards in a named collection.
     * Card must still be in a playable zone (hand, or exile with GrantMayPlayFromExile).
     */
    fun GrantPlayWithoutPayingCost(from: String): Effect = GrantPlayWithoutPayingCostEffect(from)

    /**
     * Grant a single target entity in exile permission to be cast without paying its mana cost.
     * Optionally marks the spell to be exiled instead of going to graveyard after resolution.
     * Used for effects like "you may cast target card without paying its mana cost. If that
     * spell would be put into a graveyard, exile it instead."
     */
    fun GrantFreeCastTargetFromExile(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        exileAfterResolve: Boolean = false
    ): Effect = GrantFreeCastTargetFromExileEffect(target, exileAfterResolve)

    /**
     * Put onto the battlefield.
     */
    fun PutOntoBattlefield(target: EffectTarget, tapped: Boolean = false): Effect =
        MoveToZoneEffect(target, Zone.BATTLEFIELD, if (tapped) ZonePlacement.Tapped else ZonePlacement.Default)

    /**
     * Put onto the battlefield under your control (the effect controller's control).
     */
    fun PutOntoBattlefieldUnderYourControl(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.BATTLEFIELD, controllerOverride = EffectTarget.Controller)

    /**
     * Return this card from its current zone to the battlefield face down under your control.
     * Used by Ashcloud Phoenix and similar cards that return as morph creatures.
     * Only moves if the card is currently in [fromZone] (defaults to GRAVEYARD).
     */
    fun PutOntoBattlefieldFaceDown(
        target: EffectTarget = EffectTarget.Self,
        controllerOverride: EffectTarget = EffectTarget.Controller,
        fromZone: Zone? = Zone.GRAVEYARD
    ): Effect = MoveToZoneEffect(
        target = target,
        destination = Zone.BATTLEFIELD,
        controllerOverride = controllerOverride,
        fromZone = fromZone,
        faceDown = true
    )

    /**
     * Return this card from its current zone to the battlefield attached to the target.
     * Used by the Dragon aura cycle (Dragon Shadow, Dragon Breath, etc.).
     */
    fun ReturnSelfToBattlefieldAttached(target: EffectTarget = EffectTarget.TriggeringEntity): Effect =
        ReturnSelfToBattlefieldAttachedEffect(target)

    /**
     * Take the top card from the source's linked exile pile and put it into your hand.
     * Used by Parallel Thoughts and similar cards.
     */
    fun TakeFromLinkedExile(): Effect = EffectPatterns.takeFromLinkedExile()

    /**
     * Repeatedly exile cards from the top of your library until you exile a card
     * matching [matchFilter], then put that card into your hand. If the card's mana
     * value is at least [repeatIfManaValueAtLeast], repeat the process. Deal
     * [damagePerCard] damage to you for each card put into your hand this way.
     *
     * Used for Demonlord Belzenlok and similar repeating exile-to-hand effects.
     */
    fun ExileFromTopRepeating(
        matchFilter: GameObjectFilter = GameObjectFilter.Nonland,
        repeatIfManaValueAtLeast: Int = 4,
        damagePerCard: Int = 1
    ): Effect = ExileFromTopRepeatingEffect(matchFilter, repeatIfManaValueAtLeast, damagePerCard)

    // =========================================================================
    // Stat Modification Effects
    // =========================================================================

    /**
     * Modify power and toughness.
     */
    fun ModifyStats(power: Int, toughness: Int, target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        ModifyStatsEffect(power, toughness, target)

    /**
     * Modify power and toughness by dynamic amounts.
     * Used for effects like "Target creature gets -X/-X where X is the number of Zombies."
     */
    fun ModifyStats(power: DynamicAmount, toughness: DynamicAmount, target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        ModifyStatsEffect(power, toughness, target)

    /**
     * Grant hexproof to a target (player or permanent) until end of turn.
     * For players: adds PlayerHexproofComponent.
     * For permanents: creates a floating effect granting the Hexproof keyword.
     */
    fun GrantHexproof(target: EffectTarget = EffectTarget.Controller, duration: Duration = Duration.EndOfTurn): Effect =
        GrantHexproofEffect(target, duration)

    /**
     * Grant a keyword until end of turn.
     */
    fun GrantKeyword(keyword: Keyword, target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        GrantKeywordEffect(keyword.name, target)

    /**
     * Grant an ability flag until end of turn.
     */
    fun GrantKeyword(flag: AbilityFlag, target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        GrantKeywordEffect(flag.name, target)

    /**
     * Mark a permanent so that if it would leave the battlefield, it is exiled instead.
     * Used by Kheru Lich Lord, Whip of Erebos, Sneak Attack, etc.
     */
    fun GrantExileOnLeave(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        GrantExileOnLeaveEffect(target)

    /**
     * Grant a keyword to all attacking creatures that were blocked by the target creature.
     * "Creatures that were blocked by that creature this combat gain [keyword] until end of turn."
     */
    fun GrantKeywordToAttackersBlockedBy(
        keyword: Keyword,
        target: EffectTarget = EffectTarget.ContextTarget(0),
        duration: Duration = Duration.EndOfTurn
    ): Effect = GrantKeywordToAttackersBlockedByEffect(target, keyword.name, duration)

    /**
     * Remove a keyword from a single target.
     * "It loses defender."
     */
    fun RemoveKeyword(keyword: Keyword, target: EffectTarget = EffectTarget.ContextTarget(0), duration: Duration = Duration.EndOfTurn): Effect =
        RemoveKeywordEffect(keyword.name, target, duration)

    /**
     * Remove an ability flag from a single target.
     */
    fun RemoveKeyword(flag: AbilityFlag, target: EffectTarget = EffectTarget.ContextTarget(0), duration: Duration = Duration.EndOfTurn): Effect =
        RemoveKeywordEffect(flag.name, target, duration)

    /**
     * Remove all abilities from a target creature until end of turn (or other duration).
     * "Target creature loses all abilities until end of turn."
     */
    fun RemoveAllAbilities(target: EffectTarget = EffectTarget.ContextTarget(0), duration: Duration = Duration.EndOfTurn): Effect =
        RemoveAllAbilitiesEffect(target, duration)

    /**
     * Remove all creature types from a target creature.
     * "Target creature loses all creature types until end of turn."
     */
    fun LoseAllCreatureTypes(target: EffectTarget = EffectTarget.ContextTarget(0), duration: Duration = Duration.EndOfTurn): Effect =
        LoseAllCreatureTypesEffect(target, duration)

    /**
     * Set creature subtypes for a single target.
     * "It becomes a Bird Giant."
     */
    fun SetCreatureSubtypes(subtypes: Set<String>, target: EffectTarget = EffectTarget.Self, duration: Duration = Duration.Permanent): Effect =
        SetCreatureSubtypesEffect(subtypes, target, duration)

    /**
     * Add a creature subtype in addition to other types.
     * "It becomes a Zombie in addition to its other types."
     */
    fun AddCreatureType(subtype: String, target: EffectTarget = EffectTarget.Self, duration: Duration = Duration.Permanent): Effect =
        AddCreatureTypeEffect(subtype, target, duration)

    /**
     * Add counters.
     */
    fun AddCounters(counterType: String, count: Int, target: EffectTarget): Effect =
        AddCountersEffect(counterType, count, target)

    /**
     * Add a dynamic number of counters.
     */
    fun AddDynamicCounters(counterType: String, amount: DynamicAmount, target: EffectTarget): Effect =
        AddDynamicCountersEffect(counterType, amount, target)

    /**
     * Add counters to all entities in a named collection.
     */
    fun AddCountersToCollection(collectionName: String, counterType: String, count: Int = 1): Effect =
        AddCountersToCollectionEffect(collectionName, counterType, count)

    /**
     * Distribute any number of counters from this creature onto other creatures.
     * Used for Forgotten Ancient's upkeep ability.
     */
    fun DistributeCountersFromSelf(counterType: String = Counters.PLUS_ONE_PLUS_ONE): Effect =
        com.wingedsheep.sdk.scripting.effects.DistributeCountersFromSelfEffect(counterType)

    /**
     * Distribute counters among targets from context.
     * "Distribute N counters among one or more target creatures."
     */
    fun DistributeCountersAmongTargets(totalCounters: Int, counterType: String = Counters.PLUS_ONE_PLUS_ONE, minPerTarget: Int = 1): Effect =
        com.wingedsheep.sdk.scripting.effects.DistributeCountersAmongTargetsEffect(totalCounters, counterType, minPerTarget)

    /**
     * Add a card type to a target permanent.
     * "That creature becomes an artifact in addition to its other types."
     */
    fun AddCardType(cardType: String, target: EffectTarget, duration: Duration = Duration.Permanent): Effect =
        AddCardTypeEffect(cardType, target, duration)

    /**
     * Add a subtype to a target permanent (any type — creature, land, artifact, etc.).
     * "Target land becomes a Forest in addition to its other types."
     */
    fun AddSubtype(subtype: String, target: EffectTarget, duration: Duration = Duration.EndOfTurn): Effect =
        AddSubtypeEffect(subtype, target, duration)

    /**
     * Set a creature's base power to a dynamic value.
     * "Change this creature's base power to target creature's power."
     */
    fun SetBasePower(
        target: EffectTarget = EffectTarget.Self,
        power: DynamicAmount,
        duration: Duration = Duration.Permanent
    ): Effect = SetBasePowerEffect(target, power, duration)

    // =========================================================================
    // Mana Effects
    // =========================================================================

    /**
     * Add mana of a specific color.
     */
    fun AddMana(color: Color, amount: Int = 1, restriction: ManaRestriction? = null): Effect =
        AddManaEffect(color, amount, restriction)

    /**
     * Add a dynamic amount of mana of a specific color.
     * Used for effects like "Add {R} for each Goblin on the battlefield."
     */
    fun AddMana(color: Color, amount: DynamicAmount, restriction: ManaRestriction? = null): Effect =
        AddManaEffect(color, amount, restriction)

    /**
     * Add colorless mana.
     */
    fun AddColorlessMana(amount: Int, restriction: ManaRestriction? = null): Effect =
        AddColorlessManaEffect(amount, restriction)

    /**
     * Add a dynamic amount of colorless mana.
     */
    fun AddColorlessMana(amount: DynamicAmount, restriction: ManaRestriction? = null): Effect =
        AddColorlessManaEffect(amount, restriction)

    /**
     * Add one mana of any color.
     */
    fun AddAnyColorMana(amount: Int = 1, restriction: ManaRestriction? = null): Effect =
        AddAnyColorManaEffect(amount, restriction)

    /**
     * Add a dynamic amount of mana of any one color.
     * "Add X mana of any one color, where X is..."
     */
    fun AddAnyColorMana(amount: DynamicAmount, restriction: ManaRestriction? = null): Effect =
        AddAnyColorManaEffect(amount, restriction)

    /**
     * Add X mana in any combination of the allowed colors.
     * "Add that much mana in any combination of {R} and/or {G}."
     */
    fun AddDynamicMana(amount: DynamicAmount, allowedColors: Set<Color>, restriction: ManaRestriction? = null): Effect =
        AddDynamicManaEffect(amount, allowedColors, restriction)

    /**
     * Add one mana of the color chosen when this permanent entered the battlefield.
     * Used for cards like Uncharted Haven.
     */
    fun AddManaOfChosenColor(amount: Int = 1, restriction: ManaRestriction? = null): Effect =
        AddManaOfChosenColorEffect(amount, restriction)

    /**
     * Add one mana of any color among permanents matching a filter that you control.
     * Used for cards like Mox Amber.
     */
    fun AddManaOfColorAmong(filter: GameObjectFilter, restriction: ManaRestriction? = null): Effect =
        AddManaOfColorAmongEffect(filter, restriction)

    // =========================================================================
    // Token Effects
    // =========================================================================

    /**
     * Create creature tokens.
     * @param controller Who receives the token. Null = spell controller.
     *   Use [EffectTarget.TargetController] to give tokens to the target's controller.
     */
    fun CreateToken(
        power: Int,
        toughness: Int,
        colors: Set<Color> = emptySet(),
        creatureTypes: Set<String>,
        keywords: Set<Keyword> = emptySet(),
        count: Int = 1,
        controller: EffectTarget? = null,
        imageUri: String? = null,
        legendary: Boolean = false
    ): Effect = CreateTokenEffect(count, power, toughness, colors, creatureTypes, keywords, controller = controller, imageUri = imageUri, legendary = legendary)

    /**
     * Create creature tokens with dynamic power/toughness evaluated at resolution time.
     * Used for cards like Kin-Tree Invocation where P/T depends on game state.
     */
    fun CreateDynamicToken(
        dynamicPower: DynamicAmount,
        dynamicToughness: DynamicAmount,
        colors: Set<Color> = emptySet(),
        creatureTypes: Set<String>,
        keywords: Set<Keyword> = emptySet(),
        count: Int = 1,
        controller: EffectTarget? = null
    ): Effect = CreateTokenEffect(
        count = DynamicAmount.Fixed(count),
        power = 0, toughness = 0,
        colors = colors, creatureTypes = creatureTypes, keywords = keywords,
        controller = controller,
        dynamicPower = dynamicPower, dynamicToughness = dynamicToughness
    )

    /**
     * Create a token that's a copy of the source permanent.
     * "Create a token that's a copy of this creature."
     */
    fun CreateTokenCopyOfSelf(
        count: Int = 1,
        overridePower: Int? = null,
        overrideToughness: Int? = null
    ): Effect =
        CreateTokenCopyOfSourceEffect(count, overridePower, overrideToughness)

    /**
     * Create a token that's a copy of a targeted permanent.
     * "Create a token that's a copy of target creature, except it's 1/1."
     */
    fun CreateTokenCopyOfTarget(
        target: EffectTarget,
        count: Int = 1,
        overridePower: Int? = null,
        overrideToughness: Int? = null
    ): Effect = CreateTokenCopyOfTargetEffect(target, DynamicAmount.Fixed(count), overridePower, overrideToughness)

    /**
     * Create a token that's a copy of the equipped creature.
     * Used for equipment like Helm of the Host.
     */
    fun CreateTokenCopyOfEquippedCreature(
        removeLegendary: Boolean = false,
        grantHaste: Boolean = false
    ): Effect = CreateTokenCopyOfEquippedCreatureEffect(removeLegendary, grantHaste)

    /**
     * Create Treasure tokens.
     * "{T}, Sacrifice this artifact: Add one mana of any color."
     *
     * @param count Number of tokens to create
     * @param tapped Whether the tokens enter the battlefield tapped
     */
    fun CreateTreasure(count: Int = 1, tapped: Boolean = false): Effect =
        CreatePredefinedTokenEffect("Treasure", count, tapped = tapped)

    /**
     * Create Food artifact tokens.
     * "{2}, {T}, Sacrifice this artifact: You gain 3 life."
     *
     * @param count Number of tokens to create
     * @param controller Who controls the tokens (null = spell controller)
     */
    fun CreateFood(count: Int = 1, controller: EffectTarget? = null): Effect =
        CreatePredefinedTokenEffect("Food", count, controller)

    /**
     * Create Lander artifact tokens.
     * "{2}, {T}, Sacrifice this token: Search your library for a basic land card,
     * put it onto the battlefield tapped, then shuffle."
     *
     * @param count Number of tokens to create
     * @param controller Who controls the tokens (null = spell controller)
     */
    fun CreateLander(count: Int = 1, controller: EffectTarget? = null): Effect =
        CreatePredefinedTokenEffect("Lander", count, controller)

    // =========================================================================
    // Protection Effects
    // =========================================================================

    /**
     * Choose a color and grant protection from that color to a group of creatures.
     * "Choose a color. Creatures you control gain protection from the chosen color until end of turn."
     */
    fun ChooseColorAndGrantProtection(
        filter: GroupFilter = GroupFilter(GameObjectFilter.Creature.youControl()),
        duration: Duration = Duration.EndOfTurn
    ): Effect = ChooseColorAndGrantProtectionToGroupEffect(filter, duration)

    /**
     * Choose a color and grant protection from that color to a single target.
     * "{W}: This creature gains protection from the color of your choice until end of turn."
     */
    fun ChooseColorAndGrantProtectionToTarget(
        target: EffectTarget = EffectTarget.Self,
        duration: Duration = Duration.EndOfTurn
    ): Effect = ChooseColorAndGrantProtectionToTargetEffect(target, duration)

    // =========================================================================
    // Control Effects
    // =========================================================================

    /**
     * Gain control of target permanent.
     */
    fun GainControl(target: EffectTarget, duration: Duration = Duration.Permanent): Effect =
        GainControlEffect(target, duration)

    /**
     * Exchange control of two target creatures.
     */
    fun ExchangeControl(
        target1: EffectTarget = EffectTarget.ContextTarget(0),
        target2: EffectTarget = EffectTarget.ContextTarget(1)
    ): Effect = ExchangeControlEffect(target1, target2)

    /**
     * The player who controls the most creatures of the given subtype gains control of the target.
     */
    fun GainControlByMostOfSubtype(subtype: Subtype, target: EffectTarget = EffectTarget.Self): Effect =
        GainControlByMostOfSubtypeEffect(subtype, target)

    /**
     * Choose a creature type. If you control more creatures of that type than each
     * other player, gain control of all creatures of that type.
     */
    fun ChooseCreatureTypeGainControl(duration: Duration = Duration.Permanent): Effect =
        EffectPatterns.chooseCreatureTypeGainControl(duration)

    // =========================================================================
    // Composite Effects
    // =========================================================================

    /**
     * Combine multiple effects.
     */
    fun Composite(vararg effects: Effect): Effect =
        CompositeEffect(effects.toList())

    /**
     * Combine multiple effects from a list.
     */
    fun Composite(effects: List<Effect>): Effect =
        CompositeEffect(effects)

    /**
     * Repeat a body effect in a do-while loop controlled by a repeat condition.
     */
    fun RepeatWhile(body: Effect, repeatCondition: RepeatCondition): Effect =
        RepeatWhileEffect(body, repeatCondition)

    /**
     * Present a player with labeled options and execute the chosen effect.
     *
     * Infeasible options (per [FeasibilityCheck]) are filtered out at execution time.
     * If only one option remains, it is auto-selected. If zero remain, nothing happens.
     */
    fun ChooseAction(
        choices: List<EffectChoice>,
        player: EffectTarget = EffectTarget.Controller
    ): Effect = ChooseActionEffect(choices, player)

    // =========================================================================
    // Counter Effects
    // =========================================================================

    /**
     * Counter target spell.
     */
    fun CounterSpell(): Effect =
        CounterEffect()

    /**
     * Counter target spell. If countered, exile it instead of putting it into
     * its owner's graveyard. Optionally grants free cast from exile.
     * Used by Kheru Spellsnatcher, Spelljack.
     */
    fun CounterSpellToExile(grantFreeCast: Boolean = false): Effect =
        CounterEffect(counterDestination = CounterDestination.Exile(grantFreeCast))

    /**
     * Counter the spell that triggered this ability (non-targeted).
     * "Counter that spell."
     */
    fun CounterTriggeringSpell(): Effect =
        CounterEffect(targetSource = CounterTargetSource.TriggeringEntity)

    /**
     * Counter target spell unless its controller pays a mana cost.
     * "Counter target spell unless its controller pays {cost}."
     */
    fun CounterUnlessPays(cost: String): Effect =
        CounterEffect(condition = CounterCondition.UnlessPaysMana(ManaCost.parse(cost)))

    /**
     * Counter target spell unless its controller pays a dynamic generic mana cost.
     * "Counter target spell unless its controller pays {2} for each Wizard on the battlefield."
     */
    fun CounterUnlessDynamicPays(amount: DynamicAmount, exileOnCounter: Boolean = false): Effect =
        CounterEffect(
            condition = CounterCondition.UnlessPaysDynamic(amount),
            counterDestination = if (exileOnCounter) CounterDestination.Exile() else CounterDestination.Graveyard
        )

    /**
     * Counter target activated or triggered ability.
     * "Counter target activated or triggered ability."
     */
    fun CounterAbility(): Effect =
        CounterEffect(target = CounterTarget.Ability)

    /**
     * Change the target of a spell to another creature.
     */
    fun ChangeSpellTarget(targetMustBeSource: Boolean = false): Effect =
        ChangeSpellTargetEffect(targetMustBeSource)

    /**
     * Change the target of target spell or ability with a single target.
     */
    fun ChangeTarget(): Effect =
        ChangeTargetEffect

    /**
     * Reselect the target of the triggering spell or ability at random.
     */
    fun ReselectTargetRandomly(): Effect =
        ReselectTargetRandomlyEffect

    /**
     * Copy target instant or sorcery spell. You may choose new targets for the copy.
     */
    fun CopyTargetSpell(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        CopyTargetSpellEffect(target)

    /**
     * When you next cast an instant or sorcery spell this turn, copy that spell.
     * You may choose new targets for the copies.
     */
    fun CopyNextSpellCast(copies: Int = 1): Effect =
        CopyNextSpellCastEffect(copies)

    /**
     * Until end of turn, whenever you cast an instant or sorcery spell, copy it.
     * You may choose new targets for the copies.
     */
    fun CopyEachSpellCast(copies: Int = 1): Effect =
        CopyEachSpellCastEffect(copies)

    // =========================================================================
    // Sacrifice Effects
    // =========================================================================

    /**
     * Force a player to sacrifice permanents matching a filter.
     */
    fun Sacrifice(filter: GameObjectFilter, count: Int = 1, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
        ForceSacrificeEffect(filter, count, target)

    /**
     * Sacrifice a specific permanent identified by target.
     * Used in delayed triggers where the exact permanent was determined at resolution time.
     */
    fun SacrificeTarget(target: EffectTarget): Effect =
        SacrificeTargetEffect(target)

    /**
     * Force return own permanent to hand. Controller selects a permanent they control
     * matching the filter and returns it to hand.
     */
    fun ForceReturnOwnPermanent(filter: GameObjectFilter, excludeSource: Boolean = false): Effect =
        ForceReturnOwnPermanentEffect(filter, excludeSource)

    // =========================================================================
    // Tap/Untap Effects
    // =========================================================================

    /**
     * Tap a target.
     */
    fun Tap(target: EffectTarget): Effect =
        TapUntapEffect(target, tap = true)

    /**
     * Untap a target.
     */
    fun Untap(target: EffectTarget): Effect =
        TapUntapEffect(target, tap = false)

    // =========================================================================
    // Group Effects (atomic effect classes)
    // =========================================================================

    /**
     * All creatures matching a filter can't attack this turn.
     */
    fun CantAttackGroup(filter: GroupFilter, duration: Duration = Duration.EndOfTurn): Effect =
        CantAttackGroupEffect(filter, duration)

    /**
     * All creatures matching a filter can't block this turn.
     */
    fun CantBlockGroup(filter: GroupFilter, duration: Duration = Duration.EndOfTurn): Effect =
        CantBlockGroupEffect(filter, duration)

    /**
     * Target creature can't attack or block this turn.
     */
    fun CantAttackOrBlock(target: EffectTarget = EffectTarget.ContextTarget(0), duration: Duration = Duration.EndOfTurn): Effect =
        CantAttackOrBlockTargetEffect(target, duration)

    // =========================================================================
    // Special Effects
    // =========================================================================

    /**
     * Separate permanents into piles (Liliana ultimate style).
     */
    fun SeparatePermanentsIntoPiles(target: EffectTarget = EffectTarget.PlayerRef(Player.TargetPlayer)): Effect =
        SeparatePermanentsIntoPilesEffect(target)

    // =========================================================================
    // Player Restriction Effects
    // =========================================================================

    /**
     * Target player can't cast spells this turn.
     * Used for cards like Xantid Swarm.
     */
    fun CantCastSpells(target: EffectTarget = EffectTarget.PlayerRef(Player.Opponent), duration: Duration = Duration.EndOfTurn): Effect =
        CantCastSpellsEffect(target, duration)

    /**
     * Target player skips their next turn.
     * Used for cards like Lethal Vapors.
     */
    fun SkipNextTurn(target: EffectTarget = EffectTarget.Controller): Effect =
        SkipNextTurnEffect(target)

    /**
     * Grant a flat damage bonus to a player's sources this turn.
     * Used for cards like The Flame of Keld (Chapter III).
     */
    fun GrantDamageBonus(
        bonusAmount: Int,
        sourceFilter: com.wingedsheep.sdk.scripting.events.SourceFilter = com.wingedsheep.sdk.scripting.events.SourceFilter.Any,
        target: EffectTarget = EffectTarget.Controller,
        duration: Duration = Duration.EndOfTurn
    ): Effect = GrantDamageBonusEffect(bonusAmount, sourceFilter, target, duration)

    // =========================================================================
    // Combat Effects
    // =========================================================================

    /**
     * Provoke: untap target creature and force it to block the source creature if able.
     */
    fun Provoke(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        com.wingedsheep.sdk.scripting.effects.ProvokeEffect(target)

    /**
     * Force a target creature to block the source creature this combat if able.
     * Unlike Provoke, does NOT untap the target.
     */
    fun ForceBlock(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        com.wingedsheep.sdk.scripting.effects.ForceBlockEffect(target)

    // -------------------------------------------------------------------------
    // Damage Prevention (unified via PreventDamageEffect)
    // -------------------------------------------------------------------------

    /**
     * Prevent the next N damage that would be dealt to target this turn.
     */
    fun PreventNextDamage(amount: DynamicAmount, target: EffectTarget): Effect =
        PreventDamageEffect(target = target, amount = amount)

    /**
     * Prevent the next N damage that would be dealt to target this turn.
     */
    fun PreventNextDamage(amount: Int, target: EffectTarget): Effect =
        PreventDamageEffect(target = target, amount = DynamicAmount.Fixed(amount))

    /**
     * Prevent all combat damage that would be dealt this turn.
     */
    fun PreventAllCombatDamage(): Effect =
        PreventDamageEffect(scope = PreventionScope.CombatOnly)

    /**
     * Prevent all combat damage that would be dealt by creatures matching a filter.
     */
    fun PreventCombatDamageFrom(source: com.wingedsheep.sdk.scripting.filters.unified.GroupFilter, duration: Duration = Duration.EndOfTurn): Effect =
        PreventDamageEffect(
            scope = PreventionScope.CombatOnly,
            direction = PreventionDirection.FromTarget,
            sourceFilter = PreventionSourceFilter.FromGroup(source),
            duration = duration
        )

    /**
     * Prevent all damage that would be dealt to controller this turn by attacking creatures.
     */
    fun PreventDamageFromAttackingCreatures(): Effect =
        PreventDamageEffect(
            target = EffectTarget.Controller,
            sourceFilter = PreventionSourceFilter.AttackingCreatures
        )

    /**
     * Prevent all combat damage that would be dealt to and dealt by a creature this turn.
     */
    fun PreventCombatDamageToAndBy(target: EffectTarget = EffectTarget.Self): Effect =
        PreventDamageEffect(
            target = target,
            scope = PreventionScope.CombatOnly,
            direction = PreventionDirection.Both
        )

    /**
     * Prevent all damage target creature or spell would deal this turn.
     */
    fun PreventAllDamageDealtBy(target: EffectTarget): Effect =
        PreventDamageEffect(
            target = target,
            direction = PreventionDirection.FromTarget
        )

    /**
     * Choose a source, prevent the next damage it would deal to you this turn,
     * and deal that much damage to its controller.
     */
    fun DeflectNextDamageFromChosenSource(): Effect =
        PreventDamageEffect(
            sourceFilter = PreventionSourceFilter.ChosenSource,
            reflect = true
        )

    /**
     * Prevent the next N damage that would be dealt to a target this turn by a source of your choice.
     */
    fun PreventNextDamageFromChosenSource(amount: Int, target: EffectTarget): Effect =
        PreventDamageEffect(
            target = target,
            amount = DynamicAmount.Fixed(amount),
            sourceFilter = PreventionSourceFilter.ChosenSource
        )

    /**
     * Prevent the next time a creature of the chosen type would deal damage to you this turn.
     */
    fun PreventNextDamageFromChosenCreatureType(): Effect =
        PreventDamageEffect(
            target = EffectTarget.Controller,
            sourceFilter = PreventionSourceFilter.ChosenCreatureType
        )

    /**
     * Remove a creature from combat.
     */
    fun RemoveFromCombat(target: EffectTarget): Effect =
        RemoveFromCombatEffect(target)

    /**
     * Grant a creature "can't attack or block unless its controller pays {X} for each [creature type]
     * on the battlefield" until end of turn.
     */
    fun GrantAttackBlockTaxPerCreatureType(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        creatureType: String,
        manaCostPer: String,
        duration: Duration = Duration.EndOfTurn
    ): Effect = com.wingedsheep.sdk.scripting.effects.GrantAttackBlockTaxPerCreatureTypeEffect(
        target, creatureType, manaCostPer, duration
    )

    // =========================================================================
    // Equipment Effects
    // =========================================================================

    /**
     * Attach this equipment to a target creature.
     * Detaches from the currently equipped creature first.
     */
    fun AttachEquipment(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect(target)

    /**
     * Attach a targeted Equipment to a targeted creature.
     * Both the Equipment and creature are explicit targets (not the source).
     */
    fun AttachTargetEquipmentToCreature(
        equipmentTarget: EffectTarget = EffectTarget.ContextTarget(0),
        creatureTarget: EffectTarget = EffectTarget.ContextTarget(1)
    ): Effect = com.wingedsheep.sdk.scripting.effects.AttachTargetEquipmentToCreatureEffect(
        equipmentTarget, creatureTarget
    )

    // =========================================================================
    // Animate Effects
    // =========================================================================

    /**
     * Target land becomes an X/Y creature until end of turn. It's still a land.
     */
    fun AnimateLand(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        power: Int = 1,
        toughness: Int = 1,
        duration: Duration = Duration.EndOfTurn
    ): Effect = AnimateLandEffect(target, power, toughness, duration)

    /**
     * Target permanent becomes a creature with specified characteristics.
     * More general than AnimateLand — can remove types, grant keywords, set subtypes, change color.
     */
    fun BecomeCreature(
        target: EffectTarget = EffectTarget.Self,
        power: Int,
        toughness: Int,
        keywords: Set<Keyword> = emptySet(),
        creatureTypes: Set<String> = emptySet(),
        removeTypes: Set<String> = emptySet(),
        colors: Set<String>? = null,
        duration: Duration = Duration.EndOfTurn
    ): Effect = BecomeCreatureEffect(target, power, toughness, keywords, creatureTypes, removeTypes, colors, duration)

    // =========================================================================
    // Pipeline Targeting
    // =========================================================================

    /**
     * Select a target during effect resolution (mid-pipeline).
     * The selected target IDs are stored in a named collection for use by
     * subsequent pipeline effects via [EffectTarget.PipelineTarget].
     */
    fun SelectTarget(requirement: TargetRequirement, storeAs: String = "pipelineTarget"): Effect =
        SelectTargetEffect(requirement, storeAs)

    /**
     * Generic pipeline step: choose an option from a set (creature type, color, etc.)
     * and store the result in EffectContext.chosenValues[storeAs].
     */
    fun ChooseOption(
        optionType: OptionType,
        storeAs: String = "chosenOption",
        prompt: String? = null,
        excludedOptions: List<String> = emptyList()
    ): Effect = ChooseOptionEffect(optionType, storeAs, prompt, excludedOptions)

    /**
     * Choose a creature type. Creatures of the chosen type get +X/+Y until end of turn,
     * optionally gaining a keyword.
     */
    fun ChooseCreatureTypeModifyStats(
        powerModifier: DynamicAmount,
        toughnessModifier: DynamicAmount,
        duration: Duration = Duration.EndOfTurn,
        grantKeyword: Keyword? = null
    ): Effect = EffectPatterns.chooseCreatureTypeModifyStats(powerModifier, toughnessModifier, duration, grantKeyword)

    /**
     * Choose a creature type. Creatures of the chosen type get +X/+Y until end of turn,
     * optionally gaining a keyword. Convenience overload with Int values.
     */
    fun ChooseCreatureTypeModifyStats(
        powerModifier: Int,
        toughnessModifier: Int,
        duration: Duration = Duration.EndOfTurn,
        grantKeyword: Keyword? = null
    ): Effect = EffectPatterns.chooseCreatureTypeModifyStats(
        DynamicAmount.Fixed(powerModifier), DynamicAmount.Fixed(toughnessModifier), duration, grantKeyword
    )

    // =========================================================================
    // Chain Copy Effects
    // =========================================================================

    /**
     * Destroy target permanent, then its controller may copy this spell.
     * Used for Chain of Acid.
     */
    fun DestroyAndChainCopy(
        target: EffectTarget,
        targetFilter: com.wingedsheep.sdk.scripting.filters.unified.TargetFilter,
        spellName: String
    ): Effect = com.wingedsheep.sdk.scripting.effects.ChainCopyEffect(
        action = Destroy(target),
        target = target,
        targetFilter = targetFilter,
        copyRecipient = com.wingedsheep.sdk.scripting.effects.CopyRecipient.TARGET_CONTROLLER,
        copyTargetRequirement = com.wingedsheep.sdk.scripting.targets.TargetObject(filter = targetFilter),
        spellName = spellName
    )

    /**
     * Bounce target permanent, then its controller may sacrifice a land to copy.
     * Used for Chain of Vapor.
     */
    fun BounceAndChainCopy(
        target: EffectTarget,
        targetFilter: com.wingedsheep.sdk.scripting.filters.unified.TargetFilter,
        spellName: String
    ): Effect = com.wingedsheep.sdk.scripting.effects.ChainCopyEffect(
        action = ReturnToHand(target),
        target = target,
        targetFilter = targetFilter,
        copyRecipient = com.wingedsheep.sdk.scripting.effects.CopyRecipient.TARGET_CONTROLLER,
        copyCost = com.wingedsheep.sdk.scripting.costs.PayCost.Sacrifice(
            filter = com.wingedsheep.sdk.scripting.GameObjectFilter.Land
        ),
        copyTargetRequirement = com.wingedsheep.sdk.scripting.targets.TargetObject(filter = targetFilter),
        spellName = spellName
    )

    /**
     * Deal damage to any target, then that player may discard a card to copy.
     * Used for Chain of Plasma.
     */
    fun DamageAndChainCopy(
        amount: Int,
        target: EffectTarget,
        spellName: String
    ): Effect = com.wingedsheep.sdk.scripting.effects.ChainCopyEffect(
        action = DealDamage(amount, target),
        target = target,
        copyRecipient = com.wingedsheep.sdk.scripting.effects.CopyRecipient.AFFECTED_PLAYER,
        copyCost = com.wingedsheep.sdk.scripting.costs.PayCost.Discard(),
        copyTargetRequirement = com.wingedsheep.sdk.scripting.targets.AnyTarget(),
        spellName = spellName
    )

    /**
     * Target player discards cards, then may copy and choose a new target.
     * Used for Chain of Smog.
     */
    fun DiscardAndChainCopy(
        count: Int,
        target: EffectTarget,
        spellName: String
    ): Effect = com.wingedsheep.sdk.scripting.effects.ChainCopyEffect(
        action = EffectPatterns.discardCards(count, target),
        target = target,
        copyRecipient = com.wingedsheep.sdk.scripting.effects.CopyRecipient.TARGET_PLAYER,
        copyTargetRequirement = com.wingedsheep.sdk.scripting.targets.TargetPlayer(),
        spellName = spellName
    )

    /**
     * Prevent all damage target creature would deal, then its controller may sacrifice a land to copy.
     * Used for Chain of Silence.
     */
    fun PreventDamageAndChainCopy(
        target: EffectTarget,
        targetFilter: com.wingedsheep.sdk.scripting.filters.unified.TargetFilter,
        spellName: String
    ): Effect = com.wingedsheep.sdk.scripting.effects.ChainCopyEffect(
        action = PreventAllDamageDealtBy(target),
        target = target,
        targetFilter = targetFilter,
        copyRecipient = com.wingedsheep.sdk.scripting.effects.CopyRecipient.TARGET_CONTROLLER,
        copyCost = com.wingedsheep.sdk.scripting.costs.PayCost.Sacrifice(
            filter = com.wingedsheep.sdk.scripting.GameObjectFilter.Land
        ),
        copyTargetRequirement = com.wingedsheep.sdk.scripting.targets.TargetObject(filter = targetFilter),
        spellName = spellName
    )

    // =========================================================================
    // Gift
    // =========================================================================

    /**
     * Signal that a gift was given (Bloomburrow gift mechanic).
     * Add this to gift modes so that "whenever you give a gift" triggers fire.
     */
    fun GiftGiven(): Effect = GiftGivenEffect

    // =========================================================================
    // Spell Keyword Grants
    // =========================================================================

    /**
     * Grant a keyword to spells of a certain type that the controller casts.
     * Used for emblems like "Instant and sorcery spells you cast have storm."
     */
    fun GrantSpellKeyword(
        keyword: com.wingedsheep.sdk.core.Keyword,
        spellFilter: com.wingedsheep.sdk.scripting.GameObjectFilter
    ): Effect = GrantSpellKeywordEffect(keyword, spellFilter)
}
