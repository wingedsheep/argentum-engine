package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChainCopyEffect
import kotlinx.serialization.Serializable

/**
 * Resume after primary discard effect needs card selection (Chain of Smog).
 *
 * When the card selection is answered, discards the selected cards, then offers
 * the chain copy via ChainCopyDecisionContinuation.
 *
 * @property effect The unified chain copy effect (carries all variant info)
 * @property playerId The player who is discarding
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class ChainCopyPrimaryDiscardContinuation(
    override val decisionId: String,
    val effect: ChainCopyEffect,
    val playerId: EntityId,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after the affected player decides whether to copy the chain spell (yes/no).
 *
 * - Yes → present cost payment (if any) or target selection
 * - No → chain ends
 *
 * @property effect The unified chain copy effect
 * @property copyControllerId The player who gets to copy
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class ChainCopyDecisionContinuation(
    override val decisionId: String,
    val effect: ChainCopyEffect,
    val copyControllerId: EntityId,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after the copying player selects a cost resource (land to sacrifice / card to discard).
 *
 * After paying cost, presents target selection for the copy.
 *
 * @property effect The unified chain copy effect
 * @property copyControllerId The player who is creating the copy
 * @property sourceId The source entity of the original spell/ability
 * @property candidateOptions The list of valid cost resource entity IDs (for validation)
 */
@Serializable
data class ChainCopyCostContinuation(
    override val decisionId: String,
    val effect: ChainCopyEffect,
    val copyControllerId: EntityId,
    val sourceId: EntityId?,
    val candidateOptions: List<EntityId>
) : ContinuationFrame

/**
 * Resume after the copying player selects a target for the chain copy.
 *
 * Creates a TriggeredAbilityOnStackComponent with ChainCopyEffect targeting
 * the selected entity, enabling recursive chaining.
 *
 * @property effect The unified chain copy effect
 * @property copyControllerId The player who is creating the copy
 * @property sourceId The source entity of the original spell/ability
 * @property candidateTargets The list of valid target entity IDs (for validation)
 */
@Serializable
data class ChainCopyTargetContinuation(
    override val decisionId: String,
    val effect: ChainCopyEffect,
    val copyControllerId: EntityId,
    val sourceId: EntityId?,
    val candidateTargets: List<EntityId>
) : ContinuationFrame
