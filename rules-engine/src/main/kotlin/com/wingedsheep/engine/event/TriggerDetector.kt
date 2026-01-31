package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*

/**
 * Detects triggered abilities that should fire based on game events.
 *
 * Triggers are returned in APNAP (Active Player, Non-Active Player) order for
 * proper stack placement.
 */
class TriggerDetector(
    private val cardRegistry: CardRegistry? = null,
    private val abilityRegistry: AbilityRegistry = AbilityRegistry()
) {

    /**
     * Get triggered abilities for a card, checking both the AbilityRegistry
     * and falling back to the CardRegistry for card definitions.
     */
    private fun getTriggeredAbilities(entityId: EntityId, cardDefinitionId: String): List<TriggeredAbility> {
        // First check the AbilityRegistry (for manually registered abilities)
        val registryAbilities = abilityRegistry.getTriggeredAbilities(entityId, cardDefinitionId)
        if (registryAbilities.isNotEmpty()) {
            return registryAbilities
        }

        // Fall back to looking up from CardRegistry
        return cardRegistry?.getCard(cardDefinitionId)?.triggeredAbilities ?: emptyList()
    }

    /**
     * Detect all triggers that should fire based on the given events.
     *
     * @param state The current game state
     * @param events The events that occurred
     * @return List of pending triggers in APNAP order
     */
    fun detectTriggers(
        state: GameState,
        events: List<EngineGameEvent>
    ): List<PendingTrigger> {
        val triggers = mutableListOf<PendingTrigger>()

        for (event in events) {
            triggers.addAll(detectTriggersForEvent(state, event))
        }

        // Sort by APNAP order
        return sortByApnapOrder(state, triggers)
    }

    /**
     * Detect triggers for phase/step changes.
     */
    fun detectPhaseStepTriggers(
        state: GameState,
        step: Step,
        activePlayerId: EntityId
    ): List<PendingTrigger> {
        val triggers = mutableListOf<PendingTrigger>()

        // Check all permanents on the battlefield for step-based triggers
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val controllerId = container.get<ControllerComponent>()?.playerId ?: continue

            val abilities = getTriggeredAbilities(entityId, cardComponent.cardDefinitionId)

            for (ability in abilities) {
                if (matchesStepTrigger(ability.trigger, step, controllerId, activePlayerId)) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entityId,
                            sourceName = cardComponent.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext(step = step)
                        )
                    )
                }
            }
        }

        return sortByApnapOrder(state, triggers)
    }

    private fun detectTriggersForEvent(
        state: GameState,
        event: EngineGameEvent
    ): List<PendingTrigger> {
        val triggers = mutableListOf<PendingTrigger>()

        // Check all permanents on the battlefield
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val controllerId = container.get<ControllerComponent>()?.playerId ?: continue

            val abilities = getTriggeredAbilities(entityId, cardComponent.cardDefinitionId)

            for (ability in abilities) {
                if (matchesTrigger(ability.trigger, event, entityId, controllerId, state)) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entityId,
                            sourceName = cardComponent.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext.fromEvent(event)
                        )
                    )
                }
            }
        }

        // Handle death triggers (source might not be on battlefield anymore)
        if (event is ZoneChangeEvent && event.toZone == com.wingedsheep.sdk.core.ZoneType.GRAVEYARD &&
            event.fromZone == com.wingedsheep.sdk.core.ZoneType.BATTLEFIELD) {
            detectDeathTriggers(state, event, triggers)
        }

        // Handle damage-source triggers
        if (event is DamageDealtEvent && event.sourceId != null) {
            detectDamageSourceTriggers(state, event, triggers)
        }

        return triggers
    }

    private fun detectDeathTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.entityId
        val container = state.getEntity(entityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return

        // For "When this creature dies" - the creature might be in graveyard now
        // Look up abilities by card definition
        val abilities = getTriggeredAbilities(entityId, cardComponent.cardDefinitionId)
        val controllerId = event.ownerId

        for (ability in abilities) {
            val trigger = ability.trigger
            if (trigger is OnDeath && trigger.selfOnly) {
                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = entityId,
                        sourceName = cardComponent.name,
                        controllerId = controllerId,
                        triggerContext = TriggerContext.fromEvent(event)
                    )
                )
            }
        }
    }

    private fun detectDamageSourceTriggers(
        state: GameState,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val sourceId = event.sourceId ?: return
        val container = state.getEntity(sourceId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return
        val controllerId = container.get<ControllerComponent>()?.playerId ?: return

        val abilities = getTriggeredAbilities(sourceId, cardComponent.cardDefinitionId)

        for (ability in abilities) {
            val trigger = ability.trigger
            if (trigger is OnDealsDamage) {
                if (matchesDealsDamageTrigger(trigger, event)) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = sourceId,
                            sourceName = cardComponent.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext.fromEvent(event)
                        )
                    )
                }
            }
        }
    }

    private fun matchesTrigger(
        trigger: Trigger,
        event: EngineGameEvent,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Boolean {
        return when (trigger) {
            is OnEnterBattlefield -> {
                event is ZoneChangeEvent &&
                    event.toZone == com.wingedsheep.sdk.core.ZoneType.BATTLEFIELD &&
                    (!trigger.selfOnly || event.entityId == sourceId)
            }

            is OnOtherCreatureEnters -> {
                event is ZoneChangeEvent &&
                    event.toZone == com.wingedsheep.sdk.core.ZoneType.BATTLEFIELD &&
                    event.entityId != sourceId &&
                    (!trigger.youControlOnly || event.ownerId == controllerId)
            }

            is OnLeavesBattlefield -> {
                event is ZoneChangeEvent &&
                    event.fromZone == com.wingedsheep.sdk.core.ZoneType.BATTLEFIELD &&
                    (!trigger.selfOnly || event.entityId == sourceId)
            }

            is OnDeath -> {
                event is ZoneChangeEvent &&
                    event.toZone == com.wingedsheep.sdk.core.ZoneType.GRAVEYARD &&
                    event.fromZone == com.wingedsheep.sdk.core.ZoneType.BATTLEFIELD &&
                    (!trigger.selfOnly || event.entityId == sourceId)
            }

            is OnOtherCreatureWithSubtypeDies -> {
                if (event !is ZoneChangeEvent ||
                    event.toZone != com.wingedsheep.sdk.core.ZoneType.GRAVEYARD ||
                    event.fromZone != com.wingedsheep.sdk.core.ZoneType.BATTLEFIELD ||
                    event.entityId == sourceId) {
                    false
                } else {
                    // Check if dying creature has the required subtype
                    val dyingCard = state.getEntity(event.entityId)?.get<CardComponent>()
                    val hasSubtype = dyingCard?.typeLine?.hasSubtype(trigger.subtype) == true
                    val controllerMatches = !trigger.youControlOnly || event.ownerId == controllerId
                    hasSubtype && controllerMatches
                }
            }

            is OnDraw -> {
                event is CardsDrawnEvent &&
                    (!trigger.controllerOnly || event.playerId == controllerId)
            }

            is OnAttack -> {
                event is AttackersDeclaredEvent &&
                    (!trigger.selfOnly || sourceId in event.attackers)
            }

            is OnYouAttack -> {
                event is AttackersDeclaredEvent &&
                    event.attackers.size >= trigger.minAttackers &&
                    // Check if active player is the controller
                    state.activePlayerId == controllerId
            }

            is OnBlock -> {
                event is BlockersDeclaredEvent &&
                    (!trigger.selfOnly || event.blockers.keys.contains(sourceId))
            }

            is OnDealsDamage -> {
                // Handled separately in detectDamageSourceTriggers
                false
            }

            is OnDamageReceived -> {
                event is DamageDealtEvent &&
                    (!trigger.selfOnly || event.targetId == sourceId)
            }

            is OnSpellCast -> {
                event is SpellCastEvent &&
                    (!trigger.controllerOnly || event.casterId == controllerId) &&
                    matchesSpellTypeFilter(trigger, event, state)
            }

            is OnBecomesTapped -> {
                event is TappedEvent &&
                    (!trigger.selfOnly || event.entityId == sourceId)
            }

            is OnBecomesUntapped -> {
                event is UntappedEvent &&
                    (!trigger.selfOnly || event.entityId == sourceId)
            }

            // Phase/step triggers are handled separately
            is OnUpkeep, is OnEndStep, is OnBeginCombat, is OnFirstMainPhase -> false

            is OnTransform -> {
                // Transform not yet implemented in new engine
                false
            }
        }
    }

    private fun matchesDealsDamageTrigger(trigger: OnDealsDamage, event: DamageDealtEvent): Boolean {
        val combatMatches = !trigger.combatOnly || event.isCombatDamage
        val targetMatches = !trigger.toPlayerOnly // TODO: Check if target is a player
        return combatMatches && targetMatches
    }

    private fun matchesStepTrigger(
        trigger: Trigger,
        step: Step,
        controllerId: EntityId,
        activePlayerId: EntityId
    ): Boolean {
        return when (trigger) {
            is OnUpkeep -> {
                step == Step.UPKEEP &&
                    (!trigger.controllerOnly || controllerId == activePlayerId)
            }

            is OnEndStep -> {
                step == Step.END &&
                    (!trigger.controllerOnly || controllerId == activePlayerId)
            }

            is OnBeginCombat -> {
                step == Step.BEGIN_COMBAT &&
                    (!trigger.controllerOnly || controllerId == activePlayerId)
            }

            is OnFirstMainPhase -> {
                step == Step.PRECOMBAT_MAIN &&
                    (!trigger.controllerOnly || controllerId == activePlayerId)
            }

            else -> false
        }
    }

    private fun matchesSpellTypeFilter(
        trigger: OnSpellCast,
        event: SpellCastEvent,
        state: GameState
    ): Boolean {
        val cardComponent = state.getEntity(event.spellEntityId)?.get<CardComponent>()
            ?: return trigger.spellType == SpellTypeFilter.ANY

        return when (trigger.spellType) {
            SpellTypeFilter.ANY -> true
            SpellTypeFilter.CREATURE -> cardComponent.typeLine.isCreature
            SpellTypeFilter.NONCREATURE -> !cardComponent.typeLine.isCreature
            SpellTypeFilter.INSTANT_OR_SORCERY ->
                cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery
        }
    }

    /**
     * Sort triggers by APNAP order.
     * Active player's triggers go on the stack first (resolve last),
     * then non-active players in turn order.
     */
    private fun sortByApnapOrder(
        state: GameState,
        triggers: List<PendingTrigger>
    ): List<PendingTrigger> {
        val activePlayerId = state.activePlayerId ?: return triggers

        // Group by controller
        val byController = triggers.groupBy { it.controllerId }

        // Get player order starting from active player
        val playerOrder = state.turnOrder.let { players ->
            val activeIndex = players.indexOf(activePlayerId)
            if (activeIndex == -1) players
            else players.drop(activeIndex) + players.take(activeIndex)
        }

        // Build result in APNAP order
        // Active player's triggers first (they go on stack first = resolve last)
        return playerOrder.flatMap { playerId ->
            byController[playerId] ?: emptyList()
        }
    }
}

