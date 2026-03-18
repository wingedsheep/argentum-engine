package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.GameEvent

/**
 * Handles all aura/equipment "enchanted/equipped creature" triggers.
 */
class AttachmentTriggerDetector {

    /**
     * Detect "when enchanted creature is dealt damage" triggers on auras.
     * Uses pre-indexed auras-by-target instead of scanning all battlefield permanents.
     */
    fun detectEnchantedCreatureDamageTriggers(
        state: GameState,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState,
        index: TriggerIndex
    ) {
        val damagedEntityId = event.targetId

        for (entry in index.aurasByTarget[damagedEntityId].orEmpty()) {
            for (ability in entry.abilities) {
                if (ability.trigger is GameEvent.EnchantedCreatureDamageReceivedEvent) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = entry.controllerId,
                            triggerContext = TriggerContext.fromEvent(event)
                        )
                    )
                }
            }
        }
    }

    /**
     * Detect "when enchanted creature deals combat damage to a player" triggers on auras.
     * Uses pre-indexed auras-by-target instead of scanning all battlefield permanents.
     */
    fun detectEnchantedCreatureDealsDamageTriggers(
        state: GameState,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState,
        index: TriggerIndex
    ) {
        val sourceEntityId = event.sourceId ?: return

        for (entry in index.aurasByTarget[sourceEntityId].orEmpty()) {
            for (ability in entry.abilities) {
                if (ability.trigger is GameEvent.EnchantedCreatureDealsCombatDamageToPlayerEvent) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = entry.controllerId,
                            triggerContext = TriggerContext.fromEvent(event)
                        )
                    )
                }
            }
        }
    }

    /**
     * Detect "when enchanted creature deals damage" (any type) triggers on auras.
     * Uses pre-indexed auras-by-target instead of scanning all battlefield permanents.
     */
    fun detectEnchantedCreatureDealsDamageAnyTriggers(
        state: GameState,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState,
        index: TriggerIndex
    ) {
        val sourceEntityId = event.sourceId ?: return

        for (entry in index.aurasByTarget[sourceEntityId].orEmpty()) {
            for (ability in entry.abilities) {
                if (ability.trigger is GameEvent.EnchantedCreatureDealsDamageEvent) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = entry.controllerId,
                            triggerContext = TriggerContext.fromEvent(event)
                        )
                    )
                }
            }
        }
    }

    /**
     * Detect "when attached creature attacks" triggers on auras and equipment.
     * Uses pre-indexed auras-by-target (which includes equipment) instead of scanning all battlefield permanents.
     * Matches AttachedCreatureAttacksEvent — the generalized event for both auras and equipment.
     */
    fun detectAttachedCreatureAttacksTriggers(
        state: GameState,
        event: AttackersDeclaredEvent,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState,
        index: TriggerIndex
    ) {
        for (attackerId in event.attackers) {
            for (entry in index.aurasByTarget[attackerId].orEmpty()) {
                for (ability in entry.abilities) {
                    if (ability.trigger is GameEvent.AttachedCreatureAttacksEvent) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entry.entityId,
                                sourceName = entry.cardComponent.name,
                                controllerId = entry.controllerId,
                                triggerContext = TriggerContext(triggeringEntityId = attackerId)
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Detect "when enchanted creature is turned face up" triggers on auras.
     * Uses pre-indexed auras-by-target instead of scanning all battlefield permanents.
     */
    fun detectEnchantedCreatureTurnedFaceUpTriggers(
        state: GameState,
        event: TurnFaceUpEvent,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState,
        index: TriggerIndex
    ) {
        val turnedFaceUpEntityId = event.entityId

        for (entry in index.aurasByTarget[turnedFaceUpEntityId].orEmpty()) {
            for (ability in entry.abilities) {
                if (ability.trigger is GameEvent.EnchantedCreatureTurnedFaceUpEvent) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = entry.controllerId,
                            triggerContext = TriggerContext.fromEvent(event)
                        )
                    )
                }
            }
        }
    }

    /**
     * Detect "when enchanted permanent becomes tapped" triggers on auras.
     * Uses pre-indexed auras-by-target instead of scanning all battlefield permanents.
     */
    fun detectEnchantedPermanentBecomesTappedTriggers(
        state: GameState,
        event: TappedEvent,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState,
        index: TriggerIndex
    ) {
        val tappedEntityId = event.entityId

        for (entry in index.aurasByTarget[tappedEntityId].orEmpty()) {
            for (ability in entry.abilities) {
                if (ability.trigger is GameEvent.EnchantedPermanentBecomesTappedEvent) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = entry.controllerId,
                            triggerContext = TriggerContext.fromEvent(event)
                        )
                    )
                }
            }
        }
    }
}
