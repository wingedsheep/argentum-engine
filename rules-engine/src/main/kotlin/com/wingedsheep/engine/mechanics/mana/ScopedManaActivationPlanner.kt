package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler
import com.wingedsheep.engine.handlers.actions.ability.ActivatedAbilityResolver
import com.wingedsheep.engine.handlers.actions.decision.DecisionValidators
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
import com.wingedsheep.sdk.scripting.values.DynamicAmount

sealed interface ScopedManaPlanResult {
    data class Found(val execution: ExecutionResult) : ScopedManaPlanResult
    data object Impossible : ScopedManaPlanResult
    data class Unknown(val reasons: Set<ScopedManaSearchLimit>) : ScopedManaPlanResult
}

enum class ScopedManaSearchLimit {
    NODE_BUDGET, UNSUPPORTED_ACTIVATION, UNSUPPORTED_DECISION, HIDDEN_BATTLEFIELD,
    CONTINUATION_BOUNDARY, EXECUTION_FAILURE, NO_EXECUTION_PROVIDER, NO_SPENDING_SCOPE,
}

/** Searches actual immutable activation results; only a complete, contributing plan is published. */
class ScopedManaActivationPlanner(private val services: EngineServices, private val nodeLimit: Int = 256) {
    private val handler by lazy { ActivateAbilityHandler.create(services) }
    private val resolver by lazy { ActivatedAbilityResolver(services.cardRegistry, services.castPermissionUtils) }

