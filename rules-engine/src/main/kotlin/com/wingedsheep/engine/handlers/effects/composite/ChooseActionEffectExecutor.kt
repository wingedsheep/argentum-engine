package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [ChooseActionEffect].
 *
 * Presents a player with labeled options and executes the chosen effect.
 * Infeasible options are filtered out. If only one remains, it auto-executes.
 * If zero remain, nothing happens.
 */
class ChooseActionEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<ChooseActionEffect> {

    override val effectType: KClass<ChooseActionEffect> = ChooseActionEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: ChooseActionEffect,
        context: EffectContext
    ): EffectResult {
        // Resolve who makes the choice
        val choosingPlayerId = context.resolvePlayerTarget(effect.player)
            ?: return EffectResult.error(state, "Could not resolve player for ChooseActionEffect")

        // Filter to feasible choices
        val feasibleChoices = effect.choices.filter { choice ->
            isFeasible(state, choosingPlayerId, choice.feasibilityCheck)
        }

        if (feasibleChoices.isEmpty()) {
            return EffectResult.success(state)
        }

        // If only one option, auto-execute it
        if (feasibleChoices.size == 1) {
            return effectExecutor(state, feasibleChoices[0].effect, context)
        }

        // Present options to the choosing player
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = choosingPlayerId,
            prompt = "Choose one for ${sourceName ?: "ability"}",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = feasibleChoices.map { it.label }
        )

        val continuation = ChooseActionContinuation(
            decisionId = decisionId,
            choosingPlayerId = choosingPlayerId,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            choices = feasibleChoices,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets,
            opponentId = context.opponentId,
            triggeringEntityId = context.triggeringEntityId
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = choosingPlayerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }

    private fun isFeasible(
        state: GameState,
        playerId: com.wingedsheep.sdk.model.EntityId,
        check: FeasibilityCheck?
    ): Boolean = checkFeasibility(state, playerId, check, predicateEvaluator)
}

/**
 * Check whether a [FeasibilityCheck] is satisfied for the given player.
 * Shared by [ChooseActionEffectExecutor], [MayEffectExecutor], and [ReflexiveTriggerEffectExecutor].
 */
internal fun checkFeasibility(
    state: GameState,
    playerId: com.wingedsheep.sdk.model.EntityId,
    check: FeasibilityCheck?,
    predicateEvaluator: PredicateEvaluator = PredicateEvaluator()
): Boolean {
    if (check == null) return true

    return when (check) {
        is FeasibilityCheck.ControlsPermanentMatching -> {
            val matching = BattlefieldFilterUtils.findMatchingOnBattlefield(
                state,
                check.filter.youControl(),
                PredicateContext(controllerId = playerId)
            )
            matching.size >= check.count
        }
        is FeasibilityCheck.HasCardsInZone -> {
            val zoneKey = ZoneKey(playerId, check.zone)
            val cards = state.getZone(zoneKey)
            if (check.filter == com.wingedsheep.sdk.scripting.GameObjectFilter.Any) {
                cards.size >= check.count
            } else {
                val context = PredicateContext(controllerId = playerId)
                cards.count { cardId ->
                    predicateEvaluator.matches(state, cardId, check.filter, context)
                } >= check.count
            }
        }
    }
}
