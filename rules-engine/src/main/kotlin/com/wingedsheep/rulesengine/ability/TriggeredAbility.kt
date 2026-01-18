package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.player.PlayerId
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
    val sourceId: CardId,
    val sourceName: String,
    val controllerId: PlayerId,
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
        val cardId: CardId,
        val cardName: String,
        val fromZone: String,
        val toZone: String
    ) : TriggerContext

    /** Context for damage triggers */
    @Serializable
    data class DamageDealt(
        val sourceId: CardId?,
        val targetId: String,
        val amount: Int,
        val isPlayer: Boolean,
        val isCombat: Boolean
    ) : TriggerContext

    /** Context for phase/step triggers */
    @Serializable
    data class PhaseStep(
        val phase: String,
        val step: String,
        val activePlayerId: PlayerId
    ) : TriggerContext

    /** Context for spell cast triggers */
    @Serializable
    data class SpellCast(
        val spellId: CardId,
        val spellName: String,
        val casterId: PlayerId
    ) : TriggerContext

    /** Context for card draw triggers */
    @Serializable
    data class CardDrawn(
        val playerId: PlayerId,
        val cardId: CardId
    ) : TriggerContext

    /** Context for attack/block triggers */
    @Serializable
    data class Combat(
        val attackerId: CardId?,
        val blockerId: CardId?,
        val defendingPlayerId: PlayerId?
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
    val sourceId: CardId get() = pendingTrigger.sourceId
    val controllerId: PlayerId get() = pendingTrigger.controllerId
    val description: String get() = pendingTrigger.description
}

/**
 * A target that has been chosen for an effect.
 */
@Serializable
sealed interface ChosenTarget {
    @Serializable
    data class PlayerTarget(val playerId: PlayerId) : ChosenTarget

    @Serializable
    data class CardTarget(val cardId: CardId) : ChosenTarget
}
