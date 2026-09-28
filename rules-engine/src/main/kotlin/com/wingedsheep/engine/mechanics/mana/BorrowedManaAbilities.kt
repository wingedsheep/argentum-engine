package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.TapForManaGrant
import com.wingedsheep.engine.state.components.player.TapForManaGrantsComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * The one reader of [TapForManaGrantsComponent] — "you may tap [filter]s you don't control for
 * mana" (Piracy). CR 602.2 lets only a permanent's controller activate its abilities; a grant lifts
 * that for the {T} mana abilities of matching permanents (CR 106.12's "tap for mana"), and the mana
 * they make carries the grant's restriction.
 *
 * Every site that lets a player use a mana ability asks here, so activation, the manual tap offer
 * and auto-pay agree on which permanents are borrowable:
 *  - `ActivationValidator` — may this player activate that mana ability at all;
 *  - `ManaAbilityEnumerator` — offer the opponent's land as a tap-for-mana action;
 *  - `ActivatedManaAbilityResolver` — stamp the restriction on the mana produced;
 *  - `ManaSolver.findAvailableManaSources` — auto-pay may tap it, for a payment the restriction admits.
 */
object BorrowedManaAbilities {

    /** True when [playerId] holds any tap-for-mana grant — the cheap gate every caller checks first. */
    fun hasAny(state: GameState, playerId: EntityId): Boolean =
        state.getEntity(playerId)?.get<TapForManaGrantsComponent>()?.grants?.isNotEmpty() == true

    /**
     * The grant that lets [playerId] tap [sourceId] for mana, or null — including when [playerId]
     * controls [sourceId], which needs no grant (and whose mana must not pick up its restriction).
     * An unrestricted matching grant wins over a restricted one.
     */
    fun grantFor(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        predicateEvaluator: PredicateEvaluator,
    ): TapForManaGrant? {
        val grants = state.getEntity(playerId)?.get<TapForManaGrantsComponent>()?.grants
        if (grants.isNullOrEmpty()) return null
        val projected = state.projectedState
        val controller = projected.getController(sourceId) ?: return null
        if (controller == playerId) return null
        val context = PredicateContext(controllerId = playerId)
        val matching = grants.filter { predicateEvaluator.matches(state, projected, sourceId, it.filter, context) }
        return matching.firstOrNull { it.restriction == null } ?: matching.firstOrNull()
    }

    /**
     * The battlefield permanents [playerId] doesn't control but may tap for mana, each with the
     * grant that allows it. Empty (and allocation-free) for a player with no grant.
     */
    fun borrowable(
        state: GameState,
        playerId: EntityId,
        predicateEvaluator: PredicateEvaluator,
    ): Map<EntityId, TapForManaGrant> {
        if (!hasAny(state, playerId)) return emptyMap()
        val projected = state.projectedState
        // Every other player, not just opponents — a teammate's lands are lands you don't control too.
        return state.turnOrder.filter { it != playerId }
            .flatMap { projected.getBattlefieldControlledBy(it) }
            .mapNotNull { id -> grantFor(state, playerId, id, predicateEvaluator)?.let { id to it } }
            .toMap()
    }

    /** Only mana abilities with {T} in their cost are "tapping for mana" (CR 106.12). */
    fun isTapManaAbility(ability: ActivatedAbility): Boolean =
        ability.isManaAbility && costHasTap(ability.cost)

    /** Both restrictions at once — [added] on top of whatever the mana already carried. */
    fun combine(existing: ManaRestriction?, added: ManaRestriction?): ManaRestriction? = when {
        added == null -> existing
        existing == null || existing == added -> added
        else -> ManaRestriction.AllOf(listOf(existing, added))
    }

    private fun costHasTap(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.Tap -> true
        is AbilityCost.Composite -> cost.costs.any { costHasTap(it) }
        else -> false
    }
}
