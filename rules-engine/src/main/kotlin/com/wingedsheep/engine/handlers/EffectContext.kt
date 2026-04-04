package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Context for effect execution.
 *
 * Core fields (sourceId, controllerId, targets, etc.) are always relevant.
 * Pipeline-specific state (collections, named values, iteration targets) lives
 * in [pipeline] to keep the two concerns separate and make pipeline extensions
 * self-contained.
 */
@Serializable
data class EffectContext(
    // --- Core ---
    val sourceId: EntityId?,
    val controllerId: EntityId,
    val opponentId: EntityId?,
    val targets: List<ChosenTarget> = emptyList(),
    val xValue: Int? = null,
    val wasKicked: Boolean = false,
    // --- Cast-time state ---
    val sacrificedPermanents: List<EntityId> = emptyList(),
    /** Projected subtypes of sacrificed permanents at time of sacrifice (before zone change) */
    val sacrificedPermanentSubtypes: Map<EntityId, Set<String>> = emptyMap(),
    /** Pre-chosen damage distribution for DividedDamageEffect spells (target ID -> damage amount) */
    val damageDistribution: Map<EntityId, Int>? = null,
    /** Number of cards exiled as an additional cost (for ExileVariableCards) */
    val exiledCardCount: Int = 0,
    /** Permanents tapped as part of an activated ability's cost (e.g., Cryptic Gateway) */
    val tappedPermanents: List<EntityId> = emptyList(),
    // --- Trigger state ---
    /** Amount of damage from a trigger context (e.g., "Whenever ~ is dealt damage") */
    val triggerDamageAmount: Int? = null,
    /** Last known +1/+1 counter count from a death trigger context (e.g., Hooded Hydra) */
    val triggerCounterCount: Int? = null,
    /** The entity that caused the trigger to fire (e.g., creature that dealt damage for Aurification) */
    val triggeringEntityId: EntityId? = null,
    /** The player associated with the trigger event (e.g., the player who cast a spell for SpellCastEvent) */
    val triggeringPlayerId: EntityId? = null,
    /** The spell or ability that targeted a permanent (for ward triggers) */
    val targetingSourceEntityId: EntityId? = null,
    // --- Choice state ---
    /** Color chosen for "add one mana of any color" abilities */
    val manaColorChoice: Color? = null,
    /** Creature type chosen during casting (e.g., Aphetto Dredging) */
    val chosenCreatureType: String? = null,
    // --- Zone state ---
    /** Zone the spell was cast from (e.g., HAND, GRAVEYARD for flashback) */
    val castFromZone: Zone? = null,
    // --- Pipeline state ---
    val pipeline: PipelineState = PipelineState.EMPTY
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
