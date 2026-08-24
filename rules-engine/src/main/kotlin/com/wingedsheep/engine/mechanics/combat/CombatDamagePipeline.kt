package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.targeting.PlayerProtectionRules
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Represents the intent to deal combat damage from one source to one target.
 *
 * Created during the "propose" phase of the combat damage pipeline.
 * Modifiers can transform these assignments before they are applied to game state.
 */
data class CombatDamageAssignment(
    val sourceId: EntityId,
    val targetId: EntityId,
    val amount: Int
)

/**
 * Plugin interface for modifying combat damage assignments before they are applied.
 *
 * Modifiers inspect the proposed assignments and return a transformed list.
 * They can zero out, filter, or retarget assignments.
 */
interface CombatDamageModifier {
    fun modify(
        state: GameState,
        projected: ProjectedState,
        assignments: List<CombatDamageAssignment>
    ): List<CombatDamageAssignment>
}

/** Prevents all combat damage (Fog, Moment's Peace). */
internal class PreventAllCombatDamageModifier : CombatDamageModifier {
    override fun modify(state: GameState, projected: ProjectedState, assignments: List<CombatDamageAssignment>): List<CombatDamageAssignment> {
        val prevented = state.floatingEffects.any { it.effect.modification is SerializableModification.PreventAllCombatDamage }
        return if (prevented) emptyList() else assignments
    }
}

/** Prevents all damage from a specific source (Chain of Silence). */
internal class PreventAllDamageFromSourceModifier : CombatDamageModifier {
    override fun modify(state: GameState, projected: ProjectedState, assignments: List<CombatDamageAssignment>): List<CombatDamageAssignment> {
        return assignments.filter { !DamageUtils.isAllDamageFromSourcePrevented(state, it.sourceId) }
    }
}

/** Prevents combat damage to and from specific creatures. */
internal class PreventCombatDamageToAndByModifier : CombatDamageModifier {
    override fun modify(state: GameState, projected: ProjectedState, assignments: List<CombatDamageAssignment>): List<CombatDamageAssignment> {
        val preventedEntities = state.floatingEffects
            .filter { it.effect.modification is SerializableModification.PreventCombatDamageToAndBy }
            .flatMap { it.effect.affectedEntities }
            .toSet()
        if (preventedEntities.isEmpty()) return assignments
        return assignments.filter { it.sourceId !in preventedEntities && it.targetId !in preventedEntities }
    }
}

/** Prevents combat damage from creatures matching a group filter. */
internal class PreventCombatDamageFromGroupModifier : CombatDamageModifier {
    private val predicateEvaluator = PredicateEvaluator()
    override fun modify(state: GameState, projected: ProjectedState, assignments: List<CombatDamageAssignment>): List<CombatDamageAssignment> {
        val groupEffects = state.floatingEffects.filter { it.effect.modification is SerializableModification.PreventCombatDamageFromGroup }
        if (groupEffects.isEmpty()) return assignments
        return assignments.filter { assignment ->
            !groupEffects.any { floatingEffect ->
                val mod = floatingEffect.effect.modification as SerializableModification.PreventCombatDamageFromGroup
                val context = PredicateContext(controllerId = floatingEffect.controllerId)
                predicateEvaluator.matches(state, projected, assignment.sourceId, mod.filter, context)
            }
        }
    }
}

/** Prevents damage from attacking creatures to protected players (Deep Wood). */
internal class PreventDamageFromAttackingCreaturesModifier : CombatDamageModifier {
    override fun modify(state: GameState, projected: ProjectedState, assignments: List<CombatDamageAssignment>): List<CombatDamageAssignment> {
        val protectedPlayers = state.floatingEffects
            .filter { it.effect.modification is SerializableModification.PreventDamageFromAttackingCreatures }
            .flatMap { it.effect.affectedEntities }
            .toSet()
        if (protectedPlayers.isEmpty()) return assignments
        val attackerIds = state.findEntitiesWith<AttackingComponent>().map { it.first }.toSet()
        return assignments.filter { !(it.sourceId in attackerIds && it.targetId in protectedPlayers) }
    }
}

