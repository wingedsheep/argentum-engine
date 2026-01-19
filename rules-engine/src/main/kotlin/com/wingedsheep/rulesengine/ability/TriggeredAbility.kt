package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * A triggered ability is an ability that fires when a specific condition is met.
 * It combines a trigger condition with an effect.
 */
@Serializable
data class TriggeredAbility(
    val id: AbilityId,
    val trigger: Trigger,
    val effect: Effect,
    val optional: Boolean = false
) {
    val description: String
        get() = buildString {
            append(trigger.description)
            if (optional) append(", you may")
            append(", ")
            append(effect.description.replaceFirstChar { it.lowercase() })
            append(".")
        }

    companion object {
        fun create(trigger: Trigger, effect: Effect, optional: Boolean = false): TriggeredAbility =
            TriggeredAbility(
                id = AbilityId.generate(),
                trigger = trigger,
                effect = effect,
                optional = optional
            )
    }
}

/**
 * Unique identifier for an ability instance.
 */
@JvmInline
@Serializable
value class AbilityId(val value: String) {
    companion object {
        private var counter = 0L

        fun generate(): AbilityId = AbilityId("ability_${++counter}")
    }
}

/**
 * Represents a triggered ability that has been triggered and is waiting to go on the stack.
 * Contains information about the source, controller, and any relevant context.
 */
@Serializable
data class PendingTrigger(
    val ability: TriggeredAbility,
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val triggerContext: TriggerContext
) {
    val description: String
        get() = "$sourceName: ${ability.description}"
}

/**
 * Context information about what caused a trigger to fire.
 * This information may be needed when the ability resolves.
 */
@Serializable
sealed interface TriggerContext {
    /** No additional context needed */
    @Serializable
    data object None : TriggerContext

    /** Context for zone change triggers */
    @Serializable
    data class ZoneChange(
        val cardId: EntityId,
        val cardName: String,
        val fromZone: String,
        val toZone: String
    ) : TriggerContext

    /** Context for damage triggers */
    @Serializable
    data class DamageDealt(
        val sourceId: EntityId?,
        val targetId: EntityId,
        val amount: Int,
        val isPlayer: Boolean,
        val isCombat: Boolean
    ) : TriggerContext

    /** Context for phase/step triggers */
    @Serializable
    data class PhaseStep(
        val phase: String,
        val step: String,
        val activePlayerId: EntityId
    ) : TriggerContext

    /** Context for spell cast triggers */
    @Serializable
    data class SpellCast(
        val spellId: EntityId,
        val spellName: String,
        val casterId: EntityId
    ) : TriggerContext

    /** Context for card draw triggers */
    @Serializable
    data class CardDrawn(
        val playerId: EntityId,
        val cardId: EntityId
    ) : TriggerContext

    /** Context for attack/block triggers */
    @Serializable
    data class Combat(
        val attackerId: EntityId?,
        val blockerId: EntityId?,
        val defendingPlayerId: EntityId?
    ) : TriggerContext
}

/**
 * A triggered ability on the stack, ready to resolve.
 * This is what gets put on the stack when a trigger fires.
 */
@Serializable
data class StackedTrigger(
    val pendingTrigger: PendingTrigger,
    val chosenTargets: List<ChosenTarget> = emptyList()
) {
    val sourceId: EntityId get() = pendingTrigger.sourceId
    val controllerId: EntityId get() = pendingTrigger.controllerId
    val description: String get() = pendingTrigger.description
}

/**
 * A target that has been chosen for an effect.
 */
@Serializable
sealed interface ChosenTarget {
    /**
     * A player as a chosen target.
     */
    @Serializable
    data class PlayerTarget(val entityId: EntityId) : ChosenTarget

    /**
     * A card/permanent as a chosen target.
     */
    @Serializable
    data class CardTarget(val entityId: EntityId) : ChosenTarget
}
