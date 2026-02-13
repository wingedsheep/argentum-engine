package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.SurveilEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for SurveilEffect.
 * "Surveil N" - Look at the top N cards of your library, then put any number of them
 * into your graveyard and the rest on top of your library in any order.
 *
 * Uses a SplitPilesDecision with two piles:
 * - Pile 0 = top of library (cards to keep on top, in order)
 * - Pile 1 = graveyard (cards to put into graveyard)
 */
class SurveilExecutor : EffectExecutor<SurveilEffect> {

    override val effectType: KClass<SurveilEffect> = SurveilEffect::class

    override fun execute(
        state: GameState,
        effect: SurveilEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val library = state.getZone(libraryZone)

        val topCards = library.take(effect.count)

        if (topCards.isEmpty()) {
            return ExecutionResult.success(state.tick())
        }

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decisionId = UUID.randomUUID().toString()

        val decision = SplitPilesDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Surveil ${effect.count}: Put cards into your graveyard or on top of your library",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            cards = topCards,
            numberOfPiles = 2,
            pileLabels = listOf("Top of Library", "Graveyard")
        )

        val continuation = SurveilContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "SPLIT_PILES",
                    prompt = decision.prompt
                )
            )
        )
    }
}