/**
 * A triggered ability that is waiting to go on the stack.
 */
data class PendingTrigger(
    val ability: TriggeredAbility,
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val triggerContext: TriggerContext
)

/**
 * Context information about what caused a trigger.
 */
data class TriggerContext(
    val triggeringEntityId: EntityId? = null,
    val triggeringPlayerId: EntityId? = null,
    val damageAmount: Int? = null,
    val step: Step? = null
) {
    companion object {
        fun fromEvent(event: EngineGameEvent): TriggerContext {
            return when (event) {
                is ZoneChangeEvent -> TriggerContext(triggeringEntityId = event.entityId)
                is DamageDealtEvent -> TriggerContext(
                    triggeringEntityId = event.targetId,
                    damageAmount = event.amount
                )
                is SpellCastEvent -> TriggerContext(
                    triggeringEntityId = event.spellEntityId,
                    triggeringPlayerId = event.casterId
                )
                is CardsDrawnEvent -> TriggerContext(triggeringPlayerId = event.playerId)
                is AttackersDeclaredEvent -> TriggerContext()
                is BlockersDeclaredEvent -> TriggerContext()
                is TappedEvent -> TriggerContext(triggeringEntityId = event.entityId)
                is UntappedEvent -> TriggerContext(triggeringEntityId = event.entityId)
                is LifeChangedEvent -> TriggerContext(triggeringPlayerId = event.playerId)
                else -> TriggerContext()
            }
        }
    }
}

/**
 * Registry that provides triggered abilities for entities.
 */
class AbilityRegistry {
    private val abilitiesByDefinition = mutableMapOf<String, List<TriggeredAbility>>()

    /**
     * Register triggered abilities for a card definition.
     */
    fun register(cardDefinitionId: String, abilities: List<TriggeredAbility>) {
        abilitiesByDefinition[cardDefinitionId] = abilities
    }

    /**
     * Get all triggered abilities for an entity.
     */
    fun getTriggeredAbilities(entityId: EntityId, cardDefinitionId: String): List<TriggeredAbility> {
        return abilitiesByDefinition[cardDefinitionId] ?: emptyList()
    }

    /**
     * Clear all registered abilities.
     */
    fun clear() {
        abilitiesByDefinition.clear()
    }
}
