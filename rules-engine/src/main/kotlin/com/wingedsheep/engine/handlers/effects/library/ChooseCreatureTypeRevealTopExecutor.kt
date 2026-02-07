package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.ChooseCreatureTypeRevealTopEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ChooseCreatureTypeRevealTopEffect.
 *
 * "Choose a creature type. Reveal the top card of your library.
 * If that card is a creature card of the chosen type, put it into your hand.
 * Otherwise, put it into your graveyard."
 *
 * This executor:
 * 1. Checks that the controller's library is non-empty
 * 2. Presents a ChooseOptionDecision with all creature types
 * 3. Pushes a ChooseCreatureTypeRevealTopContinuation for the next step
 */
class ChooseCreatureTypeRevealTopExecutor : EffectExecutor<ChooseCreatureTypeRevealTopEffect> {

    override val effectType: KClass<ChooseCreatureTypeRevealTopEffect> =
        ChooseCreatureTypeRevealTopEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseCreatureTypeRevealTopEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val libraryZone = ZoneKey(controllerId, Zone.LIBRARY)
        val library = state.getZone(libraryZone)

        // If the library is empty, nothing happens
        if (library.isEmpty()) {
            return ExecutionResult.success(state.tick())
        }

        val allCreatureTypes = Subtype.ALL_CREATURE_TYPES
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a creature type",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = allCreatureTypes
        )

        val continuation = ChooseCreatureTypeRevealTopContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            creatureTypes = allCreatureTypes
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
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
