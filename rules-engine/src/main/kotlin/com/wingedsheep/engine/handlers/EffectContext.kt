package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
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
    val iterationTarget: EntityId? = null,
    /** Named targets map for BoundVariable resolution (target name -> chosen target) */
    val namedTargets: Map<String, ChosenTarget> = emptyMap(),
    /** Named values chosen by the player during pipeline execution (e.g., creature type, color). */
    val chosenValues: Map<String, String> = emptyMap()
) {
    companion object {
        /**
         * Build a named targets map from target requirements and chosen targets.
         *
         * For each requirement with a non-null `id`:
         * - If count == 1: maps `id` -> chosenTarget
         * - If count > 1: maps `id[0]` -> target0, `id[1]` -> target1, etc.
         *
         * Requirements with `id == null` are skipped (backward compat with ContextTarget).
         */
        fun buildNamedTargets(
            requirements: List<TargetRequirement>,
            targets: List<ChosenTarget>
        ): Map<String, ChosenTarget> {
            val result = mutableMapOf<String, ChosenTarget>()
            var targetIndex = 0
            for (req in requirements) {
                val id = req.id
                if (id != null) {
                    if (req.count == 1) {
                        targets.getOrNull(targetIndex)?.let { result[id] = it }
                    } else {
                        for (i in 0 until req.count) {
                            targets.getOrNull(targetIndex + i)?.let { result["$id[$i]"] = it }
                        }
                    }
                }
                targetIndex += req.count
            }
            return result
        }
    }
}
