package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EntersWithReplacements
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
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
            return createTokenCopy(state, candidates.first(), controllerId, staticAbilityHandler, cardRegistry)
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
            staticAbilityHandler: StaticAbilityHandler? = null,
            cardRegistry: CardRegistry? = null
        ): EffectResult {
            val chosenContainer = state.getEntity(chosenId)
                ?: return EffectResult.success(state)

            val chosenCard = chosenContainer.get<CardComponent>()
                ?: return EffectResult.success(state)

            val (tokenId, stateWithId) = state.newEntity()

            // Copy the chosen permanent's CardComponent
            val tokenCard = chosenCard.copy(ownerId = controllerId)

            var container = ComponentContainer.of(
                tokenCard,
                TokenComponent,
                ControllerComponent(controllerId),
                SummoningSicknessComponent
            )

            // CR 707.8a: a token copy of a double-faced permanent has both faces and enters
            // with the same face up as the source.
            chosenContainer.get<DoubleFacedComponent>()?.let { sourceDfc ->
                container = container.with(
                    DoubleFacedComponent(
                        frontCardDefinitionId = sourceDfc.frontCardDefinitionId,
                        backCardDefinitionId = sourceDfc.backCardDefinitionId,
                        currentFace = sourceDfc.currentFace
                    )
                )
            }

            // Add static abilities from the card definition
            if (staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponent(container)
                container = staticAbilityHandler.addReplacementEffectComponent(container)
            }

            var newState = stateWithId.withEntity(tokenId, container)
            newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
                .place(newState, controllerId, tokenId)

            // A token copy honors global "[filter] enter tapped" replacements (Authority of the
            // Consuls taps an opponent's token copy of a creature).
            newState = com.wingedsheep.engine.handlers.effects.EnterTappedReplacements
                .applyCreatedTokenEntryTap(newState, tokenId, controllerId)

            // As-enters "enters with counters" (CR 614.1c): the copied card's own EntersWithCounters
            // (a copy of a creature that "enters with a +1/+1 counter") plus global grants from other
            // permanents (Gev, Scaled Scorch). Fall back to the global-only path when no registry is
            // available (the token's own definition can't be resolved without it).
            val (stateWithCounters, counterEvents) = if (cardRegistry != null) {
                EntersWithReplacements.applyOnEntry(newState, tokenId, controllerId, cardRegistry)
            } else {
                EntersWithReplacements.applyGlobal(newState, tokenId, controllerId)
            }
            newState = stateWithCounters

            // As-enters "choose X as this enters" (CR 614.12) + granted riot (CR 702.136): pause for
            // the player's decision. Exactly one token is created here, so there is no batch to
            // resume; the entry ZoneChangeEvent is omitted on a pause (the choice resumer synthesizes
            // it after the choice resolves so ETB triggers fire once). Counters ride along.
            if (cardRegistry != null) {
                val choicePlan = TokenEntryReplacements.firstEntersWithChoice(newState, tokenId, cardRegistry)
                if (choicePlan != null) {
                    val paused = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                        .pauseForEntersWithChoice(
                            state = newState,
                            entityId = tokenId,
                            controllerId = controllerId,
                            cardComponent = tokenCard,
                            choice = choicePlan.choice,
                            fromZone = null,
                            carryEvents = counterEvents,
                            syntheticRiot = choicePlan.syntheticRiot,
                            syntheticRiotRemaining = choicePlan.syntheticRiotRemaining,
                        )
                    if (paused != null) return EffectResult.from(paused)
                }
            }

            val event = ZoneChangeEvent(
                entityId = tokenId,
                entityName = tokenCard.name,
                fromZone = null,
                toZone = Zone.BATTLEFIELD,
                ownerId = controllerId
            )

            // CR 714.2b/714.3a: a token copy of a Saga enters as a Saga with its on-enter lore
            // counter. BattlefieldEntry.place skips enters-with-counters setup, so apply the shared
            // Saga-entry helper (as the standard moveToZone pipeline does). No-op for non-Sagas.
            val (sagaState, sagaEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .applySagaEntryIfNeeded(newState, tokenId)

            // CR 306.5b: likewise a token copy of a planeswalker enters with the copied printed
            // loyalty (a copiable value, CR 707.2), or state-based actions (CR 704.5i) bin it on
            // arrival. No-op for non-planeswalkers.
            val (loyaltyState, loyaltyEvents) = cardRegistry?.let { registry ->
                com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                    .applyIntrinsicEntryCountersIfNeeded(sagaState, tokenId, controllerId, registry)
            } ?: (sagaState to emptyList())

            return EffectResult.success(
                loyaltyState, listOf(event) + counterEvents + sagaEvents + loyaltyEvents
            )
        }
    }
}
