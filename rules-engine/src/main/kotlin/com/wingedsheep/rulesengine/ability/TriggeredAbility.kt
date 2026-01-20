package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.targeting.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * A triggered ability is an ability that fires when a specific condition is met.
 * It combines a trigger condition with an effect.
 *
 * Triggered abilities can optionally require targets. When a triggered ability
 * has a targetRequirement, the player must choose valid targets when the ability
 * goes on the stack. If no legal targets exist, the ability is removed from
 * the stack without resolving.
 */
@Serializable
data class TriggeredAbility(
    val id: AbilityId,
    val trigger: Trigger,
    val effect: Effect,
    val optional: Boolean = false,
    val targetRequirement: TargetRequirement? = null
) {
    val description: String
        get() = buildString {
            append(trigger.description)
            if (optional) append(", you may")
            append(", ")
            if (targetRequirement != null) {
                append(targetRequirement.description)
                append(" ")
            }
            append(effect.description.replaceFirstChar { it.lowercase() })
            append(".")
        }

    /** Whether this triggered ability requires targets */
    val requiresTargets: Boolean
        get() = targetRequirement != null

    companion object {
        fun create(
            trigger: Trigger,
            effect: Effect,
            optional: Boolean = false,
            targetRequirement: TargetRequirement? = null
        ): TriggeredAbility =
            TriggeredAbility(
                id = AbilityId.generate(),
                trigger = trigger,
                effect = effect,
                optional = optional,
                targetRequirement = targetRequirement
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
 *
 * Note: Uses ChosenTarget from ecs.event package for target representation.
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
