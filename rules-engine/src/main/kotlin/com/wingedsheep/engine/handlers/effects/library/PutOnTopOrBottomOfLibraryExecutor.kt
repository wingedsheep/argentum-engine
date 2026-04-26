package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.scripting.effects.PutOnLibraryPositionOfChoiceEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for PutOnLibraryPositionOfChoiceEffect.
 *
 * Pauses for the target's owner to choose between the offered library positions
 * (e.g., top/bottom for Hinder, second-from-top/bottom for Temporal Cleansing),
 * then delegates the actual zone move to the continuation resumer. Accepts both
 * battlefield permanents (e.g., Dire Downdraft) and spells on the stack
 * (e.g., Swat Away's "target spell or creature") — the resumer handles each
 * case appropriately.
 */
class PutOnTopOrBottomOfLibraryExecutor : EffectExecutor<PutOnLibraryPositionOfChoiceEffect> {

    override val effectType: KClass<PutOnLibraryPositionOfChoiceEffect> = PutOnLibraryPositionOfChoiceEffect::class

    override fun execute(
        state: GameState,
        effect: PutOnLibraryPositionOfChoiceEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for put on top or bottom of library")

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target entity not found: $targetId")

        val cardComponent = container.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card: $targetId")

        val ownerId = container.get<OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return EffectResult.error(state, "Cannot determine card owner")

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val positions = effect.positions.ifEmpty {
            return EffectResult.error(state, "PutOnLibraryPositionOfChoiceEffect requires at least one position")
        }
        val options = positions.map { it.label }
        val promptPhrase = positions.joinToString(" or ") {
            it.label.replaceFirstChar { c -> c.lowercase() }
        }
        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = ownerId,
            prompt = "Put ${cardComponent.name} on $promptPhrase",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = options
        )

        val continuation = PutOnTopOrBottomContinuation(
            decisionId = decisionId,
            ownerId = ownerId,
            cardId = targetId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            options = options,
            positions = positions
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = ownerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }
}