    fun plan(
        state: GameState, player: EntityId, cost: ManaCost, context: SpellPaymentContext?,
        xAmount: Int = 0, xColors: Set<Color> = emptySet(), excludeSources: Set<EntityId> = emptySet(),
        reservedLife: Int = 0,
    ): ScopedManaPlanResult {
        val limits = mutableSetOf<ScopedManaSearchLimit>()
        if (state.activeManaSpendingScope(player) == null)
            return ScopedManaPlanResult.Unknown(setOf(ScopedManaSearchLimit.NO_SPENDING_SCOPE))
        fun complete(current: GameState): Boolean {
            if (reservedLife > 0 && current.lifeTotal(player) < reservedLife) return false
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
        data class Prefix(val state: GameState, val events: List<GameEvent>)
        fun search(initial: GameState): ScopedManaPlanResult {
            // Floating payment needs no source enumeration, even on hidden boards or at zero budget.
            if (complete(initial)) return ScopedManaPlanResult.Found(ExecutionResult.success(initial))
            // Hidden identities must not affect this public proof boundary.
            if (initial.getBattlefield().any { initial.getEntity(it)?.has<FaceDownComponent>() == true })
                return ScopedManaPlanResult.Unknown(setOf(ScopedManaSearchLimit.HIDDEN_BATTLEFIELD))
            if (nodeLimit <= 0) return ScopedManaPlanResult.Unknown(setOf(ScopedManaSearchLimit.NODE_BUDGET))
            val pending = ArrayDeque<Prefix>()
            pending.add(Prefix(initial, emptyList()))
            val continuationFloor = initial.continuationStack.size
            var admitted = 1
            // Short proofs must precede permutations of irrelevant taps. Bound queued states too.
            while (pending.isNotEmpty()) {
                val (current, events) = pending.removeFirst()
                if (current.pendingDecision == null && current.continuationStack.size == continuationFloor &&
                    complete(current)) return ScopedManaPlanResult.Found(ExecutionResult.success(current, events))
                if (current.pendingDecision != null) {
                    // Branch only over the paying player's production answers. Each branch uses
                    // the real validation and continuation dispatch, but cannot consume caller work.
                    val responses = productionResponses(current, player)
                    if (responses == null) {
                        limits.add(ScopedManaSearchLimit.UNSUPPORTED_DECISION)
                        continue
                    }
                    for (response in responses) {
                        val decision = current.pendingDecision ?: break
                        if (DecisionValidators.validate(decision, response, current) != null) continue
                        if (admitted >= nodeLimit) {
                            limits.add(ScopedManaSearchLimit.NODE_BUDGET)
                            break
                        }
                        val result = services.continuationHandler.resumeWithin(current, response, continuationFloor)
                        if (result.error != null) {
                            limits.add(ScopedManaSearchLimit.EXECUTION_FAILURE)
                            continue
                        }
                        if (result.state.gameOver) continue
                        pending.add(Prefix(result.state, events + DecisionSubmittedEvent(decision.id, player) + result.events))
                        admitted++
                    }
                    continue
                }
                // A production boundary must finish before another activation or final payment.
                if (current.continuationStack.size != continuationFloor) {
                    limits.add(ScopedManaSearchLimit.CONTINUATION_BOUNDARY)
                    continue
                }
                // New activations cannot produce a unit bearing an earlier zero-output identity.
                val liveIds = current.getEntity(player)?.get<ManaPoolComponent>()?.restrictedMana
                    .orEmpty().flatMap { it.obligationIds }.toSet()
                if (current.continuationStack.filterIsInstance<ManaSpendingObligationsContinuation>()
                        .any { it.playerId == player && !liveIds.containsAll(it.pendingIds) }) continue
                val candidates = services.legalActionEnumerator.enumerateManaAbilities(current, player)
                for (candidate in candidates) {
                    val base = candidate.action as? ActivateAbility
                    if (base == null) {
                        limits.add(ScopedManaSearchLimit.UNSUPPORTED_ACTIVATION)
                        continue
                    }
                    if (base.sourceId in excludeSources) continue
                    val ability = resolver.lookup(current, base.sourceId, base.abilityId)?.ability
                    if (ability == null || candidate.hasXCost || !safeCost(ability.cost) || !manaOnly(ability.effect)) {
                        limits.add(ScopedManaSearchLimit.UNSUPPORTED_ACTIVATION)
                        continue
                    }
                    if (!candidate.affordable) continue
                    // A composite can contain independently chosen colors. A shared activation
                    // color would couple those choices, so let its production leaves ask separately.
                    val colors: List<Color?> = if (candidate.requiresManaColorChoice &&
                        ability.effect !is CompositeEffect)
                        candidate.availableManaColors ?: Color.entries else listOf(null)
                    for (color in colors) {
                        val action = base.copy(manaColorChoice = color, paymentStrategy = PaymentStrategy.FromPool)
                        if (handler.validate(current, action) != null) continue
                        if (admitted >= nodeLimit) {
                            limits.add(ScopedManaSearchLimit.NODE_BUDGET)
                            break
                        }
                        val result = handler.execute(current, action)
                        // Paused production remains private to this speculative branch. A plan
                        // is published only after all its questions and its full payment finish.
                        if (result.error != null) {
                            limits.add(ScopedManaSearchLimit.EXECUTION_FAILURE)
                            continue
                        }
                        if (result.state.gameOver) continue
                        pending.add(Prefix(result.state, events + result.events))
                        admitted++
                    }
                }
            }
            return if (limits.isEmpty()) ScopedManaPlanResult.Impossible
                else ScopedManaPlanResult.Unknown(limits.toSet())
        }
        // A caller may ask while production is paused; its partial pool is not a final proof.
        if (state.continuationStack.any { it is ScopedManaProductionContinuation && it.playerId == player })
            return ScopedManaPlanResult.Unknown(setOf(ScopedManaSearchLimit.CONTINUATION_BOUNDARY))
        val suspended = state.continuationStack.lastOrNull() as? Suspension
        val searchState = if (suspended == null) state else state.copy(continuationStack = state.continuationStack.dropLast(1))
        val outcome = search(searchState)
        if (outcome !is ScopedManaPlanResult.Found) return outcome
        val result = outcome.execution
        return ScopedManaPlanResult.Found(result.copy(state = result.state.copy(
            continuationStack = result.state.continuationStack + listOfNotNull(suspended),
            priorityPlayerId = state.priorityPlayerId, priorityPassedBy = state.priorityPassedBy)))
    }

    private fun productionResponses(state: GameState, player: EntityId): Sequence<DecisionResponse>? {
        val suspension = state.continuationStack.lastOrNull() as? Suspension ?: return null
        val question = suspension.question
        if (question.playerId != player) return null
        return when (suspension.answer) {
            is ChooseManaColorContinuation, is AddManaPipsContinuation, is ChooseAnyColorTapBonusContinuation -> {
                val colors = question as? ChooseColorDecision ?: return null
                if (colors.maxColors != 1) return null
                colors.availableColors.asSequence().map { ColorChosenResponse(question.id, it) }
            }
            is AddDynamicManaContinuation -> {
                val number = question as? ChooseNumberDecision ?: return null
                (number.minValue..number.maxValue).asSequence().map { NumberChosenResponse(question.id, it) }
            }
            else -> null
        }
    }

    // Only costs with no object/type/amount choice. The real handler validates and pays each
    // prefix, so repeated activations share life, counters and floating mana rather than counting
    // the same resource twice. Taps and sacrifices naturally remove their source's availability.
    // The node budget also bounds positive-mana loops; no hypothetical resources are published.
    private fun safeCost(cost: AbilityCost): Boolean = when (cost) {
        AbilityCost.Tap, AbilityCost.SacrificeSelf -> true
        is AbilityCost.Composite -> cost.costs.isNotEmpty() && cost.costs.all(::supportedCost) && cost.costs.any(::safeCost)
        is AbilityCost.Atom -> when (val atom = cost.atom) {
            is CostAtom.Mana -> !atom.cost.hasX && atom.cost.cmc > 0
            is CostAtom.PayLife -> atom.amount > 0
            is CostAtom.RemoveCounters -> atom.self && atom.counterType != null &&
                (atom.count as? DynamicAmount.Fixed)?.amount?.let { it > 0 } == true
            else -> false
        }
        else -> false
    }

    // A zero atom is harmless beside a consuming cost (e.g. {0}, {T}); it must not make a
    // genuinely free ability eligible for repeated search by itself.
    private fun supportedCost(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.Composite -> cost.costs.all(::supportedCost)
        is AbilityCost.Atom -> when (val atom = cost.atom) {
            is CostAtom.Mana -> !atom.cost.hasX
            is CostAtom.PayLife -> atom.amount >= 0
            else -> safeCost(cost)
        }
        else -> safeCost(cost)
    }

    private fun manaOnly(effect: Effect): Boolean = when (effect) {
        is AddManaEffect, is AddColorlessManaEffect, is AddManaOfChoiceEffect, is AddDynamicManaEffect -> true
        is CompositeEffect -> effect.effects.isNotEmpty() && effect.effects.all(::manaOnly)
        else -> false
    }
}
