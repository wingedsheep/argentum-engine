package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.combat.CombatStaticScan
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CanBlockAsThoughHadReach

/**
 * Single source of truth for "does [blockerId] count as having reach against [attackerId]?" —
 * the reach half of CR 702.9b / 702.17b ("a creature with flying can't be blocked except by
 * creatures with flying and/or reach"). True when the blocker has reach, or when a
 * [CanBlockAsThoughHadReach] static covers it for this attacker ("can block Dragons as though it
 * had reach", Dragon Hunter).
 *
 * The statics are read through [CombatStaticScan] like the other declaration-time combat statics:
 * live statics on battlefield permanents (not face down, not ability-stripped, text changes
 * applied, conditions evaluated) plus runtime grants still in force. The blocker is matched by the
 * static's group filter and the attacker by its `attackers` filter, both with the holder as
 * predicate source.
 *
 * Read by the engine's [FlyingRule] and by the AI's combat math,
 * so the blockers the AI predicts agree with the ones the engine allows.
 */
object BlockAsThoughReach {

    fun hasReachAgainst(
        state: GameState,
        blockerId: EntityId,
        attackerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        val projected = state.projectedState
        if (projected.hasKeyword(blockerId, Keyword.REACH)) return true
        val entries = CombatStaticScan.collect(state, cardRegistry, predicateEvaluator) { it as? CanBlockAsThoughHadReach }
        return entries.any { entry ->
            CombatStaticScan.covers(state, predicateEvaluator, entry.holder, entry.controller, entry.ability.filter, blockerId) &&
                predicateEvaluator.matches(
                    state, projected, attackerId, entry.ability.attackers,
                    PredicateContext(sourceId = entry.holder, controllerId = entry.controller)
                )
        }
    }
}
