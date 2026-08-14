package com.wingedsheep.ai.insight

import com.wingedsheep.sdk.model.EntityId

/**
 * Collects the combat plans [com.wingedsheep.ai.engine.CombatAdvisor]'s local search actually
 * simulated, so the ones it rejected are visible next to the one it kept.
 *
 * A fresh instance is created per decision and threaded down the call stack, never shared — the
 * advisor is also driven from inside rollout playouts, and a shared collector would mix a
 * hypothetical combat into the real one's trace. Null (the default everywhere) means no recording.
 *
 * Plans are deduplicated on the assignment map: local search re-evaluates a plan across iterations,
 * and scoring is deterministic, so the first score is the only one.
 */
class CombatPlanTrace {
    private val entries = LinkedHashMap<Any, CombatPlan>()

    val plans: List<CombatPlan> get() = entries.values.toList()

    fun recordAttack(attackers: Map<EntityId, EntityId>, score: Double) {
        // Snapshot before keying: local search hands us its live plan map and then mutates it, and a
        // key that changes after insertion no longer matches itself — the same plan would come back
        // as a second, identical row.
        val snapshot = attackers.toMap()
        entries.putIfAbsent(snapshot, CombatPlan.Attack(snapshot, score))
    }

    fun recordBlock(blockers: Map<EntityId, List<EntityId>>, score: Double) {
        val normalized = blockers.filterValues { it.isNotEmpty() }
        entries.putIfAbsent(normalized, CombatPlan.Block(normalized, score))
    }
}

/** One evaluated combat assignment and the board score it simulated to. */
sealed interface CombatPlan {
    val score: Double

    data class Attack(val attackers: Map<EntityId, EntityId>, override val score: Double) : CombatPlan

    data class Block(val blockers: Map<EntityId, List<EntityId>>, override val score: Double) : CombatPlan
}
