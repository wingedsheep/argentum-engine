package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent

/**
 * Detects triggered abilities that should fire based on game events.
 *
 * This is the ECS version of TriggerDetector, working with EntityId and GameState
 * instead of CardId/PlayerId and GameState.
 *
 * Triggers are returned in APNAP (Active Player, Non-Active Player) order for
 * proper stack placement.
 */
class TriggerDetector {

    /**
     * Detect all triggers that should fire based on the given events.
     *
     * @param state The current game state (for looking up abilities)
     * @param events The events that occurred
     * @param abilityRegistry Provider of triggered abilities for each entity
     * @return List of pending triggers in APNAP order
     */
    fun detectTriggers(
        state: GameState,
        events: List<GameEvent>,
        abilityRegistry: AbilityRegistry
    ): List<PendingTrigger> {
        val triggers = mutableListOf<PendingTrigger>()

        for (event in events) {
            triggers.addAll(detectTriggersForEvent(state, event, abilityRegistry))
        }

        // Sort by APNAP order
        return sortByApnapOrder(state, triggers)
    }

    /**
     * Detect triggers for phase/step events.
     */
    fun detectPhaseStepTriggers(
        state: GameState,
        phase: String,
        step: String,
        activePlayerId: EntityId,
        abilityRegistry: AbilityRegistry
    ): List<PendingTrigger> {
        val event = when {
            step == "Upkeep" -> GameEvent.UpkeepBegan(activePlayerId)
            step == "End" -> GameEvent.EndStepBegan(activePlayerId)
            step == "BeginCombat" -> {
                // Get defending player (opponent)
                val defendingPlayerId = state.getPlayerIds().find { it != activePlayerId } ?: return emptyList()
                GameEvent.CombatBegan(activePlayerId, defendingPlayerId)
            }
            else -> return emptyList()
        }

        return detectTriggersForEvent(state, event, abilityRegistry)
    }

