package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CrewSaddleContributorsComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.BecomeSaddledEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Handler for the [SaddleMount] action (CR 702.171a).
 *
 * Saddle is an activated ability whose cost is tapping any number of *other* untapped creatures
 * the player controls with total power >= the saddle requirement, and which can be activated only
 * as a sorcery. The cost (tapping creatures) is paid immediately; the effect (the permanent
 * becomes saddled until end of turn) goes on the stack as an activated ability.
 *
 * Mirrors [CrewVehicleHandler], but gated on sorcery-speed timing and resolving to a saddled
 * marker rather than animating the permanent into a creature.
 */
class SaddleMountHandler(
    private val cardRegistry: CardRegistry,
    private val stackResolver: StackResolver,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor
) : ActionHandler<SaddleMount> {
    override val actionType: KClass<SaddleMount> = SaddleMount::class

    override fun validate(state: GameState, action: SaddleMount): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        // "Activate only as a sorcery" (CR 702.171a)
        if (!state.step.isMainPhase || state.stack.isNotEmpty() ||
            !state.isActiveTurnFor(action.playerId)
        ) {
            return "Saddle can only be activated during your main phase while the stack is empty"
        }

        val mountContainer = state.getEntity(action.mountId)
            ?: return "Mount not found: ${action.mountId}"

        val mountCard = mountContainer.get<CardComponent>()
            ?: return "Not a card: ${action.mountId}"

        if (action.mountId !in state.getBattlefield()) {
            return "Mount is not on the battlefield"
        }

        val projected = state.projectedState
        if (projected.getController(action.mountId) != action.playerId) {
            return "You don't control this permanent"
        }

        val cardDef = cardRegistry.getCard(mountCard.cardDefinitionId)
            ?: return "Card definition not found"

        val saddleAbility = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Numeric>()
            .firstOrNull { it.keyword == Keyword.SADDLE }
            ?: return "This permanent doesn't have saddle"

        if (action.saddleCreatures.isEmpty()) {
            return "Must select at least one creature to saddle"
        }

        var totalPower = 0
        for (creatureId in action.saddleCreatures) {
            if (creatureId == action.mountId) {
                return "A mount cannot saddle itself"
            }

            val creatureContainer = state.getEntity(creatureId)
                ?: return "Creature not found: $creatureId"

            if (creatureId !in state.getBattlefield()) {
                return "Creature is not on the battlefield: $creatureId"
            }

            if (projected.getController(creatureId) != action.playerId) {
                return "You don't control creature: $creatureId"
            }

            if (!projected.isCreature(creatureId)) {
                return "Not a creature: $creatureId"
            }

            if (creatureContainer.has<TappedComponent>()) {
                return "Creature is already tapped: $creatureId"
            }

            totalPower += projected.getPower(creatureId) ?: 0
        }

        if (totalPower < saddleAbility.n) {
            return "Total power ($totalPower) is less than saddle requirement (${saddleAbility.n})"
        }

        return null
    }

    override fun execute(state: GameState, action: SaddleMount): ExecutionResult {
        val mountContainer = state.getEntity(action.mountId)
            ?: return ExecutionResult.error(state, "Mount not found")

        val mountCard = mountContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        var currentState = state
        val events = mutableListOf<GameEvent>()

        // Pay the cost: tap each saddling creature (CR 702.171c — these creatures "saddle" it).
        for (creatureId in action.saddleCreatures) {
            val creatureName = currentState.getEntity(creatureId)
                ?.get<CardComponent>()?.name ?: "Unknown"
            currentState = currentState.updateEntity(creatureId) { c ->
                c.with(TappedComponent)
            }
            events.add(TappedEvent(creatureId, creatureName))
        }

        // Record the saddlers so Mount payoffs can read "creatures that saddled it this turn".
        // Union across activations: saddle may be activated again even while already saddled.
        currentState = currentState.updateEntity(action.mountId) { c ->
            val existing = c.get<CrewSaddleContributorsComponent>()
            c.with(
                CrewSaddleContributorsComponent(
                    creatureIds = (existing?.creatureIds ?: emptySet()) + action.saddleCreatures,
                    crewActivations = existing?.crewActivations ?: 0
                )
            )
        }

        // Put the saddle ability on the stack: this permanent becomes saddled until end of turn.
        val abilityOnStack = ActivatedAbilityOnStackComponent(
            sourceId = action.mountId,
            sourceName = mountCard.name,
            controllerId = action.playerId,
            effect = BecomeSaddledEffect(target = EffectTarget.Self)
        )

        val stackResult = stackResolver.putActivatedAbility(currentState, abilityOnStack)
        currentState = stackResult.newState
        events.addAll(stackResult.events)

        // Detect and process triggers from tapping creatures.
        val allEvents = events.toList()
        val triggers = triggerDetector.detectTriggers(currentState, allEvents)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(currentState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state.withPriority(action.playerId),
                    triggerResult.pendingDecision!!,
                    allEvents + triggerResult.events
                )
            }

            return ExecutionResult.success(
                triggerResult.newState.withPriority(action.playerId),
                allEvents + triggerResult.events
            )
        }

        return ExecutionResult.success(
            currentState.withPriority(action.playerId),
            allEvents
        )
    }

    companion object {
        fun create(services: EngineServices): SaddleMountHandler {
            return SaddleMountHandler(
                services.cardRegistry,
                services.stackResolver,
                services.triggerDetector,
                services.triggerProcessor
            )
        }
    }
}
