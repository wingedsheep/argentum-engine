package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.scripting.values.LandControllerScope
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import com.wingedsheep.sdk.scripting.effects.AddOneManaOfEachColorAmongEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import com.wingedsheep.sdk.scripting.effects.AddCardTypeEffect
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.OpenLifeBidEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveAllLastKnownCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddSubtypeEffect
import com.wingedsheep.sdk.scripting.effects.SetLandTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AnimateLandEffect
import com.wingedsheep.sdk.scripting.effects.ExploreEffect
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.BecomeSaddledEffect
import com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.effects.SetBasePowerEffect

import com.wingedsheep.sdk.scripting.effects.ChooseColorThenEffect
import com.wingedsheep.sdk.scripting.effects.ChooseNumberThenEffect
import com.wingedsheep.sdk.scripting.effects.GrantHexproofFromChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.GrantProtectionFromChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.ForEachColorOfEffect
import com.wingedsheep.sdk.scripting.effects.GrantCantBeBlockedByChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.GrantCantBeBlockedExceptByEffect
import com.wingedsheep.sdk.scripting.effects.GrantFlashbackEffect
import com.wingedsheep.sdk.scripting.effects.GrantHarmonizeEffect
import com.wingedsheep.sdk.scripting.effects.GrantToxicEffect
import com.wingedsheep.sdk.scripting.effects.CantAttackGroupEffect
import com.wingedsheep.sdk.scripting.effects.CantAttackEffect
import com.wingedsheep.sdk.scripting.effects.CantBlockEffect
import com.wingedsheep.sdk.scripting.effects.GoadEffect
import com.wingedsheep.sdk.scripting.effects.SetSuspectedEffect
import com.wingedsheep.sdk.scripting.effects.CantBlockGroupEffect
import com.wingedsheep.sdk.scripting.effects.CantActivateLoyaltyAbilitiesEffect
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect
import com.wingedsheep.sdk.scripting.effects.PreventLandPlaysThisTurnEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.CopyCardIntoCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CastAnyNumberFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.AnyPlayerMayPayEffect
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.effects.GrantDamageBonusEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.EachPlayerReturnsPermanentToHandEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MakePlottedEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.GrantFreeCastTargetFromExileEffect
import com.wingedsheep.sdk.scripting.effects.FightEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.effects.ExchangeControlEffect
import com.wingedsheep.sdk.scripting.effects.ExchangeLifeAndPowerEffect
import com.wingedsheep.sdk.scripting.effects.GainControlByMostEffect
import com.wingedsheep.sdk.scripting.effects.PlayerRankMetric
import com.wingedsheep.sdk.scripting.effects.GiftGivenEffect
import com.wingedsheep.sdk.scripting.effects.GrantSpellKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantFlashToSpellsEffect
import com.wingedsheep.sdk.scripting.effects.GrantSpellsCantBeCounteredEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.GrantExileOnLeaveEffect
import com.wingedsheep.sdk.scripting.effects.GrantHexproofEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantStaticAbilityEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordToAttackersBlockedByEffect
import com.wingedsheep.sdk.scripting.effects.RemoveAllAbilitiesEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.RemoveKeywordEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.SetLifeTotalEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.GrantSuspendEffect
import com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition
import com.wingedsheep.sdk.scripting.effects.PutOnLibraryPositionOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.ExileFromTopRepeatingEffect
import com.wingedsheep.sdk.scripting.effects.ExileLibraryUntilManaValueEffect
import com.wingedsheep.sdk.scripting.effects.ExileOpponentsGraveyardsEffect
import com.wingedsheep.sdk.scripting.effects.ExileUntilLeavesEffect
import com.wingedsheep.sdk.scripting.effects.ExileAndGrantOwnerPlayPermissionEffect
import com.wingedsheep.sdk.scripting.effects.CreateGlobalTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.ReturnCreaturesPutInGraveyardThisTurnEffect
import com.wingedsheep.sdk.scripting.effects.ReturnOneFromLinkedExileEffect
import com.wingedsheep.sdk.scripting.effects.ReturnSelfFromExileTransformedEffect
import com.wingedsheep.sdk.scripting.effects.ReturnSelfToBattlefieldAttachedEffect
import com.wingedsheep.sdk.scripting.effects.DrawUpToEffect
import com.wingedsheep.sdk.scripting.effects.RemoveFromCombatEffect
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.effects.RepeatWhileEffect
import com.wingedsheep.sdk.scripting.effects.ReplaceNextDrawWithEffect
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.NoteCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.ChangeColorToChosenEffect
import com.wingedsheep.sdk.scripting.effects.ChooseColorForTargetEffect
import com.wingedsheep.sdk.scripting.effects.BecomeChosenManaColorEffect
import com.wingedsheep.sdk.scripting.effects.ChangeColorEffect
import com.wingedsheep.sdk.scripting.effects.ChangeWordInTextEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.effects.CreateRandomCreatureTokenWithManaValueEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfEquippedCreatureEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfSourceEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.CreateRoleTokenEffect
import com.wingedsheep.sdk.scripting.effects.CounterAllOnStackEffect
import com.wingedsheep.sdk.scripting.effects.CounterCondition
import com.wingedsheep.sdk.scripting.effects.CounterDestination
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.effects.ReturnSpellToOwnersHandEffect
import com.wingedsheep.sdk.scripting.effects.CounterTarget
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource
import com.wingedsheep.sdk.scripting.effects.ChangeSpellTargetEffect
import com.wingedsheep.sdk.scripting.effects.ChangeTargetEffect
import com.wingedsheep.sdk.scripting.effects.ReselectTargetRandomlyEffect
import com.wingedsheep.sdk.scripting.effects.CopyEachSpellCastEffect
import com.wingedsheep.sdk.scripting.effects.CopyNextSpellCastEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetTriggeredAbilityEffect
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
import com.wingedsheep.sdk.scripting.effects.HijackNextTurnEffect
import com.wingedsheep.sdk.scripting.effects.SkipNextDrawStepEffect
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
 * For composite effect patterns (search, scry, mill, discard, etc.), use the domain
 * pattern objects directly: [LibraryPatterns], [HandPatterns], [GroupPatterns],
 * [ExilePatterns], [CreatureTypePatterns], [MechanicPatterns].
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

    /**
     * Scryfall art for the white Spirit token created by Endure (Tarkir: Dragonstorm).
     * Every Endure card produces this identical token (an N/N white Spirit), so the
     * image is shared here rather than duplicated per card.
     */
    private const val ENDURE_SPIRIT_TOKEN_IMAGE =
        "https://cards.scryfall.io/large/front/8/e/8ea4fc2f-95a4-49d0-b06e-b88d19637737.jpg?1743176763"

    // =========================================================================
    // Damage Effects
    // =========================================================================

    /**
     * Deal damage to a target.
     * No default — every damage effect must explicitly declare its target.
     */
    fun DealDamage(amount: Int, target: EffectTarget, damageSource: EffectTarget? = null): Effect =
        DealDamageEffect(amount, target, damageSource = damageSource)

    /**
     * Deal dynamic damage to a target.
     * Used for effects like "deal damage equal to the number of lands you control".
     */
    fun DealDamage(amount: DynamicAmount, target: EffectTarget, damageSource: EffectTarget? = null): Effect =
        DealDamageEffect(amount, target, damageSource = damageSource)

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
     * Target player wins the game (e.g., Simic Ascendancy, Coalition Victory).
     */
    fun WinGame(target: EffectTarget = EffectTarget.Controller, message: String? = null): Effect =
        com.wingedsheep.sdk.scripting.effects.WinGameEffect(target, message)

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
     * Composed from atomic pipeline primitives — see [HandPatterns.readTheRunes].
     */
    fun ReadTheRunes(): Effect = HandPatterns.readTheRunes()

    /**
     * Target player discards N cards (controller chooses, mandatory).
     * Delegates to the LibraryPatterns/HandPatterns pipeline: Gather → Select → Move (Discard).
     */
    fun Discard(count: Int = 1, target: EffectTarget = EffectTarget.Controller): Effect =
        HandPatterns.discardCards(count, target)

    /**
     * Target player discards a [DynamicAmount] of cards (controller chooses, mandatory) — e.g.
     * "discard X cards, where X is the number of colors of mana spent" (Converge). Delegates to
     * the same Gather → Select → Move pipeline as the fixed-count overload.
     */
    fun Discard(count: DynamicAmount, target: EffectTarget = EffectTarget.Controller): Effect =
        HandPatterns.discardCards(count, target)

    /**
     * Connive (CR 702.166): draw a card, then discard a card. If the discarded card
     * is a nonland, put a +1/+1 counter on [target].
     *
     * Composed entirely from atomic pipeline primitives — see [HandPatterns.connive].
     */
    fun Connive(target: EffectTarget = EffectTarget.Self): Effect =
        HandPatterns.connive(target)

    /**
     * Each opponent discards N cards.
     * Delegates to the LibraryPatterns/HandPatterns pipeline: ForEachPlayer(EachOpponent) → Gather → Select → Move.
     */
    fun EachOpponentDiscards(count: Int = 1): Effect = HandPatterns.eachOpponentDiscards(count)

    /**
     * "Any player may [cost]. If a player does, [consequence]."
     * Each player in APNAP order is offered the cost; the first to pay triggers [consequence].
     * (Prowling Pangolin: "any player may sacrifice two creatures. If a player does, sacrifice this.")
     */
    fun AnyPlayerMayPay(cost: PayCost, consequence: Effect): Effect =
        AnyPlayerMayPayEffect(cost = cost, consequence = consequence)

    /**
     * "[effect] unless any player pays [cost]." — the inverse reading of [AnyPlayerMayPay].
     * Each player in APNAP order may pay; if any does, nothing happens. If none pays, [effect] runs.
     * (Aether Rift: "return it from your graveyard to the battlefield unless any player pays 5 life.")
     */
    fun UnlessAnyPlayerPays(cost: PayCost, effect: Effect): Effect =
        AnyPlayerMayPayEffect(cost = cost, consequence = null, consequenceIfNonePaid = effect)

    /**
     * Each player returns a permanent they control to its owner's hand.
     * Used as a replacement in Words of Wind:
     * ReplaceNextDraw(Effects.EachPlayerReturnPermanentToHand())
     */
    fun EachPlayerReturnPermanentToHand(): Effect = EachPlayerReturnsPermanentToHandEffect

    /**
     * Each player draws cards equal to the damage dealt to the source this turn by sources
     * they controlled. Reads the per-player damage map captured on LTB. Used for
     * Grothama, All-Devouring.
     */
    fun EachPlayerDrawsForDamageDealtToSource(): Effect =
        com.wingedsheep.sdk.scripting.effects.EachPlayerDrawsForDamageDealtToSourceEffect

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
     *
     * @param noRegenerate If true, the target can't be regenerated — composes the
     *   "can't be regenerated" marker before the destroy (Terror, Smother, Tunnel).
     */
    fun Destroy(target: EffectTarget, noRegenerate: Boolean = false): Effect {
        val destroy = MoveToZoneEffect(target, Zone.GRAVEYARD, byDestruction = true)
        return if (noRegenerate) CantBeRegeneratedEffect(target) then destroy else destroy
    }

    /**
     * Destroy all permanents matching a filter using the pipeline.
     * Correctly handles indestructible and regeneration via MoveType.Destroy.
     *
     * @param filter Which permanents to destroy
     * @param noRegenerate If true, destroyed permanents can't be regenerated
     * @param storeDestroyedAs If set, stores actually-destroyed IDs for follow-up effects
     *   (count available as DynamicAmount.VariableReference("{key}_count"))
     * @param excludeTriggering If true, the triggering entity is excluded from the destroyed
     *   set — for "destroy all *other* … with it" triggers (Spreading Plague).
     */
    fun DestroyAll(
        filter: GameObjectFilter,
        noRegenerate: Boolean = false,
        storeDestroyedAs: String? = null,
        excludeTriggering: Boolean = false
    ): Effect = GroupPatterns.destroyAllPipeline(filter, noRegenerate, storeDestroyedAs, excludeTriggering)

    /**
     * Destroy all permanents matching [filter] and all permanents attached to them.
     * Used by End Hostilities-style board wipes.
     */
    fun DestroyAllAndAttached(
        filter: GameObjectFilter,
        noRegenerate: Boolean = false
    ): Effect = GroupPatterns.destroyAllAndAttachedPipeline(filter, noRegenerate)

    /**
     * Destroy the creature with the least power among all creatures on the battlefield. On a tie
     * for least power the controller chooses which one (CR — Drop of Honey).
     */
    fun DestroyLeastPowerCreature(
        noRegenerate: Boolean = false
    ): Effect = GroupPatterns.destroyLeastPowerCreature(noRegenerate)

    /**
     * Destroy all creatures blocking or blocked by the effect's source (CR 509), using the
     * combat pairing last known when the source left the battlefield. Intended for a dies
     * trigger (Abu Ja'far) — the live combat cross-references are already gone by resolution, so
     * the pairing is read from the leaves-battlefield snapshot.
     */
    fun DestroyCreaturesBlockingOrBlockedBySource(
        noRegenerate: Boolean = false
    ): Effect = GroupPatterns.destroyCombatPairedWithSourcePipeline(noRegenerate)

    /**
     * Destroy all creatures sharing a creature type with the sacrificed creature.
     * Requires a creature sacrificed as additional cost.
     */
    fun DestroyAllSharingTypeWithSacrificed(
        noRegenerate: Boolean = false
    ): Effect = CreatureTypePatterns.destroyAllSharingTypeWithSacrificed(noRegenerate)

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
     * Exile a target and let its owner play it while it remains exiled.
     * Optionally taxes opponents who cast it this way.
     */
    fun ExileAndGrantOwnerPlayPermission(
        target: EffectTarget,
        opponentCostIncrease: Int = 0
    ): Effect = ExileAndGrantOwnerPlayPermissionEffect(target, opponentCostIncrease)

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
        ExilePatterns.exileGroupAndLink(filter, storeAs)

    /**
     * Return all cards linked to the source permanent (via LinkedExileComponent)
     * to the battlefield under the controller's control.
     */
    fun ReturnLinkedExile(): Effect = ExilePatterns.returnLinkedExile()

    /**
     * Return all cards linked to the source permanent (via LinkedExileComponent)
     * to the battlefield under their owners' control.
     */
    fun ReturnLinkedExileUnderOwnersControl(): Effect = ExilePatterns.returnLinkedExile(underOwnersControl = true)

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
     * Craft resolution effect — return the source from exile to the battlefield transformed
     * under its owner's control. Paired with [com.wingedsheep.sdk.scripting.AbilityCost.Craft]
     * as the activated ability's effect; see CR 702.167a.
     */
    val ReturnSelfFromExileTransformed: Effect = ReturnSelfFromExileTransformedEffect

    /**
     * Return to hand all creature cards in a player's graveyard that were put there this turn.
     * Used by Garna, the Bloodflame and similar effects.
     */
    fun ReturnCreaturesPutInGraveyardThisTurn(player: Player = Player.You): Effect =
        ReturnCreaturesPutInGraveyardThisTurnEffect(player)

    /**
     * Create a global triggered ability that is not attached to any specific permanent, lasting for
     * the given [duration].
     *
     * The duration is a plain parameter, so this one method covers every lifetime:
     * [Duration.EndOfTurn] (False Cure, Death Frenzy), [Duration.UntilYourNextTurn]
     * (Season of the Bold), [Duration.EndOfCombat], [Duration.Permanent] (Dimensional Breach,
     * planeswalker emblems), etc. Pass [descriptionOverride] to set emblem display text.
     */
    fun CreateGlobalTriggeredAbility(
        ability: TriggeredAbility,
        duration: Duration = Duration.Permanent,
        descriptionOverride: String? = null
    ): Effect = CreateGlobalTriggeredAbilityEffect(ability, duration, descriptionOverride)

    /**
     * Create a permanent emblem that grants a static modification to permanents matching a filter.
     * Used for planeswalker -X abilities that produce static-ability emblems.
     */
    fun CreatePermanentEmblem(
        groupFilter: com.wingedsheep.sdk.scripting.filters.unified.GroupFilter,
        powerBonus: Int = 0,
        toughnessBonus: Int = 0,
        grantedKeywords: List<String> = emptyList(),
        emblemDescription: String
    ): Effect = com.wingedsheep.sdk.scripting.effects.CreatePermanentEmblemEffect(
        groupFilter = groupFilter,
        powerBonus = powerBonus,
        toughnessBonus = toughnessBonus,
        grantedKeywords = grantedKeywords,
        emblemDescription = emblemDescription
    )

    /**
     * Grants the city's blessing to a player (CR 702.131 / 700.5).
     *
     * Once granted, never lost — applying again is a no-op. Used as the
     * resolution effect of Ascend triggers, typically gated by an intervening-if
     * such as `Conditions.ControlPermanentsAtLeast(10)`.
     */
    fun GainCitysBlessing(target: EffectTarget = EffectTarget.Controller): Effect =
        com.wingedsheep.sdk.scripting.effects.GainCitysBlessingEffect(target)

    /**
     * "The Ring tempts you" (CR 701.54). The target player gets the Ring emblem (if they don't have
     * one), then chooses a creature they control to become their Ring-bearer. Defaults to the
     * controller. Tempting still happens even if the player controls no creatures.
     */
    fun TheRingTemptsYou(target: EffectTarget = EffectTarget.Controller): Effect =
        com.wingedsheep.sdk.scripting.effects.TheRingTemptsYouEffect(target)

    /**
     * "Amass [subtype] N" (CR 701.47). The controller puts N +1/+1 counters on an Army they
     * control, creating a 0/0 black [subtype] Army token first if they control no Army, and the
     * chosen Army becomes that subtype in addition to its other types.
     */
    fun Amass(count: Int, subtype: String): Effect =
        com.wingedsheep.sdk.scripting.effects.AmassEffect(DynamicAmount.Fixed(count), subtype)

    /** "Amass [subtype] X" with a dynamic amount (e.g. Fall of Cair Andros, The Mouth of Sauron). */
    fun Amass(amount: DynamicAmount, subtype: String): Effect =
        com.wingedsheep.sdk.scripting.effects.AmassEffect(amount, subtype)

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
     * Put on the bottom of its owner's library (forced — no player choice). Mirror of
     * [PutOnTopOfLibrary]; used by graveyard-hate abilities like Sundering Archaic's
     * "{2}: Put target card from a graveyard on the bottom of its owner's library."
     */
    fun PutOnBottomOfLibrary(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.LIBRARY, ZonePlacement.Bottom)

    /**
     * Owner chooses to put target on top or bottom of their library.
     */
    fun PutOnTopOrBottomOfLibrary(target: EffectTarget): Effect =
        PutOnLibraryPositionOfChoiceEffect(
            target,
            listOf(LibraryChoicePosition.Top, LibraryChoicePosition.Bottom)
        )

    /**
     * Owner chooses to put target second from the top or on the bottom of their library.
     * Used by Hinder/Spell Crumple-style effects (e.g., Temporal Cleansing).
     */
    fun PutSecondFromTopOrBottomOfLibrary(target: EffectTarget): Effect =
        PutOnLibraryPositionOfChoiceEffect(
            target,
            listOf(LibraryChoicePosition.SecondFromTop, LibraryChoicePosition.Bottom)
        )

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
     *
     * Set [landEntersTapped] for "each land played this way enters tapped" clauses
     * (Lightstall Inquisitor). Pair with [GrantPlayWithCostIncrease] to also tax
     * spells cast via the permission.
     */
    fun GrantMayPlayFromExile(
        from: String,
        expiry: com.wingedsheep.sdk.scripting.effects.MayPlayExpiry =
            com.wingedsheep.sdk.scripting.effects.MayPlayExpiry.EndOfTurn,
        withAnyManaType: Boolean = false,
        condition: com.wingedsheep.sdk.scripting.conditions.Condition? = null,
        landEntersTapped: Boolean = false
    ): Effect = GrantMayPlayFromExileEffect(from, expiry, withAnyManaType, condition, landEntersTapped)

    /**
     * Make every card in a named collection *plotted* (CR 718). The cards must already be in
     * exile (chain after a `MoveCollection` to `Zone.EXILE`). Each card gets the plotted
     * designation + a permanent free-cast-as-a-sorcery-on-a-later-turn permission — the Plot
     * keyword's state without the plot cost. Used by Make Your Own Luck ("it becomes plotted").
     */
    fun MakePlotted(from: String): Effect = MakePlottedEffect(from)

    /**
     * Grant "play without paying mana cost" permission to all cards in a named collection.
     * Card must still be in a playable zone (hand, or exile with GrantMayPlayFromExile).
     */
    fun GrantPlayWithoutPayingCost(from: String): Effect = GrantPlayWithoutPayingCostEffect(from)

    /**
     * Tax spells cast from a named collection — each card in [from] gets a
     * generic-mana surcharge so the controller pays [amount] more to cast it.
     * Pair with [GrantMayPlayFromExile] for "each spell cast this way costs {N}
     * more to cast" clauses (Lightstall Inquisitor).
     */
    fun GrantPlayWithCostIncrease(from: String, amount: Int): Effect =
        com.wingedsheep.sdk.scripting.effects.GrantPlayWithCostIncreaseEffect(from, amount)

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
     * Mark a spell on the stack so that it is exiled with the given counters on
     * it instead of being put into its owner's graveyard when it resolves.
     * Used by triggered abilities that read like a replacement effect, e.g.
     * Goliath Daydreamer's "exile that card with a dream counter on it instead
     * of putting it into your graveyard as it resolves".
     */
    fun MarkSpellExileWithCounters(
        target: EffectTarget = EffectTarget.TriggeringEntity,
        counterType: String = com.wingedsheep.sdk.core.Counters.PLUS_ONE_PLUS_ONE,
        count: Int = 1
    ): Effect = com.wingedsheep.sdk.scripting.effects.MarkSpellExileWithCountersEffect(
        target = target,
        counterType = counterType,
        count = count
    )

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
    fun TakeFromLinkedExile(): Effect = ExilePatterns.takeFromLinkedExile()

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

    /**
     * For each matching player, exile cards from the top of their library until
     * the total mana value of cards exiled this way for that player reaches at
     * least [threshold]. All exiled card entity IDs are stored in the pipeline
     * collection [storeAs] for downstream pipeline steps (e.g., granting free
     * cast permission via [GrantMayPlayFromExileEffect] +
     * [GrantPlayWithoutPayingCostEffect]).
     *
     * Used for Dream Harvest ("Each opponent exiles cards from the top of their
     * library until they have exiled cards with total mana value 5 or greater").
     */
    fun ExileLibraryUntilManaValue(
        players: Player = Player.EachOpponent,
        threshold: Int,
        storeAs: String
    ): Effect = ExileLibraryUntilManaValueEffect(
        players = players,
        threshold = DynamicAmount.Fixed(threshold),
        storeAs = storeAs
    )

    /**
     * Cascade (CR 702.85). Resolves the cascade ability of the triggering spell.
     * Reads the triggering spell's mana value from the trigger context, then
     * exiles top of library until a nonland card with lower mana value is
     * exiled, lets the controller cast it for free, and puts the remaining
     * exiled cards on the bottom of the library in a random order.
     */
    val Cascade: Effect = com.wingedsheep.sdk.scripting.effects.CascadeEffect

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
     * Grant a keyword to a target.
     */
    fun GrantKeyword(
        keyword: Keyword,
        target: EffectTarget = EffectTarget.ContextTarget(0),
        duration: Duration = Duration.EndOfTurn
    ): Effect =
        GrantKeywordEffect(keyword.name, target, duration)

    /**
     * Grant an ability flag to a target.
     */
    fun GrantKeyword(
        flag: AbilityFlag,
        target: EffectTarget = EffectTarget.ContextTarget(0),
        duration: Duration = Duration.EndOfTurn
    ): Effect =
        GrantKeywordEffect(flag.name, target, duration)

    /**
     * Grant a static ability to a target until end of turn (or another [duration]).
     *
     * The runtime sibling of a printed [com.wingedsheep.sdk.scripting.StaticAbility] — e.g.
     * granting [com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan] so the combat blocker
     * validation honors a temporarily-conferred "can't be blocked by more than one creature"
     * (Full Steam Ahead). Compose inside [ForEachInGroup] with [EffectTarget.Self] for
     * "each creature you control gains ...".
     */
    fun GrantStaticAbility(
        ability: com.wingedsheep.sdk.scripting.StaticAbility,
        target: EffectTarget = EffectTarget.ContextTarget(0),
        duration: Duration = Duration.EndOfTurn
    ): Effect = GrantStaticAbilityEffect(ability, target, duration)

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
     * Put every counter that was on the triggering source onto a target.
     * Reads the source's last-known counter map (captured on leave-battlefield),
     * not just +1/+1 counters. Use for "put its counters on target creature you
     * control" style triggered abilities (e.g., Essence Channeler).
     */
    fun MoveAllLastKnownCounters(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        MoveAllLastKnownCountersEffect(target)

    /**
     * Install a temporary, duration-scoped counter-placement *modifier* controlled by the
     * resolving ability's controller — the activated/spell-granted analogue of the static
     * `ModifyCounterPlacement` replacement (Hardened Scales).
     *
     * While active, if the controller would put [counterType] counters on a recipient matching
     * [recipient] (resolved relative to that controller — the default "a creature you control"
     * means a creature the controller controls), [modifier] additional counters are placed
     * instead. The store is consulted from the single counter-placement chokepoint, so every
     * counter-adding effect honors it; it expires per [duration] (default end of turn).
     *
     * Defaults reproduce the common case (+1/+1, creature you control, until end of turn), which is
     * Prairie Dog's "{4}{W}: Until end of turn, if you would put one or more +1/+1 counters on a
     * creature you control, put that many plus one +1/+1 counters on it instead."
     */
    fun GrantCounterPlacementModifier(
        modifier: Int = 1,
        duration: Duration = Duration.EndOfTurn,
        counterType: com.wingedsheep.sdk.scripting.events.CounterTypeFilter =
            com.wingedsheep.sdk.scripting.events.CounterTypeFilter.PlusOnePlusOne,
        recipient: com.wingedsheep.sdk.scripting.events.RecipientFilter =
            com.wingedsheep.sdk.scripting.events.RecipientFilter.CreatureYouControl
    ): Effect =
        com.wingedsheep.sdk.scripting.effects.GrantCounterPlacementModifierEffect(
            modifier = modifier,
            duration = duration,
            counterType = counterType,
            recipient = recipient
        )

    /**
     * Double the number of counters of [counterType] already on a target (one-shot).
     * Reads the current count and puts that many more on the target, so the total
     * doubles. Distinct from the [DoubleCounterPlacement] replacement, which doubles
     * counters as they are placed in the future. Used by Sage of the Fang.
     */
    fun DoubleCounters(
        counterType: String = Counters.PLUS_ONE_PLUS_ONE,
        target: EffectTarget = EffectTarget.ContextTarget(0)
    ): Effect =
        com.wingedsheep.sdk.scripting.effects.DoubleCountersEffect(counterType, target)

    /**
     * Remove counters of a given type from a target. No-op if the target has fewer
     * than `count` counters of that type.
     */
    fun RemoveCounters(counterType: String, count: Int, target: EffectTarget): Effect =
        RemoveCountersEffect(counterType, count, target)

    /**
     * Remove any number of counters from a target permanent. The controller chooses
     * how many of each kind to remove (one prompt per counter kind currently on the
     * target). Used by Rhys, the Evermore.
     */
    fun RemoveAnyNumberOfCounters(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        com.wingedsheep.sdk.scripting.effects.RemoveAnyNumberOfCountersEffect(target)

    /**
     * Remove every counter (of any kind) from a target permanent. Mandatory; clears
     * all counter kinds currently on the target. Used by Perfect Intimidation.
     */
    fun RemoveAllCounters(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        com.wingedsheep.sdk.scripting.effects.RemoveAllCountersEffect(target)

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
     * Proliferate — choose any number of permanents and/or players with counters,
     * then give each another counter of each kind already there.
     */
    fun Proliferate(): Effect =
        com.wingedsheep.sdk.scripting.effects.ProliferateEffect

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
     * Set a target land's basic land subtype, replacing all existing land subtypes (Rule 305.7).
     * "Target land becomes an Island until end of turn." Pass [fromChosenValueKey] to read the
     * type from a preceding `ChooseOption(OptionType.BASIC_LAND_TYPE)` instead of [landType].
     */
    fun SetLandType(
        landType: String = "",
        target: EffectTarget = EffectTarget.ContextTarget(0),
        duration: Duration = Duration.EndOfTurn,
        fromChosenValueKey: String? = null
    ): Effect = SetLandTypeEffect(landType, target, duration, fromChosenValueKey)

    /** Choose a color and store it on a target permanent. */
    fun ChooseColorForTarget(
        target: EffectTarget = EffectTarget.Self,
        prompt: String = "Choose a color"
    ): Effect = ChooseColorForTargetEffect(target, prompt)

    /**
     * Make the target become the color the controller chose when activating an
     * "Add one mana of any color" mana ability. Pair inside a [Composite]
     * alongside [AddAnyColorMana] so both share the player's pick.
     */
    fun BecomeChosenManaColor(
        target: EffectTarget = EffectTarget.Self,
        duration: Duration = Duration.EndOfTurn
    ): Effect = BecomeChosenManaColorEffect(target, duration)

    /**
     * Replace the colors of a single target with [colors] until [duration] expires.
     * Pass an empty set to make the target colorless.
     */
    fun ChangeColor(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        colors: Set<Color>,
        duration: Duration = Duration.EndOfTurn
    ): Effect = ChangeColorEffect(target, colors.map { it.name }.toSet(), duration)

    /**
     * Make a single target become all five colors until [duration] expires.
     * Used by Tam, Mindful First-Year: "Target creature you control becomes all colors until end of turn."
     */
    fun BecomeAllColors(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        duration: Duration = Duration.EndOfTurn
    ): Effect = ChangeColorEffect(target, Color.entries.map { it.name }.toSet(), duration)

    /**
     * Make [target] become the chosen color for [duration]. Must run inside a [ChooseColorThen]
     * block — reads the chosen color from the effect context. The target may be a spell on the
     * stack or a permanent. Used by Blind Seer:
     * "{1}{U}: Target spell or permanent becomes the color of your choice until end of turn."
     */
    fun ChangeColorToChosen(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        duration: Duration = Duration.EndOfTurn
    ): Effect = ChangeColorToChosenEffect(target = target, duration = duration)

    /**
     * Change the text of a target spell or permanent by replacing one color word with another,
     * or one basic land type with another, for [duration]. The player chooses the word to replace
     * and its replacement at resolution. Used by Crystal Spray (until end of turn).
     */
    fun ChangeWordInText(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        duration: Duration = Duration.EndOfTurn
    ): Effect = ChangeWordInTextEffect(target = target, duration = duration)

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
     * Pay a dynamically-computed amount of generic mana at resolution, optionally from a player
     * other than the controller. The dynamic twin of a flat [PayManaCostEffect]; the building block
     * for "pay {N} for each X" templating (e.g. `PayDynamicMana(DynamicAmount.Multiply(
     * DynamicAmount.VariableReference("chosen_count"), 4), Player.TriggeringPlayer)`).
     */
    fun PayDynamicMana(
        amount: DynamicAmount,
        payer: Player = Player.You,
        color: com.wingedsheep.sdk.core.Color? = null
    ): Effect =
        com.wingedsheep.sdk.scripting.effects.PayDynamicManaCostEffect(amount, payer, color)

    /**
     * Add mana of a color the player chooses from a [ManaColorSet] resolved at resolution
     * time. The unified "choose-from-set" primitive — see [AddManaOfChoiceEffect].
     *
     * Defaults to all five colors and one mana, matching the legacy "Add one mana of any
     * color" shape.
     */
    fun AddManaOfChoice(
        colorSet: ManaColorSet = ManaColorSet.AnyColor,
        amount: Int = 1,
        restriction: ManaRestriction? = null,
    ): Effect = AddManaOfChoiceEffect(colorSet, DynamicAmount.Fixed(amount), restriction)

    /** Dynamic-amount variant of [AddManaOfChoice]. */
    fun AddManaOfChoice(
        colorSet: ManaColorSet,
        amount: DynamicAmount,
        restriction: ManaRestriction? = null,
    ): Effect = AddManaOfChoiceEffect(colorSet, amount, restriction)

    /**
     * Add N mana of any *one* color ("Add three mana of any one color" — Gilded Lotus):
     * the player picks a single color and gets [amount] of it. For "any **combination** of
     * colors" (each mana independently colored), use [AddManaInAnyCombination] instead.
     */
    fun AddAnyColorMana(amount: Int = 1, restriction: ManaRestriction? = null): Effect =
        AddManaOfChoiceEffect(ManaColorSet.AnyColor, DynamicAmount.Fixed(amount), restriction)

    /** Dynamic-amount variant of [AddAnyColorMana]. */
    fun AddAnyColorMana(amount: DynamicAmount, restriction: ManaRestriction? = null): Effect =
        AddManaOfChoiceEffect(ManaColorSet.AnyColor, amount, restriction)

    /**
     * Add one mana of any color, restricted to spells (and optionally activated abilities)
     * of the source's chosen subtype, optionally carrying spell riders.
     *
     * Examples:
     *  - Unclaimed Territory / Eclipsed Realms variants — default args:
     *    `AddAnyColorManaSpendOnChosenType()`
     *  - Cavern of Souls — creature spells only, uncounterable:
     *    `AddAnyColorManaSpendOnChosenType(creatureOnly = true,
     *     riders = setOf(ManaSpellRider.MakesSpellUncounterable))`
     *
     * The executor reads the source's `CastChoicesComponent` and bakes that
     * subtype into the restriction at the moment mana is added to the pool.
     */
    fun AddAnyColorManaSpendOnChosenType(
        amount: Int = 1,
        creatureOnly: Boolean = false,
        riders: Set<ManaSpellRider> = emptySet()
    ): Effect = AddAnyColorManaSpendOnChosenTypeEffect(amount, creatureOnly, riders)

    /**
     * Add X mana in any combination of the allowed colors.
     * "Add that much mana in any combination of {R} and/or {G}."
     */
    fun AddDynamicMana(amount: DynamicAmount, allowedColors: Set<Color>, restriction: ManaRestriction? = null): Effect =
        AddDynamicManaEffect(amount, allowedColors, restriction)

    /**
     * Add N mana in any combination of the given colors. Player picks each pip's color
     * independently at resolution. Defaults to all five colors — "Add N mana in any
     * combination of colors" (e.g., Interdimensional Web Watch).
     *
     * For one or two allowed colors the executor falls back to its bulk/two-color split
     * paths; with three or more colors it prompts pip-by-pip.
     */
    fun AddManaInAnyCombination(
        amount: Int,
        allowedColors: Set<Color> = Color.entries.toSet(),
        restriction: ManaRestriction? = null
    ): Effect = AddDynamicManaEffect(DynamicAmount.Fixed(amount), allowedColors, restriction)

    fun AddManaInAnyCombination(
        amount: DynamicAmount,
        allowedColors: Set<Color> = Color.entries.toSet(),
        restriction: ManaRestriction? = null
    ): Effect = AddDynamicManaEffect(amount, allowedColors, restriction)

    /**
     * Add one mana of the color chosen when this permanent entered the battlefield.
     * Used for cards like Uncharted Haven. Sugar for
     * `AddManaOfChoice(ManaColorSet.SourceChosenColor, amount)`.
     */
    fun AddManaOfChosenColor(amount: Int = 1, restriction: ManaRestriction? = null): Effect =
        AddManaOfChoiceEffect(ManaColorSet.SourceChosenColor, DynamicAmount.Fixed(amount), restriction)

    /**
     * Add one mana of any color among permanents matching a filter that you control.
     * Used for cards like Mox Amber. Sugar for
     * `AddManaOfChoice(ManaColorSet.AmongPermanents(filter))`.
     */
    fun AddManaOfColorAmong(filter: GameObjectFilter, restriction: ManaRestriction? = null): Effect =
        AddManaOfChoiceEffect(ManaColorSet.AmongPermanents(filter), DynamicAmount.Fixed(1), restriction)

    /**
     * For each color among permanents matching a filter, add one mana of that color.
     * Used for cards like Bloom Tender — produces one mana of every color present (0–5 total).
     */
    fun AddOneManaOfEachColorAmong(filter: GameObjectFilter, restriction: ManaRestriction? = null): Effect =
        AddOneManaOfEachColorAmongEffect(filter, restriction)

    /**
     * Add one mana of any color that a land in the given scope could produce.
     * Used for cards like Fellwar Stone (OPPONENTS), Exotic Orchard (OPPONENTS),
     * Reflecting Pool (YOU). Sugar for
     * `AddManaOfChoice(ManaColorSet.LandsCouldProduce(scope))`.
     */
    fun AddManaOfColorLandsCouldProduce(
        scope: LandControllerScope,
        restriction: ManaRestriction? = null
    ): Effect = AddManaOfChoiceEffect(ManaColorSet.LandsCouldProduce(scope), DynamicAmount.Fixed(1), restriction)

    /**
     * Add one mana of any color in your commander's color identity. Used by Arcane Signet
     * and Command Tower. Sugar for
     * `AddManaOfChoice(ManaColorSet.CommanderIdentity)`.
     */
    fun AddManaOfColorInCommanderColorIdentity(
        restriction: ManaRestriction? = null
    ): Effect = AddManaOfChoiceEffect(ManaColorSet.CommanderIdentity, DynamicAmount.Fixed(1), restriction)

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
     * Create a dynamic number of creature tokens — the count is evaluated at resolution
     * time (e.g. "create X 1/1 green Saproling creature tokens" for Verdeloth the Ancient,
     * where X is the kicker amount read via [DynamicAmount.XValue]). Distinct from the
     * `Int`-count overload above; callers pass `count = DynamicAmount.XValue` etc.
     */
    fun CreateToken(
        count: DynamicAmount,
        power: Int,
        toughness: Int,
        colors: Set<Color> = emptySet(),
        creatureTypes: Set<String>,
        keywords: Set<Keyword> = emptySet(),
        controller: EffectTarget? = null,
        imageUri: String? = null,
        legendary: Boolean = false
    ): Effect = CreateTokenEffect(
        count = count, power = power, toughness = toughness, colors = colors,
        creatureTypes = creatureTypes, keywords = keywords,
        controller = controller, imageUri = imageUri, legendary = legendary
    )

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
        controller: EffectTarget? = null,
        imageUri: String? = null
    ): Effect = CreateTokenEffect(
        count = DynamicAmount.Fixed(count),
        power = 0, toughness = 0,
        colors = colors, creatureTypes = creatureTypes, keywords = keywords,
        controller = controller,
        dynamicPower = dynamicPower, dynamicToughness = dynamicToughness,
        imageUri = imageUri
    )

    /**
     * Create a creature token whose color and creature type are the ones the source locked into its
     * cast-choice slots (e.g. Riptide Replicator: "create an X/X creature token of the chosen color
     * and type"), with dynamic power/toughness. Reads `ChoiceSlot.COLOR` / `ChoiceSlot.CREATURE_TYPE`
     * from the source's durable cast-choices bag at resolution — the generic replacement for the old
     * one-off `CreateChosenTokenEffect`.
     */
    fun CreateTokenOfChosenColorAndType(
        dynamicPower: DynamicAmount,
        dynamicToughness: DynamicAmount,
        count: Int = 1
    ): Effect = CreateTokenEffect(
        count = DynamicAmount.Fixed(count),
        power = 0, toughness = 0,
        colors = emptySet(), creatureTypes = emptySet(),
        dynamicPower = dynamicPower, dynamicToughness = dynamicToughness,
        colorsFromChoice = com.wingedsheep.sdk.scripting.ChoiceSlot.COLOR,
        creatureTypesFromChoice = com.wingedsheep.sdk.scripting.ChoiceSlot.CREATURE_TYPE
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
     * Create a token that's a copy of a randomly chosen creature card with mana value [manaValue]
     * (the Momir Basic avatar payoff). Pool is the active [com.wingedsheep.sdk.core.Format.MomirBasic]'s
     * eligible creatures; see [CreateRandomCreatureTokenWithManaValueEffect].
     */
    fun CreateRandomCreatureTokenWithManaValue(manaValue: DynamicAmount): Effect =
        CreateRandomCreatureTokenWithManaValueEffect(manaValue)

    /**
     * Create a token that's a copy of a targeted permanent.
     * "Create a token that's a copy of target creature, except it's 1/1."
     */
    fun CreateTokenCopyOfTarget(
        target: EffectTarget,
        count: Int = 1,
        overridePower: Int? = null,
        overrideToughness: Int? = null,
        tapped: Boolean = false,
        attacking: Boolean = false,
        triggeredAbilities: List<TriggeredAbility> = emptyList(),
        addedKeywords: Set<com.wingedsheep.sdk.core.Keyword> = emptySet(),
        addedSupertypes: Set<com.wingedsheep.sdk.core.Supertype> = emptySet(),
        removedSupertypes: Set<com.wingedsheep.sdk.core.Supertype> = emptySet(),
        overrideColors: Set<com.wingedsheep.sdk.core.Color>? = null,
        overrideSubtypes: Set<com.wingedsheep.sdk.core.Subtype>? = null,
        sacrificeAtStep: com.wingedsheep.sdk.core.Step? = null,
        sacrificeOnlyOnControllersTurn: Boolean = false
    ): Effect = CreateTokenCopyOfTargetEffect(
        target = target,
        count = DynamicAmount.Fixed(count),
        overridePower = overridePower,
        overrideToughness = overrideToughness,
        tapped = tapped,
        attacking = attacking,
        triggeredAbilities = triggeredAbilities,
        addedKeywords = addedKeywords,
        addedSupertypes = addedSupertypes,
        removedSupertypes = removedSupertypes,
        overrideColors = overrideColors,
        overrideSubtypes = overrideSubtypes,
        sacrificeAtStep = sacrificeAtStep,
        sacrificeOnlyOnControllersTurn = sacrificeOnlyOnControllersTurn
    )

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
     * Create a dynamic number of Treasure tokens — the count is evaluated at resolution
     * time. Used for cards like Goldvein Hydra ("create a number of tapped Treasure tokens
     * equal to its power") where the amount depends on game state.
     */
    fun CreateTreasure(count: DynamicAmount, tapped: Boolean = false): Effect =
        CreatePredefinedTokenEffect("Treasure", tapped = tapped, dynamicCount = count)

    /**
     * "You may behold a [filter]. If you do, [ifBeheld]." — the resolution-time behold
     * (choose a matching permanent you control or reveal a matching card from hand). See
     * [com.wingedsheep.sdk.scripting.effects.BeholdEffect]. Used by Sarkhan, Dragon Ascendant.
     */
    fun Behold(
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter,
        ifBeheld: Effect? = null
    ): Effect = com.wingedsheep.sdk.scripting.effects.BeholdEffect(filter, ifBeheld)

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

    /**
     * Create Mutavault land tokens.
     * "{T}: Add {C}."
     * "{1}: This token becomes a 2/2 creature with all creature types until end of turn.
     *  It's still a land."
     *
     * @param count Number of tokens to create
     * @param tapped Whether the tokens enter the battlefield tapped
     * @param controller Who controls the tokens (null = spell controller)
     */
    fun CreateMutavault(count: Int = 1, tapped: Boolean = false, controller: EffectTarget? = null): Effect =
        CreatePredefinedTokenEffect("Mutavault", count, controller, tapped)

    /**
     * Create a Role token attached to a target creature.
     * Role tokens are Enchantment — Aura Role tokens. If the target already has a Role
     * the same player controls, that Role is put into the graveyard first.
     *
     * @param roleName The Role type (e.g., "Sorcerer Role")
     * @param target The creature to attach the Role to
     */
    fun CreateRoleToken(roleName: String, target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        CreateRoleTokenEffect(roleName, target)

    /**
     * Incubate N (CR 701.53). Create an Incubator token with N +1/+1 counters on it
     * and "{2}: Transform this token." It transforms into a 0/0 Phyrexian artifact creature.
     *
     * Implemented purely as composition: the predefined token executor publishes the
     * created token's entity ID into pipeline collection [CREATED_TOKENS], and a
     * subsequent [AddCountersEffect] places the +1/+1 counters via [EffectTarget.PipelineTarget].
     */
    fun Incubate(n: Int): Effect = MechanicPatterns.incubate(n)

    /**
     * Incubate X (CR 701.53), where the +1/+1 counter count is a [DynamicAmount]
     * resolved at resolution time (e.g., the triggering spell's mana value).
     */
    fun Incubate(amount: com.wingedsheep.sdk.scripting.values.DynamicAmount): Effect =
        MechanicPatterns.incubate(amount)

    /**
     * Target creature explores.
     *
     * "Reveal the top card of your library. If it's a land card, put it into your hand.
     * Otherwise, put a +1/+1 counter on this creature, then put the card back on top of
     * your library or put it into your graveyard."
     *
     * @param target The creature that explores (default: context target 0)
     */
    fun Explore(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        ExploreEffect(target)

    /**
     * Create a Map artifact token.
     * "{1}, {T}, Sacrifice this artifact: Target creature you control explores.
     *  Activate only as a sorcery."
     *
     * @param count Number of tokens to create
     */
    fun CreateMapToken(count: Int = 1): Effect =
        CreatePredefinedTokenEffect("Map", count)

    /**
     * Create Drone artifact creature tokens.
     * "Flying. This token can block only creatures with flying."
     *
     * @param count Number of tokens to create
     */
    fun CreateDroneToken(count: Int = 1): Effect =
        CreatePredefinedTokenEffect("Drone", count)

    /**
     * Create Munitions artifact tokens (noncreature). Created by Weapons Manufacturing.
     * "When this token leaves the battlefield, it deals 2 damage to any target."
     *
     * @param count Number of tokens to create
     */
    fun CreateMunitionsToken(count: Int = 1): Effect =
        CreatePredefinedTokenEffect("Munitions", count)

    // =========================================================================
    // Protection Effects
    // =========================================================================

    /**
     * Grant protection from a fixed color to a target (no player choice).
     * "{W}: Target creature gains protection from red until end of turn." (Crimson Acolyte)
     *
     * Composes [GrantKeywordEffect] with the `PROTECTION_FROM_<COLOR>` string keyword —
     * the same projected keyword the static "Protection from red" ability produces.
     */
    fun GrantProtectionFromColor(
        color: Color,
        target: EffectTarget = EffectTarget.ContextTarget(0),
        duration: Duration = Duration.EndOfTurn
    ): Effect = GrantKeywordEffect("PROTECTION_FROM_${color.name}", target, duration)

    /**
     * Choose a color, then run [then] with the chosen color exposed via the effect
     * context. Atomic effects under [then] (e.g. [GrantHexproofFromChosenColor],
     * [GrantCantBeBlockedByChosenColor]) read the color and apply per-color
     * modifications. Compose with [Composite] for multi-grant cards (e.g. Skrelv).
     */
    fun ChooseColorThen(
        then: Effect,
        prompt: String = "Choose a color"
    ): Effect = ChooseColorThenEffect(then, prompt)

    /**
     * Choose a number, then run [then] with the chosen number exposed via the effect
     * context (as X). Atomic effects and filters under [then] read it through
     * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueEqualsX] (via
     * `GameObjectFilter`/`GroupFilter` `manaValueEqualsX()`). Compose with [Composite]
     * for multi-step cards (Void: destroy all, then reveal & discard). [minValue]/[maxValue]
     * bound the legal choice.
     */
    fun ChooseNumberThen(
        then: Effect,
        minValue: Int = 0,
        maxValue: Int = 16,
        prompt: String = "Choose a number"
    ): Effect = ChooseNumberThenEffect(then, minValue, maxValue, prompt)

    /**
     * Grant Toxic N to a target until end of turn. Resolves to a `TOXIC_<n>`
     * keyword grant; combat damage reads granted toxic amounts from projected keywords.
     */
    fun GrantToxic(
        amount: Int,
        target: EffectTarget = EffectTarget.ContextTarget(0),
        duration: Duration = Duration.EndOfTurn
    ): Effect = GrantToxicEffect(amount, target, duration)

    /**
     * Grant Harmonize (CR 702.180) to a target instant or sorcery card in a graveyard.
     * "Target instant or sorcery card in your graveyard gains harmonize until end of turn. Its
     * harmonize cost is equal to its mana cost." — Songcrafter Mage.
     *
     * [cost] defaults to `null`, meaning the harmonize cost equals the card's own mana cost;
     * pass a [ManaCost] to grant a fixed harmonize cost instead.
     */
    fun GrantHarmonize(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        cost: ManaCost? = null,
        duration: Duration = Duration.EndOfTurn
    ): Effect = GrantHarmonizeEffect(target, cost, duration)

    /**
     * Grant Flashback (CR 702.34) to a target instant or sorcery card in a graveyard.
     * "Target instant or sorcery card in your graveyard gains flashback until end of turn. The
     * flashback cost is equal to its mana cost." — Archmage's Newt.
     *
     * [cost] defaults to `null`, meaning the flashback cost equals the card's own mana cost;
     * pass a [ManaCost] to grant a fixed flashback cost instead (e.g. `{0}` when saddled).
     */
    fun GrantFlashback(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        cost: ManaCost? = null,
        duration: Duration = Duration.EndOfTurn
    ): Effect = GrantFlashbackEffect(target, cost, duration)

    /**
     * Grant "hexproof from the chosen color" to a target. Must run inside a
     * [ChooseColorThen] block — reads the chosen color from the effect context.
     */
    fun GrantHexproofFromChosenColor(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        duration: Duration = Duration.EndOfTurn
    ): Effect = GrantHexproofFromChosenColorEffect(target, duration)

    /**
     * Grant "protection from the chosen color" to a target. Must run inside a
     * [ChooseColorThen] block — reads the chosen color from the effect context.
     * Pair with [GrantHexproofFromChosenColor] / [GrantCantBeBlockedByChosenColor]
     * under one [ChooseColorThen] for multi-grant cards.
     */
    fun GrantProtectionFromChosenColor(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        duration: Duration = Duration.EndOfTurn
    ): Effect = GrantProtectionFromChosenColorEffect(target, duration)

    /**
     * Run [effect] once per color of [source], with that color set as the context's chosen
     * color — the non-interactive sibling of [ChooseColorThen]. Compose with any per-color
     * atom that reads the chosen color (e.g. [GrantProtectionFromChosenColor]).
     *
     * "[group] gain protection from each of [source]'s colors" (Éowyn, Fearless Knight) is:
     *
     *     Effects.ForEachColorOf(
     *         source = EntityReference.Target(0),
     *         effect = ForEachInGroupEffect(group, Effects.GrantProtectionFromChosenColor(EffectTarget.Self)),
     *     )
     *
     * Colors are read from projected state on the battlefield (Layer-5 / Devoid honored),
     * else from printed colors (last-known information); a colorless source runs zero times.
     * When [source] is an about-to-leave permanent, run [ForEachColorOf] **before** the
     * exile/destroy step so its projected colors are still readable.
     */
    fun ForEachColorOf(
        source: EntityReference,
        effect: Effect
    ): Effect = ForEachColorOfEffect(source, effect)

    /**
     * Grant "can't be blocked by creatures of the chosen color" to a target.
     * Must run inside a [ChooseColorThen] block — reads the chosen color from
     * the effect context.
     */
    fun GrantCantBeBlockedByChosenColor(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        duration: Duration = Duration.EndOfTurn
    ): Effect = GrantCantBeBlockedByChosenColorEffect(target, duration)

    /**
     * Grant a creature "can't be blocked except by creatures matching [blockerFilter]" for
     * [duration]. The one-shot, floating-effect counterpart to the static
     * [com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy] ability; for the color-only sibling use
     * [GrantCantBeBlockedExceptByColor]. Routes through the same projected evasion channel the
     * static ability uses, so the existing block rules enforce it.
     *
     * Used by Resilient Roadrunner: "{3}: This creature can't be blocked this turn except by
     * creatures with haste."
     */
    fun GrantCantBeBlockedExceptBy(
        target: EffectTarget,
        blockerFilter: GameObjectFilter,
        duration: Duration = Duration.EndOfTurn
    ): Effect = GrantCantBeBlockedExceptByEffect(target, blockerFilter, duration)

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
        GainControlByMostEffect(PlayerRankMetric.CreaturesOfSubtype(subtype), target)

    /**
     * The player with strictly more life than every other player gains control of the
     * target. On a tie for highest life, nothing happens. (Ghazbán Ogre.)
     */
    fun GainControlByMostLife(target: EffectTarget = EffectTarget.Self): Effect =
        GainControlByMostEffect(PlayerRankMetric.LifeTotal, target)

    /**
     * Choose a creature type. If you control more creatures of that type than each
     * other player, gain control of all creatures of that type.
     */
    fun ChooseCreatureTypeGainControl(duration: Duration = Duration.Permanent): Effect =
        CreatureTypePatterns.chooseCreatureTypeGainControl(duration)

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
     *
     * @param stopOnError when true, abort the remaining effects if one fails.
     * @param descriptionOverride render a single hand-written sentence instead of joining sub-effects.
     * @param descriptionAmounts dynamic values interpolated into `{0}`, `{1}`, … of [descriptionOverride] at runtime.
     */
    fun Composite(
        effects: List<Effect>,
        stopOnError: Boolean = false,
        descriptionOverride: String? = null,
        descriptionAmounts: List<DynamicAmount> = emptyList()
    ): Effect = CompositeEffect(effects, stopOnError, descriptionOverride, descriptionAmounts)

    /**
     * Compose an inline Gather → Select → Move pipeline with typed slot handles —
     * the facade-respecting replacement for hand-threading string slot keys between
     * raw pipeline step constructors. Serializes to the exact [CompositeEffect] tree
     * the steps would produce by hand; see [PipelineBuilder] for the step vocabulary.
     *
     * ```kotlin
     * effect = Effects.Pipeline {
     *     val looked = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(7)))
     *     val (kept, rest) = chooseExactlySplit(2, from = looked)
     *     toHand(kept)
     *     toGraveyard(rest)
     * }
     * ```
     *
     * @param stopOnError when true, abort the remaining steps if one fails.
     * @param descriptionOverride render a single hand-written sentence instead of joining steps.
     * @param descriptionAmounts dynamic values interpolated into `{0}`, `{1}`, … of [descriptionOverride] at runtime.
     */
    fun Pipeline(
        stopOnError: Boolean = false,
        descriptionOverride: String? = null,
        descriptionAmounts: List<DynamicAmount> = emptyList(),
        block: PipelineBuilder.() -> Unit
    ): Effect = PipelineBuilder.build(stopOnError, descriptionOverride, descriptionAmounts, block)

    /**
     * Move [target] to [destination] zone — the foundational single-target zone-change effect.
     *
     * Prefer the named shortcuts ([Destroy], [Exile], [ReturnToHand], [PutOnTopOfLibrary],
     * [ShuffleIntoLibrary], [PutOntoBattlefield], …) when one fits; reach for `Move` for the
     * less-common shapes (custom placement, `fromZone` gating, face-down entry, linked exile,
     * `positionFromTop`, controller override).
     */
    fun Move(
        target: EffectTarget,
        destination: Zone,
        placement: ZonePlacement = ZonePlacement.Default,
        byDestruction: Boolean = false,
        controllerOverride: EffectTarget? = null,
        fromZone: Zone? = null,
        faceDown: Boolean = false,
        linkToSource: Boolean = false,
        positionFromTop: Int? = null
    ): Effect = MoveToZoneEffect(
        target = target,
        destination = destination,
        placement = placement,
        byDestruction = byDestruction,
        controllerOverride = controllerOverride,
        fromZone = fromZone,
        faceDown = faceDown,
        linkToSource = linkToSource,
        positionFromTop = positionFromTop
    )

    /**
     * Apply [effect] to every entity matching [filter] (Rule: "each", "all"). Within the inner
     * effect, [EffectTarget.Self] resolves to the current iteration entity.
     *
     * The group is snapshotted before any iteration applies.
     *
     * @param noRegenerate affected entities cannot be regenerated.
     */
    fun ForEachInGroup(
        filter: GroupFilter,
        effect: Effect,
        noRegenerate: Boolean = false
    ): Effect = ForEachInGroupEffect(filter, effect, noRegenerate)

    /**
     * Copy a card referenced by [source] into the pipeline collection [storeAs] (Rule 707.12).
     * The copy is created in the card's current zone and becomes castable; pair with
     * [CastFromCollectionWithoutPayingCost] reading the same key to "cast the copy".
     */
    fun CopyCardIntoCollection(source: EffectTarget, storeAs: String): Effect =
        CopyCardIntoCollectionEffect(source = source, storeAs = storeAs)

    /**
     * Cast the (0..1) card stored under [from] without paying its mana cost. The card must
     * already be in a zone where casting is legal (e.g. exile after a move/copy step).
     */
    fun CastFromCollectionWithoutPayingCost(from: String): Effect =
        CastFromCollectionWithoutPayingCostEffect(from = from)

    /**
     * Suspend an already-exiled [target] with [timeCounters] time counters (CR 702.62) — a
     * reusable two-step chain: put N time counters on the card, then mark it suspended. The
     * marker is what the engine keys on to synthesize the owner's-upkeep countdown that
     * removes a counter each turn and, when the last is gone, lets the owner play the card
     * for free with haste (see [com.wingedsheep.sdk.scripting.Suspend]).
     *
     * The caller is responsible for getting the card into exile first, because that step
     * differs by source zone: a spell on the stack is exiled with [CounterSpellToExile]
     * (Taigam, Master Opportunist exiles "the spell you cast"); a printed `suspend N—[cost]`
     * exiles the card from hand as its cast cost.
     */
    fun Suspend(target: EffectTarget, timeCounters: Int): Effect =
        CompositeEffect(
            listOf(
                AddCountersEffect(Counters.TIME, timeCounters, target),
                GrantSuspendEffect(target),
            )
        )

    /**
     * Cast any number of the cards stored under [from] without paying their mana costs,
     * during this effect's resolution, with card-type timing restrictions ignored. The
     * controller is offered the cards one at a time and may stop at any point; cards left
     * uncast stay where they are. Filter the collection to the eligible set upstream.
     */
    fun CastAnyNumberFromCollectionWithoutPayingCost(from: String): Effect =
        CastAnyNumberFromCollectionWithoutPayingCostEffect(from = from)

    /**
     * Repeat a body effect in a do-while loop controlled by a repeat condition.
     */
    fun RepeatWhile(body: Effect, repeatCondition: RepeatCondition): Effect =
        RepeatWhileEffect(body, repeatCondition)

    /**
     * "[action]. If you do, [ifYouDo]" — gates [ifYouDo] on whether [action] actually
     * accomplished its work, not on a yes/no decision. Wrap with `MayEffect` for the
     * common "You may [action]. If you do, [effect]" pattern.
     *
     * The default [SuccessCriterion.Auto] is only legal on action shapes it can infer
     * success from (a terminal zone move) — card-load validation rejects it elsewhere;
     * pass [SuccessCriterion.Always] / [SuccessCriterion.CollectionNonEmpty] explicitly
     * for actions whose outcome isn't a zone-size delta.
     */
    fun IfYouDo(
        action: Effect,
        ifYouDo: Effect,
        ifYouDont: Effect? = null,
        successCriterion: SuccessCriterion = SuccessCriterion.Auto
    ): Effect = IfYouDoEffect(action, ifYouDo, ifYouDont, successCriterion)

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
     *
     * [onPaid] is an optional rider that runs **only if** the spell's controller pays
     * — the "If they do, …" clause on cards like Divert Disaster (controller of the
     * counter, not of the countered spell, becomes the rider's `controllerId`).
     */
    fun CounterUnlessPays(cost: String, onPaid: Effect? = null): Effect =
        CounterEffect(condition = CounterCondition.UnlessPaysMana(ManaCost.parse(cost), onPaid))

    /**
     * Counter target spell unless its controller pays a dynamic generic mana cost.
     * "Counter target spell unless its controller pays {2} for each Wizard on the battlefield."
     *
     * [onPaid] mirrors [CounterUnlessPays]'s rider.
     */
    fun CounterUnlessDynamicPays(
        amount: DynamicAmount,
        exileOnCounter: Boolean = false,
        onPaid: Effect? = null
    ): Effect =
        CounterEffect(
            condition = CounterCondition.UnlessPaysDynamic(amount, onPaid),
            counterDestination = if (exileOnCounter) CounterDestination.Exile() else CounterDestination.Graveyard
        )

    /**
     * Counter target activated or triggered ability.
     * "Counter target activated or triggered ability."
     */
    fun CounterAbility(): Effect =
        CounterEffect(target = CounterTarget.Ability)

    /**
     * Open life-bidding auction between you and another participant.
     * "You and [participant] bid life. You start the bidding with a bid of 1. In turn order,
     * each player may top the high bid. The bidding ends if the high bid stands. The high
     * bidder loses life equal to the high bid. If you win the bidding, [onWin]." [onWin] runs
     * only if you win, with the original targets in context. For Mages' Contest, bid against
     * the targeted spell's controller and counter it — pair with a `TargetSpell` requirement:
     * `Effects.OpenLifeBid(Effects.CounterSpell(), Player.ControllerOf("target spell"))`.
     */
    fun OpenLifeBid(onWin: Effect, participant: Player = Player.AnOpponent): Effect =
        OpenLifeBidEffect(onWin = onWin, participant = participant)

    /**
     * Counter target spell or activated/triggered ability. Used by cards like
     * Teferi's Response that can target either a spell or an ability on the stack.
     */
    fun CounterSpellOrAbility(filter: com.wingedsheep.sdk.scripting.filters.unified.TargetFilter? = null): Effect =
        CounterEffect(target = CounterTarget.SpellOrAbility, filter = filter)

    /**
     * If the spell/ability being countered is an activated or triggered ability whose
     * source is a permanent on the battlefield, destroy that permanent. Atomic step for
     * "If a permanent's ability is countered this way, destroy that permanent" — compose
     * via `CompositeEffect` with [CounterSpellOrAbility] (place this step *before* the
     * counter so the stack entity's ability component is still readable).
     */
    fun DestroySourceOfTargetedAbility(): Effect =
        com.wingedsheep.sdk.scripting.effects.DestroySourceOfTargetedAbilityEffect

    /**
     * Return target spell to its owner's hand.
     *
     * Distinct from a counter — "this spell can't be countered" does not
     * prevent the bounce. Used by cards like Hullbreaker Horror.
     */
    fun ReturnSpellToOwnersHand(): Effect = ReturnSpellToOwnersHandEffect

    /**
     * Counter all spells and abilities your opponents control on the stack.
     * "Counter all spells your opponents control and all abilities your opponents control."
     *
     * If [storeCountAs] is set, the countered entity IDs are stored in the pipeline
     * under that name. A subsequent effect can reference the count via
     * `DynamicAmount.VariableReference("${storeCountAs}_count")`.
     *
     * Used by Glen Elendra's Answer for its Faerie-token-per-countered-object rider.
     */
    fun CounterAllOpponentStackObjects(
        spells: Boolean = true,
        abilities: Boolean = true,
        storeCountAs: String? = null
    ): Effect = CounterAllOnStackEffect(
        spells = spells,
        abilities = abilities,
        opponentsOnly = true,
        storeCountAs = storeCountAs
    )

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
     * The player named by [chooser] may change the target or targets of the triggering spell or
     * ability. The non-random, player-chosen counterpart of [ReselectTargetRandomly]; resolve from
     * a trigger on the spell/ability (e.g. a "whenever a player chooses targets" trigger).
     */
    fun ChangeTriggeringObjectTargets(
        chooser: com.wingedsheep.sdk.scripting.effects.RetargetChooser =
            com.wingedsheep.sdk.scripting.effects.RetargetChooser.Controller
    ): Effect =
        com.wingedsheep.sdk.scripting.effects.ChangeTriggeringObjectTargetsEffect(chooser)

    /**
     * Copy target instant or sorcery spell. You may choose new targets for the copy.
     *
     * If [keywordsForCopy] is non-empty, the copy will also be treated as having those
     * keywords for the duration of its time on the stack (e.g., wither, lifelink).
     */
    fun CopyTargetSpell(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        keywordsForCopy: List<com.wingedsheep.sdk.core.Keyword> = emptyList(),
        removeLegendary: Boolean = false
    ): Effect =
        CopyTargetSpellEffect(target, keywordsForCopy.map { it.name }, removeLegendary)

    /**
     * Grant a keyword to a spell or ability on the stack (e.g., wither, lifelink).
     * Lasts while the spell remains on the stack. Used for "that spell gains X"
     * effects on triggered abilities like Spinerock Tyrant.
     */
    fun GrantKeywordToSpell(
        keyword: com.wingedsheep.sdk.core.Keyword,
        target: EffectTarget = EffectTarget.TriggeringEntity
    ): Effect =
        com.wingedsheep.sdk.scripting.effects.GrantKeywordToSpellEffect(keyword.name, target)

    /**
     * Copy target triggered ability. You may choose new targets for the copy.
     */
    fun CopyTargetTriggeredAbility(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        CopyTargetTriggeredAbilityEffect(target)

    /**
     * Copy target spell **or** ability. You may choose new targets for the copy. Dispatches at
     * resolution on the chosen object: an instant/sorcery spell copies via the spell-copy path, an
     * activated/triggered ability copies its ability-on-stack component. Pair with a target that
     * permits both (e.g. [com.wingedsheep.sdk.dsl.Targets.InstantSorcerySpellOrAbility]) — the
     * "copy target instant/sorcery spell, activated ability, or triggered ability" clause
     * (Return the Favor).
     */
    fun CopyTargetSpellOrAbility(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        com.wingedsheep.sdk.scripting.effects.CopyTargetSpellOrAbilityEffect(target)

    /**
     * When you next cast a spell matching [spellFilter] this turn, copy that spell.
     * You may choose new targets for the copies. Defaults to instant or sorcery.
     */
    fun CopyNextSpellCast(
        copies: Int = 1,
        spellFilter: GameObjectFilter = GameObjectFilter.InstantOrSorcery
    ): Effect = CopyNextSpellCastEffect(copies, spellFilter)

    /**
     * Until end of turn, whenever you cast a spell matching [spellFilter], copy it.
     * You may choose new targets for the copies. Defaults to instant or sorcery.
     */
    fun CopyEachSpellCast(
        copies: Int = 1,
        spellFilter: GameObjectFilter = GameObjectFilter.InstantOrSorcery
    ): Effect = CopyEachSpellCastEffect(copies, spellFilter)

    /**
     * The next spell matching [spellFilter] you cast this turn can't be countered (one-shot).
     * Unlike [GrantSpellsCantBeCountered], which protects every matching spell for a duration,
     * this protects only the next one. Defaults to any spell. Used by Mistrise Village.
     */
    fun MakeNextSpellUncounterable(
        spellFilter: GameObjectFilter = GameObjectFilter.Any
    ): Effect = com.wingedsheep.sdk.scripting.effects.MakeNextSpellUncounterableEffect(spellFilter)

    // =========================================================================
    // Sacrifice Effects
    // =========================================================================

    /**
     * Force a player to sacrifice permanents matching a filter.
     */
    fun Sacrifice(filter: GameObjectFilter, count: Int = 1, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
        ForceSacrificeEffect(filter, count, target)

    /**
     * Force a player to sacrifice a [DynamicAmount] of permanents matching a filter — e.g.
     * "sacrifices half the creatures they control, rounded up" (Rush of Dread). The amount is
     * evaluated at resolution against the resolving context, so a per-target player reference
     * (`Player.ContextPlayer(0)` / `Player.TargetOpponent`) counts the chosen player's permanents.
     */
    fun Sacrifice(filter: GameObjectFilter, count: DynamicAmount, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
        ForceSacrificeEffect(filter = filter, target = target, dynamicCount = count)

    /**
     * Sacrifice a specific permanent identified by target.
     * Used in delayed triggers where the exact permanent was determined at resolution time.
     */
    fun SacrificeTarget(target: EffectTarget): Effect =
        SacrificeTargetEffect(target)

    // =========================================================================
    // Reveal Effects
    // =========================================================================

    /**
     * "You may reveal a [filter] card from your hand" — atomic optional reveal.
     *
     * If the controller has no matching card in hand, no prompt is shown and
     * [otherwise] (if any) runs immediately. If they have one or more matches,
     * they're prompted to pick one to reveal; declining (or confirming with no
     * selection) runs [otherwise]. Revealing emits a `CardsRevealedEvent` and
     * stops there.
     *
     * Compose with [Tap]/[Sacrifice]/etc. via [otherwise] to express "if you
     * don't, X" riders — e.g. SOI shadow lands ("you may reveal a Mountain or
     * Forest from your hand; if you don't, this enters tapped").
     */
    fun MayRevealCardFromHand(
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter,
        otherwise: Effect? = null,
    ): Effect = com.wingedsheep.sdk.scripting.effects.MayRevealCardFromHandEffect(filter, otherwise)

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

    /**
     * Tap every creature/permanent chosen as a target ("tap up to N target creatures").
     *
     * Composes [ForEachTargetEffect] over [Effects.Tap], so the number of targets is owned
     * entirely by the spell's `TargetCreature`/`TargetPermanent` (its `count`, `unlimited`, or
     * `dynamicMaxCount`) — not duplicated on the effect. Used by Tidal Surge, Choking Tethers,
     * Eddymurk Crab, Icy Blast.
     */
    fun TapEachTarget(): Effect =
        com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect(
            listOf(TapUntapEffect(EffectTarget.ContextTarget(0), tap = true))
        )

    /**
     * Untap every permanent chosen as a target ("untap each of those creatures"). The untap twin of
     * [TapEachTarget]: composes [ForEachTargetEffect] over [Effects.Untap], so the number of targets is
     * owned entirely by the spell's `TargetCreature`/`TargetPermanent`, not duplicated on the effect.
     */
    fun UntapEachTarget(): Effect =
        com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect(
            listOf(TapUntapEffect(EffectTarget.ContextTarget(0), tap = false))
        )

    /**
     * Phase out a target permanent (Rule 702.26). It's treated as though it
     * doesn't exist until it phases back in before its controller's next untap step.
     */
    fun PhaseOut(target: EffectTarget = EffectTarget.Self): Effect =
        com.wingedsheep.sdk.scripting.effects.PhaseOutEffect(target)

    /**
     * Phase a target permanent out indefinitely, linked to the effect's source — it stays phased
     * out until the source leaves the battlefield (paired with [PhaseInLinkedToSource] on the
     * source's leaves trigger). The phasing analogue of `ExileUntilLeaves` (Oubliette). Set
     * [tapOnPhaseIn] to tap the permanent as it phases back in.
     */
    fun PhaseOutUntilLeaves(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        tapOnPhaseIn: Boolean = false
    ): Effect =
        com.wingedsheep.sdk.scripting.effects.PhaseOutUntilLeavesEffect(target, tapOnPhaseIn)

    /**
     * Phase in everything the effect's source phased out via [PhaseOutUntilLeaves]. Use on the
     * source's leaves-battlefield trigger (Oubliette).
     */
    fun PhaseInLinkedToSource(): Effect =
        com.wingedsheep.sdk.scripting.effects.PhaseInLinkedToSourceEffect

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
     * Target creature can't attack this turn.
     */
    fun CantAttack(target: EffectTarget = EffectTarget.ContextTarget(0), duration: Duration = Duration.EndOfTurn): Effect =
        CantAttackEffect(target, duration)

    /**
     * Target creature can't block this turn.
     */
    fun CantBlock(target: EffectTarget = EffectTarget.ContextTarget(0), duration: Duration = Duration.EndOfTurn): Effect =
        CantBlockEffect(target, duration)

    /**
     * Target creature can't attack or block this turn.
     */
    fun CantAttackOrBlock(target: EffectTarget = EffectTarget.ContextTarget(0), duration: Duration = Duration.EndOfTurn): Effect =
        CompositeEffect(listOf(CantAttackEffect(target, duration), CantBlockEffect(target, duration)))

    /**
     * Goad target creature (CR 701.15). Until the goader's next turn, the creature
     * attacks each combat if able and attacks a player other than the goader if able.
     * The goader is recorded as the controller of the effect at resolution; multiple
     * goaders stack, and a goader re-goading their own already-goaded creature is a
     * no-op (CR 701.15c-d).
     */
    fun Goad(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        GoadEffect(target)

    /**
     * Target creature becomes suspected (CR 701.60): atomic composite of the
     * named "suspected" status, granted menace, and "can't block".
     *
     * Sub-effects share a timestamp because [CompositeEffect] doesn't tick
     * `state.timestamp` between children, so Rule 613 layer ordering treats them
     * as one application. The named status is carried by [SetSuspectedEffect] so
     * future cards can still query or react to "becomes suspected" specifically.
     */
    fun Suspect(target: EffectTarget = EffectTarget.ContextTarget(0), duration: Duration = Duration.Permanent): Effect =
        CompositeEffect(
            effects = listOf(
                SetSuspectedEffect(target, duration),
                GrantKeywordEffect(Keyword.MENACE, target, duration),
                CantBlockEffect(target, duration)
            ),
            descriptionOverride = "${target.description} becomes suspected"
        )

    // =========================================================================
    // Special Effects
    // =========================================================================

    // =========================================================================
    // Player Restriction Effects
    // =========================================================================

    /**
     * Target player can't cast spells this turn.
     * Used for cards like Xantid Swarm.
     */
    fun CantCastSpells(target: EffectTarget, duration: Duration = Duration.EndOfTurn): Effect =
        CantCastSpellsEffect(target, duration)

    /**
     * A player can't play lands for the rest of this turn (sets remaining land drops to 0).
     * Defaults to the controller (Rock Jockey); pass a target for "target player can't
     * play lands this turn" cards like Turf Wound.
     */
    fun CantPlayLandsThisTurn(target: EffectTarget = EffectTarget.Controller): Effect =
        PreventLandPlaysThisTurnEffect(target)

    /**
     * Target player can't activate planeswalkers' loyalty abilities for the duration.
     * Compose with [CantCastSpells] for cards that forbid both (e.g. Revel in Silence).
     */
    fun CantActivateLoyaltyAbilities(target: EffectTarget, duration: Duration = Duration.EndOfTurn): Effect =
        CantActivateLoyaltyAbilitiesEffect(target, duration)

    /**
     * Target player skips their next turn.
     * Used for cards like Lethal Vapors.
     */
    fun SkipNextTurn(target: EffectTarget = EffectTarget.Controller): Effect =
        SkipNextTurnEffect(target)

    /**
     * Target player skips their next draw step.
     * Used for cards like Elfhame Sanctuary ("you skip your draw step this turn").
     */
    fun SkipNextDrawStep(target: EffectTarget = EffectTarget.Controller): Effect =
        SkipNextDrawStepEffect(target)

    /**
     * Controller controls the target player during that player's next turn (Mindslaver-style).
     * Used by The Dominion Bracelet. PR 1 ships as a no-op event; full mechanic lands later.
     */
    fun HijackNextTurn(target: EffectTarget = EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.TargetOpponent)): Effect =
        HijackNextTurnEffect(target)

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
     * Choose a source on resolution, prevent the next damage it would deal to you this turn, then run
     * [onPrevented] — an arbitrary follow-up effect — as a triggered ability when that damage is
     * prevented ("When damage is prevented this way, …"). Inside the follow-up the prevented amount is
     * [DynamicAmounts.preventedDamage] ("that much"/"that many") and the prevented source's controller
     * is `EffectTarget.ControllerOfTriggeringEntity` ("that source's controller"). Compose the payoff
     * from ordinary atomic effects — no bespoke reaction type: Deflecting Palm reflects
     * (`DealDamage(ControllerOfTriggeringEntity, preventedDamage())`); New Way Forward reflects and draws.
     */
    fun PreventNextDamageFromChosenSource(onPrevented: Effect): Effect =
        PreventDamageEffect(
            sourceFilter = PreventionSourceFilter.ChosenSource,
            onPrevented = onPrevented
        )

    /**
     * Choose a source, prevent the next damage it would deal to you this turn, and deal that much
     * damage to its controller (Deflecting Palm) — the canonical reflect, expressed as a follow-up.
     */
    fun DeflectNextDamageFromChosenSource(): Effect =
        PreventNextDamageFromChosenSource(
            onPrevented = DealDamageEffect(
                amount = DynamicAmounts.preventedDamage(),
                target = EffectTarget.ControllerOfTriggeringEntity
            )
        )

    /**
     * Choose a source; the next time it would deal damage to you this turn, the damage is still
     * dealt to you in full **and** that much damage is dealt to that source's controller (Eye for
     * an Eye). Same chosen-source reaction machinery as [DeflectNextDamageFromChosenSource], but
     * with `preventDamage = false` so the original damage is not prevented.
     */
    fun ReflectNextDamageFromChosenSourceToController(): Effect =
        PreventDamageEffect(
            sourceFilter = PreventionSourceFilter.ChosenSource,
            preventDamage = false,
            onPrevented = DealDamageEffect(
                amount = DynamicAmounts.preventedDamage(),
                target = EffectTarget.ControllerOfTriggeringEntity
            )
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
     * Prevent all damage that would be dealt to a target this turn by a source of your choice.
     * If [gainLifeFromColors] is non-empty, whenever damage from a source of one of those colors is
     * prevented this way, the controller gains that much life (Samite Ministration).
     */
    fun PreventAllDamageFromChosenSource(
        target: EffectTarget = EffectTarget.Controller,
        gainLifeFromColors: Set<com.wingedsheep.sdk.core.Color> = emptySet()
    ): Effect =
        PreventDamageEffect(
            target = target,
            amount = null,
            sourceFilter = PreventionSourceFilter.ChosenSource,
            gainLifeFromColors = gainLifeFromColors
        )

    /**
     * Prevent all damage that would be dealt to a target this turn by a source of your choice
     * that shares a color with the mana spent — i.e. only colored sources are eligible
     * (a colorless source shares a color with no mana). Protective Sphere.
     */
    fun PreventAllDamageFromChosenColoredSource(
        target: EffectTarget = EffectTarget.Controller
    ): Effect =
        PreventDamageEffect(
            target = target,
            amount = null,
            sourceFilter = PreventionSourceFilter.ChosenColoredSource
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
     *
     * @param unblockSoleBlockedAttackers If true, attackers the target was sole blocker
     *   of become unblocked (their `BlockedComponent` is cleared). Defaults to false; only
     *   set for cards whose oracle text explicitly says creatures become unblocked, e.g.
     *   Ydwen Efreet's "Creatures it was blocking that had become blocked by only this
     *   creature this combat become unblocked."
     */
    fun RemoveFromCombat(target: EffectTarget, unblockSoleBlockedAttackers: Boolean = false): Effect =
        RemoveFromCombatEffect(target, unblockSoleBlockedAttackers)

    /**
     * Let a creature attack this turn as though it didn't have defender (Krotiq Nestguard).
     * The activated/temporary counterpart to the static [com.wingedsheep.sdk.scripting.CanAttackDespiteDefender].
     */
    fun CanAttackDespiteDefenderThisTurn(target: EffectTarget = EffectTarget.Self): Effect =
        com.wingedsheep.sdk.scripting.effects.CanAttackDespiteDefenderThisTurnEffect(target)

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

    /**
     * Put a targeted Aura or Equipment card onto the battlefield attached to a permanent the
     * controller chooses at resolution (default: a creature you control). Works for both
     * Auras and Equipment; the host is chosen, not targeted (One Last Job).
     */
    fun PutOntoBattlefieldAttachedToChosen(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        hostFilter: com.wingedsheep.sdk.scripting.GameObjectFilter =
            com.wingedsheep.sdk.scripting.GameObjectFilter.Creature.youControl()
    ): Effect = com.wingedsheep.sdk.scripting.effects.PutOntoBattlefieldAttachedToChosenEffect(
        target, hostFilter
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
     * Earthbend N (TLA keyword action) — target land becomes a 0/0 creature-land
     * with haste, get N +1/+1 counters, and gains a triggered ability:
     * "When this dies or is exiled, return it to the battlefield tapped."
     *
     * Modeled as data — no new keyword. The return clause is a single granted
     * self-trigger on `from = BATTLEFIELD, to = null` (any leave). At resolution,
     * two zone-gated [MoveToZoneEffect]s run in sequence: one tries to move the
     * card from the graveyard back to the battlefield, the other from exile.
     * For non-grave/exile leaves (hand bounce, library shuffle), both branches
     * skip via `MoveToZoneEffect.fromZone` and the trigger resolves as a no-op,
     * matching the printed "When it dies or is exiled" reading in practice.
     *
     * Folding into a single grant keeps the FE active-effects badges quiet —
     * one "Granted Ability" tile while the land is earthbended, not two.
     */
    fun Earthbend(amount: Int, target: EffectTarget): Effect {
        val returnTapped = TriggeredAbility.create(
            trigger = EventPattern.ZoneChangeEvent(from = Zone.BATTLEFIELD, to = null),
            binding = TriggerBinding.SELF,
            effect = CompositeEffect(listOf(
                MoveToZoneEffect(
                    target = EffectTarget.Self,
                    destination = Zone.BATTLEFIELD,
                    placement = ZonePlacement.Tapped,
                    fromZone = Zone.GRAVEYARD
                ),
                MoveToZoneEffect(
                    target = EffectTarget.Self,
                    destination = Zone.BATTLEFIELD,
                    placement = ZonePlacement.Tapped,
                    fromZone = Zone.EXILE
                ),
            )),
            descriptionOverride = "When this dies or is exiled, return it to the battlefield tapped."
        )
        return CompositeEffect(listOf(
            AnimateLandEffect(target, 0, 0, Duration.Permanent),
            GrantKeywordEffect(Keyword.HASTE, target, Duration.Permanent),
            AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, amount, target),
            GrantTriggeredAbilityEffect(returnTapped, target, Duration.Permanent),
        ))
    }

    /**
     * Endure N (Tarkir: Dragonstorm keyword action) — the enduring permanent's
     * controller chooses one: put N +1/+1 counters on the enduring permanent,
     * or create an N/N white Spirit creature token.
     *
     * Modeled as data — no new keyword. "Endure N" never appears in a card's
     * keyword line; it is always the effect of a triggered or activated ability
     * ("Whenever this attacks, endure 2"), so it composes a
     * [ModalEffect.chooseOne] of the two existing halves: an
     * [AddDynamicCountersEffect] on the enduring permanent and a single N/N
     * white Spirit [CreateTokenEffect]. The mode choice is made by the ability's
     * controller at resolution time (the modal executor's resolution-time path).
     *
     * @param amount N — [DynamicAmount.Fixed] for "endure 2",
     *   [DynamicAmount.XValue] for "endures X" (Krumar Initiate), or any other
     *   dynamic value (Warden of the Grove endures "the number of counters on
     *   this creature").
     * @param target the permanent that endures. Defaults to [EffectTarget.Self]
     *   ("it"/"this creature endures"); cards like Warden of the Grove endure the
     *   triggering creature, so pass [EffectTarget.TriggeringEntity].
     */
    fun Endure(
        amount: DynamicAmount,
        target: EffectTarget = EffectTarget.Self
    ): Effect = ModalEffect.chooseOne(
        Mode.noTarget(
            AddDynamicCountersEffect(Counters.PLUS_ONE_PLUS_ONE, amount, target),
            "Put ${amount.description} +1/+1 counter(s) on ${target.description}"
        ),
        Mode.noTarget(
            CreateTokenEffect(
                count = DynamicAmount.Fixed(1),
                power = 0,
                toughness = 0,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Spirit"),
                dynamicPower = amount,
                dynamicToughness = amount,
                imageUri = ENDURE_SPIRIT_TOKEN_IMAGE
            ),
            "Create a ${amount.description}/${amount.description} white Spirit creature token"
        ),
        countsAsModalSpell = false
    )

    /** Endure N with a fixed [amount] — sugar for `Endure(DynamicAmount.Fixed(amount), target)`. */
    fun Endure(amount: Int, target: EffectTarget = EffectTarget.Self): Effect =
        Endure(DynamicAmount.Fixed(amount), target)

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

    /**
     * Target permanent becomes saddled until end of turn (CR 702.171b) — the resolving effect of
     * a Saddle ability. Defaults to the source, since Saddle always saddles its own permanent.
     */
    fun BecomeSaddled(target: EffectTarget = EffectTarget.Self): Effect =
        BecomeSaddledEffect(target)

    /**
     * Each permanent matching [filter] becomes a copy of [target].
     *
     * Used by Mirrorform ("Each nonland permanent you control becomes a copy of
     * target non-Aura permanent"). Copies printable/copiable characteristics only
     * (Rule 707) — counters, tapped state, and attached auras/equipment stay put.
     *
     * Pass [duration] = [Duration.EndOfTurn] for "until end of turn" copies that revert at
     * cleanup, and [excludeTarget] = true for "each **other** … becomes a copy of that …"
     * wordings where the copy source keeps its own identity (Naga Fleshcrafter's renew).
     *
     * Pass [affected] (another [EffectTarget], e.g. a second `ContextTarget`) for the
     * single-permanent "target permanent A becomes a copy of target permanent B" shape
     * (Fleeting Reflection) — then only that one resolved permanent becomes a copy of [target],
     * and [filter] / [excludeTarget] are ignored.
     */
    fun EachPermanentBecomesCopyOfTarget(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        filter: GroupFilter = GroupFilter(
            com.wingedsheep.sdk.scripting.GameObjectFilter.NonlandPermanent.youControl()
        ),
        duration: Duration = Duration.Permanent,
        excludeTarget: Boolean = false,
        affected: EffectTarget? = null,
    ): Effect = EachPermanentBecomesCopyOfTargetEffect(target, filter, duration, excludeTarget, affected)

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
     * Evaluate a dynamic amount once and store it under [name] in pipeline `storedNumbers`.
     * Later effects in the same composite can read it via
     * [DynamicAmount.VariableReference] — useful when a value (e.g., an X derived from
     * creature powers) must be frozen before sub-effects alter projected state.
     */
    fun StoreNumber(name: String, amount: DynamicAmount): Effect =
        com.wingedsheep.sdk.scripting.effects.StoreNumberEffect(name, amount)

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
     * "Note a creature type that hasn't been noted for this <source>."
     *
     * Pairs the choice (creature type, source's already-noted types excluded) with persistence
     * on the source permanent. After resolution: chosen type is in `chosenValues[storeAs]` for
     * downstream pipeline steps AND appended to the source's `NotedCreatureTypesComponent`.
     */
    fun NoteCreatureType(
        storeAs: String = "notedType",
        prompt: String? = null
    ): Effect = NoteCreatureTypeEffect(storeAs, prompt)

    /** The five basic land card names, excluded by "name a card other than a basic land card name" effects. */
    private val BASIC_LAND_CARD_NAMES = listOf("Plains", "Island", "Swamp", "Mountain", "Forest")

    /**
     * Name a card. The chosen name is stored in `chosenValues[storeAs]`; match cards
     * against it later with [com.wingedsheep.sdk.scripting.filters.unified.GameObjectFilter.namedFromVariable].
     * Used by "name a card …" effects (Desperate Research).
     *
     * @param excludeBasicLandNames When true, the five basic land card names are not
     *   offered ("other than a basic land card name").
     */
    fun ChooseCardName(
        storeAs: String = "chosenCardName",
        prompt: String? = null,
        excludeBasicLandNames: Boolean = false
    ): Effect = ChooseOptionEffect(
        optionType = OptionType.CARD_NAME,
        storeAs = storeAs,
        prompt = prompt,
        excludedOptions = if (excludeBasicLandNames) BASIC_LAND_CARD_NAMES else emptyList()
    )

    /**
     * Capture the name of the first card in stored collection [from] into
     * `chosenValues[storeAs]`. The "choose a card, then act on cards with that name"
     * counterpart to [ChooseCardName] (Lobotomy).
     */
    fun StoreCardName(from: String, storeAs: String = "chosenCardName"): Effect =
        com.wingedsheep.sdk.scripting.effects.StoreCardNameEffect(from, storeAs)

    /**
     * Choose a creature type. Creatures of the chosen type get +X/+Y until end of turn,
     * optionally gaining a keyword.
     */
    fun ChooseCreatureTypeModifyStats(
        powerModifier: DynamicAmount,
        toughnessModifier: DynamicAmount,
        duration: Duration = Duration.EndOfTurn,
        grantKeyword: Keyword? = null
    ): Effect = CreatureTypePatterns.chooseCreatureTypeModifyStats(powerModifier, toughnessModifier, duration, grantKeyword)

    /**
     * Choose a creature type. Creatures of the chosen type get +X/+Y until end of turn,
     * optionally gaining a keyword. Convenience overload with Int values.
     */
    fun ChooseCreatureTypeModifyStats(
        powerModifier: Int,
        toughnessModifier: Int,
        duration: Duration = Duration.EndOfTurn,
        grantKeyword: Keyword? = null
    ): Effect = CreatureTypePatterns.chooseCreatureTypeModifyStats(
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
        copyCost = com.wingedsheep.sdk.scripting.costs.PayCost.Atom(
            com.wingedsheep.sdk.scripting.costs.CostAtom.Sacrifice(
                filter = com.wingedsheep.sdk.scripting.GameObjectFilter.Land
            )
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
        copyCost = com.wingedsheep.sdk.scripting.costs.PayCost.Atom(
            com.wingedsheep.sdk.scripting.costs.CostAtom.Discard()
        ),
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
        action = HandPatterns.discardCards(count, target),
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
        copyCost = com.wingedsheep.sdk.scripting.costs.PayCost.Atom(
            com.wingedsheep.sdk.scripting.costs.CostAtom.Sacrifice(
                filter = com.wingedsheep.sdk.scripting.GameObjectFilter.Land
            )
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

    /**
     * Spells matching [spellFilter] that [target] casts can't be countered for [duration].
     * Used for Domri, Anarch of Bolas's +1 ("Creature spells you cast this turn can't be countered.").
     */
    fun GrantSpellsCantBeCountered(
        target: com.wingedsheep.sdk.scripting.targets.EffectTarget = com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller,
        spellFilter: com.wingedsheep.sdk.scripting.GameObjectFilter = com.wingedsheep.sdk.scripting.GameObjectFilter.Creature,
        duration: com.wingedsheep.sdk.scripting.Duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn
    ): Effect = GrantSpellsCantBeCounteredEffect(target, spellFilter, duration)

    /**
     * [target] may cast spells matching [spellFilter] as though they had flash for [duration].
     * Used for Borne Upon a Wind ("You may cast spells this turn as though they had flash.").
     * Sibling of the permanent-static [com.wingedsheep.sdk.scripting.GrantFlashToSpellType], which
     * lives on a battlefield permanent. Both are read by the cast-legality check; this Effect
     * survives the source spell leaving the stack and the static survives its source's static lifetime.
     */
    fun GrantFlashToSpells(
        target: com.wingedsheep.sdk.scripting.targets.EffectTarget = com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller,
        spellFilter: com.wingedsheep.sdk.scripting.GameObjectFilter = com.wingedsheep.sdk.scripting.GameObjectFilter.Any,
        duration: com.wingedsheep.sdk.scripting.Duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn
    ): Effect = GrantFlashToSpellsEffect(target, spellFilter, duration)
}
