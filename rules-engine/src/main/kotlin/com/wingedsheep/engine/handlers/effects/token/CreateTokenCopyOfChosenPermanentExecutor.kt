package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EntersWithCountersHelper
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfChosenPermanentEffect
import kotlin.reflect.KClass

/**
 * Executor for CreateTokenCopyOfChosenPermanentEffect.
 *
 * Finds permanents matching the filter that the controller controls,
 * presents a selection decision, then creates a token copy of the chosen permanent.
 */
class CreateTokenCopyOfChosenPermanentExecutor(
    private val cardRegistry: CardRegistry,
    private val staticAbilityHandler: StaticAbilityHandler? = null,
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<CreateTokenCopyOfChosenPermanentEffect> {

    override val effectType: KClass<CreateTokenCopyOfChosenPermanentEffect> =
        CreateTokenCopyOfChosenPermanentEffect::class

    override fun execute(
        state: GameState,
        effect: CreateTokenCopyOfChosenPermanentEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val sourceId = context.sourceId
        val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // Find matching permanents the controller controls
        val filter = effect.filter.youControl()
        val candidates = BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, filter, PredicateContext(controllerId = controllerId)
        )

        if (candidates.isEmpty()) {
            return EffectResult.success(state)
        }

        if (candidates.size == 1) {
            // Auto-select the only option
            return createTokenCopy(state, candidates.first(), controllerId)
        }

        // Present choice to the player
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = "Choose a ${effect.filter.description} you control to copy",
            options = candidates,
            minSelections = 1,
            maxSelections = 1,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            useTargetingUI = true
        )

        val continuation = CreateTokenCopyOfChosenContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            controllerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    companion object {
        /**
         * Create a token copy of the chosen permanent, used by both the executor
         * (auto-select) and the continuation resumer.
         */
        fun createTokenCopy(
            state: GameState,
            chosenId: EntityId,
            controllerId: EntityId,
            staticAbilityHandler: StaticAbilityHandler? = null
        ): EffectResult {
            val chosenContainer = state.getEntity(chosenId)
                ?: return EffectResult.success(state)

            val chosenCard = chosenContainer.get<CardComponent>()
                ?: return EffectResult.success(state)

            val tokenId = EntityId.generate()

            // Copy the chosen permanent's CardComponent
            val tokenCard = chosenCard.copy(ownerId = controllerId)

            var container = ComponentContainer.of(
                tokenCard,
                TokenComponent,
                ControllerComponent(controllerId),
                SummoningSicknessComponent
            )

            // Add static abilities from the card definition
            if (staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponent(container)
                container = staticAbilityHandler.addReplacementEffectComponent(container)
            }

            var newState = state.withEntity(tokenId, container)
            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)

            val event = ZoneChangeEvent(
                entityId = tokenId,
                entityName = tokenCard.name,
                fromZone = null,
                toZone = Zone.BATTLEFIELD,
                ownerId = controllerId
            )

            // Apply "enters with counters" replacement effects from other battlefield permanents
            // (e.g., Gev, Scaled Scorch granting +1/+1 counters to token copies).
            val (stateWithCounters, counterEvents) = EntersWithCountersHelper.applyGlobalEntersWithCounters(
                newState, tokenId, controllerId
            )

            return EffectResult.success(stateWithCounters, listOf(event) + counterEvents)
        }
    }
}
