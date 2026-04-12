package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import kotlinx.serialization.Serializable

/**
 * Resume token creation after the player answers a "may" question for
 * ReplaceTokenCreationWithEquippedCopy (Mirrormind Crown).
 *
 * If yes: create [tokenCount] token copies of the equipped creature.
 * If no: execute the original [originalEffect] normally.
 *
 * @property equipmentId The equipment with the replacement effect
 * @property equippedCreatureId The creature equipped at the time the decision was posed
 * @property originalEffect The original token creation effect (used if player declines)
 * @property tokenCount The evaluated number of tokens to create
 * @property effectContext The execution context from the original effect
 */
@Serializable
data class TokenCreationReplacementContinuation(
    override val decisionId: String,
    val equipmentId: EntityId,
    val equippedCreatureId: EntityId,
    val originalEffect: CreateTokenEffect,
    val tokenCount: Int,
    val effectContext: EffectContext
) : ContinuationFrame
