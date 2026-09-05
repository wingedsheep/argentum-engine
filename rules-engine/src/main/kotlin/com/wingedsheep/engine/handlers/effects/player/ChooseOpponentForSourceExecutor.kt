package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ChooseOpponentForSourceContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.effects.ChooseOpponentForSourceEffect
import kotlin.reflect.KClass

/**
 * Executor for [ChooseOpponentForSourceEffect].
 *
 * Records the controller's chosen opponent on the source entity's cast-choices bag under
 * [ChoiceSlot.OPPONENT], where [com.wingedsheep.sdk.scripting.references.Player.ChosenOpponent]
 * reads it back (gift recipient). With a single opponent the choice is forced and recorded
 * without a prompt — 2-player games never see a decision. With several opponents it pauses
 * for a [ChooseOptionDecision] and the [ChooseOpponentForSourceContinuation] resumer writes
 * the pick. With no source entity or no opponents the effect is a no-op.
 */
class ChooseOpponentForSourceExecutor : EffectExecutor<ChooseOpponentForSourceEffect> {

    override val effectType: KClass<ChooseOpponentForSourceEffect> = ChooseOpponentForSourceEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseOpponentForSourceEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: return EffectResult.success(state)
        val source = state.getEntity(sourceId)

        val opponents = state.getOpponents(context.controllerId)
        if (opponents.isEmpty()) return EffectResult.success(state)

        // Sole opponent → forced choice, no prompt.
        if (opponents.size == 1) {
            val newState = if (source != null && context.objectReferences.isCurrent(context.objectReferences.source, state)) state.updateEntity(sourceId) { container ->
                container.withCastChoice(ChoiceSlot.OPPONENT, ChoiceValue.EntityChoice(opponents.single()))
            } else state
            return EffectResult.success(newState).copy(updatedCollections = mapOf(
                com.wingedsheep.engine.handlers.RESOLUTION_CHOSEN_OPPONENT to listOf(opponents.single()),
            ))
        }

        val opponentNames = opponents.map { pid ->
            state.getEntity(pid)?.get<PlayerComponent>()?.name ?: "Player ${pid.value}"
        }
        val decisionId = "choose-opponent-for-source-${sourceId.value}"
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = effect.prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = source?.get<CardComponent>()?.name ?: "Unknown",
                phase = DecisionPhase.RESOLUTION
            ),
            options = opponentNames
        )
        val continuation = ChooseOpponentForSourceContinuation(
            decisionId = decisionId,
            sourceId = sourceId,
            objectReferences = context.objectReferences,
            controllerId = context.controllerId,
            opponentIds = opponents
        )

        return EffectResult.paused(
            state.withPendingDecision(decision).pushContinuation(continuation),
            decision
        )
    }
}
