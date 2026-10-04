package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler
import com.wingedsheep.engine.handlers.actions.ability.ActivatedAbilityResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.activeManaSpendingScope
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.*

/** Searches actual immutable activation results; only a complete, contributing plan is published. */
class ScopedManaActivationPlanner(private val services: EngineServices, private val nodeLimit: Int = 256) {
    private val handler by lazy { ActivateAbilityHandler.create(services) }
    private val resolver by lazy { ActivatedAbilityResolver(services.cardRegistry, services.castPermissionUtils) }

    fun plan(
        state: GameState, player: EntityId, cost: ManaCost, context: SpellPaymentContext?,
        xAmount: Int = 0, xColors: Set<Color> = emptySet(), excludeSources: Set<EntityId> = emptySet(),
    ): ExecutionResult? {
        if (state.activeManaSpendingScope(player) == null) return null
        // The existing public proof boundary is uniform on boards with hidden printed statics.
        if (state.getBattlefield().any { state.getEntity(it)?.has<FaceDownComponent>() == true }) return null
        fun complete(current: GameState): Boolean {
            val component = current.getEntity(player)?.get<ManaPoolComponent>() ?: return false
            val pool = ManaPool(component.white, component.blue, component.black, component.red,
                component.green, component.colorless, restrictedMana = component.restrictedMana,
                snowMana = component.snowMana, snowColorless = component.snowColorless)
                .withSpendingColors(current, player)
            val allocation = pool.allocateFloating(cost, context, xAmount, xColors) ?: return false
            val required = current.continuationStack.filterIsInstance<ManaSpendingObligationsContinuation>()
                .filter { it.playerId == player }.flatMap { it.pendingIds }.toSet()
            return allocation.pool.dischargedObligations.containsAll(required)
        }
        data class Prefix(val state: GameState, val used: Set<EntityId>, val events: List<GameEvent>)
        fun search(initial: GameState): ExecutionResult? {
            if (nodeLimit <= 0) return null
            val pending = ArrayDeque<Prefix>()
            pending.add(Prefix(initial, emptySet(), emptyList()))
            var admitted = 1
            // Short proofs must precede permutations of irrelevant taps. Bound queued states too.
            while (pending.isNotEmpty()) {
                val (current, used, events) = pending.removeFirst()
                if (complete(current)) return ExecutionResult.success(current, events)
                if (admitted >= nodeLimit) continue
                // New activations cannot produce a unit bearing an earlier zero-output identity.
                val liveIds = current.getEntity(player)?.get<ManaPoolComponent>()?.restrictedMana
                    .orEmpty().flatMap { it.obligationIds }.toSet()
                if (current.continuationStack.filterIsInstance<ManaSpendingObligationsContinuation>()
                        .any { it.playerId == player && !liveIds.containsAll(it.pendingIds) }) continue
                val candidates = services.legalActionEnumerator.enumerateManaAbilities(current, player)
                for (candidate in candidates) {
                    val base = candidate.action as? ActivateAbility ?: continue
                    if (!candidate.affordable || candidate.hasXCost || base.sourceId in used ||
                        base.sourceId in excludeSources) continue
                    val ability = resolver.lookup(current, base.sourceId, base.abilityId)?.ability ?: continue
                    if (!safeCost(ability.cost) || !manaOnly(ability.effect)) continue
                    val colors: List<Color?> = if (candidate.requiresManaColorChoice)
                        candidate.availableManaColors ?: Color.entries else listOf(null)
                    for (color in colors) {
                        if (admitted >= nodeLimit) break
                        val action = base.copy(manaColorChoice = color, paymentStrategy = PaymentStrategy.FromPool)
                        if (handler.validate(current, action) != null) continue
                        val result = handler.execute(current, action)
                        // Choice-bearing bonuses/production belong to a future resumable planner.
                        // No rejected branch, pause, or intermediate events escape the transaction.
                        if (result.outcome !is Outcome.Done || result.state.pendingDecision != null || result.state.gameOver) continue
                        pending.add(Prefix(result.state, used + base.sourceId, events + result.events))
                        admitted++
                    }
                }
            }
            return null
        }
        val suspended = state.continuationStack.lastOrNull() as? Suspension
        val searchState = if (suspended == null) state else state.copy(continuationStack = state.continuationStack.dropLast(1))
        val result = search(searchState) ?: return null
        return result.copy(state = result.state.copy(
            continuationStack = result.state.continuationStack + listOfNotNull(suspended),
            priorityPlayerId = state.priorityPlayerId, priorityPassedBy = state.priorityPassedBy))
    }

    // Each source is tapped at most once. No implicit sacrifice, life, counter or secondary tap costs.
    private fun safeCost(cost: AbilityCost): Boolean = cost == AbilityCost.Tap ||
        (cost is AbilityCost.Composite && cost.costs.count { it == AbilityCost.Tap } == 1 &&
            cost.costs.all { it == AbilityCost.Tap ||
                (it is AbilityCost.Atom && it.atom is CostAtom.Mana && !(it.atom as CostAtom.Mana).cost.hasX) })

    private fun manaOnly(effect: Effect): Boolean = when (effect) {
        is AddManaEffect, is AddColorlessManaEffect, is AddManaOfChoiceEffect -> true
        is CompositeEffect -> effect.effects.isNotEmpty() && effect.effects.all(::manaOnly)
        else -> false
    }
}
