package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.action.GameEvent
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.game.Phase
import com.wingedsheep.rulesengine.game.Step
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.zone.ZoneType

/**
 * Detects when triggers should fire based on game events.
 */
object TriggerDetector {

    /**
     * Check all events and return any triggered abilities that should fire.
     * Returns triggers in APNAP order (Active Player, Non-Active Player).
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
        return sortByAPNAP(triggers, state)
    }

    /**
     * Detect triggers for a single event.
     */
    private fun detectTriggersForEvent(
        state: GameState,
        event: GameEvent,
        abilityRegistry: AbilityRegistry
    ): List<PendingTrigger> {
        return when (event) {
            is GameEvent.CardMoved -> detectZoneChangeTriggers(state, event, abilityRegistry)
            is GameEvent.CreatureDied -> detectDeathTriggers(state, event, abilityRegistry)
            is GameEvent.CardDrawn -> detectDrawTriggers(state, event, abilityRegistry)
            is GameEvent.DamageDealt -> detectDamageTriggers(state, event, abilityRegistry)
            else -> emptyList()
        }
    }

    /**
     * Detect triggers for zone changes (enter/leave battlefield).
     */
    private fun detectZoneChangeTriggers(
        state: GameState,
        event: GameEvent.CardMoved,
        abilityRegistry: AbilityRegistry
    ): List<PendingTrigger> {
        val triggers = mutableListOf<PendingTrigger>()
        val context = TriggerContext.ZoneChange(
            cardId = CardId(event.cardId),
            cardName = event.cardName,
            fromZone = event.fromZone,
            toZone = event.toZone
        )

        // Check for enter battlefield triggers
        if (event.toZone == ZoneType.BATTLEFIELD.name) {
            // Check abilities on all permanents (including the one that just entered)
            for (permanent in state.battlefield.cards) {
                val abilities = abilityRegistry.getTriggeredAbilities(permanent.definition)
                for (ability in abilities) {
                    if (shouldTriggerOnEnterBattlefield(ability, permanent, event)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = permanent.id,
                                sourceName = permanent.name,
                                controllerId = PlayerId.of(permanent.controllerId),
                                triggerContext = context
                            )
                        )
                    }
                }
            }
        }

        // Check for leave battlefield triggers
        if (event.fromZone == ZoneType.BATTLEFIELD.name) {
            // The permanent has left, so we need to check remembered abilities
            // For "when this leaves" triggers, we use last-known information
            for (permanent in state.battlefield.cards) {
                val abilities = abilityRegistry.getTriggeredAbilities(permanent.definition)
                for (ability in abilities) {
                    if (shouldTriggerOnLeavesBattlefield(ability, permanent, event)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = permanent.id,
                                sourceName = permanent.name,
                                controllerId = PlayerId.of(permanent.controllerId),
                                triggerContext = context
                            )
                        )
                    }
                }
            }
        }

        return triggers
    }

    /**
     * Detect death triggers.
     */
    private fun detectDeathTriggers(
        state: GameState,
        event: GameEvent.CreatureDied,
        abilityRegistry: AbilityRegistry
    ): List<PendingTrigger> {
        val triggers = mutableListOf<PendingTrigger>()
        val context = TriggerContext.ZoneChange(
            cardId = CardId(event.cardId),
            cardName = event.cardName,
            fromZone = ZoneType.BATTLEFIELD.name,
            toZone = ZoneType.GRAVEYARD.name
        )

        // Check for "whenever a creature dies" triggers on all permanents
        for (permanent in state.battlefield.cards) {
            val abilities = abilityRegistry.getTriggeredAbilities(permanent.definition)
            for (ability in abilities) {
                if (shouldTriggerOnDeath(ability, permanent, event)) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = permanent.id,
                            sourceName = permanent.name,
                            controllerId = PlayerId.of(permanent.controllerId),
                            triggerContext = context
                        )
                    )
                }
            }
        }

        return triggers
    }

    /**
     * Detect draw triggers.
     */
    private fun detectDrawTriggers(
        state: GameState,
        event: GameEvent.CardDrawn,
        abilityRegistry: AbilityRegistry
    ): List<PendingTrigger> {
        val triggers = mutableListOf<PendingTrigger>()
        val context = TriggerContext.CardDrawn(
            playerId = PlayerId.of(event.playerId),
            cardId = CardId(event.cardId)
        )

        for (permanent in state.battlefield.cards) {
            val abilities = abilityRegistry.getTriggeredAbilities(permanent.definition)
            for (ability in abilities) {
                if (shouldTriggerOnDraw(ability, permanent, event)) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = permanent.id,
                            sourceName = permanent.name,
                            controllerId = PlayerId.of(permanent.controllerId),
                            triggerContext = context
                        )
                    )
                }
            }
        }

        return triggers
    }

    /**
     * Detect damage triggers.
     */
    private fun detectDamageTriggers(
        state: GameState,
        event: GameEvent.DamageDealt,
        abilityRegistry: AbilityRegistry
    ): List<PendingTrigger> {
        val triggers = mutableListOf<PendingTrigger>()
        val context = TriggerContext.DamageDealt(
            sourceId = event.sourceId?.let { CardId(it) },
            targetId = event.targetId,
            amount = event.amount,
            isPlayer = event.isPlayer,
            isCombat = false // Would need additional context for combat damage
        )

        for (permanent in state.battlefield.cards) {
            val abilities = abilityRegistry.getTriggeredAbilities(permanent.definition)
            for (ability in abilities) {
                if (shouldTriggerOnDamage(ability, permanent, event)) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = permanent.id,
                            sourceName = permanent.name,
                            controllerId = PlayerId.of(permanent.controllerId),
                            triggerContext = context
                        )
                    )
                }
            }
        }

        return triggers
    }

    /**
     * Detect phase/step triggers (upkeep, end step, etc.)
     */
    fun detectPhaseStepTriggers(
        state: GameState,
        phase: Phase,
        step: Step,
        abilityRegistry: AbilityRegistry
    ): List<PendingTrigger> {
        val triggers = mutableListOf<PendingTrigger>()
        val context = TriggerContext.PhaseStep(
            phase = phase.name,
            step = step.name,
            activePlayerId = state.turnState.activePlayer
        )

        for (permanent in state.battlefield.cards) {
            val abilities = abilityRegistry.getTriggeredAbilities(permanent.definition)
            for (ability in abilities) {
                when (ability.trigger) {
                    is OnUpkeep -> {
                        if (step == Step.UPKEEP) {
                            val trigger = ability.trigger as OnUpkeep
                            val shouldFire = if (trigger.controllerOnly) {
                                permanent.controllerId == state.turnState.activePlayer.value
                            } else {
                                true
                            }
                            if (shouldFire) {
                                triggers.add(
                                    PendingTrigger(
                                        ability = ability,
                                        sourceId = permanent.id,
                                        sourceName = permanent.name,
                                        controllerId = PlayerId.of(permanent.controllerId),
                                        triggerContext = context
                                    )
                                )
                            }
                        }
                    }
                    is OnEndStep -> {
                        if (step == Step.END) {
                            val trigger = ability.trigger as OnEndStep
                            val shouldFire = if (trigger.controllerOnly) {
                                permanent.controllerId == state.turnState.activePlayer.value
                            } else {
                                true
                            }
                            if (shouldFire) {
                                triggers.add(
                                    PendingTrigger(
                                        ability = ability,
                                        sourceId = permanent.id,
                                        sourceName = permanent.name,
                                        controllerId = PlayerId.of(permanent.controllerId),
                                        triggerContext = context
                                    )
                                )
                            }
                        }
                    }
                    is OnBeginCombat -> {
                        if (step == Step.BEGIN_COMBAT) {
                            val trigger = ability.trigger as OnBeginCombat
                            val shouldFire = if (trigger.controllerOnly) {
                                permanent.controllerId == state.turnState.activePlayer.value
                            } else {
                                true
                            }
                            if (shouldFire) {
                                triggers.add(
                                    PendingTrigger(
                                        ability = ability,
                                        sourceId = permanent.id,
                                        sourceName = permanent.name,
                                        controllerId = PlayerId.of(permanent.controllerId),
                                        triggerContext = context
                                    )
                                )
                            }
                        }
                    }
                    else -> { /* Not a phase/step trigger */ }
                }
            }
        }

        return sortByAPNAP(triggers, state)
    }

    // =============================================================================
    // Trigger Condition Checks
    // =============================================================================

    private fun shouldTriggerOnEnterBattlefield(
        ability: TriggeredAbility,
        sourcePermanent: CardInstance,
        event: GameEvent.CardMoved
    ): Boolean {
        val trigger = ability.trigger
        if (trigger !is OnEnterBattlefield) return false

        return if (trigger.selfOnly) {
            // Only triggers when this specific permanent enters
            event.cardId == sourcePermanent.id.value
        } else {
            // Triggers whenever any permanent enters
            true
        }
    }

    private fun shouldTriggerOnLeavesBattlefield(
        ability: TriggeredAbility,
        sourcePermanent: CardInstance,
        event: GameEvent.CardMoved
    ): Boolean {
        val trigger = ability.trigger
        if (trigger !is OnLeavesBattlefield) return false

        return if (trigger.selfOnly) {
            // This would trigger from the card that left
            // Since it's no longer on battlefield, we need last-known info
            event.cardId == sourcePermanent.id.value
        } else {
            true
        }
    }

    private fun shouldTriggerOnDeath(
        ability: TriggeredAbility,
        sourcePermanent: CardInstance,
        event: GameEvent.CreatureDied
    ): Boolean {
        val trigger = ability.trigger
        if (trigger !is OnDeath) return false

        return if (trigger.selfOnly) {
            // Note: "When this creature dies" triggers from the graveyard using LKI
            // The source permanent might not be on the battlefield anymore
            event.cardId == sourcePermanent.id.value
        } else {
            true
        }
    }

    private fun shouldTriggerOnDraw(
        ability: TriggeredAbility,
        sourcePermanent: CardInstance,
        event: GameEvent.CardDrawn
    ): Boolean {
        val trigger = ability.trigger
        if (trigger !is OnDraw) return false

        return if (trigger.controllerOnly) {
            event.playerId == sourcePermanent.controllerId
        } else {
            true
        }
    }

    private fun shouldTriggerOnDamage(
        ability: TriggeredAbility,
        sourcePermanent: CardInstance,
        event: GameEvent.DamageDealt
    ): Boolean {
        val trigger = ability.trigger

        return when (trigger) {
            is OnDealsDamage -> {
                if (trigger.selfOnly) {
                    event.sourceId == sourcePermanent.id.value
                } else {
                    true
                }
            }
            is OnDamageReceived -> {
                if (trigger.selfOnly) {
                    !event.isPlayer && event.targetId == sourcePermanent.id.value
                } else {
                    !event.isPlayer
                }
            }
            else -> false
        }
    }

    // =============================================================================
    // APNAP Ordering
    // =============================================================================

    /**
     * Sort triggers by APNAP order (Active Player, Non-Active Player).
     * Within each player's triggers, they can choose the order.
     */
    private fun sortByAPNAP(triggers: List<PendingTrigger>, state: GameState): List<PendingTrigger> {
        val activePlayer = state.turnState.activePlayer
        val playerOrder = state.turnState.playerOrder

        return triggers.sortedBy { trigger ->
            val controllerIndex = playerOrder.indexOf(trigger.controllerId)
            val activeIndex = playerOrder.indexOf(activePlayer)
            // Rotate so active player comes first
            (controllerIndex - activeIndex + playerOrder.size) % playerOrder.size
        }
    }
}
