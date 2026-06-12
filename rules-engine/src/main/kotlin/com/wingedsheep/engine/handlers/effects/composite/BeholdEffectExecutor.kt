package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.BeholdContinuation
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.BeholdEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [BeholdEffect] — the resolution-time "you may behold a [filter]" choice.
 *
 * Flow:
 *  1. Compute the beholder's eligible objects: matching permanents they control (projected
 *     state) and matching cards in their hand (base state, matching the cast-time Behold
 *     convention in [com.wingedsheep.engine.handlers.CostHandler]).
 *  2. If none are eligible, the player can't behold — skip the prompt and don't run `ifBeheld`.
 *  3. Otherwise present a [SelectCardsDecision] with `minSelections = 0, maxSelections = 1` over
 *     the union. The player either beholds one object or submits an empty selection (declines).
 *  4. On behold: if the chosen object is a hand card, emit the public reveal; either way, run
 *     `effect.ifBeheld` (the payoff, e.g. create a Treasure token).
 *  5. On decline: nothing happens.
 *
 * @param effectExecutor sub-effect runner provided by the registry, used to chain into
 *   `ifBeheld` (which itself may pause).
 */
class BeholdEffectExecutor(
    private val effectExecutor: (GameState, com.wingedsheep.sdk.scripting.effects.Effect, EffectContext) -> EffectResult,
    private val predicateEvaluator: PredicateEvaluator = PredicateEvaluator(),
) : EffectExecutor<BeholdEffect> {

    override val effectType: KClass<BeholdEffect> = BeholdEffect::class

    override fun execute(
        state: GameState,
        effect: BeholdEffect,
        context: EffectContext,
    ): EffectResult {
        val beholder = context.controllerId
        val predicateContext = PredicateContext(
            controllerId = beholder,
            sourceId = context.sourceId,
            targetOpponentId = context.targets.firstNotNullOfOrNull {
                (it as? com.wingedsheep.engine.state.components.stack.ChosenTarget.Player)?.playerId
            },
        )

        val projected = state.projectedState
        val battlefieldMatches = projected.getBattlefieldControlledBy(beholder).filter { permId ->
            predicateEvaluator.matches(state, projected, permId, effect.filter, predicateContext)
        }
        val handMatches = state.getHand(beholder).filter { cardId ->
            predicateEvaluator.matches(state, state.projectedState, cardId, effect.filter, predicateContext)
        }

        val options = (battlefieldMatches + handMatches).distinct()
        if (options.isEmpty()) {
            // Can't behold — the "if you do" payoff doesn't run.
            return EffectResult.success(state)
        }

        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val decisionId = UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = beholder,
            prompt = "You may behold a ${effect.filter.description}",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION,
            ),
            options = options,
            minSelections = 0,
            maxSelections = 1,
        )

        val continuation = BeholdContinuation(
            decisionId = decisionId,
            beholderId = beholder,
            sourceName = sourceName,
            handOptionIds = handMatches.toSet(),
            ifBeheld = effect.ifBeheld,
            effectContext = context,
        )

        val paused = state
            .pushContinuation(continuation)
            .withPendingDecision(decision)
        return EffectResult.paused(paused, decision)
    }
}
