package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.NotedCreatureTypesComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.NoteCreatureTypeEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [NoteCreatureTypeEffect].
 *
 * Reads the source permanent's current [NotedCreatureTypesComponent] (if any), uses its `types`
 * as the excluded set, and presents a `ChooseOptionDecision` to the controller. The resumer
 * ([com.wingedsheep.engine.handlers.continuations.CreatureTypeChoiceContinuationResumer.resumeNoteCreatureType])
 * writes the chosen type back to the source's component and into `EffectContext.chosenValues`.
 *
 * Fizzles silently if there is no source (the effect text "note … for this <permanent>" has no
 * meaning without one) or if every creature type has already been noted on this source (you
 * can't pick a fresh one).
 */
class NoteCreatureTypePipelineExecutor : EffectExecutor<NoteCreatureTypeEffect> {

    override val effectType: KClass<NoteCreatureTypeEffect> = NoteCreatureTypeEffect::class

    override fun execute(
        state: GameState,
        effect: NoteCreatureTypeEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: return EffectResult.success(state)
        val sourceContainer = state.getEntity(sourceId)
        val sourceName = sourceContainer?.get<CardComponent>()?.name
        val controllerId = context.controllerId

        val noted = sourceContainer?.get<NotedCreatureTypesComponent>()?.types ?: emptySet()
        val excludedLower = noted.map { it.lowercase() }.toSet()
        // An empty `options` means "any creature type"; a non-empty one narrows the offer to those
        // types ("secretly choose Human, Merfolk, or Goblin"). Already-noted types drop out of
        // whichever set that is, so the dedup rule holds for both shapes.
        val offered = effect.options.ifEmpty { Subtype.ALL_CREATURE_TYPES }
        val options = offered.filter { it.lowercase() !in excludedLower }

        if (options.isEmpty()) {
            return EffectResult.success(state)
        }

        val prompt = effect.prompt
            ?: if (effect.secret) "Secretly choose a creature type" else "Note a creature type"

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = options
        )

        val continuation = NoteCreatureTypePipelineContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            storeAs = effect.storeAs,
            options = options,
            secret = effect.secret
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }
}
