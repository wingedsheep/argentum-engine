package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Module providing all player-related effect executors.
 *
 * Uses deferred initialization to inject the parent registry's execute function
 * into PayOrSufferExecutor, which needs it for executing arbitrary suffer effects.
 */
class PlayerExecutors(
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val cardRegistry: CardRegistry
) : ExecutorModule {
    private lateinit var effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult

    private val payOrSufferExecutor by lazy {
        PayOrSufferExecutor(cardRegistry = cardRegistry, executeEffect = effectExecutor)
    }

    /**
     * Initialize the module with the parent registry's execute function.
     * Must be called before executors() is accessed.
     */
    fun initialize(executor: (GameState, Effect, EffectContext) -> ExecutionResult) {
        this.effectExecutor = executor
    }

    override fun executors(): List<EffectExecutor<*>> = listOf(
        AddCombatPhaseExecutor(),
        AnyPlayerMayPayExecutor(),
        CantCastSpellsExecutor(),
        CreateGlobalTriggeredAbilityUntilEndOfTurnExecutor(),
        CreateGlobalTriggeredAbilityWithDurationExecutor(),
        CreatePermanentGlobalTriggeredAbilityExecutor(),
        EachPlayerChoosesCreatureTypeExecutor(),
        GiftGivenExecutor(),
        GrantCastCreaturesFromGraveyardWithForageExecutor(),
        GrantSpellKeywordExecutor(),
        GrantDamageBonusExecutor(),
        GrantHexproofExecutor(),
        GrantShroudExecutor(),
        LoseGameExecutor(),
        payOrSufferExecutor,
        PlayAdditionalLandsExecutor(),
        PreventLandPlaysThisTurnExecutor(),
        SecretBidExecutor(decisionHandler),
        SkipCombatPhasesExecutor(),
        SkipNextTurnExecutor(),
        SkipUntapExecutor(),
        TakeExtraTurnExecutor()
    )
}
