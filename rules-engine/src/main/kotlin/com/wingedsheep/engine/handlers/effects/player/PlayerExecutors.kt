package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Module providing all player-related effect executors.
 *
 * PayOrSufferExecutor runs arbitrary suffer effects through the parent registry's execute
 * function, which the registry hands in at construction.
 */
class PlayerExecutors(
    /** The registry's re-entrant entry point, for the executors that run sub-effects. */
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val zones: ZoneTransitionService,
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val cardRegistry: CardRegistry,
    private val costPaymentService: () -> CostPaymentService
) : ExecutorModule {
    private val payOrSufferExecutor = PayOrSufferExecutor(
        zones,
        cardRegistry = cardRegistry,
        executeEffect = effectExecutor,
        costPaymentService = costPaymentService
    )

    private val openLifeBidExecutor = OpenLifeBidExecutor(executeEffect = effectExecutor)

    override fun executors(): List<EffectExecutor<*>> = listOf(
        AmassExecutor(effectExecutor, amountEvaluator = zones.predicateEvaluator.amounts),
        CollectEvidenceExecutor(zones, decisionHandler),
        CollectEvidenceChosenAmountExecutor(predicateEvaluator = zones.predicateEvaluator),
        AddAdditionalUpkeepStepsExecutor(amountEvaluator = zones.predicateEvaluator.amounts),
        AddAdditionalEndStepsExecutor(amountEvaluator = zones.predicateEvaluator.amounts),
        AddCombatPhaseExecutor(),
        AddMainPhaseExecutor(),
        AnyPlayerMayPayExecutor(executeEffect = effectExecutor, predicateEvaluator = zones.predicateEvaluator),
        CantActivateLoyaltyAbilitiesExecutor(),
        CantCastSpellsExecutor(),
        CantSearchLibrariesExecutor(),
        CantCastSpellsFromNonHandZonesExecutor(),
        CantPlayCardsFromHandExecutor(),
        ChooseNumberForSourceExecutor(decisionHandler),
        ChooseOpponentForSourceExecutor(),
        ChooseCardTypeForSourceExecutor(),
        CreateGlobalTriggeredAbilityExecutor(),
        CreatePermanentEmblemExecutor(),
        EachPlayerChoosesCreatureTypeExecutor(),
        EndTheTurnExecutor(),
        GainCitysBlessingExecutor(),
        ChangeSpeedExecutor(amountEvaluator = zones.predicateEvaluator.amounts),
        RemoveMaximumHandSizeExecutor(),
        ReduceMaximumHandSizeExecutor(amountEvaluator = zones.predicateEvaluator.amounts),
        GiftGivenExecutor(),
        ForagedExecutor(),
        GrantCastCreaturesFromGraveyardWithForageExecutor(),
        GrantFlashToSpellsExecutor(),
        GrantInstantSpeedLoyaltyAbilitiesExecutor(),
        GrantSpellKeywordExecutor(),
        GrantSpellsCantBeCounteredExecutor(),
        GrantDamageBonusExecutor(),
        GrantEvasionKeywordExecutor(),
        GrantPlayerProtectionExecutor(),
        HijackNextTurnExecutor(),
        ControlCombatDeclarationsExecutor(),
        LockLifeGainExecutor(),
        openLifeBidExecutor,
        LoseGameExecutor(predicateEvaluator = zones.predicateEvaluator),
        WinGameExecutor(predicateEvaluator = zones.predicateEvaluator),
        payOrSufferExecutor,
        PlayAdditionalLandsExecutor(),
        PreventLandPlaysThisTurnExecutor(),
        SecretBidExecutor(decisionHandler),
        SetDayNightExecutor(cardRegistry),
        SkipCombatPhasesExecutor(),
        SkipNextDrawStepExecutor(),
        SkipNextUntapStepExecutor(),
        SkipStepOrPhaseThisTurnExecutor(),
        PayAnyAmountOfLifeAsEntersExecutor(amountEvaluator = zones.predicateEvaluator.amounts),
        SkipNextTurnExecutor(amountEvaluator = zones.predicateEvaluator.amounts),
        SkipUntapExecutor(),
        TakeExtraTurnExecutor(),
        TheRingTemptsYouExecutor()
    )
}
