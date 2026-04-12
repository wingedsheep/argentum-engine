package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.scripting.effects.PutOnTopOrBottomOfLibraryEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for PutOnTopOrBottomOfLibraryEffect.
 *
 * Pauses for the target creature's owner to choose top or bottom of their library,
 * then delegates the actual zone move to the continuation resumer.
 */
class PutOnTopOrBottomOfLibraryExecutor : EffectExecutor<PutOnTopOrBottomOfLibraryEffect> {

    override val effectType: KClass<PutOnTopOrBottomOfLibraryEffect> = PutOnTopOrBottomOfLibraryEffect::class

    override fun execute(
        state: GameState,
        effect: PutOnTopOrBottomOfLibraryEffect,
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

        val options = listOf("Top of library", "Bottom of library")
        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = ownerId,
            prompt = "Put ${cardComponent.name} on top or bottom of your library",
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
            options = options
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
