package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.text.SubtypeReplacer
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
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
    private val abilityRegistry: AbilityRegistry = AbilityRegistry(),
    private val stateProjector: StateProjector = StateProjector(),
    private val conditionEvaluator: ConditionEvaluator = ConditionEvaluator()
) {

    /**
     * Get triggered abilities for a card, checking both the AbilityRegistry
     * and falling back to the CardRegistry for card definitions.
     *
     * If the entity has a TextReplacementComponent (from Artificial Evolution etc.),
     * creature type references in triggers and effects are transformed accordingly.
     */
    private fun getTriggeredAbilities(entityId: EntityId, cardDefinitionId: String, state: GameState): List<TriggeredAbility> {
        // First check the AbilityRegistry (for manually registered abilities)
        val registryAbilities = abilityRegistry.getTriggeredAbilities(entityId, cardDefinitionId)
        val base = if (registryAbilities.isNotEmpty()) {
            registryAbilities
        } else {
            // Fall back to looking up from CardRegistry
            cardRegistry?.getCard(cardDefinitionId)?.triggeredAbilities ?: emptyList()
        }

        // Merge in any temporarily granted triggered abilities (e.g., from Commando Raid)
        val grantedAbilities = state.grantedTriggeredAbilities
            .filter { it.entityId == entityId }
            .map { it.ability }
        val combined = if (grantedAbilities.isNotEmpty()) base + grantedAbilities else base

        // Apply text replacement if the entity has one
        val textReplacement = state.getEntity(entityId)?.get<TextReplacementComponent>()
        return if (textReplacement != null) {
            combined.map { SubtypeReplacer.replaceTriggeredAbility(it, textReplacement) }
        } else {
            combined
        }
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

        // Rule 603.10: "Look back in time" for simultaneous deaths.
        // When multiple creatures die at the same time (e.g., from Infest),
        // each creature's death triggers should still see the others dying.
        // The main loop in detectTriggersForEvent only checks battlefield creatures,
        // so dead creatures miss each other's death events. Fix that here.
        detectSimultaneousDeathTriggers(state, events, triggers)

        // Rule 603.4: Filter out triggers with unmet intervening-if conditions
        return sortByApnapOrder(state, filterByTriggerCondition(state, triggers))
    }

    /**
     * Detect delayed triggers that should fire at the given step.
     * Returns the pending triggers and the IDs of consumed delayed triggers.
     */
    fun detectDelayedTriggers(state: GameState, step: Step): Pair<List<PendingTrigger>, Set<String>> {
        val matching = state.delayedTriggers.filter { it.fireAtStep == step }
        if (matching.isEmpty()) return emptyList<PendingTrigger>() to emptySet()

        val triggers = matching.map { delayed ->
            PendingTrigger(
                ability = TriggeredAbility.create(
                    trigger = OnEndStep(controllerOnly = false),
                    effect = delayed.effect
                ),
                sourceId = delayed.sourceId,
                sourceName = delayed.sourceName,
                controllerId = delayed.controllerId,
                triggerContext = TriggerContext(step = step)
            )
        }
        val consumedIds = matching.map { it.id }.toSet()
        return sortByApnapOrder(state, triggers) to consumedIds
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
        val projected = stateProjector.project(state)

        // Check all permanents on the battlefield for step-based triggers
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val controllerId = projected.getController(entityId) ?: continue

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

            val abilities = getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

            for (ability in abilities) {
                if (ability.activeZone != com.wingedsheep.sdk.core.Zone.BATTLEFIELD) continue

                // Special handling for OnEnchantedCreatureControllerUpkeep:
                // The trigger fires on the enchanted creature's controller's upkeep,
                // and the trigger's controller is the enchanted creature's controller (not the aura's).
                if (ability.trigger is OnEnchantedCreatureControllerUpkeep) {
                    if (step == Step.UPKEEP) {
                        val attachedTo = container.get<AttachedToComponent>()?.targetId
                        if (attachedTo != null) {
                            val enchantedCreatureController = projected.getController(attachedTo)
                            if (enchantedCreatureController != null && enchantedCreatureController == activePlayerId) {
                                triggers.add(
                                    PendingTrigger(
                                        ability = ability,
                                        sourceId = entityId,
                                        sourceName = cardComponent.name,
                                        controllerId = enchantedCreatureController,
                                        triggerContext = TriggerContext(step = step)
                                    )
                                )
                            }
                        }
                    }
                    continue
                }

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

        // Check graveyard cards for step-based triggers with activeZone == GRAVEYARD
        for (playerId in state.turnOrder) {
            for (entityId in state.getGraveyard(playerId)) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                val abilities = getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

                for (ability in abilities) {
                    if (ability.activeZone != com.wingedsheep.sdk.core.Zone.GRAVEYARD) continue
                    // Use the card's owner as controller (graveyard cards are owned, not controlled)
                    val ownerId = cardComponent.ownerId
                        ?: container.get<OwnerComponent>()?.playerId
                        ?: continue
                    if (matchesStepTrigger(ability.trigger, step, ownerId, activePlayerId)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entityId,
                                sourceName = cardComponent.name,
                                controllerId = ownerId,
                                triggerContext = TriggerContext(step = step)
                            )
                        )
                    }
                }
            }
        }

        // Rule 603.4: Filter out triggers with unmet intervening-if conditions
        return sortByApnapOrder(state, filterByTriggerCondition(state, triggers))
    }

    private fun detectTriggersForEvent(
        state: GameState,
        event: EngineGameEvent
    ): List<PendingTrigger> {
        val triggers = mutableListOf<PendingTrigger>()
        val projected = stateProjector.project(state)

        // Check all permanents on the battlefield
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val controllerId = projected.getController(entityId) ?: continue

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

            val abilities = getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

            for (ability in abilities) {
                if (matchesTrigger(ability.trigger, event, entityId, controllerId, state)) {
                    // For "whenever a creature attacks" (OnAttack with selfOnly = false),
                    // create one trigger per attacking creature (Rule 603.2c)
                    if (ability.trigger is OnAttack && !(ability.trigger as OnAttack).selfOnly &&
                        event is AttackersDeclaredEvent) {
                        for (attackerId in event.attackers) {
                            triggers.add(
                                PendingTrigger(
                                    ability = ability,
                                    sourceId = entityId,
                                    sourceName = cardComponent.name,
                                    controllerId = controllerId,
                                    triggerContext = TriggerContext(triggeringEntityId = attackerId)
                                )
                            )
                        }
                    }
                    // For "whenever a creature you control becomes blocked" (OnBecomesBlocked with selfOnly = false),
                    // create one trigger per blocked creature controlled by the ability's controller
                    else if (ability.trigger is OnBecomesBlocked && !(ability.trigger as OnBecomesBlocked).selfOnly &&
                        event is BlockersDeclaredEvent) {
                        val blockedAttackers = event.blockers.values.flatten().distinct()
                        for (attackerId in blockedAttackers) {
                            val attackerController = projected.getController(attackerId)
                            if (attackerController == controllerId) {
                                triggers.add(
                                    PendingTrigger(
                                        ability = ability,
                                        sourceId = entityId,
                                        sourceName = cardComponent.name,
                                        controllerId = controllerId,
                                        triggerContext = TriggerContext(triggeringEntityId = attackerId)
                                    )
                                )
                            }
                        }
                    } else {
                        // For abilities like Death Match where the triggered ability should be
                        // controlled by the triggering entity's controller, not the source's controller
                        val effectiveControllerId = if (ability.controlledByTriggeringEntityController) {
                            val triggeringEntityId = TriggerContext.fromEvent(event).triggeringEntityId
                            if (triggeringEntityId != null) {
                                projected.getController(triggeringEntityId) ?: controllerId
                            } else {
                                controllerId
                            }
                        } else {
                            controllerId
                        }

                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entityId,
                                sourceName = cardComponent.name,
                                controllerId = effectiveControllerId,
                                triggerContext = TriggerContext.fromEvent(event)
                            )
                        )
                    }
                }
            }
        }

        // Check global granted triggered abilities (e.g., False Cure)
        detectGlobalGrantedTriggers(state, event, triggers)

        // Handle death triggers (source might not be on battlefield anymore)
        if (event is ZoneChangeEvent && event.toZone == com.wingedsheep.sdk.core.Zone.GRAVEYARD &&
            event.fromZone == com.wingedsheep.sdk.core.Zone.BATTLEFIELD) {
            detectDeathTriggers(state, event, triggers)
        }

        // Handle leaves-the-battlefield triggers (source is no longer on battlefield)
        if (event is ZoneChangeEvent && event.fromZone == com.wingedsheep.sdk.core.Zone.BATTLEFIELD) {
            detectLeavesBattlefieldTriggers(state, event, triggers)
        }

        // Handle cycling triggers on the cycled card itself (e.g., Renewed Faith)
        if (event is CardCycledEvent) {
            detectCyclingCardTriggers(state, event, triggers)
        }

        // Handle damage-received triggers for creatures no longer on the battlefield
        // (e.g., Broodhatch Nantuko dies from combat damage but trigger still fires)
        if (event is DamageDealtEvent && event.targetId !in state.getBattlefield()) {
            detectDamageReceivedTriggers(state, event, triggers)
        }

        // Handle damage-source triggers
        if (event is DamageDealtEvent && event.sourceId != null) {
            detectDamageSourceTriggers(state, event, triggers, projected)
        }

        // Handle "whenever a creature/spell deals damage to this" triggers (e.g., Tephraderm)
        if (event is DamageDealtEvent && event.sourceId != null) {
            detectDamagedBySourceTriggers(state, event, triggers)
        }

        // Handle "whenever a creature deals damage to you" triggers (e.g., Aurification)
        if (event is DamageDealtEvent && event.sourceId != null && event.targetId in state.turnOrder) {
            detectDamageToControllerTriggers(state, event, triggers, projected)
        }

        // Handle "whenever a [subtype] deals combat damage to a player" triggers (e.g., Cabal Slaver)
        if (event is DamageDealtEvent && event.sourceId != null && event.isCombatDamage && event.targetId in state.turnOrder) {
            detectSubtypeDamageToPlayerTriggers(state, event, triggers, projected)
        }

        return triggers
    }

    /**
     * Detect triggers from global granted triggered abilities (e.g., False Cure).
     * These are triggered abilities not attached to any permanent, created by
     * spell effects and stored in GameState.globalGrantedTriggeredAbilities.
     */
    private fun detectGlobalGrantedTriggers(
        state: GameState,
        event: EngineGameEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        if (state.globalGrantedTriggeredAbilities.isEmpty()) return

        for (global in state.globalGrantedTriggeredAbilities) {
            val ability = global.ability
            // Use a dummy sourceId for matchesTrigger (global abilities aren't attached to entities)
            if (matchesTrigger(ability.trigger, event, global.sourceId, global.controllerId, state)) {
                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = global.sourceId,
                        sourceName = global.sourceName,
                        controllerId = global.controllerId,
                        triggerContext = TriggerContext.fromEvent(event)
                    )
                )
            }
        }
    }

    /**
     * Detect cycling triggers on the card that was cycled.
     * Cards like Renewed Faith have "When you cycle this card, you may gain 2 life."
     * The card is now in the graveyard, but its cycling trigger still fires.
     */
    private fun detectCyclingCardTriggers(
        state: GameState,
        event: CardCycledEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.cardId
        val container = state.getEntity(entityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return

        val abilities = getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

        for (ability in abilities) {
            if (ability.trigger is OnCycle) {
                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = entityId,
                        sourceName = cardComponent.name,
                        controllerId = event.playerId,
                        triggerContext = TriggerContext(triggeringPlayerId = event.playerId)
                    )
                )
            }
        }
    }

    private fun detectDeathTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.entityId
        val container = state.getEntity(entityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return

        // Face-down creatures have no abilities (Rule 707.2)
        if (container.has<FaceDownComponent>()) return

        // For "When this creature dies" - the creature might be in graveyard now
        // Look up abilities by card definition
        val abilities = getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)
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

    /**
     * Rule 603.10: Handle "look back in time" for simultaneous deaths.
     * When multiple creatures die at the same time, each dead creature should
     * still see the others dying for trigger purposes. The main battlefield loop
     * misses these because the creatures are already in the graveyard.
     *
     * This checks dead creatures' non-self death triggers (e.g., OnOtherCreatureWithSubtypeDies,
     * OnDeath with selfOnly=false) against other death events in the same batch.
     */
    private fun detectSimultaneousDeathTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>
    ) {
        // Collect all death events from this batch
        val deathEvents = events.filterIsInstance<ZoneChangeEvent>().filter {
            it.toZone == com.wingedsheep.sdk.core.Zone.GRAVEYARD &&
                it.fromZone == com.wingedsheep.sdk.core.Zone.BATTLEFIELD
        }
        if (deathEvents.size < 2) return // Need at least 2 simultaneous deaths

        // For each dead creature, check its triggers against OTHER death events
        for (deadEvent in deathEvents) {
            val deadEntityId = deadEvent.entityId
            val container = state.getEntity(deadEntityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

            // Skip if this creature is still on the battlefield (already handled by main loop)
            if (deadEntityId in state.getBattlefield()) continue

            val abilities = getTriggeredAbilities(deadEntityId, cardComponent.cardDefinitionId, state)
            val controllerId = deadEvent.ownerId

            for (ability in abilities) {
                for (otherDeathEvent in deathEvents) {
                    if (otherDeathEvent.entityId == deadEntityId) continue // Skip self

                    if (matchesTrigger(ability.trigger, otherDeathEvent, deadEntityId, controllerId, state)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = deadEntityId,
                                sourceName = cardComponent.name,
                                controllerId = controllerId,
                                triggerContext = TriggerContext.fromEvent(otherDeathEvent)
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Detect "leaves the battlefield" triggers on permanents that just left.
     * Similar to detectDeathTriggers, but handles OnLeavesBattlefield(selfOnly=true).
     */
    private fun detectLeavesBattlefieldTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.entityId
        val container = state.getEntity(entityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return

        // Face-down creatures have no abilities (Rule 707.2)
        if (container.has<FaceDownComponent>()) return

        val abilities = getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)
        val controllerId = event.ownerId

        for (ability in abilities) {
            val trigger = ability.trigger
            if (trigger is OnLeavesBattlefield && trigger.selfOnly) {
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

    /**
     * Detect "whenever this creature is dealt damage" triggers on creatures that
     * are no longer on the battlefield (e.g., died from the damage via SBAs).
     * Similar to detectDeathTriggers pattern.
     */
    private fun detectDamageReceivedTriggers(
        state: GameState,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.targetId
        val container = state.getEntity(entityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return
        // ControllerComponent is stripped when creature dies via SBAs, fall back to ownerId
        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return

        // Face-down creatures have no abilities (Rule 707.2)
        if (container.has<FaceDownComponent>()) return

        val abilities = getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

        for (ability in abilities) {
            val trigger = ability.trigger
            if (trigger is OnDamageReceived && trigger.selfOnly) {
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

    /**
     * Detect "whenever a creature deals damage to you" triggers on permanents
     * controlled by the damaged player. The triggeringEntityId is set to the
     * damage SOURCE creature (the creature that dealt the damage).
     */
    private fun detectDamageToControllerTriggers(
        state: GameState,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState
    ) {
        val damageSourceId = event.sourceId ?: return
        val damagedPlayerId = event.targetId

        // Verify the damage source is a creature on the battlefield
        val sourceContainer = state.getEntity(damageSourceId) ?: return
        val sourceCard = sourceContainer.get<CardComponent>() ?: return
        if (!sourceCard.typeLine.isCreature) return

        // Check all permanents controlled by the damaged player for OnCreatureDealsDamageToYou triggers
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val controllerId = projected.getController(entityId) ?: continue

            // Only triggers on permanents controlled by the damaged player
            if (controllerId != damagedPlayerId) continue

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

            val abilities = getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

            for (ability in abilities) {
                if (ability.trigger is OnCreatureDealsDamageToYou) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entityId,
                            sourceName = cardComponent.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext(
                                triggeringEntityId = damageSourceId,
                                damageAmount = event.amount
                            )
                        )
                    )
                }
            }
        }
    }

    /**
     * Detect "whenever a [subtype] deals combat damage to a player" triggers
     * on all permanents on the battlefield (e.g., Cabal Slaver).
     *
     * Uses projected state to check the damage source's subtypes, so type-changing
     * effects like Artificial Evolution are respected.
     * Sets triggeringEntityId to the damaged player for "that player" resolution.
     */
    private fun detectSubtypeDamageToPlayerTriggers(
        state: GameState,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState
    ) {
        val damageSourceId = event.sourceId ?: return
        val damagedPlayerId = event.targetId

        // Verify the damage source is a creature (face-down creatures have no subtypes)
        val sourceContainer = state.getEntity(damageSourceId) ?: return
        val sourceCard = sourceContainer.get<CardComponent>() ?: return
        if (!sourceCard.typeLine.isCreature) return
        if (sourceContainer.has<FaceDownComponent>()) return

        // Check all permanents on the battlefield for matching triggers
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val controllerId = projected.getController(entityId) ?: continue

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

            val abilities = getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

            for (ability in abilities) {
                val trigger = ability.trigger
                if (trigger is OnCreatureWithSubtypeDealsCombatDamageToPlayer) {
                    // Check if the damage source has the required subtype using projected state
                    if (projected.hasSubtype(damageSourceId, trigger.subtype.value)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entityId,
                                sourceName = cardComponent.name,
                                controllerId = controllerId,
                                triggerContext = TriggerContext(
                                    triggeringEntityId = damagedPlayerId,
                                    damageAmount = event.amount
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun detectDamageSourceTriggers(
        state: GameState,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState
    ) {
        val sourceId = event.sourceId ?: return
        val container = state.getEntity(sourceId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return
        // Fall back to ownerId if ControllerComponent was stripped (e.g., creature died to SBA
        // during combat damage, but its damage trigger should still fire per Rule 603.10)
        val controllerId = projected.getController(sourceId)
            ?: container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId ?: return

        // Face-down creatures have no abilities (Rule 707.2)
        if (container.has<FaceDownComponent>()) return

        val abilities = getTriggeredAbilities(sourceId, cardComponent.cardDefinitionId, state)

        for (ability in abilities) {
            val trigger = ability.trigger
            if (trigger is OnDealsDamage) {
                if (matchesDealsDamageTrigger(trigger, event, state)) {
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

    /**
     * Detect "whenever a creature/spell deals damage to this" triggers.
     * For OnDamagedByCreature: source must be a creature on the battlefield.
     * For OnDamagedBySpell: source must be an instant or sorcery.
     * TriggeringEntityId is set to the damage SOURCE for retaliation effects.
     *
     * Handles both on-battlefield and off-battlefield cases (e.g., creature
     * dies from lethal damage but trigger still fires per Rule 603.10).
     */
    private fun detectDamagedBySourceTriggers(
        state: GameState,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val sourceId = event.sourceId ?: return
        val damagedEntityId = event.targetId

        // Get the damaged entity (might be on battlefield or in graveyard)
        val container = state.getEntity(damagedEntityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return
        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId ?: return

        // Face-down creatures have no abilities (Rule 707.2)
        if (container.has<FaceDownComponent>()) return

        val abilities = getTriggeredAbilities(damagedEntityId, cardComponent.cardDefinitionId, state)

        // Determine source type
        val sourceContainer = state.getEntity(sourceId) ?: return
        val sourceCard = sourceContainer.get<CardComponent>()
        val isCreatureSource = sourceId in state.getBattlefield() && sourceCard?.typeLine?.isCreature == true
        val isSpellSource = sourceCard != null && (sourceCard.typeLine.isInstant || sourceCard.typeLine.isSorcery)

        for (ability in abilities) {
            val trigger = ability.trigger
            val matches = when {
                trigger is OnDamagedByCreature && trigger.selfOnly && isCreatureSource -> true
                trigger is OnDamagedBySpell && trigger.selfOnly && isSpellSource -> true
                else -> false
            }

            if (matches) {
                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = damagedEntityId,
                        sourceName = cardComponent.name,
                        controllerId = controllerId,
                        triggerContext = TriggerContext(
                            triggeringEntityId = sourceId,
                            damageAmount = event.amount
                        )
                    )
                )
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
                    event.toZone == com.wingedsheep.sdk.core.Zone.BATTLEFIELD &&
                    (!trigger.selfOnly || event.entityId == sourceId)
            }

            is OnOtherCreatureEnters -> {
                if (event !is ZoneChangeEvent ||
                    event.toZone != com.wingedsheep.sdk.core.Zone.BATTLEFIELD ||
                    event.entityId == sourceId) {
                    false
                } else {
                    val projected = stateProjector.project(state)
                    val isCreature = projected.hasType(event.entityId, "CREATURE")
                    val controllerMatches = !trigger.youControlOnly || event.ownerId == controllerId
                    isCreature && controllerMatches
                }
            }

            is OnLeavesBattlefield -> {
                event is ZoneChangeEvent &&
                    event.fromZone == com.wingedsheep.sdk.core.Zone.BATTLEFIELD &&
                    (!trigger.selfOnly || event.entityId == sourceId)
            }

            is OnDeath -> {
                event is ZoneChangeEvent &&
                    event.toZone == com.wingedsheep.sdk.core.Zone.GRAVEYARD &&
                    event.fromZone == com.wingedsheep.sdk.core.Zone.BATTLEFIELD &&
                    (!trigger.selfOnly || event.entityId == sourceId) &&
                    (!trigger.youControlOnly || event.ownerId == controllerId)
            }

            is OnOtherCreatureWithSubtypeDies -> {
                if (event !is ZoneChangeEvent ||
                    event.toZone != com.wingedsheep.sdk.core.Zone.GRAVEYARD ||
                    event.fromZone != com.wingedsheep.sdk.core.Zone.BATTLEFIELD ||
                    event.entityId == sourceId) {
                    false
                } else {
                    // The dying creature is no longer on the battlefield, so projected state
                    // won't have its subtypes. Use base state CardComponent instead.
                    // Face-down creatures have no subtypes (Rule 707.2).
                    val dyingEntity = state.getEntity(event.entityId)
                    val isFaceDown = dyingEntity?.has<FaceDownComponent>() == true
                    val hasSubtype = if (isFaceDown) {
                        false
                    } else {
                        val cardComponent = dyingEntity?.get<CardComponent>()
                        cardComponent?.typeLine?.hasSubtype(com.wingedsheep.sdk.core.Subtype(trigger.subtype.value)) == true
                    }
                    val controllerMatches = !trigger.youControlOnly || event.ownerId == controllerId
                    hasSubtype && controllerMatches
                }
            }

            is OnOtherCreatureWithSubtypeEnters -> {
                // Rule 702.37e: Turning face-up doesn't cause entering the battlefield
                event is ZoneChangeEvent &&
                    event.toZone == com.wingedsheep.sdk.core.Zone.BATTLEFIELD &&
                    event.entityId != sourceId &&
                    run {
                        val projected = stateProjector.project(state)
                        val hasSubtype = projected.hasSubtype(event.entityId, trigger.subtype.value)
                        val controllerMatches = !trigger.youControlOnly || event.ownerId == controllerId
                        hasSubtype && controllerMatches
                    }
            }

            is OnCreatureWithSubtypeEnters -> {
                // Rule 702.37e: Turning face-up doesn't cause entering the battlefield
                event is ZoneChangeEvent &&
                    event.toZone == com.wingedsheep.sdk.core.Zone.BATTLEFIELD &&
                    run {
                        val projected = stateProjector.project(state)
                        val hasSubtype = projected.hasSubtype(event.entityId, trigger.subtype.value)
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

            is OnBecomesBlocked -> {
                event is BlockersDeclaredEvent &&
                    (!trigger.selfOnly || event.blockers.values.any { it.contains(sourceId) })
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

            is OnCycle -> {
                event is CardCycledEvent &&
                    (!trigger.controllerOnly || event.playerId == controllerId)
            }

            is OnBecomesTapped -> {
                event is TappedEvent &&
                    (!trigger.selfOnly || event.entityId == sourceId)
            }

            is OnBecomesUntapped -> {
                event is UntappedEvent &&
                    (!trigger.selfOnly || event.entityId == sourceId)
            }

            // Handled separately in detectDamageToControllerTriggers
            is OnCreatureDealsDamageToYou -> false

            // Handled separately in detectSubtypeDamageToPlayerTriggers
            is OnCreatureWithSubtypeDealsCombatDamageToPlayer -> false

            // Handled separately in detectDamagedBySourceTriggers
            is OnDamagedByCreature -> false
            is OnDamagedBySpell -> false

            // Phase/step triggers are handled separately
            is OnUpkeep, is OnEndStep, is OnBeginCombat, is OnFirstMainPhase -> false
            is OnEnchantedCreatureControllerUpkeep -> false

            is OnLifeGain -> {
                event is LifeChangedEvent &&
                    event.reason == com.wingedsheep.engine.core.LifeChangeReason.LIFE_GAIN &&
                    (!trigger.controllerOnly || event.playerId == controllerId)
            }

            is OnTransform -> {
                // Transform not yet implemented in new engine
                false
            }

            is OnTurnFaceUp -> {
                event is TurnFaceUpEvent &&
                    (!trigger.selfOnly || event.entityId == sourceId)
            }
        }
    }

    private fun matchesDealsDamageTrigger(trigger: OnDealsDamage, event: DamageDealtEvent, state: GameState): Boolean {
        val combatMatches = !trigger.combatOnly || event.isCombatDamage
        val targetMatches = !trigger.toPlayerOnly || event.targetId in state.turnOrder
        val creatureMatches = !trigger.toCreatureOnly || event.targetId !in state.turnOrder
        return combatMatches && targetMatches && creatureMatches
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

            // Handled specially in detectPhaseStepTriggers
            is OnEnchantedCreatureControllerUpkeep -> false

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
            SpellTypeFilter.ENCHANTMENT -> cardComponent.typeLine.isEnchantment
        }
    }

    /**
     * Filter out triggers whose intervening-if condition (Rule 603.4) is not met.
     * If a triggered ability has a triggerCondition, it is only allowed to fire
     * when that condition is true at the time of trigger detection.
     */
    private fun filterByTriggerCondition(
        state: GameState,
        triggers: List<PendingTrigger>
    ): List<PendingTrigger> {
        return triggers.filter { trigger ->
            val condition = trigger.ability.triggerCondition ?: return@filter true
            val context = EffectContext(
                sourceId = trigger.sourceId,
                controllerId = trigger.controllerId,
                opponentId = state.turnOrder.firstOrNull { it != trigger.controllerId }
            )
            conditionEvaluator.evaluate(state, condition, context)
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
@kotlinx.serialization.Serializable
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
@kotlinx.serialization.Serializable
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
                is CardCycledEvent -> TriggerContext(triggeringPlayerId = event.playerId)
                is AttackersDeclaredEvent -> TriggerContext()
                is BlockersDeclaredEvent -> TriggerContext()
                is TappedEvent -> TriggerContext(triggeringEntityId = event.entityId)
                is UntappedEvent -> TriggerContext(triggeringEntityId = event.entityId)
                is LifeChangedEvent -> TriggerContext(
                    triggeringEntityId = event.playerId,
                    triggeringPlayerId = event.playerId,
                    damageAmount = if (event.reason == com.wingedsheep.engine.core.LifeChangeReason.LIFE_GAIN)
                        (event.newLife - event.oldLife) else null
                )
                is TurnFaceUpEvent -> TriggerContext(
                    triggeringEntityId = event.entityId,
                    triggeringPlayerId = event.controllerId
                )
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
