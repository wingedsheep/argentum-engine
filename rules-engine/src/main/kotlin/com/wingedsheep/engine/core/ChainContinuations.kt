package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChainCopyEffect
import kotlinx.serialization.Serializable

/**
 * Resume after the inner action's continuations have all resolved.
 *
 * Pushed before executing the inner action; fires after any intermediate
 * decisions (e.g., discard card selection) complete. Simply calls offerChainCopy().
 *
 * @property effect The unified chain copy effect (carries all variant info)
 * @property recipientPlayerId The player who will be offered the copy
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class ChainCopyAfterActionContinuation(
    val effect: ChainCopyEffect,
    val recipientPlayerId: EntityId,
    val sourceId: EntityId?,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
) : AutomaticContinuation

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
    val effect: ChainCopyEffect,
    val copyControllerId: EntityId,
    val sourceId: EntityId?,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
) : AnswerContinuation

/**
 * Resume after the copying player pays the copy cost: selects a cost resource (land to sacrifice /
 * card to discard), or — for a mana copy cost (Chain Lightning's {R}{R}) — picks mana sources in a
 * mana-payment window.
 *
 * After paying cost, presents target selection for the copy.
 *
 * @property effect The unified chain copy effect
 * @property copyControllerId The player who is creating the copy
 * @property sourceId The source entity of the original spell/ability
 * @property candidateOptions The list of valid cost resource entity IDs (for validation); empty for a mana cost
 * @property availableSources The mana-payment window's source menu, for a mana copy cost
 */
@Serializable
data class ChainCopyCostContinuation(
    val effect: ChainCopyEffect,
    val copyControllerId: EntityId,
    val sourceId: EntityId?,
    val candidateOptions: List<EntityId>,
    val availableSources: List<ManaSourceOption> = emptyList(),
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
) : AnswerContinuation

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
 * @property remainingCopies Copies still to be made, this one included — more than one when a
 *   copy-count replacement (Twinning Staff) resized "copy this spell"; each gets its own target
 */
@Serializable
data class ChainCopyTargetContinuation(
    val effect: ChainCopyEffect,
    val copyControllerId: EntityId,
    val sourceId: EntityId?,
    val candidateTargets: List<EntityId>,
    val remainingCopies: Int = 1,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
) : AnswerContinuation
