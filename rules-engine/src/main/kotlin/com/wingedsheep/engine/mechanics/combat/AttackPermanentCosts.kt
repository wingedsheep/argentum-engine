package com.wingedsheep.engine.mechanics.combat

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
     * Permanents [attackingPlayer] controls that could pay [requirement]. The attacking creature
     * itself is excluded: sacrificing or returning it would remove it from combat, so it can never be its own
     * cost.
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
        return state.getBattlefield(attackingPlayer).filter { candidate ->
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
