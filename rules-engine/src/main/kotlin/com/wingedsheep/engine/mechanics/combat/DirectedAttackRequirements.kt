package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.combat.MustAttackDefendersThisTurnComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.MustAttackDefender

/**
 * The players a [MustAttack] naming a specific defender tells each creature to attack — "All
 * creatures attack enchanted creature's controller each combat if able" (Public Enemy,
 * [MustAttackDefender.EnchantedCreatureController]). The named player is read live: the projected
 * controller of whatever the ability's holder is attached to. A holder attached to nothing names
 * nobody.
 *
 * It also reads the per-creature mark a resolved effect leaves naming the defender to attack —
 * encore's "a token … that attacks that opponent this turn if able" (CR 702.141a) or Gideon,
 * Battle-Forged's "attacks Gideon during its controller's next turn if able"
 * ([MustAttackDefendersThisTurnComponent]). All are the same kind of requirement: attack this
 * defender if able. Only a mark names a planeswalker; the statics name players.
 *
 * This only says whom the requirements *name*; [AttackPhaseManager] decides which of them can
 * bind the creature this combat (a defender it could attack at no cost — CR 508.1d).
 * Build once per declaration, ask per creature, like [GoadRules].
 */
class DirectedAttackRequirements(
    private val state: GameState,
    registry: CardRegistry,
    private val predicates: PredicateEvaluator,
) {
    private val entries = CombatStaticScan.collect(state, registry, predicates) { ability ->
        (ability as? MustAttack)?.takeIf { it.defender == MustAttackDefender.EnchantedCreatureController }
    }

    private val hasMarks: Boolean = state.getBattlefield().any {
        state.getEntity(it)?.has<MustAttackDefendersThisTurnComponent>() == true
    }

    /** No such requirement is in play — the overwhelmingly common case. */
    val isEmpty: Boolean get() = entries.isEmpty() && !hasMarks

    /** Defenders — players, or planeswalkers a mark names — [creatureId] is told to attack. */
    fun namedDefendersOf(creatureId: EntityId): Set<EntityId> {
        val marked = if (hasMarks) {
            state.getEntity(creatureId)?.get<MustAttackDefendersThisTurnComponent>()?.defenderIds.orEmpty()
        } else {
            emptySet()
        }
        if (entries.isEmpty()) return marked
        return entries.mapNotNullTo(marked.toMutableSet()) { entry ->
            if (!CombatStaticScan.covers(state, predicates, entry.holder, entry.controller, entry.ability.filter, creatureId)) {
                return@mapNotNullTo null
            }
            val attached = state.getEntity(entry.holder)?.get<AttachedToComponent>()?.targetId
                ?: return@mapNotNullTo null
            state.projectedState.getController(attached)
        }
    }
}
