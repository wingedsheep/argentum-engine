package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.EmitExploredEventEffect
import com.wingedsheep.sdk.scripting.effects.ExploreEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ExploreEffect.
 *
 * "Reveal the top card of your library. If it's a land card, put it into your hand.
 * Otherwise, put a +1/+1 counter on this creature, then put the card back on top of your
 * library or put it into your graveyard."
 *
 * The exploring player is the effect controller. The exploring creature is [ExploreEffect.target].
 */
class ExploreEffectExecutor : EffectExecutor<ExploreEffect> {

    override val effectType: KClass<ExploreEffect> = ExploreEffect::class

    override fun execute(
        state: GameState,
        effect: ExploreEffect,
        context: EffectContext
    ): EffectResult {
        val exploringCreatureId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        val explorerId = context.controllerId
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // CR 701.44b: the permanent "explores" even if the reveal was impossible (empty library).
        // Emit the explored event (revealedCardWasLand = null) so "whenever a creature you control
        // explores" (ANY) still fires; the land/nonland reveal-type triggers don't match.
        fun exploredEvent(wasLand: Boolean?) = PermanentExploredEvent(
            exploringPermanentId = exploringCreatureId,
            controllerId = explorerId,
            revealedCardWasLand = wasLand,
            sourceName = sourceName
        )

        val library = state.getLibrary(explorerId)
        if (library.isEmpty()) {
            return EffectResult.success(state, listOf(exploredEvent(null)))
        }

        val topCardId = library.first()
        val topCardContainer = state.getEntity(topCardId)
            ?: return EffectResult.success(state)
        val topCardComponent = topCardContainer.get<CardComponent>()
            ?: return EffectResult.success(state)

        val topCardName = topCardComponent.name
        val topCardImageUri = topCardComponent.imageUri

        val revealEvent = CardsRevealedEvent(
            revealingPlayerId = explorerId,
            cardIds = listOf(topCardId),
            cardNames = listOf(topCardName),
            imageUris = listOf(topCardImageUri),
            source = sourceName
        )

        return if (topCardComponent.typeLine.isLand) {
            // Land: move directly to hand
            val transition = ZoneTransitionService.moveToZone(state, topCardId, Zone.HAND)
            EffectResult.success(
                transition.state,
                listOf(revealEvent) + transition.events + exploredEvent(true)
            )
        } else {
            // Non-land: mark card revealed to all players, add +1/+1 counter, then ask:
            // back to top of library (stays visible) or graveyard?
            val revealedComponent = state.turnOrder.fold(RevealedToComponent(emptySet())) { acc, pid ->
                acc.withPlayer(pid)
            }
            val stateWithRevealed = state.updateEntity(topCardId) { it.with(revealedComponent) }

            val (stateAfterCounter, counterEvents) = addPlusOneCounter(stateWithRevealed, exploringCreatureId, context)

            val decisionId = UUID.randomUUID().toString()
            val decision = YesNoDecision(
                id = decisionId,
                playerId = explorerId,
                prompt = "Put $topCardName back on top of your library? (No = graveyard)",
                context = DecisionContext(
                    sourceId = context.sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                yesText = "Library (top)",
                noText = "Graveyard"
            )

            // Defer the explored event to after the top/graveyard move resolves (CR 701.44b): a
            // game event emitted in the paused batch below does not reliably fire watcher triggers,
            // so both continuation branches end by emitting it from a completed resolution.
            val emitExplored = EmitExploredEventEffect(
                target = EffectTarget.SpecificEntity(exploringCreatureId),
                revealedCardWasLand = false
            )
            val continuation = MayAbilityContinuation(
                decisionId = decisionId,
                playerId = explorerId,
                sourceName = sourceName,
                effectIfYes = CompositeEffect(listOf(
                    MoveToZoneEffect(
                        target = EffectTarget.SpecificEntity(topCardId),
                        destination = Zone.LIBRARY,
                        placement = ZonePlacement.Top
                    ),
                    emitExplored
                )),
                effectIfNo = CompositeEffect(listOf(
                    MoveToZoneEffect(
                        target = EffectTarget.SpecificEntity(topCardId),
                        destination = Zone.GRAVEYARD
                    ),
                    emitExplored
                )),
                effectContext = context
            )

            val stateWithContinuation = stateAfterCounter
                .withPendingDecision(decision)
                .pushContinuation(continuation)

            EffectResult.paused(
                stateWithContinuation,
                decision,
                listOf(revealEvent) + counterEvents + listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = explorerId,
                        decisionType = "YES_NO",
                        prompt = decision.prompt
                    )
                )
            )
        }
    }

    private fun addPlusOneCounter(
        state: GameState,
        creatureId: EntityId,
        context: EffectContext
    ): Pair<GameState, List<GameEvent>> {
        if (!state.projectedState.canReceiveCounters(creatureId)) {
            return state to emptyList()
        }
        val current = state.getEntity(creatureId)?.get<CountersComponent>() ?: CountersComponent()
        val count = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state, creatureId, CounterType.PLUS_ONE_PLUS_ONE, 1, placerId = context.controllerId
        )
        val updated = state.updateEntity(creatureId) {
            it.with(current.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
        }
        val (newState, firstThisTurn) =
            com.wingedsheep.engine.handlers.effects.DamageUtils.recordCounterPlacement(updated, creatureId)
        val name = state.getEntity(creatureId)?.get<CardComponent>()?.name ?: ""
        return newState to listOf(CountersAddedEvent(creatureId, "PLUS_ONE_PLUS_ONE", count, name, firstThisTurn, placedBy = context.controllerId))
    }
}
