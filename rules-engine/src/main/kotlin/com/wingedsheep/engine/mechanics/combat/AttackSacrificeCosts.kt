package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantAttackUnlessSacrifice

/**
 * The sacrifice cost of declaring an attacker — [CantAttackUnlessSacrifice], Leviathan's "this
 * creature can't attack unless you sacrifice two Islands". A restriction checked at CR 508.1c whose
 * cost is determined and paid at CR 508.1h–j, not an optional attack cost (CR 508.1g).
 *
 * One place for both halves of the rule so they cannot drift: the *affordability* check that makes
 * an unpayable declaration illegal ([com.wingedsheep.engine.mechanics.combat.rules.CantAttackUnlessSacrificeRule])
 * and the *payment* pause in [AttackPhaseManager]. When they disagree, a player is either offered a
 * choice they can't complete or blocked from an attack they could have paid for.
 */
object AttackSacrificeCosts {

    private val predicateEvaluator = PredicateEvaluator()

    /**
     * The sacrifice requirement [attackerId] carries, or null when it has none. Face-down creatures
     * have no abilities (CR 708.2), so they never carry one.
     */
    fun requirementFor(
        state: GameState,
        attackerId: EntityId,
        cardRegistry: CardRegistry,
    ): CantAttackUnlessSacrifice? {
        val container = state.getEntity(attackerId) ?: return null
        if (container.has<FaceDownComponent>()) return null
        val cardComponent = container.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return null
        return cardDef.staticAbilities.filterIsInstance<CantAttackUnlessSacrifice>().firstOrNull()
    }

    /**
     * Permanents [attackingPlayer] controls that could pay [requirement]. The attacking creature
     * itself is excluded: sacrificing it would remove it from combat, so it can never be its own
     * cost.
     */
    fun eligiblePermanents(
        state: GameState,
        attackingPlayer: EntityId,
        attackerId: EntityId,
        requirement: CantAttackUnlessSacrifice,
    ): List<EntityId> {
        val projected = state.projectedState
        val context = PredicateContext(controllerId = attackingPlayer, sourceId = attackerId)
        return state.getBattlefield(attackingPlayer).filter { candidate ->
            candidate != attackerId &&
                predicateEvaluator.matches(
                    state, projected, candidate, requirement.sacrificeFilter, context
                )
        }
    }

    /**
     * Every declared attacker that owes a sacrifice, paired with its requirement, in declaration
     * order. Empty when nothing in the attack costs anything — the overwhelmingly common case, and
     * the one that must stay allocation-free.
     */
    fun requirementsFor(
        state: GameState,
        attackers: Collection<EntityId>,
        cardRegistry: CardRegistry,
    ): List<Pair<EntityId, CantAttackUnlessSacrifice>> =
        attackers.mapNotNull { attackerId ->
            requirementFor(state, attackerId, cardRegistry)?.let { attackerId to it }
        }
}
