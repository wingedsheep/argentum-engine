package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.PendingAttackPermanentCost
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AttackPermanentCostAction
import com.wingedsheep.sdk.scripting.CantAttackUnlessPermanentCost

/**
 * The permanent cost of declaring an attacker — [CantAttackUnlessPermanentCost], Leviathan's "this
 * creature can't attack unless you sacrifice two Islands" and Floodtide Serpent's "can't attack
 * unless you return an enchantment you control to its owner's hand". A restriction checked at CR 508.1c whose
 * cost is determined and paid at CR 508.1h–j, not an optional attack cost (CR 508.1g).
 *
 * One place for both halves of the rule so they cannot drift: the *affordability* check that makes
 * an unpayable declaration illegal ([com.wingedsheep.engine.mechanics.combat.rules.CantAttackUnlessPermanentCostRule])
 * and the *payment* pause in [AttackPhaseManager]. When they disagree, a player is either offered a
 * choice they can't complete or blocked from an attack they could have paid for.
 */
object AttackPermanentCosts {

    /**
     * The permanent-cost requirement [attackerId] carries, or null when it has none. Face-down creatures
     * have no abilities (CR 708.2), so they never carry one.
     */
    fun requirementFor(
        state: GameState,
        attackerId: EntityId,
        cardRegistry: CardRegistry,
    ): CantAttackUnlessPermanentCost? {
        val container = state.getEntity(attackerId) ?: return null
        if (container.has<FaceDownComponent>()) return null
        val cardComponent = container.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return null
        return cardDef.staticAbilities.filterIsInstance<CantAttackUnlessPermanentCost>().firstOrNull()
    }

    /**
     * Permanents [attackingPlayer] controls (projected control, so a stolen permanent can pay and
     * one an opponent took can't) that could pay [requirement]. The attacking creature itself is
     * excluded: sacrificing or returning it would remove it from combat, so it can never be its
     * own cost. Another declared attacker may pay; it then simply doesn't attack (CR 508.1k makes
     * only chosen creatures still under the player's control attacking creatures).
     */
    fun eligiblePermanents(
        state: GameState,
        attackingPlayer: EntityId,
        attackerId: EntityId,
        requirement: CantAttackUnlessPermanentCost,
        predicateEvaluator: PredicateEvaluator
    ): List<EntityId> {
        val projected = state.projectedState
        val context = PredicateContext(controllerId = attackingPlayer, sourceId = attackerId)
        return state.controlledBattlefield(attackingPlayer).filter { candidate ->
            candidate != attackerId &&
                predicateEvaluator.matches(
                    state, projected, candidate, requirement.filter, context
                )
        }
    }

    /**
     * Every declared attacker that owes a permanent cost, paired with its requirement, in declaration
     * order. Empty when nothing in the attack costs anything — the overwhelmingly common case, and
     * the one that must stay allocation-free.
     */
    fun requirementsFor(
        state: GameState,
        attackers: Collection<EntityId>,
        cardRegistry: CardRegistry,
    ): List<Pair<EntityId, CantAttackUnlessPermanentCost>> =
        attackers.mapNotNull { attackerId ->
            requirementFor(state, attackerId, cardRegistry)?.let { attackerId to it }
        }

    /**
     * The first declared attacker whose cost can't be paid alongside the others', or null when
     * every cost can be paid at once. Each attacker's own check ([eligiblePermanents]) only asks
     * whether *its* cost is payable; two Floodtide Serpents beside one enchantment each pass it
     * but can't both be paid.
     */
    fun unaffordableAttacker(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Collection<EntityId>,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): EntityId? {
        val costs = requirementsFor(state, attackers, cardRegistry)
        if (costs.size < 2) return null // a lone cost is the per-attacker rule's job
        return firstUnmatched(
            state, attackingPlayer, costs.map { (id, req) -> Owed(id, req, req.count) }, emptySet(), predicateEvaluator
        )
    }

