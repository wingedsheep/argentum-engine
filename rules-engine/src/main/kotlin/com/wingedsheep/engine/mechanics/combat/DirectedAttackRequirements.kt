package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
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
 * This only says whom the requirements *name*; [AttackPhaseManager] decides which of them can
 * bind the creature this combat (an opponent it could attack at no cost — CR 508.1d).
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

    /** No such requirement is in play — the overwhelmingly common case. */
    val isEmpty: Boolean get() = entries.isEmpty()

    /** Players [creatureId] is told to attack. */
    fun namedDefendersOf(creatureId: EntityId): Set<EntityId> {
        if (entries.isEmpty()) return emptySet()
        return entries.mapNotNullTo(mutableSetOf()) { entry ->
            if (!CombatStaticScan.covers(state, predicates, entry.holder, entry.controller, entry.ability.filter, creatureId)) {
                return@mapNotNullTo null
            }
            val attached = state.getEntity(entry.holder)?.get<AttachedToComponent>()?.targetId
                ?: return@mapNotNullTo null
            state.projectedState.getController(attached)
        }
    }
}