    private fun detectTriggersForEvent(
        state: GameState,
        event: GameEvent,
        abilityRegistry: AbilityRegistry
    ): List<PendingTrigger> {
        val triggers = mutableListOf<PendingTrigger>()

        // Get all permanents on battlefield that might have triggered abilities
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val controllerId = container.get<ControllerComponent>()?.controllerId ?: continue

            // Get triggered abilities for this entity
            val abilities = abilityRegistry.getTriggeredAbilities(entityId, cardComponent.definition)

            for (ability in abilities) {
                if (matchesTrigger(ability.trigger, event, entityId, controllerId, state)) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entityId,
                            sourceName = cardComponent.definition.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext.fromEvent(event)
                        )
                    )
                }
            }
        }

        // Also check for death triggers (source might not be on battlefield anymore)
        if (event is GameEvent.CreatureDied) {
            val entityId = event.entityId
            val container = state.getEntity(entityId) ?: return triggers
            val cardComponent = container.get<CardComponent>() ?: return triggers
            val controllerId = container.get<ControllerComponent>()?.controllerId ?: return triggers

            val abilities = abilityRegistry.getTriggeredAbilities(entityId, cardComponent.definition)
            for (ability in abilities) {
                if (ability.trigger is OnDeath && (ability.trigger as OnDeath).selfOnly) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entityId,
                            sourceName = cardComponent.definition.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext.fromEvent(event)
                        )
                    )
                }
            }
        }

        return triggers
    }

    private fun matchesTrigger(
        trigger: Trigger,
        event: GameEvent,
        sourceId: EntityId,
        controllerId: EntityId,
        state: GameState
    ): Boolean {
        return when (trigger) {
            is OnEnterBattlefield -> {
                event is GameEvent.EnteredBattlefield &&
                    (!trigger.selfOnly || event.entityId == sourceId)
            }

            is OnLeavesBattlefield -> {
                event is GameEvent.LeftBattlefield &&
                    (!trigger.selfOnly || event.entityId == sourceId)
            }

            is OnDeath -> {
                event is GameEvent.CreatureDied &&
                    (!trigger.selfOnly || event.entityId == sourceId)
            }

            is OnDraw -> {
                event is GameEvent.CardDrawn &&
                    (!trigger.controllerOnly || event.playerId == controllerId)
            }

            is OnAttack -> {
                event is GameEvent.AttackerDeclared &&
                    (!trigger.selfOnly || event.creatureId == sourceId)
            }

            is OnBlock -> {
                event is GameEvent.BlockerDeclared &&
                    (!trigger.selfOnly || event.blockerId == sourceId)
            }

            is OnDealsDamage -> {
                when (event) {
                    is GameEvent.DamageDealtToPlayer -> {
                        val matches = !trigger.selfOnly || event.sourceId == sourceId
                        val combatMatches = !trigger.combatOnly || event.isCombatDamage
                        matches && combatMatches
                    }
                    is GameEvent.DamageDealtToCreature -> {
                        val matches = !trigger.selfOnly || event.sourceId == sourceId
                        val combatMatches = !trigger.combatOnly || event.isCombatDamage
                        val targetMatches = !trigger.toPlayerOnly
                        matches && combatMatches && targetMatches
                    }
                    else -> false
                }
            }

            is OnUpkeep -> {
                event is GameEvent.UpkeepBegan &&
                    (!trigger.controllerOnly || event.activePlayerId == controllerId)
            }

            is OnEndStep -> {
                event is GameEvent.EndStepBegan &&
                    (!trigger.controllerOnly || event.activePlayerId == controllerId)
            }

            is OnBeginCombat -> {
                event is GameEvent.CombatBegan &&
                    (!trigger.controllerOnly || event.attackingPlayerId == controllerId)
            }

            is OnDamageReceived -> {
                event is GameEvent.DamageDealtToCreature &&
                    (!trigger.selfOnly || event.targetCreatureId == sourceId)
            }

            is OnSpellCast -> {
                event is GameEvent.SpellCast &&
                    (!trigger.controllerOnly || event.casterId == controllerId) &&
                    matchesSpellTypeFilter(trigger.spellType, event)
            }

            is OnFirstMainPhase -> {
                event is GameEvent.FirstMainPhaseBegan &&
                    (!trigger.controllerOnly || event.activePlayerId == controllerId)
            }

            is OnTransform -> {
                event is GameEvent.Transformed &&
                    (!trigger.selfOnly || event.entityId == sourceId) &&
                    (trigger.intoBackFace == null || trigger.intoBackFace == event.toBackFace)
            }
        }
    }

    private fun matchesSpellTypeFilter(filter: SpellTypeFilter, event: GameEvent.SpellCast): Boolean {
        return when (filter) {
            SpellTypeFilter.ANY -> true
            SpellTypeFilter.CREATURE -> event.isCreatureSpell
            SpellTypeFilter.NONCREATURE -> !event.isCreatureSpell
            SpellTypeFilter.INSTANT_OR_SORCERY -> event.isInstantOrSorcery
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
        val playerOrder = state.getPlayerIds().let { players ->
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
 * Registry that provides triggered abilities for entities.
 * Implementations can look up abilities from CardDefinitions,
 * external files, or other sources.
 */
interface AbilityRegistry {
    /**
     * Get all triggered abilities for the given entity.
     */
    fun getTriggeredAbilities(
        entityId: EntityId,
        definition: com.wingedsheep.rulesengine.card.CardDefinition
    ): List<TriggeredAbility>
}

/**
 * Simple implementation that stores and retrieves triggered abilities.
 *
 * In the full system, abilities would be loaded from card scripts or a database.
 * This implementation allows registering abilities manually for testing.
 */
class CardDefinitionAbilityRegistry : AbilityRegistry {

    private val abilitiesByCard = mutableMapOf<String, List<TriggeredAbility>>()

    /**
     * Register triggered abilities for a card name.
     */
    fun register(cardName: String, abilities: List<TriggeredAbility>) {
        abilitiesByCard[cardName] = abilities
    }

    override fun getTriggeredAbilities(
        entityId: EntityId,
        definition: com.wingedsheep.rulesengine.card.CardDefinition
    ): List<TriggeredAbility> {
        return abilitiesByCard[definition.name] ?: emptyList()
    }
}
