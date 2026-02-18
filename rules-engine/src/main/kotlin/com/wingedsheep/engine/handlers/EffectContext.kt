package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Context for effect execution.
 */
data class EffectContext(
    val sourceId: EntityId?,
    val controllerId: EntityId,
    val opponentId: EntityId?,
    val targets: List<ChosenTarget> = emptyList(),
    val xValue: Int? = null,
    val sacrificedPermanents: List<EntityId> = emptyList(),
    /** Projected subtypes of sacrificed permanents at time of sacrifice (before zone change) */
    val sacrificedPermanentSubtypes: Map<EntityId, Set<String>> = emptyMap(),
    /** Pre-chosen damage distribution for DividedDamageEffect spells (target ID -> damage amount) */
    val damageDistribution: Map<EntityId, Int>? = null,
    /** Amount of damage from a trigger context (e.g., "Whenever ~ is dealt damage") */
    val triggerDamageAmount: Int? = null,
    /** The entity that caused the trigger to fire (e.g., creature that dealt damage for Aurification) */
    val triggeringEntityId: EntityId? = null,
    /** Color chosen for "add one mana of any color" abilities */
    val manaColorChoice: Color? = null,
    /** Creature type chosen during casting (e.g., Aphetto Dredging) */
    val chosenCreatureType: String? = null,
    /** Permanents tapped as part of an activated ability's cost (e.g., Cryptic Gateway) */
    val tappedPermanents: List<EntityId> = emptyList(),
    /** Named card collections for pipeline effects (GatherCards → SelectFromCollection → MoveCollection) */
    val storedCollections: Map<String, List<EntityId>> = emptyMap(),
    /** When inside a ForEachInGroupEffect, the current iteration entity. EffectTarget.Self resolves to this. */
    val iterationTarget: EntityId? = null
)