    /**
     * The permanents that may pay one unit of [payingAttacker]'s cost now without stranding a
     * later payment: every eligible permanent after whose removal the rest of this cost
     * ([count] − 1) and every [remaining] cost can still be paid together. Costs are paid one
     * attacker at a time, so without this a first choice could take the only permanent a later
     * attacker's cost could use.
     */
    fun payableOptions(
        state: GameState,
        attackingPlayer: EntityId,
        payingAttacker: EntityId,
        requirement: CantAttackUnlessPermanentCost,
        count: Int,
        remaining: List<PendingAttackPermanentCost>,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): List<EntityId> {
        val eligible = eligiblePermanents(state, attackingPlayer, payingAttacker, requirement, predicateEvaluator)
        val later = owedFor(state, remaining, cardRegistry)
        if (later.isEmpty() && count <= eligible.size) return eligible
        val rest = listOf(Owed(payingAttacker, requirement, count - 1)) + later
        return eligible.filter { candidate ->
            firstUnmatched(state, attackingPlayer, rest, setOf(candidate), predicateEvaluator) == null
        }
    }

    /** Whether every [remaining] cost can still be paid together in [state]. */
    fun remainingAffordable(
        state: GameState,
        attackingPlayer: EntityId,
        remaining: List<PendingAttackPermanentCost>,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): Boolean = firstUnmatched(
        state, attackingPlayer, owedFor(state, remaining, cardRegistry), emptySet(), predicateEvaluator
    ) == null

    private data class Owed(val attackerId: EntityId, val requirement: CantAttackUnlessPermanentCost, val count: Int)

    private fun owedFor(
        state: GameState,
        remaining: List<PendingAttackPermanentCost>,
        cardRegistry: CardRegistry,
    ): List<Owed> = remaining.mapNotNull { pending ->
        requirementFor(state, pending.attackerId, cardRegistry)?.let { Owed(pending.attackerId, it, pending.count) }
    }

    /**
     * Each owed permanent is a slot matched to a distinct eligible permanent not in [excluded]
     * (bipartite matching by Kuhn's augmenting paths) — exact even when the filters overlap only
     * partly. Returns the attacker of the first slot left unmatched, or null when all are. Tiny in
     * practice: a handful of slots over a handful of permanents.
     */
    private fun firstUnmatched(
        state: GameState,
        attackingPlayer: EntityId,
        owed: List<Owed>,
        excluded: Set<EntityId>,
        predicateEvaluator: PredicateEvaluator,
    ): EntityId? {
        val slots = owed.flatMap { (attackerId, requirement, count) ->
            if (count <= 0) return@flatMap emptyList()
            val eligible = eligiblePermanents(state, attackingPlayer, attackerId, requirement, predicateEvaluator)
                .filter { it !in excluded }
            List(count) { attackerId to eligible }
        }
        val holderOf = HashMap<EntityId, Int>()
        fun assign(slot: Int, seen: MutableSet<EntityId>): Boolean {
            for (candidate in slots[slot].second) {
                if (!seen.add(candidate)) continue
                val holder = holderOf[candidate]
                if (holder == null || assign(holder, seen)) {
                    holderOf[candidate] = slot
                    return true
                }
            }
            return false
        }
        for (slot in slots.indices) {
            if (!assign(slot, mutableSetOf())) return slots[slot].first
        }
        return null
    }

    /** The payment prompt: "Sacrifice 2 Islands to attack with Leviathan". */
    fun prompt(requirement: CantAttackUnlessPermanentCost, count: Int, attackerName: String): String {
        val what = "$count ${requirement.filter.description}"
        return when (requirement.action) {
            AttackPermanentCostAction.SACRIFICE -> "Sacrifice $what to attack with $attackerName"
            AttackPermanentCostAction.RETURN_TO_HAND ->
                "Return $what to its owner's hand to attack with $attackerName"
        }
    }
}
