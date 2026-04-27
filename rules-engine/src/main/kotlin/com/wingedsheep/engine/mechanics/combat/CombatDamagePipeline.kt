package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.mechanics.layers.ProjectedState
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
                predicateEvaluator.matchesWithProjection(state, projected, assignment.sourceId, mod.filter, context)
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
        // If damage can't be prevented globally (Sunspine Lynx), skip protection checks
        if (DamageUtils.isDamagePreventionDisabled(state)) return assignments
        return assignments.filter { assignment ->
            val sourceColors = projected.getColors(assignment.sourceId)
            val sourceSubtypes = projected.getSubtypes(assignment.sourceId)
            val protectedByColor = sourceColors.any { projected.hasKeyword(assignment.targetId, "PROTECTION_FROM_$it") }
            val protectedBySubtype = sourceSubtypes.any { projected.hasKeyword(assignment.targetId, "PROTECTION_FROM_SUBTYPE_${it.uppercase()}") }
            val protectedFromOpponent = projected.hasKeyword(assignment.targetId, "PROTECTION_FROM_EACH_OPPONENT") &&
                run {
                    val srcController = projected.getController(assignment.sourceId)
                    val tgtController = projected.getController(assignment.targetId)
                    srcController != null && tgtController != null && srcController != tgtController
                }
            !protectedByColor && !protectedBySubtype && !protectedFromOpponent
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
