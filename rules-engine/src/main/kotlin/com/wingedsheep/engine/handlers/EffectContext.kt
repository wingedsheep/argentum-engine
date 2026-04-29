package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.PermanentSnapshot
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
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
    /** True if the spell's optional Blight additional cost was paid (BlightOrPay path chosen). */
    val wasBlightPaid: Boolean = false,
    // --- Cast-time state ---
    /**
     * Projected snapshots of permanents sacrificed as part of the cost (Rule 112.7a /
     * 608.2h — "as it last existed on the battlefield"). Captured before the zone change
     * so downstream effects can read power, toughness, and subtypes after the permanent
     * has left the battlefield.
     */
    val sacrificedPermanents: List<PermanentSnapshot> = emptyList(),
    /** Pre-chosen damage distribution for DividedDamageEffect spells (target ID -> damage amount) */
    val damageDistribution: Map<EntityId, Int>? = null,
    /**
     * Pre-chosen modes for modal spells/abilities (700.2). Populated at stack resolution
     * from either [SpellOnStackComponent] or [TriggeredAbilityOnStackComponent]. When
     * non-empty, [ModalEffectExecutor] iterates these modes with per-mode targets from
     * [modeTargetsOrdered] instead of prompting for a mode decision.
     */
    val chosenModes: List<Int> = emptyList(),
    val modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),
    val modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap(),
    /** Number of cards exiled as an additional cost (for ExileVariableCards) */
    val exiledCardCount: Int = 0,
    /** Permanents tapped as part of an activated ability's cost (e.g., Cryptic Gateway) */
    val tappedPermanents: List<EntityId> = emptyList(),
    // --- Trigger state ---
    /** Amount of damage from a trigger context (e.g., "Whenever ~ is dealt damage") */
    val triggerDamageAmount: Int? = null,
    /** Last known +1/+1 counter count from a death trigger context (e.g., Hooded Hydra) */
    val triggerCounterCount: Int? = null,
    /** Last known total counter count (all types) from a death trigger context (e.g., Shadow Urchin) */
    val triggerTotalCounterCount: Int? = null,
    /** Last known -1/-1 counter count from a death trigger context (e.g., Retched Wretch) */
    val triggerMinusOneMinusOneCounterCount: Int? = null,
    /** The entity that caused the trigger to fire (e.g., creature that dealt damage for Aurification) */
    val triggeringEntityId: EntityId? = null,
    /** The player associated with the trigger event (e.g., the player who cast a spell for SpellCastEvent) */
    val triggeringPlayerId: EntityId? = null,
    /** The spell or ability that targeted a permanent (for ward triggers) */
    val targetingSourceEntityId: EntityId? = null,
    /** Power of the triggering entity the moment it left the battlefield (dies/leaves triggers) */
    val triggerLastKnownPower: Int? = null,
    /** Toughness of the triggering entity the moment it left the battlefield (dies/leaves triggers) */
    val triggerLastKnownToughness: Int? = null,
    // --- Choice state ---
    /** Color chosen for "add one mana of any color" abilities */
    val manaColorChoice: Color? = null,
    /** Creature type chosen during casting (e.g., Aphetto Dredging) */
    val chosenCreatureType: String? = null,
    // --- Zone state ---
    /** Zone the spell was cast from (e.g., HAND, GRAVEYARD for flashback) */
    val castFromZone: Zone? = null,
    // --- Projection state ---
    /** The entity being modified during continuous effect projection (for DynamicAmount evaluation) */
    val affectedEntityId: EntityId? = null,
    // --- Pipeline state ---
    val pipeline: PipelineState = PipelineState.EMPTY
) {
    /**
     * Resolve a symbolic effect target to a concrete entity id using just the context.
     *
     * Stateless resolution — handles self, controller, context targets, bound variables,
     * pipeline targets, triggering entity, etc. For relational references that need to look
     * up components (e.g., [EffectTarget.EnchantedCreature], [EffectTarget.TargetController]),
     * use the overload that also takes [GameState].
     */
    fun resolveTarget(target: EffectTarget): EntityId? =
        TargetResolutionUtils.resolveTarget(target, this)

    /**
     * Resolve a symbolic effect target to a concrete entity id, consulting [state] for
     * relational references (attachments, controllers, owners).
     */
    fun resolveTarget(target: EffectTarget, state: GameState): EntityId? =
        TargetResolutionUtils.resolveTarget(target, this, state)

    /**
     * Resolve a symbolic effect target and throw if it cannot be resolved.
     * Use when the caller knows by construction that the target must exist.
     */
    fun requireTarget(target: EffectTarget): EntityId =
        resolveTarget(target) ?: error("Cannot resolve target: $target")

    /**
     * Resolve a symbolic effect target using [state] and throw if it cannot be resolved.
     */
    fun requireTarget(target: EffectTarget, state: GameState): EntityId =
        resolveTarget(target, state) ?: error("Cannot resolve target: $target")

    /**
     * Resolve a player reference target (e.g., "target player", "each opponent") to a
     * single player entity id. Stateless overload — see [resolvePlayerTargets] for
     * multi-player results.
     */
    fun resolvePlayerTarget(target: EffectTarget): EntityId? =
        TargetResolutionUtils.resolvePlayerTarget(target, this)

    /**
     * Resolve a player reference target, consulting [state] for relational references
     * like [com.wingedsheep.sdk.scripting.references.Player.OwnerOf] /
     * [com.wingedsheep.sdk.scripting.references.Player.ControllerOf].
     */
    fun resolvePlayerTarget(target: EffectTarget, state: GameState): EntityId? =
        TargetResolutionUtils.resolvePlayerTarget(target, this, state)

    /**
     * Resolve a player reference target to a list of player ids (for multi-player effects
     * like "each player" / "each opponent").
     */
    fun resolvePlayerTargets(target: EffectTarget, state: GameState): List<EntityId> =
        TargetResolutionUtils.resolvePlayerTargets(target, state, this)

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
