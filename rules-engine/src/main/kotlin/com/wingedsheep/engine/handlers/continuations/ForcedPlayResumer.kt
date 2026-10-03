package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.library.ForcePlayExecutor
import com.wingedsheep.sdk.scripting.effects.ForcePlayEffect

class ForcedPlayResumer(private val services: EngineServices) : ContinuationResumerModule, AutoResumerModule {
    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ForcedPlayContinuation::class) { state, frame, response, checkForMore ->
            val action = (response as? PlayCardResponse)?.action
                ?: return@resumer ExecutionResult.error(state, "Expected a card play")
            if (!state.isCurrentObject(frame.card) || action.playerId != frame.playerId) {
                return@resumer ExecutionResult.error(state, "The instructed card or player changed")
            }
            val result = when (action) {
                is CastSpell -> if (action.cardId == frame.card.entityId)
                    services.castSpellHandler.executeDuringResolution(state, action)
                    else ExecutionResult.error(state, "Play the instructed card")
                is PlayLand -> if (action.cardId == frame.card.entityId)
                    services.playLandHandler.executeDuringResolution(state, action)
                    else ExecutionResult.error(state, "Play the instructed card")
                else -> ExecutionResult.error(state, "Only a spell cast or land play is allowed")
            }
            if (result.error != null || result.pendingDecision != null) result else checkForMore(result.state, result.events)
        }
    )
    override fun autoResumers(): List<AutoResumer<*>> = listOf(
        autoResumer(FinishForcedPlayContinuation::class) { state, frame, events, checkForMore ->
            if (state.isCurrentObject(frame.card)) {
                // Cancelling a local casting choice does not decline a mandatory instruction.
                val retry = ForcePlayExecutor { services.legalActionEnumerator }.execute(state,
                    ForcePlayEffect(frame.from, storePlayedTo = frame.storePlayedTo),
                    frame.effectContext.copy(controllerId = frame.playerId))
                if (retry.pendingDecision != null) ExecutionResult.propagatePause(retry.state, events + retry.events)
                else checkForMore(retry.state, events + retry.events)
            } else {
                val collections = frame.storePlayedTo?.let { mapOf(it to listOf(frame.card.entityId)) }.orEmpty()
                checkForMore(exposeCollectionsToNextFrame(state, collections), events)
            }
        }
    )
}
