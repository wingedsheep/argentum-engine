package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.InvestigatedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.InvestigatedThisTurnComponent
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.InvestigateEffect
import kotlin.reflect.KClass

/**
 * Executor for [InvestigateEffect] — CR 701.16a, "create a Clue token", performed N times.
 *
 * The Clues go through [CreatePredefinedTokenExecutor] unchanged, so token-count replacements,
 * token substitutions and the [com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS] publication
 * behave exactly as for a plain "create a Clue token". On top of that it emits one
 * [InvestigatedEvent] per investigate — whatever the replacements did to the Clues, the player
 * still investigated — and marks the player with [InvestigatedThisTurnComponent] so only the first
 * investigate of the turn carries `firstThisTurn`.
 */
class InvestigateExecutor(
    private val createPredefinedToken: CreatePredefinedTokenExecutor,
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<InvestigateEffect> {

    override val effectType: KClass<InvestigateEffect> = InvestigateEffect::class

    override fun execute(
        state: GameState,
        effect: InvestigateEffect,
        context: EffectContext
    ): EffectResult {
        val times = amountEvaluator.evaluate(state, effect.count, context).coerceAtLeast(0)
        if (times == 0) return EffectResult.success(state)

        val investigatorId = effect.controller?.let { context.resolvePlayerTarget(it, state) }
            ?: context.controllerId

        val created = createPredefinedToken.execute(
            state,
            CreatePredefinedTokenEffect("Clue", count = times, controller = effect.controller),
            context
        )
        if (created.outcome is Outcome.Rejected) return created

        val alreadyInvestigated = created.state.getEntity(investigatorId)
            ?.has<InvestigatedThisTurnComponent>() == true
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val investigated = List(times) { i ->
            InvestigatedEvent(
                playerId = investigatorId,
                firstThisTurn = i == 0 && !alreadyInvestigated,
                sourceName = sourceName
            )
        }
        val marked = if (alreadyInvestigated) created.state
            else created.state.updateEntity(investigatorId) { it.with(InvestigatedThisTurnComponent) }

        return created.copy(state = marked, events = created.events + investigated)
    }
}
