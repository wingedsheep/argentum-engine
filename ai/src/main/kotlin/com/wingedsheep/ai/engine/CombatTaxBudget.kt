package com.wingedsheep.ai.engine

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.combat.CombatTaxes
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Trims a combat declaration down to what the player can actually pay the tax for — Ghostly Prison,
 * Windborn Muse, Baird, Archangel of Tithes, Whipgrass Entangler.
 *
 * **Why this exists.** A tax is part of the cost of declaring (CR 508.1a / 509.1a), and the engine
 * asks for it *after* the declaration, as a `SelectManaSourcesDecision` the declaring player may
 * decline. Declining is a clean no-op: no attackers, and the step is still waiting for a
 * declaration. So an AI that proposes an attack it cannot pay for, is asked to pay, and declines,
 * lands back in exactly the position it just chose from — and chooses the same attack again, for
 * ever. That livelock is what this fixes: price the plan before proposing it, and propose one that
 * can be paid for.
 *
 * The tax is monotone in the declared set ([CombatTaxes]), so a plan is made affordable by dropping
 * creatures one at a time — cheapest first, so the creatures that survive the trim are the ones
 * worth taxing. Mandatory attackers/blockers are never dropped: leaving one out is its own illegal
 * declaration, and which of the two illegalities the engine complains about is its call, not ours.
 */
object CombatTaxBudget {

    /**
     * [attackers] (creature → defender), reduced until its attack tax is payable.
     *
     * Returns the plan unchanged when there is no tax (the overwhelming majority of combats — one
     * cheap scan of the defender's permanents and no solver run), or when nothing can be dropped.
     */
    fun affordableAttack(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        cardRegistry: CardRegistry?,
        attackers: Map<EntityId, EntityId>,
        mandatory: Set<EntityId> = emptySet(),
    ): Map<EntityId, EntityId> {
        if (cardRegistry == null || attackers.isEmpty()) return attackers
        val predicateEvaluator = PredicateEvaluator(cardRegistry)
        val solver = ManaSolver(cardRegistry, predicateEvaluator)
        val payable = { plan: Map<EntityId, EntityId> ->
            payable(state, playerId, solver, CombatTaxes.attackTax(state, cardRegistry, plan, projected, predicateEvaluator))
        }
        if (payable(attackers)) return attackers

        var plan = attackers
        for (attackerId in cheapestFirst(state, projected, attackers.keys - mandatory)) {
            plan = plan - attackerId
            if (plan.isEmpty()) return emptyMap()
            if (payable(plan)) return plan
        }
        return plan
    }

    /**
     * [blockers] (blocker → the attackers it blocks), reduced until its block tax is payable.
     *
     * Blocks are dropped cheapest-blocker-first for the same reason attacks are: with only part of
     * the tax payable, the blocks worth keeping are the ones made by the creatures worth paying for.
     */
    fun affordableBlock(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        cardRegistry: CardRegistry?,
        blockers: Map<EntityId, List<EntityId>>,
        mandatory: Set<EntityId> = emptySet(),
    ): Map<EntityId, List<EntityId>> {
        if (cardRegistry == null) return blockers
        val declared = blockers.filterValues { it.isNotEmpty() }
        if (declared.isEmpty()) return blockers
        val predicateEvaluator = PredicateEvaluator(cardRegistry)
        val solver = ManaSolver(cardRegistry, predicateEvaluator)
        val payable = { plan: Map<EntityId, List<EntityId>> ->
            payable(state, playerId, solver, CombatTaxes.blockTax(state, cardRegistry, plan.keys, projected, predicateEvaluator))
        }
        if (payable(declared)) return blockers

        var plan = declared
        for (blockerId in cheapestFirst(state, projected, declared.keys - mandatory)) {
            plan = plan - blockerId
            if (plan.isEmpty()) return emptyMap()
            if (payable(plan)) return plan
        }
        return plan
    }

    /**
     * Whether [playerId] can cover a [tax] of generic mana right now.
     *
     * Deliberately the *narrower* question than [ManaSolver.canPay]: floating mana first, then the
     * auto-tap solver for what is left, which is exactly what
     * `CombatTaxContinuationResumer.payTax` will do when the prompt comes back. `canPay` would also
     * count Treasures and Springleaf-Drum-style sources, and combat-tax payment refuses both — so
     * trusting it here would leave the AI declaring an attack whose prompt it can only decline,
     * which is the livelock this class exists to prevent.
     */
    private fun payable(state: GameState, playerId: EntityId, solver: ManaSolver, tax: Int): Boolean {
        if (tax <= 0) return true
        val pool = state.getEntity(playerId)?.get<ManaPoolComponent>()?.let {
            ManaPool(it.white, it.blue, it.black, it.red, it.green, it.colorless, it.restrictedMana, snowMana = it.snowMana, snowColorless = it.snowColorless)
        } ?: ManaPool()
        val remaining = pool.payPartial(CombatTaxes.genericCost(tax)).remainingCost
        return remaining.isEmpty() || solver.solve(state, playerId, remaining) != null
    }

    private fun cheapestFirst(
        state: GameState,
        projected: ProjectedState,
        creatures: Set<EntityId>,
    ): List<EntityId> = creatures.sortedBy { CombatMath.creatureValue(state, projected, it) }
}
