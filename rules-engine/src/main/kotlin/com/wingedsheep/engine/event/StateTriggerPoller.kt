package com.wingedsheep.engine.event

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.StateTriggerLatchesComponent
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
 * For each ability:
 *  - Evaluate its condition against the source permanent's controller.
 *  - Compare against the entity's [StateTriggerLatchesComponent]:
 *      - condition true AND not latched → produce a [PendingTrigger] and latch it.
 *      - condition false AND latched → clear the latch so the next true transition refires.
 *  - Otherwise, leave the latch alone (no event).
 *
 * State-triggered abilities go on the stack as ordinary triggered abilities. The synthetic
 * [EventPattern.StateConditionMetEvent] is used only to satisfy [TriggeredAbility]'s
 * `trigger` slot; it is never matched against real events ([TriggerMatcher] returns
 * `false` for it).
 */
class StateTriggerPoller(
    private val cardRegistry: CardRegistry,
    private val conditionEvaluator: ConditionEvaluator = ConditionEvaluator()
) {

    /**
     * Result of one poll pass.
     *
     * @property newState the state with updated latch components.
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
        var workingState = state
        val newTriggers = mutableListOf<PendingTrigger>()

        for (permanentId in state.getBattlefield()) {
            val container = workingState.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue
            val controllerId = projected.getController(permanentId) ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val abilities = cardDef.script.stateTriggeredAbilities
            if (abilities.isEmpty()) continue

            val opponentId = workingState.turnOrder.firstOrNull { it != controllerId }
            val effectContext = EffectContext(
                sourceId = permanentId,
                controllerId = controllerId,
                opponentId = opponentId
            )

            var latches = container.get<StateTriggerLatchesComponent>() ?: StateTriggerLatchesComponent()
            var latchesChanged = false

            for (ability in abilities) {
                val conditionMet = conditionEvaluator.evaluate(workingState, ability.condition, effectContext)
                val wasLatched = latches.isLatched(ability.id)

                when {
                    conditionMet && !wasLatched -> {
                        latches = latches.withLatched(ability.id)
                        latchesChanged = true
                        newTriggers += PendingTrigger(
                            ability = ability.asTriggeredAbility(),
                            sourceId = permanentId,
                            sourceName = card.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext()
                        )
                    }
                    !conditionMet && wasLatched -> {
                        latches = latches.withoutLatched(ability.id)
                        latchesChanged = true
                    }
                    else -> { /* no transition */ }
                }
            }

            if (latchesChanged) {
                workingState = workingState.withEntity(permanentId, container.withComponent(latches))
            }
        }

        return Result(workingState, newTriggers)
    }

    private fun StateTriggeredAbility.asTriggeredAbility(): TriggeredAbility =
        TriggeredAbility(
            id = id,
            trigger = EventPattern.StateConditionMetEvent,
            binding = TriggerBinding.SELF,
            effect = effect,
            activeZone = activeZone,
            descriptionOverride = descriptionOverride ?: description
        )
}
