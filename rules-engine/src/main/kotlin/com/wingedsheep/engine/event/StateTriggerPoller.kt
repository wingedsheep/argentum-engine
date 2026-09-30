package com.wingedsheep.engine.event

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.StateTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * Polls [StateTriggeredAbility] instances on every battlefield permanent at each priority
 * pass (CR 603.8).
 *
 * A source object's ability is suppressed while its original trigger is waiting or on the
 * stack, even if the condition becomes false meanwhile. Once that trigger leaves the stack,
 * a still-true condition may fire again. Copies do not extend the original trigger's lifetime.
 *
 * State-triggered abilities go on the stack as ordinary triggered abilities. The synthetic
 * [EventPattern.StateConditionMetEvent] is used only to satisfy [TriggeredAbility]'s
 * `trigger` slot; it is never matched against real events ([TriggerMatcher] returns
 * `false` for it).
 */
class StateTriggerPoller(
    private val cardRegistry: CardRegistry,
    private val conditionEvaluator: ConditionEvaluator
) {

    /**
     * Result of one poll pass.
     *
     * @property newState the unchanged input state; lifecycle occupancy lives on pending and stack objects.
     * @property pendingTriggers triggers to enqueue (already in APNAP order: the engine's
     *   battlefield iteration is already APNAP-stable within a single pass for our purposes;
     *   downstream [TriggerProcessor] re-orders by controller if necessary).
     */
    data class Result(
        val newState: GameState,
        val pendingTriggers: List<PendingTrigger>
    )

    fun poll(state: GameState): Result {
        val projected = state.projectedState
        // Build once per poll, rather than scanning the stack for each permanent/ability.
        val occupied = mutableSetOf<Pair<ObjectRef?, AbilityId>>()
        for (trigger in state.pendingTriggers) {
            if (trigger.ability.trigger == EventPattern.StateConditionMetEvent) {
                occupied += trigger.objectReferences.origin to trigger.ability.id
            }
        }
        for (stackId in state.stack) {
            val trigger = state.getEntity(stackId)?.get<TriggeredAbilityOnStackComponent>() ?: continue
            val abilityId = trigger.stateTriggerAbilityId ?: continue
            occupied += trigger.objectReferences.origin to abilityId
        }
        val newTriggers = mutableListOf<PendingTrigger>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue
            val controllerId = projected.getController(permanentId) ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            // Fold in unlocked Room-face state triggers (CR 709.5) so a locked door's state
            // trigger stays inert until its door is unlocked (Promising Stairs).
            val printed = if (projected.hasLostAllAbilities(permanentId)) emptyList() else com.wingedsheep.engine.state.components.identity.RoomFaceStatics
                .activeStateTriggeredAbilities(container, cardDef)
            // A granted ability has its own id, so it has an independent lifecycle.
            val granted = state.grantedStateTriggeredAbilities
                .filter { it.entityId == permanentId }
                .map { it.ability }
            val textReplacement = TextChanges.of(state, permanentId)
            val effectivePrinted = if (textReplacement == null) printed
                else printed.map { it.applyTextReplacement(textReplacement) }
            val abilities = if (granted.isEmpty()) effectivePrinted else effectivePrinted + granted
            if (abilities.isEmpty()) continue

            val effectContext = EffectContext(
                sourceId = permanentId,
                controllerId = controllerId,
            )

            val sourceObject = state.objectRef(permanentId)
            for (ability in abilities) {
                val key = sourceObject to ability.id
                if (key in occupied) continue
                if (!conditionEvaluator.evaluate(state, ability.condition, effectContext)) continue
                occupied += key
                newTriggers += PendingTrigger(
                    ability = ability.asTriggeredAbility(),
                    sourceId = permanentId,
                    sourceName = card.name,
                    controllerId = controllerId,
                    triggerContext = TriggerContext(),
                    objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(
                        captured = true, origin = sourceObject, source = sourceObject
                    )
                )
            }
        }

        return Result(state, newTriggers)
    }

    private fun StateTriggeredAbility.asTriggeredAbility(): TriggeredAbility =
        TriggeredAbility(
            id = id,
            trigger = EventPattern.StateConditionMetEvent,
            binding = TriggerBinding.SELF,
            effect = effect,
            activeZones = setOf(activeZone),
            descriptionOverride = descriptionOverride ?: description
        )
}
