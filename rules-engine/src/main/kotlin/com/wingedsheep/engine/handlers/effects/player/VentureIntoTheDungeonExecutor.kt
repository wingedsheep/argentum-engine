package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.mechanics.dungeon.VentureRestrictions
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DungeonCardComponent
import com.wingedsheep.engine.mechanics.initiative.InitiativeService
import com.wingedsheep.sdk.scripting.effects.VentureIntoTheDungeonEffect
import kotlin.reflect.KClass

/**
 * Resolves [VentureIntoTheDungeonEffect] (CR 701.49). [DungeonService.venture] does the work; this
 * executor only resolves the venturing player and turns a choice the venture needs — which dungeon
 * (CR 701.49a), which arrow (CR 309.5a) — into a pause the
 * [com.wingedsheep.engine.handlers.continuations.DungeonContinuationResumer] resumes.
 *
 * A player who has left the game doesn't venture, and neither does one a
 * [com.wingedsheep.sdk.scripting.RestrictVenturesPerTurn] cap has used up this turn — that venture
 * simply doesn't happen.
 */
class VentureIntoTheDungeonExecutor(
    private val cardRegistry: CardRegistry,
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<VentureIntoTheDungeonEffect> {

    override val effectType: KClass<VentureIntoTheDungeonEffect> = VentureIntoTheDungeonEffect::class

    override fun execute(
        state: GameState,
        effect: VentureIntoTheDungeonEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)
        if (playerId !in state.activePlayers) return EffectResult.success(state)
        if (VentureRestrictions.cantVenture(state, playerId, cardRegistry, predicateEvaluator)) {
            return EffectResult.success(state)
        }

        val sourceName = sourceName(state, context)
        return when (val step = DungeonService.venture(state, playerId, effect.into, sourceName, context.sourceId)) {
            is DungeonService.Step.Done -> EffectResult.success(step.state, step.events)
            is DungeonService.Step.Choose ->
                EffectResult.from(step.state.suspendForDecision(step.decision, step.continuation, step.events))
        }
    }

    /**
     * The card that made the player venture; a room ability's source is the dungeon card, and the
     * initiative's inherent ventures have the player as their source.
     */
    private fun sourceName(state: GameState, context: EffectContext): String {
        val source = context.sourceId?.let { state.getEntity(it) }
        return source?.get<CardComponent>()?.name
            ?: source?.get<DungeonCardComponent>()?.dungeon
            ?: if (context.sourceId != null && context.sourceId in state.turnOrder) InitiativeService.SOURCE_NAME
            else "Venture into the dungeon"
    }
}
