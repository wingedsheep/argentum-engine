package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Effect
import kotlinx.serialization.Serializable

/**
 * Represents a continuation frame - a reified "what to do next" after a decision.
 *
 * When the engine pauses for player input (e.g., "choose cards to discard"),
 * it pushes a ContinuationFrame onto the stack describing how to resume
 * execution once the player responds.
 *
 * This is a serializable alternative to closures/lambdas, allowing the
 * continuation state to be persisted and transferred across sessions.
 */
@Serializable
sealed interface ContinuationFrame {
    /** The decision ID this continuation is waiting for */
    val decisionId: String
}

/**
 * Resume after player selects cards to discard.
 *
 * @property playerId The player who is discarding
 * @property sourceId The spell/ability that caused the discard (for events)
 * @property sourceName Name of the source for event messages
 */
@Serializable
data class DiscardContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume after player orders cards for scry.
 *
 * @property playerId The player who is scrying
 * @property sourceId The spell/ability that caused the scry
 * @property sourceName Name of the source for event messages
 */
@Serializable
data class ScryContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume a composite effect with remaining effects to execute.
 *
 * When a sub-effect of a CompositeEffect pauses for a decision, we push
 * this frame to remember which effects still need to run after the
 * decision is resolved.
 *
 * @property remainingEffects Effects that still need to execute (serialized)
 * @property context The execution context for these effects
 */
@Serializable
data class EffectContinuation(
    override val decisionId: String,
    val remainingEffects: List<Effect>,
    val sourceId: EntityId?,
    val controllerId: EntityId,
    val opponentId: EntityId?,
    val xValue: Int?
) : ContinuationFrame {
    /**
     * Reconstruct the EffectContext from serialized fields.
     */
    fun toEffectContext(): EffectContext = EffectContext(
        sourceId = sourceId,
        controllerId = controllerId,
        opponentId = opponentId,
        xValue = xValue
    )
}

/**
 * Resume a triggered ability after target selection.
 *
 * @property triggeredAbilityId The entity ID of the triggered ability on the stack
 * @property sourceId The permanent that has the triggered ability
 * @property controllerId The controller of the triggered ability
 */
@Serializable
data class TriggeredAbilityContinuation(
    override val decisionId: String,
    val triggeredAbilityId: EntityId,
    val sourceId: EntityId,
    val controllerId: EntityId
) : ContinuationFrame

/**
 * Resume combat damage assignment.
 *
 * @property attackerId The attacking creature assigning damage
 * @property defendingPlayerId The defending player
 */
@Serializable
data class DamageAssignmentContinuation(
    override val decisionId: String,
    val attackerId: EntityId,
    val defendingPlayerId: EntityId
) : ContinuationFrame

/**
 * Resume spell resolution after target or mode selection.
 *
 * @property spellId The spell entity on the stack
 * @property casterId The player who cast the spell
 */
@Serializable
data class ResolveSpellContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val casterId: EntityId
) : ContinuationFrame

/**
 * Resume after player selects cards for sacrifice.
 *
 * @property playerId The player who is sacrificing
 * @property sourceId The spell/ability that caused the sacrifice
 * @property sourceName Name of the source for event messages
 */
@Serializable
data class SacrificeContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume after player makes a yes/no choice (may abilities).
 *
 * @property playerId The player who made the choice
 * @property sourceId The spell/ability with the may clause
 * @property sourceName Name of the source
 * @property effectIfYes The effect to execute if player chose yes
 * @property effectIfNo The effect to execute if player chose no (usually null/no-op)
 */
@Serializable
data class MayAbilityContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val effectIfYes: Effect?,
    val effectIfNo: Effect?,
    val controllerId: EntityId,
    val opponentId: EntityId?,
    val xValue: Int?
) : ContinuationFrame {
    fun toEffectContext(): EffectContext = EffectContext(
        sourceId = sourceId,
        controllerId = controllerId,
        opponentId = opponentId,
        xValue = xValue
    )
}
