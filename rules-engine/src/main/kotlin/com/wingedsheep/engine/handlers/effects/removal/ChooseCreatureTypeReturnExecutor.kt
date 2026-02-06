package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.ChooseCreatureTypeReturnFromGraveyardEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ChooseCreatureTypeReturnFromGraveyardEffect.
 *
 * "Return up to three target creature cards of the creature type of your choice
 * from your graveyard to your hand."
 *
 * This executor:
 * 1. Scans the controller's graveyard for creature cards
 * 2. Collects distinct creature subtypes present
 * 3. Presents a ChooseOptionDecision with those types
 * 4. Pushes a ChooseCreatureTypeReturnContinuation for the next step
 */
class ChooseCreatureTypeReturnExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ChooseCreatureTypeReturnFromGraveyardEffect> {

    override val effectType: KClass<ChooseCreatureTypeReturnFromGraveyardEffect> =
        ChooseCreatureTypeReturnFromGraveyardEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseCreatureTypeReturnFromGraveyardEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
        val graveyard = state.getZone(graveyardZone)

        // Collect distinct creature subtypes from creatures in graveyard
        val creatureTypes = mutableSetOf<String>()
        for (cardId in graveyard) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            val typeLine = cardComponent.typeLine ?: continue
            if (typeLine.isCreature) {
                for (subtype in typeLine.subtypes) {
                    creatureTypes.add(subtype.value)
                }
            }
        }

        // If no creature types in graveyard, nothing to do
        if (creatureTypes.isEmpty()) {
            return ExecutionResult.success(state.tick())
        }

        val sortedTypes = creatureTypes.sorted()
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // Create a choose-option decision for the creature type
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
            options = sortedTypes
        )

        // Push continuation
        val continuation = ChooseCreatureTypeReturnContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            count = effect.count,
            creatureTypes = sortedTypes
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
