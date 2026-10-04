package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler
import com.wingedsheep.engine.handlers.actions.ability.ActivatedAbilityResolver
import com.wingedsheep.engine.handlers.actions.decision.DecisionValidators
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.activeManaSpendingScope
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
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
            var attempted = 1
            // Short proofs precede irrelevant permutations. Bound attempts, including rejected choices.
            while (pending.isNotEmpty()) {
                val (current, events) = pending.removeFirst()
                if (current.pendingDecision == null && current.continuationStack.size == continuationFloor &&
                    complete(current)) return ScopedManaPlanResult.Found(ExecutionResult.success(current, events))
                if (current.pendingDecision != null) {
                    // Branch only over the paying player's public cost and production answers. Each branch uses
                    // the real validation and continuation dispatch, but cannot consume caller work.
                    val responses = activationResponses(current, player)
                    if (responses == null) {
                        limits.add(ScopedManaSearchLimit.UNSUPPORTED_DECISION)
                        continue
                    }
                    for (response in responses) {
                        val decision = current.pendingDecision ?: break
                        if (attempted >= nodeLimit) {
                            limits.add(ScopedManaSearchLimit.NODE_BUDGET)
                            break
                        }
                        attempted++
                        if (DecisionValidators.validate(decision, response, current) != null) continue
                        val result = services.continuationHandler.resumeWithin(current, response, continuationFloor)
                        if (result.error != null) {
                            limits.add(ScopedManaSearchLimit.EXECUTION_FAILURE)
                            continue
                        }
                        if (result.state.gameOver) continue
                        pending.add(Prefix(result.state, events + DecisionSubmittedEvent(decision.id, player) + result.events))
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
                    if (ability == null || !safeCost(ability.cost) || exileCostCount(ability.cost) > 1 ||
                        hasNestedExileCost(ability.cost) || !manaOnly(ability.effect)) {
                        limits.add(ScopedManaSearchLimit.UNSUPPORTED_ACTIVATION)
                        continue
                    }
                    // Choice previews can omit source-relative costs; the real action validates them.
                    if (!candidate.affordable && !hasCostChoice(ability.cost)) continue
                    // A composite can contain independently chosen colors. A shared activation
                    // color would couple those choices, so let its production leaves ask separately.
                    val colors: List<Color?> = if (candidate.requiresManaColorChoice &&
                        ability.effect !is CompositeEffect)
                        candidate.availableManaColors ?: Color.entries else listOf(null)
                    if (colors.isEmpty()) continue
                    // A named self-counter X has no handler picker; carry its enumerated bound
                    // on the real action. Tap-X and mana-X use their ordinary nested questions.
                    val xs: Sequence<Int?> = if (candidate.hasXCost && hasSelfCounterX(ability.cost)) {
                        val maximum = candidate.maxAffordableX
                        if (maximum == null || maximum == Int.MAX_VALUE) {
                            limits.add(ScopedManaSearchLimit.UNSUPPORTED_ACTIVATION)
                            continue
                        }
                        (candidate.minX..maximum).asSequence()
                    } else sequenceOf(null)
                    val tapCost = TextChanges.of(current, base.sourceId)
                        ?.let { ability.cost.applyTextReplacement(it) } ?: ability.cost
                    val tap = fixedTapCost(tapCost)
                    val taps = if (tap == null) sequenceOf(emptyList()) else {
                        val options = services.costHandler.findMatchingCardsUnified(
                            current, current.controlledBattlefield(player), tap.filter, player, sourceId = base.sourceId)
                            .filter { (!tap.excludeSelf || it != base.sourceId) &&
                                current.getEntity(it)?.has<TappedComponent>() != true }
                        if (options.size < tap.count) continue
                        scopedManaSelections(options, tap.count, tap.count)
                    }
                    actions@ for (x in xs) for (selected in taps) for (color in colors) {
                        if (attempted >= nodeLimit) {
                            limits.add(ScopedManaSearchLimit.NODE_BUDGET)
                            break@actions
                        }
                        attempted++
                        val action = base.copy(manaColorChoice = color, xValue = x,
                            costPayment = if (tap == null) null else AdditionalCostPayment(tappedPermanents = selected),
                            paymentStrategy = PaymentStrategy.FromPool)
                        if (handler.validate(current, action) != null) continue
                        val result = handler.execute(current, action)
                        // All announcement and production questions stay private to the branch.
                        if (result.error != null) {
                            limits.add(ScopedManaSearchLimit.EXECUTION_FAILURE)
                            continue
                        }
                        if (result.state.gameOver) continue
                        pending.add(Prefix(result.state, events + result.events))
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

    private fun activationResponses(state: GameState, player: EntityId): Sequence<DecisionResponse>? {
        val suspension = state.continuationStack.lastOrNull() as? Suspension ?: return null
        val question = suspension.question
        if (question.playerId != player) return null
        return when (suspension.answer) {
            is ChooseManaColorContinuation, is AddManaPipsContinuation, is ChooseAnyColorTapBonusContinuation -> {
                val colors = question as? ChooseColorDecision ?: return null
                if (colors.maxColors != 1) return null
                colors.availableColors.asSequence().map { ColorChosenResponse(question.id, it) }
            }
            is AddDynamicManaContinuation, is ActivateAbilityChooseXContinuation,
            is ActivateAbilityChooseManaXContinuation -> {
                val number = question as? ChooseNumberDecision ?: return null
                (number.minValue..number.maxValue).asSequence().map { NumberChosenResponse(question.id, it) }
            }
            is ActivateAbilitySacrificeContinuation, is ActivateAbilityVariablePermanentsContinuation,
            is ActivateAbilityTapXTargetsContinuation, is ActivateAbilityExileFromGraveyardContinuation,
            is ActivateAbilityExileXFromGraveyardContinuation -> {
                val cards = question as? SelectCardsDecision ?: return null
                scopedManaSelections(cards.options, cards.minSelections, cards.maxSelections)
                    .map { CardsSelectedResponse(question.id, it) }
            }
            else -> null
        }
    }

    private fun hasCostChoice(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.Composite -> cost.costs.any(::hasCostChoice)
        is AbilityCost.TapXPermanents, is AbilityCost.ExileXFromGraveyard -> true
        is AbilityCost.Atom -> when (val atom = cost.atom) {
            is CostAtom.TapPermanents, is CostAtom.Sacrifice, is CostAtom.VariablePermanents,
            is CostAtom.ExileFrom -> true
            is CostAtom.Mana -> atom.cost.hasX
            is CostAtom.RemoveCounters -> atom.count is DynamicAmount.XValue
            else -> false
        }
        else -> false
    }

    private fun hasSelfCounterX(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.Composite -> cost.costs.any(::hasSelfCounterX)
        is AbilityCost.Atom -> (cost.atom as? CostAtom.RemoveCounters)?.let {
            it.self && it.count is DynamicAmount.XValue
        } == true
        else -> false
    }

    private fun fixedTapCost(cost: AbilityCost): CostAtom.TapPermanents? = when (cost) {
        is AbilityCost.Composite -> cost.costs.firstNotNullOfOrNull(::fixedTapCost)
        is AbilityCost.Atom -> cost.atom as? CostAtom.TapPermanents
        else -> null
    }

    // Public, finite choices only. The real handler validates and pays each
    // prefix, so repeated activations share life, counters and floating mana rather than counting
    // the same resource twice. Taps and sacrifices naturally remove their source's availability.
    // The node budget also bounds positive-mana loops; no hypothetical resources are published.
    private fun safeCost(cost: AbilityCost): Boolean = when (cost) {
        AbilityCost.Tap, AbilityCost.SacrificeSelf, is AbilityCost.TapXPermanents,
        is AbilityCost.ExileXFromGraveyard -> true
        is AbilityCost.Composite -> cost.costs.isNotEmpty() && cost.costs.all(::supportedCost) && cost.costs.any(::safeCost)
        is AbilityCost.Atom -> when (val atom = cost.atom) {
            is CostAtom.Mana -> atom.cost.hasX || atom.cost.cmc > 0
            is CostAtom.PayLife -> atom.amount > 0
            is CostAtom.RemoveCounters -> atom.self && atom.counterType != null &&
                ((atom.count as? DynamicAmount.Fixed)?.amount?.let { it > 0 } == true ||
                    atom.count is DynamicAmount.XValue)
            is CostAtom.Sacrifice -> atom.count > 0
            is CostAtom.ExileFrom -> atom.zone == Zone.GRAVEYARD && atom.count > 0
            is CostAtom.TapPermanents -> atom.count > 0
            is CostAtom.VariablePermanents -> atom.minCount > 0 || atom.minMeasure > 0
            else -> false
        }
        else -> false
    }

    // A zero atom is harmless beside a consuming cost (e.g. {0}, {T}); it must not make a
    // genuinely free ability eligible for repeated search by itself.
    private fun supportedCost(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.Composite -> cost.costs.all(::supportedCost)
        is AbilityCost.Atom -> when (val atom = cost.atom) {
            is CostAtom.Mana -> true
            is CostAtom.PayLife -> atom.amount >= 0
            else -> safeCost(cost)
        }
        else -> safeCost(cost)
    }

    // All exile selections share exiledCards on the action. Multiple exile atoms would need
    // per-atom choices; do not let one selection pay two different costs in a speculative proof.
    private fun exileCostCount(cost: AbilityCost): Int = when (cost) {
        is AbilityCost.Composite -> cost.costs.sumOf(::exileCostCount)
        is AbilityCost.ExileXFromGraveyard -> 1
        is AbilityCost.Atom -> if (cost.atom is CostAtom.ExileFrom) 1 else 0
        else -> 0
    }

    // Activation choice extractors only see immediate composite children. A nested exile
    // cost could silently skip its picker, so it cannot establish an impossibility proof.
    private fun hasNestedExileCost(cost: AbilityCost): Boolean =
        cost is AbilityCost.Composite && cost.costs.any {
            it is AbilityCost.Composite && exileCostCount(it) > 0
        }

    private fun manaOnly(effect: Effect): Boolean = when (effect) {
        is AddManaEffect, is AddColorlessManaEffect, is AddManaOfChoiceEffect, is AddDynamicManaEffect -> true
        is CompositeEffect -> effect.effects.isNotEmpty() && effect.effects.all(::manaOnly)
        else -> false
    }
}

/** Lazy, distinct subsets: no materialized power set, and every yielded choice consumes search budget. */
internal fun scopedManaSelections(options: List<EntityId>, minimum: Int, maximum: Int): Sequence<List<EntityId>> = sequence {
    val distinct = options.distinct()
    for (size in minimum.coerceAtLeast(0)..maximum.coerceAtMost(distinct.size)) {
        if (size == 0) {
            yield(emptyList())
            continue
        }
        val indices = IntArray(size) { it }
        while (true) {
            yield(indices.map { distinct[it] })
            var position = size - 1
            while (position >= 0 && indices[position] == distinct.size - size + position) position--
            if (position < 0) break
            indices[position]++
            for (next in position + 1 until size) indices[next] = indices[next - 1] + 1
        }
    }
}