/** Prevents damage blocked by protection from color/subtype (Rule 702.16). */
internal class ProtectionModifier : CombatDamageModifier {
    override fun modify(state: GameState, projected: ProjectedState, assignments: List<CombatDamageAssignment>): List<CombatDamageAssignment> {
        // Protection prevents damage (CR 702.16e), so it is skipped wherever prevention is off.
        // Checked *per assignment* rather than as an early-out, because the shutoff can be
        // per-recipient (Whippoorwill) as well as global (Sunspine Lynx): one marked creature must
        // take its damage in full without blanking protection for everyone else in the combat.
        return assignments.filter { assignment ->
            if (DamageUtils.isDamagePreventionDisabled(state, assignment.targetId)) return@filter true
            val sourceColors = projected.getColors(assignment.sourceId)
            val sourceSubtypes = projected.getSubtypes(assignment.sourceId)
            val sourceSupertypes = projected.getSupertypes(assignment.sourceId)
            val sourceTypes = projected.getTypes(assignment.sourceId)
            val protectedByColor = sourceColors.any { projected.hasKeyword(assignment.targetId, "PROTECTION_FROM_$it") }
            val protectedBySubtype = sourceSubtypes.any { projected.hasKeyword(assignment.targetId, "PROTECTION_FROM_SUBTYPE_${it.uppercase()}") }
            val protectedBySupertype = sourceSupertypes.any { projected.hasKeyword(assignment.targetId, "PROTECTION_FROM_SUPERTYPE_${it.uppercase()}") }
            val protectedByCardType = sourceTypes.any { projected.hasKeyword(assignment.targetId, "PROTECTION_FROM_CARDTYPE_${it.uppercase()}") }
            val protectedFromOpponent = projected.hasKeyword(assignment.targetId, "PROTECTION_FROM_EACH_OPPONENT") &&
                run {
                    val srcController = projected.getController(assignment.sourceId)
                    val tgtController = projected.getController(assignment.targetId)
                    srcController != null && tgtController != null && srcController != tgtController
                }
            !protectedByColor && !protectedBySubtype && !protectedBySupertype && !protectedByCardType && !protectedFromOpponent
        }
    }
}

/**
 * Prevents combat damage to a player with player-level protection from the damage's source
 * (CR 702.16 — the **D**amage half of DEBT; e.g. The One Ring's "protection from everything").
 * The combat Apply phase ([CombatDamageManager.applyDamageToPlayer]) reduces life directly and
 * never consults [PlayerProtectionRules], so — unlike non-combat damage via
 * [DamageUtils.dealDamageToTarget] — the prevention has to happen here, by dropping the assignment.
 *
 * The protected player is the assignment target, the attacking creature its source; this mirrors
 * the keyword [ProtectionModifier] and the floating-effect [PreventDamageFromAttackingCreaturesModifier]
 * (Deep Wood). [PlayerProtectionRules.isProtectedFromSource] is false for any non-player or
 * unprotected target, so creature assignments pass through untouched. Protection prevents damage
 * (CR 702.16e), but damage that simply can't be prevented still reduces life — so this is skipped
 * when prevention is globally disabled (Fear, Fire, Foes! / Sunspine Lynx); cf. The One Ring's
 * Gatherer ruling (2023-06-16).
 */
internal class PlayerProtectionModifier : CombatDamageModifier {
    override fun modify(state: GameState, projected: ProjectedState, assignments: List<CombatDamageAssignment>): List<CombatDamageAssignment> {
        // Left as a global early-out on purpose: every target here is a player, and the
        // per-recipient shutoff (Whippoorwill) only ever marks a creature.
        if (DamageUtils.isDamagePreventionDisabled(state)) return assignments
        return assignments.filter { assignment ->
            !PlayerProtectionRules.isProtectedFromSource(
                state,
                playerId = assignment.targetId,
                sourceId = assignment.sourceId,
                casterId = projected.getController(assignment.sourceId)
            )
        }
    }
}

/** Redirects a creature's combat damage to its controller (Goblin Psychopath). */
internal class RedirectToControllerModifier : CombatDamageModifier {
    override fun modify(state: GameState, projected: ProjectedState, assignments: List<CombatDamageAssignment>): List<CombatDamageAssignment> {
        val redirectedCreatures = state.floatingEffects
            .filter { it.effect.modification is SerializableModification.RedirectCombatDamageToController }
            .flatMap { it.effect.affectedEntities }
            .toSet()
        if (redirectedCreatures.isEmpty()) return assignments
        return assignments.map { assignment ->
            if (assignment.sourceId in redirectedCreatures) {
                val controllerId = projected.getController(assignment.sourceId) ?: assignment.targetId
                assignment.copy(targetId = controllerId)
            } else {
                assignment
            }
        }
    }
}
