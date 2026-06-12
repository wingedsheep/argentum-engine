package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.scripting.effects.ChoosePileEffect
import com.wingedsheep.sdk.scripting.effects.Chooser
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [ChoosePileEffect].
 *
 * Reads two named card collections from the pipeline, presents a binary
 * [ChooseOptionDecision] to the configured [Chooser], and (on response)
 * routes the picked pile to [ChoosePileEffect.storeChosenAs] and the other
 * to [ChoosePileEffect.storeOtherAs].
 */
class ChoosePileExecutor : EffectExecutor<ChoosePileEffect> {

    override val effectType: KClass<ChoosePileEffect> = ChoosePileEffect::class

    override fun execute(
        state: GameState,
        effect: ChoosePileEffect,
        context: EffectContext
    ): EffectResult {
        val pileA = context.pipeline.storedCollections[effect.pileA] ?: emptyList()
        val pileB = context.pipeline.storedCollections[effect.pileB] ?: emptyList()

        val deciderId = when (effect.chooser) {
            Chooser.Controller -> context.controllerId
            Chooser.Opponent -> state.getOpponents(context.controllerId).firstOrNull()
                ?: return EffectResult.error(state, "No opponent for ChoosePile chooser")
            Chooser.TargetPlayer -> context.targets.firstOrNull()?.let {
                TargetResolutionUtils.run { it.toEntityId() }
            } ?: return EffectResult.error(state, "No target player for ChoosePile chooser")
            Chooser.TriggeringPlayer -> context.triggeringEntityId
                ?: return EffectResult.error(state, "No triggering player for ChoosePile chooser")
            Chooser.SourceController -> {
                val sourceId = context.sourceId
                    ?: return EffectResult.error(state, "No source entity for ChoosePile chooser")
                state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                    ?: return EffectResult.error(state, "Source entity has no ControllerComponent for ChoosePile chooser")
            }
            Chooser.ControllerOfSelection -> {
                val deriveFrom = (pileA + pileB).firstOrNull()
                    ?: return EffectResult.error(state, "No card to derive controller for ChoosePile ControllerOfSelection chooser")
                state.projectedState.getController(deriveFrom)
                    ?: state.getEntity(deriveFrom)?.get<ControllerComponent>()?.playerId
                    ?: return EffectResult.error(state, "Could not resolve controller for ChoosePile ControllerOfSelection chooser")
            }
            Chooser.ControllerOfTarget -> {
                val targetId = context.targets.firstOrNull()?.let {
                    TargetResolutionUtils.run { it.toEntityId() }
                } ?: return EffectResult.error(state, "No target for ChoosePile ControllerOfTarget chooser")
                state.getEntity(targetId)?.get<ControllerComponent>()?.playerId
                    ?: state.getEntity(targetId)?.get<CardComponent>()?.ownerId
                    ?: return EffectResult.error(state, "Could not resolve controller for ChoosePile ControllerOfTarget chooser")
            }
        }

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val cardInfo = (pileA + pileB).associateWith { cardId ->
            val cc = state.getEntity(cardId)?.get<CardComponent>()
            SearchCardInfo(
                name = cc?.name ?: "Unknown",
                manaCost = cc?.manaCost?.toString() ?: "",
                typeLine = cc?.typeLine?.toString() ?: "",
                imageUri = null,
                colors = cc?.colors?.map { it.name }?.toList() ?: emptyList()
            )
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = deciderId,
            prompt = effect.prompt ?: "Choose a pile to keep",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = listOf(effect.pileALabel, effect.pileBLabel),
            optionCardIds = mapOf(
                0 to pileA,
                1 to pileB
            )
        )

        val continuation = ChoosePileContinuation(
            decisionId = decisionId,
            playerId = deciderId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            pileAIds = pileA,
            pileBIds = pileB,
            pileAName = effect.pileA,
            pileBName = effect.pileB,
            storeChosenAs = effect.storeChosenAs,
            storeOtherAs = effect.storeOtherAs,
            storedCollections = context.pipeline.storedCollections
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = deciderId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }
}
