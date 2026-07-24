package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.CostPaymentContinuation
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.permanent.counters.resolveCounterType
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import com.wingedsheep.sdk.scripting.costs.PayCost
import java.util.UUID

/**
 * Single, shared engine service that owns paying every [PayCost] variant.
 *
 * Before this existed, five consumers (morph turn-face-up, PayOrSuffer, AnyPlayerMayPay, chain-copy,
 * and the face-up legal-action enumerator) each carried their own `when (cost: PayCost)` switch, each
 * covering a *different, incomplete* subset of the ten variants — so a cost that worked as a morph
 * cost could silently fail as a punisher cost and vice-versa. This service consolidates the two
 * questions every one of them asks:
 *
 * - [canAfford] — the affordability pre-check (CR 118.3). Pure and allocation-light enough to run in
 *   legal-action enumeration; never mutates or prompts.
 * - [pay] — performs the payment, pausing with the right decision (battlefield targeting for
 *   sacrifice / return / tap, card-selection for discard / exile / reveal, yes/no for mana / life,
 *   option-pick for [PayCost.Choice]) and resuming through a single
 *   [com.wingedsheep.engine.core.CostPaymentContinuation].
 *
 * It is deliberately *not* a lowering of costs into effects (a cost is checked, atomic, and can't be
 * responded to — categorically not an effect): [PayCost] stays the authoring vocabulary; this is just
 * the one place that executes it.
 *
 * The payment-performing mutations ([performPayment]) live here too so the resumer is a thin caller.
 */
class CostPaymentService(private val services: EngineServices) {

    private val predicateEvaluator = PredicateEvaluator()
    private val decisionHandler = DecisionHandler()

    // ---------------------------------------------------------------------------------------------
    // Affordability (CR 118.3) — pure, no mutation, safe for the legal-action hot path.
    // ---------------------------------------------------------------------------------------------

    /**
     * Whether [payerId] can pay [cost]. For battlefield selection costs the [sourceId] is excluded
     * from the candidate pool (you can't sacrifice/return/tap the very permanent whose cost this is).
     *
     * Delegates to the [companion][Companion] form so legal-action enumerators
     * (e.g. `TurnFaceUpEnumerator`) can call affordability directly with just a [ManaSolver] —
     * one implementation, shared between the payment service and the hot enumeration path.
     */
    fun canAfford(
        state: GameState,
        payerId: EntityId,
        cost: PayCost,
        sourceId: EntityId,
        excludeSource: Boolean = true
    ): Boolean = canAfford(state, payerId, cost, sourceId, services.manaSolver, excludeSource)

    // ---------------------------------------------------------------------------------------------
    // Pay — build the right prompt and push a single continuation.
    // ---------------------------------------------------------------------------------------------

    /**
     * Attempt to pay [cost]. Returns [PaymentResult.Unaffordable] synchronously when the payer can't
     * pay; otherwise returns [PaymentResult.Pending] with a [CostPaymentContinuation] pushed. The
     * terminal paid/declined outcome (and the [ctx] follow-up) is realized when that continuation
     * resumes.
     */
    fun pay(
        state: GameState,
        payerId: EntityId,
        cost: PayCost,
        sourceId: EntityId,
        ctx: CostPaymentContext = CostPaymentContext(),
        excludeSource: Boolean = true
    ): PaymentResult {
        val resolved = resolve(state, cost, sourceId)
        if (!canAfford(state, payerId, resolved, sourceId, excludeSource)) {
            return PaymentResult.Unaffordable(state)
        }
        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "the source"

        return when (resolved) {
            is PayCost.Choice -> choicePrompt(state, payerId, resolved, sourceId, sourceName, ctx)
            // resolve() only yields OwnManaCost when unresolvable, and canAfford already rejected it.
            is PayCost.OwnManaCost -> PaymentResult.Unaffordable(state)
            is PayCost.Atom -> when (val atom = resolved.atom) {
                is CostAtom.Mana ->
                    yesNoPrompt(state, payerId, resolved, sourceId, sourceName, ctx, "Pay ${atom.cost}?", "Pay ${atom.cost}")
                is CostAtom.PayLife ->
                    yesNoPrompt(state, payerId, resolved, sourceId, sourceName, ctx, "Pay ${atom.amount} life?", "Pay ${atom.amount} life")
                is CostAtom.Discard ->
                    if (atom.random) {
                        val word = if (atom.count == 1) "a card" else "${atom.count} cards"
                        yesNoPrompt(state, payerId, resolved, sourceId, sourceName, ctx, "Discard $word at random?", "Discard")
                    } else {
                        selectionPrompt(state, payerId, resolved, sourceId, sourceName, ctx, cardsInHand(state, payerId, atom.filter), atom.count, useTargetingUI = false)
                    }
                is CostAtom.ExileFrom ->
                    selectionPrompt(state, payerId, resolved, sourceId, sourceName, ctx, cardsInZone(state, payerId, atom.filter, atom.zone), atom.count, useTargetingUI = atom.zone == Zone.BATTLEFIELD)
                is CostAtom.RevealFromHand ->
                    selectionPrompt(state, payerId, resolved, sourceId, sourceName, ctx, cardsInHand(state, payerId, atom.filter), atom.count, useTargetingUI = false)
                is CostAtom.Sacrifice ->
                    selectionPrompt(state, payerId, resolved, sourceId, sourceName, ctx, controlledMatching(state, payerId, atom.filter, sourceId), atom.count, useTargetingUI = true)
                is CostAtom.ReturnToHand ->
                    selectionPrompt(state, payerId, resolved, sourceId, sourceName, ctx, controlledMatching(state, payerId, atom.filter, sourceId), atom.count, useTargetingUI = true)
                is CostAtom.TapPermanents ->
                    selectionPrompt(state, payerId, resolved, sourceId, sourceName, ctx, controlledUntapped(state, payerId, atom.filter, if (atom.excludeSelf) sourceId else null), atom.count, useTargetingUI = true)
                // Activated-ability-scoped (see canAfford below, which reports it unaffordable as a
                // PayCost) — unreachable, but a yes/no is its shape if it is ever wired up.
                is CostAtom.PutCountersOnSelf ->
                    yesNoPrompt(state, payerId, resolved, sourceId, sourceName, ctx, "${atom.description}?", atom.description)
                is CostAtom.RemoveCounters -> {
                    val count = when (val c = atom.count) {
                        is com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed -> c.amount
                        else -> 0
                    }
                    if (atom.self) {
                        // Self-removal: no permanent selection needed, just confirm
                        val prompt = atom.description
                        yesNoPrompt(state, payerId, resolved, sourceId, sourceName, ctx, prompt, prompt)
                    } else {
                        val candidates = controlledMatching(
                            state, payerId, atom.filter, if (excludeSource) sourceId else null
                        )
                        if (candidates.isEmpty()) {
                            return PaymentResult.Unaffordable(state)
                        }
                        // A typed counter cost is player-distributed: selecting the same entity
                        // multiple times assigns multiple counters to it. Any-type costs remain
                        // auto-resolved because their counter type must be chosen per removal.
                        if (atom.counterType != null && count > 0) {
                            selectionPrompt(state, payerId, resolved, sourceId, sourceName, ctx, candidates, count, useTargetingUI = true)
                        } else {
                            val prompt = "Remove $atom?"
                            yesNoPrompt(state, payerId, resolved, sourceId, sourceName, ctx, prompt, prompt.removeSuffix("?"))
                        }
                    }
                }
                // ExilePermanents is an activated-ability-only cost, never a PayCost.
                is CostAtom.ExilePermanents -> PaymentResult.Unaffordable(state)
            }
        }
    }

    private fun yesNoPrompt(
        state: GameState,
        payerId: EntityId,
        cost: PayCost,
        sourceId: EntityId,
        sourceName: String,
        ctx: CostPaymentContext,
        prompt: String,
        yesText: String
    ): PaymentResult {
        val result = decisionHandler.createYesNoDecision(
            state = state,
            playerId = payerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = prompt,
            yesText = yesText,
            noText = "Don't pay",
            phase = DecisionPhase.RESOLUTION
        )
        val decision = result.pendingDecision!!
        val stateWithContinuation = result.state.pushContinuation(continuation(decision.id, payerId, sourceId, sourceName, cost, ctx))
        return PaymentResult.Pending(stateWithContinuation, decision, result.events)
    }

    private fun selectionPrompt(
        state: GameState,
        payerId: EntityId,
        cost: PayCost,
        sourceId: EntityId,
        sourceName: String,
        ctx: CostPaymentContext,
        options: List<EntityId>,
        count: Int,
        useTargetingUI: Boolean
    ): PaymentResult {
        val result = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = payerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = "${cost.description.replaceFirstChar { it.uppercase() }}?",
            options = options,
            minSelections = 0,
            maxSelections = count,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            useTargetingUI = useTargetingUI
        )
        val decision = result.pendingDecision!!
        val stateWithContinuation = result.state.pushContinuation(continuation(decision.id, payerId, sourceId, sourceName, cost, ctx))
        return PaymentResult.Pending(stateWithContinuation, decision, result.events)
    }

    private fun choicePrompt(
        state: GameState,
        payerId: EntityId,
        cost: PayCost.Choice,
        sourceId: EntityId,
        sourceName: String,
        ctx: CostPaymentContext
    ): PaymentResult {
        // Offer only options the payer can actually pay; index 0..affordable.size-1 maps positionally,
        // and the trailing "Don't pay" option means decline.
        val affordable = cost.options.filter { canAfford(state, payerId, it, sourceId) }
        val labels = affordable.map { it.description.replaceFirstChar { ch -> ch.uppercase() } } + "Don't pay"
        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = payerId,
            prompt = "Choose one:",
            context = DecisionContext(sourceId = sourceId, sourceName = sourceName, phase = DecisionPhase.RESOLUTION),
            options = labels
        )
        // Store the reduced (affordable-only) Choice so the resumer can map the option index directly.
        val reduced = PayCost.Choice(affordable)
        val stateWithContinuation = state.withPendingDecision(decision)
            .pushContinuation(continuation(decisionId, payerId, sourceId, sourceName, reduced, ctx))
        return PaymentResult.Pending(
            stateWithContinuation,
            decision,
            listOf(DecisionRequestedEvent(decisionId, payerId, "CHOOSE_OPTION", decision.prompt))
        )
    }

    private fun continuation(
        decisionId: String,
        payerId: EntityId,
        sourceId: EntityId,
        sourceName: String,
        cost: PayCost,
        ctx: CostPaymentContext
    ): CostPaymentContinuation = CostPaymentContinuation(
        decisionId = decisionId,
        payerId = payerId,
        sourceId = sourceId,
        sourceName = sourceName,
        cost = cost,
        onPaid = ctx.onPaid,
        onDeclined = ctx.onDeclined,
        targets = ctx.targets,
        namedTargets = ctx.namedTargets,
        storedCollections = ctx.storedCollections
    )

    // ---------------------------------------------------------------------------------------------
    // Perform payment — mutate state once the payer has committed. Called by the resumer.
    // ---------------------------------------------------------------------------------------------

    /**
     * Apply the payment for [cost], where [selected] maps each entity the payer chose to the number
     * of units assigned to it (empty for yes/no costs and random discard). Returns the new state, the emitted
     * events, and whether the payment actually completed (mana solving can still fail defensively).
     *
     * [cost] must already be resolved ([PayCost.OwnManaCost] mapped to a [PayCost.Atom] CostAtom.Mana) and, for
     * [PayCost.Choice], a single chosen sub-cost — the resumer never calls this with a Choice.
     */
    fun performPayment(
        state: GameState,
        payerId: EntityId,
        cost: PayCost,
        sourceId: EntityId,
        selected: Map<EntityId, Int>
    ): CostPaymentExecution = when (cost) {
        is PayCost.OwnManaCost -> CostPaymentExecution(state, emptyList(), success = false)
        is PayCost.Choice -> CostPaymentExecution(state, emptyList(), success = false)
        is PayCost.Atom -> when (val atom = cost.atom) {
            is CostAtom.Mana -> payMana(state, payerId, atom.cost, sourceId)
            is CostAtom.PayLife -> payLife(state, payerId, atom.amount)
            is CostAtom.Discard ->
                if (atom.random) discardRandom(state, payerId, atom.filter, atom.count)
                else discardSelected(state, payerId, selected.keys.toList())
            is CostAtom.ExileFrom -> exileSelected(state, payerId, selected.keys.toList(), atom.zone)
            is CostAtom.RevealFromHand -> revealSelected(state, payerId, selected.keys.toList())
            is CostAtom.Sacrifice -> sacrificeSelected(state, payerId, selected.keys.toList())
            is CostAtom.ReturnToHand -> returnSelected(state, selected.keys.toList())
            is CostAtom.TapPermanents -> tapSelected(state, selected.keys.toList())
            // Not offered as a PayCost (canAfford reports it unaffordable) — an activated-ability
            // cost is paid through CostHandler.payAtom, which owns the counter-placement path.
            is CostAtom.PutCountersOnSelf -> CostPaymentExecution(state, emptyList(), success = false)
            is CostAtom.RemoveCounters -> performRemoveCounters(state, payerId, atom, sourceId, selected)
            // ExilePermanents is an activated-ability-only cost, never a PayCost.
            is CostAtom.ExilePermanents -> CostPaymentExecution(state, emptyList(), success = false)
        }
    }

    private fun performRemoveCounters(
        state: GameState,
        payerId: EntityId,
        atom: CostAtom.RemoveCounters,
        sourceId: EntityId,
        selected: Map<EntityId, Int>
    ): CostPaymentExecution {
        val required = when (val c = atom.count) {
            is com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed -> c.amount
            else -> return CostPaymentExecution(state, emptyList(), success = false)
        }
        var newState = state
        val events = mutableListOf<GameEvent>()
        val counterType = atom.counterType?.let {
            resolveCounterType(it)
        }

        var remaining = required
        if (atom.self) {
            val selfId = sourceId
            val container = newState.getEntity(selfId)
                ?: return CostPaymentExecution(state, emptyList(), success = false)
            val counters = container.get<CountersComponent>()
                ?: return CostPaymentExecution(state, emptyList(), success = false)
            if (counterType != null) {
                if (counters.getCount(counterType) < required) {
                    return CostPaymentExecution(state, emptyList(), success = false)
                }
                newState = newState.updateEntity(selfId) { c ->
                    c.with(counters.withRemoved(counterType, required))
                }
                events.add(CountersRemovedEvent(selfId, atom.counterType!!, required, container.get<CardComponent>()?.name ?: "Permanent"))
                remaining = 0
            } else {
                var selfRemaining = required
                for ((type, available) in counters.counters.filterValues { it > 0 }) {
                    if (selfRemaining == 0) break
                    val toRemove = minOf(selfRemaining, available)
                    newState = newState.updateEntity(selfId) { c ->
                        val current = c.get<CountersComponent>()
                            ?: return@updateEntity c
                        c.with(current.withRemoved(type, toRemove))
                    }
                    events.add(
                        CountersRemovedEvent(
                            selfId,
                            com.wingedsheep.engine.handlers.effects.permanent.counters.counterTypeToString(type),
                            toRemove,
                            container.get<CardComponent>()?.name ?: "Permanent"
                        )
                    )
                    selfRemaining -= toRemove
                }
                remaining = selfRemaining
            }
        } else if (counterType != null && selected.isNotEmpty()) {
            val totalSelected = selected.values.sum()
            if (selected.values.any { it <= 0 } || totalSelected != required) {
                return CostPaymentExecution(state, emptyList(), success = false)
            }
            val removals = selected.map { (entityId, count) ->
                DistributedCounterRemoval(entityId, atom.counterType!!, count)
            }
            return applyDistributedCounterRemovals(newState, payerId, atom, removals)
        } else {
            // Auto-resolve: remove from permanents with the most counters first
            val projected = newState.projectedState
            val candidates = projected.getBattlefieldControlledBy(payerId).filter {
                predicateEvaluator.matches(newState, projected, it, atom.filter, PredicateContext(controllerId = payerId))
            }.sortedByDescending { entityId ->
                val counters = newState.getEntity(entityId)
                    ?.get<CountersComponent>()
                if (counterType != null) counters?.getCount(counterType) ?: 0
                else counters?.counters?.values?.sum() ?: 0
            }
            for (entityId in candidates) {
                if (remaining <= 0) break
                val container = newState.getEntity(entityId) ?: continue
                val counters = container.get<CountersComponent>() ?: continue
                val available = if (counterType != null) counters.getCount(counterType)
                else counters.counters.values.sum()
                val toRemove = minOf(remaining, available)
                if (toRemove <= 0) continue
                val removeType = counterType ?: counters.counters
                    .filterValues { it > 0 }
                    .maxByOrNull { it.value }
                    ?.key ?: continue
                newState = newState.updateEntity(entityId) { c ->
                    c.with(counters.withRemoved(removeType, toRemove))
                }
                val name = container.get<CardComponent>()?.name ?: "Permanent"
                val typeName = atom.counterType ?: com.wingedsheep.engine.handlers.effects.permanent.counters.counterTypeToString(removeType)
                events.add(CountersRemovedEvent(entityId, typeName, toRemove, name))
                remaining -= toRemove
            }
        }
        return CostPaymentExecution(newState, events, success = remaining <= 0)
    }

    private fun payMana(state: GameState, payerId: EntityId, manaCost: ManaCost, sourceId: EntityId): CostPaymentExecution {
        val playerEntity = state.getEntity(payerId) ?: return CostPaymentExecution(state, emptyList(), false)
        val poolComponent = playerEntity.get<ManaPoolComponent>() ?: ManaPoolComponent()
        val pool = ManaPool(
            poolComponent.white, poolComponent.blue, poolComponent.black,
            poolComponent.red, poolComponent.green, poolComponent.colorless
        )

        // Spend floating mana first, then tap sources for the remainder.
        val partial = pool.payPartial(manaCost)
        var combined = pool
        var current = state
        val events = mutableListOf<GameEvent>()

        if (!partial.remainingCost.isEmpty()) {
            val solution = services.manaSolver.solve(current, payerId, partial.remainingCost)
                ?: return CostPaymentExecution(state, emptyList(), false)
            val (afterTaps, tapEvents) = services.manaAbilitySideEffectExecutor
                .tapSourcesWithSideEffects(current, solution, payerId)
            current = afterTaps
            events.addAll(tapEvents)
            for ((_, production) in solution.manaProduced) {
                combined = if (production.color != null) combined.add(production.color, production.amount)
                else combined.addColorless(production.colorless)
            }
            // Bonus mana from AdditionalManaOnTap / AdditionalManaOnSourceTap (e.g. Badgermole
            // Cub's "Whenever you tap a creature for mana, add an additional {G}") and mana auras
            // is not in `manaProduced`, so credit it here — otherwise the cost comes up short even
            // though the solver counted the bonus toward affordability. Mirrors the activate-ability
            // auto-tap path (ActivateAbilityHandler.autoTapForManaCost).
            for (source in solution.sources) {
                if (source.bonusManaPerTap > 0 && source.bonusManaColor != null) {
                    combined = combined.add(source.bonusManaColor!!, source.bonusManaPerTap)
                }
            }
        }

        val newPool = combined.pay(manaCost) ?: return CostPaymentExecution(state, emptyList(), false)
        current = current.updateEntity(payerId) {
            it.with(ManaPoolComponent(newPool.white, newPool.blue, newPool.black, newPool.red, newPool.green, newPool.colorless))
        }

        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "the source"
        events.add(
            ManaSpentEvent(
                playerId = payerId,
                reason = "Pay cost for $sourceName",
                white = combined.white - newPool.white,
                blue = combined.blue - newPool.blue,
                black = combined.black - newPool.black,
                red = combined.red - newPool.red,
                green = combined.green - newPool.green,
                colorless = combined.colorless - newPool.colorless
            )
        )
        return CostPaymentExecution(current, events, success = true)
    }

    private fun payLife(state: GameState, payerId: EntityId, amount: Int): CostPaymentExecution {
        val (newState, event) = DamageUtils.loseLife(state, payerId, amount, LifeChangeReason.PAYMENT)
        return CostPaymentExecution(newState, listOfNotNull(event), success = true)
    }

    private fun discardSelected(state: GameState, payerId: EntityId, selected: List<EntityId>): CostPaymentExecution {
        val handZone = ZoneKey(payerId, Zone.HAND)
        val graveyardZone = ZoneKey(payerId, Zone.GRAVEYARD)
        var newState = state
        val events = mutableListOf<GameEvent>()
        val names = mutableListOf<String>()
        for (cardId in selected) {
            val name = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            names.add(name)
            newState = newState.removeFromZone(handZone, cardId).addToZone(graveyardZone, cardId)
            events.add(ZoneChangeEvent(cardId, name, Zone.HAND, Zone.GRAVEYARD, payerId))
        }
        events.add(0, CardsDiscardedEvent(payerId, selected, names))
        return CostPaymentExecution(newState, events, success = true)
    }

    private fun discardRandom(state: GameState, payerId: EntityId, filter: GameObjectFilter, count: Int): CostPaymentExecution {
        val handZone = ZoneKey(payerId, Zone.HAND)
        val graveyardZone = ZoneKey(payerId, Zone.GRAVEYARD)
        val context = PredicateContext(controllerId = payerId)
        val valid = state.getZone(handZone).filter {
            predicateEvaluator.matches(state, state.projectedState, it, filter, context)
        }
        if (valid.isEmpty()) return CostPaymentExecution(state, emptyList(), success = false)

        val (shuffled, stateAfterShuffle) = state.nextRandom { shuffle(valid) }
        val toDiscard = shuffled.take(count)
        var newState = stateAfterShuffle
        val events = mutableListOf<GameEvent>()
        val names = mutableListOf<String>()
        for (cardId in toDiscard) {
            val name = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            names.add(name)
            newState = newState.removeFromZone(handZone, cardId).addToZone(graveyardZone, cardId)
            events.add(ZoneChangeEvent(cardId, name, Zone.HAND, Zone.GRAVEYARD, payerId))
        }
        events.add(0, CardsDiscardedEvent(payerId, toDiscard, names))
        return CostPaymentExecution(newState, events, success = true)
    }

    private fun exileSelected(state: GameState, payerId: EntityId, selected: List<EntityId>, zone: Zone): CostPaymentExecution {
        val fromZone = ZoneKey(payerId, zone)
        val exileZone = ZoneKey(payerId, Zone.EXILE)
        var newState = state
        val events = mutableListOf<GameEvent>()
        for (cardId in selected) {
            val name = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.removeFromZone(fromZone, cardId).addToZone(exileZone, cardId)
            events.add(ZoneChangeEvent(cardId, name, zone, Zone.EXILE, payerId))
        }
        return CostPaymentExecution(newState, events, success = true)
    }

    private fun revealSelected(state: GameState, payerId: EntityId, selected: List<EntityId>): CostPaymentExecution {
        // The cards stay in hand; revealing is purely informational.
        val names = selected.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Unknown" }
        val events = listOf(CardsRevealedEvent(payerId, selected, names, source = "Cost payment"))
        return CostPaymentExecution(state, events, success = true)
    }

    private fun sacrificeSelected(state: GameState, payerId: EntityId, selected: List<EntityId>): CostPaymentExecution {
        var newState = state
        val events = mutableListOf<GameEvent>()
        if (selected.isNotEmpty()) {
            val names = selected.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Unknown" }
            events.add(PermanentsSacrificedEvent(payerId, selected, names))
            newState = ZoneTransitionService.trackPermanentSacrifice(newState, selected, payerId)
        }
        for (permanentId in selected) {
            val transition = ZoneTransitionService.moveToZone(newState, permanentId, Zone.GRAVEYARD)
            newState = transition.state
            events.addAll(transition.events)
        }
        return CostPaymentExecution(newState, events, success = true)
    }

    private fun returnSelected(state: GameState, selected: List<EntityId>): CostPaymentExecution {
        var newState = state
        val events = mutableListOf<GameEvent>()
        for (permanentId in selected) {
            val result = ZoneMovementUtils.movePermanentToZone(newState, permanentId, Zone.HAND)
            if (result.error != null) return CostPaymentExecution(state, emptyList(), success = false)
            newState = result.state
            events.addAll(result.events)
        }
        return CostPaymentExecution(newState, events, success = true)
    }

    private fun tapSelected(state: GameState, selected: List<EntityId>): CostPaymentExecution {
        var newState = state
        val events = mutableListOf<GameEvent>()
        for (permanentId in selected) {
            val (tappedState, tapEvent) = tap(newState, permanentId)
            newState = tappedState
            tapEvent?.let(events::add)
        }
        return CostPaymentExecution(newState, events, success = true)
    }

    // ---------------------------------------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------------------------------------

    /** How many entities a selection cost requires; 0 for non-selection costs. */
    fun requiredCount(cost: PayCost): Int = when (cost) {
        is PayCost.Atom -> cost.atom.selectionCount
        is PayCost.OwnManaCost, is PayCost.Choice -> 0
    }

    /**
     * Affordability + the pure cost-candidate reads live here so the one implementation is shared by
     * both the payment service (instance [pay]/[canAfford]) and legal-action enumeration, which has a
     * [ManaSolver] but not a full [EngineServices]. Everything here is pure and allocation-light
     * enough for the enumeration hot path — it never mutates or prompts.
     */
    companion object {
        private val predicateEvaluator = PredicateEvaluator()

        /**
         * Whether [payerId] can pay [cost]; see the instance [canAfford]. Takes a bare [ManaSolver]
         * so legal-action enumerators can gate the action without constructing the full service.
         */
        fun canAfford(
            state: GameState,
            payerId: EntityId,
            cost: PayCost,
            sourceId: EntityId,
            manaSolver: ManaSolver,
            excludeSource: Boolean = true
        ): Boolean {
            return when (val c = resolve(state, cost, sourceId)) {
                // Only unresolvable own-mana-costs reach here (missing source/card component) — unpayable.
                is PayCost.OwnManaCost -> false
                is PayCost.Choice -> c.options.any { canAfford(state, payerId, it, sourceId, manaSolver) }
                is PayCost.Atom -> when (val atom = c.atom) {
                    is CostAtom.Mana -> manaSolver.canPay(state, payerId, atom.cost)
                    // CR 119.4 — a player may pay life only if their life total is at least the amount; paying
                    // life that would reduce them to 0 or less is legal (they then lose as a state-based action).
                    is CostAtom.PayLife -> life(state, payerId) >= atom.amount
                    is CostAtom.Discard -> cardsInHand(state, payerId, atom.filter).size >= atom.count
                    is CostAtom.ExileFrom -> cardsInZone(state, payerId, atom.filter, atom.zone).size >= atom.count
                    is CostAtom.RevealFromHand -> cardsInHand(state, payerId, atom.filter).size >= atom.count
                    is CostAtom.Sacrifice -> {
                        val candidates = controlledMatching(
                            state, payerId, atom.filter, if (excludeSource) sourceId else null
                        )
                        if (atom.distinctNames) distinctNameCount(state, candidates) >= atom.count
                        else candidates.size >= atom.count
                    }
                    is CostAtom.ReturnToHand -> controlledMatching(state, payerId, atom.filter, sourceId).size >= atom.count
                    is CostAtom.TapPermanents -> controlledUntapped(state, payerId, atom.filter, if (atom.excludeSelf) sourceId else null).size >= atom.count
                    // Activated-ability cost only: no printed morph / "unless you …" cost puts
                    // counters on a permanent, and CostHandler owns the placement path.
                    is CostAtom.PutCountersOnSelf -> false
                    is CostAtom.RemoveCounters -> {
                        val needed = when (val c = atom.count) {
                            is com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed -> c.amount
                            is com.wingedsheep.sdk.scripting.values.DynamicAmount.XValue -> 0
                            else -> 0
                        }
                        if (needed <= 0) return@canAfford true
                        if (atom.self) {
                            val counters = state.getEntity(sourceId)?.get<CountersComponent>() ?: return@canAfford false
                            val ct = atom.counterType?.let { resolveCounterType(it) }
                            if (ct != null) counters.getCount(ct) >= needed
                            else counters.counters.values.sum() >= needed
                        } else {
                            val counterType = atom.counterType?.let { resolveCounterType(it) }
                            val projected = state.projectedState
                            val candidates = projected.getBattlefieldControlledBy(payerId).filter {
                                predicateEvaluator.matches(state, projected, it, atom.filter, PredicateContext(controllerId = payerId))
                            }
                            val total = candidates.sumOf { entityId ->
                                val counters = state.getEntity(entityId)?.get<CountersComponent>() ?: return@sumOf 0
                                if (counterType != null) counters.getCount(counterType)
                                else counters.counters.values.sum()
                            }
                            total >= needed
                        }
                    }
                    // ExilePermanents is an activated-ability-only cost, never a PayCost.
                    is CostAtom.ExilePermanents -> false
                }
            }
        }

        /**
         * Lowers [PayCost.OwnManaCost] to a concrete [PayCost.Atom] (CostAtom.Mana) against the source's printed cost so
         * every other code path sees a uniform shape. A source with no mana cost (a land, a token) has an
         * empty [ManaCost] — which is `{0}` and trivially payable. Returns the cost unchanged if the
         * source/card component is missing (unresolvable → reported unaffordable).
         */
        fun resolve(state: GameState, cost: PayCost, sourceId: EntityId): PayCost {
            if (cost !is PayCost.OwnManaCost) return cost
            val manaCost = state.getEntity(sourceId)?.get<CardComponent>()?.manaCost ?: return cost
            return PayCost.Atom(CostAtom.Mana(manaCost))
        }

        private fun life(state: GameState, playerId: EntityId): Int =
            state.lifeTotal(playerId) // CR 810.9a — team's shared total in Two-Headed Giant

        fun cardsInHand(state: GameState, playerId: EntityId, filter: GameObjectFilter): List<EntityId> {
            val context = PredicateContext(controllerId = playerId)
            return state.getZone(playerId, Zone.HAND).filter {
                predicateEvaluator.matches(state, state.projectedState, it, filter, context)
            }
        }

        fun cardsInZone(state: GameState, playerId: EntityId, filter: GameObjectFilter, zone: Zone): List<EntityId> {
            if (zone == Zone.BATTLEFIELD) {
                return BattlefieldFilterUtils.findMatchingOnBattlefield(
                    state, filter.youControl(), PredicateContext(controllerId = playerId)
                )
            }
            val context = PredicateContext(controllerId = playerId)
            return state.getZone(playerId, zone).filter {
                predicateEvaluator.matches(state, state.projectedState, it, filter, context)
            }
        }

        fun controlledMatching(
            state: GameState,
            playerId: EntityId,
            filter: GameObjectFilter,
            excludeSelfId: EntityId? = null
        ): List<EntityId> =
            BattlefieldFilterUtils.findMatchingOnBattlefield(
                state, filter.youControl(), PredicateContext(controllerId = playerId), excludeSelfId = excludeSelfId
            )

        /** [excludeSelfId] only for costs that say "another" — a plain "tap two untapped …" may tap the source itself. */
        fun controlledUntapped(state: GameState, playerId: EntityId, filter: GameObjectFilter, excludeSelfId: EntityId?): List<EntityId> =
            BattlefieldFilterUtils.findMatchingOnBattlefield(
                state, filter.youControl().untapped(), PredicateContext(controllerId = playerId), excludeSelfId = excludeSelfId
            )

        /** Number of distinct card names among [candidates] — for "with different names" costs. */
        fun distinctNameCount(state: GameState, candidates: List<EntityId>): Int =
            candidates.mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }.toSet().size

        /** Whether [selected] permanents all have pairwise-different names. */
        fun allDistinctNames(state: GameState, selected: List<EntityId>): Boolean {
            val names = selected.mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }
            return names.size == selected.size && names.toSet().size == names.size
        }

        /**
         * Apply distributed counter removals from [removals], validating each against the atom's
         * filter, controller, and counter availability. Returns the updated state, emitted events,
         * and whether the operation succeeded.
         *
         * Single implementation shared by [CostHandler][com.wingedsheep.engine.handlers.CostHandler]
         * (activated-ability costs) and [CostPaymentService] (PayCost variants: morph, punisher,
         * choice, etc.) — the one place counter-removal payment mutation lives.
         */
        internal fun applyDistributedCounterRemovals(
            state: GameState,
            playerId: EntityId,
            atom: CostAtom.RemoveCounters,
            removals: List<DistributedCounterRemoval>,
        ): CostPaymentExecution {
            if (removals.isEmpty()) return CostPaymentExecution(state, emptyList(), success = true)
            val projected = state.projectedState
            val ctx = PredicateContext(controllerId = playerId)
            val atomCounterType = atom.counterType?.let {
                resolveCounterType(it)
            }
            var newState = state
            val events = mutableListOf<GameEvent>()
            for (removal in removals) {
                if (removal.count <= 0) continue
                val container = newState.getEntity(removal.entityId)
                    ?: return CostPaymentExecution(state, emptyList(), success = false)
                if (projected.getController(removal.entityId) != playerId) {
                    return CostPaymentExecution(state, emptyList(), success = false)
                }
                if (!predicateEvaluator.matches(state, projected, removal.entityId, atom.filter, ctx)) {
                    return CostPaymentExecution(state, emptyList(), success = false)
                }
                val resolvedType = if (atomCounterType != null) {
                    val entryType = resolveCounterType(removal.counterType)
                    if (entryType != atomCounterType) {
                        return CostPaymentExecution(state, emptyList(), success = false)
                    }
                    atomCounterType
                } else {
                    resolveCounterType(removal.counterType)
                }
                val counters = container.get<CountersComponent>()
                    ?: return CostPaymentExecution(state, emptyList(), success = false)
                val available = counters.getCount(resolvedType)
                if (available < removal.count) {
                    return CostPaymentExecution(state, emptyList(), success = false)
                }
                newState = newState.updateEntity(removal.entityId) { c ->
                    c.with(counters.withRemoved(resolvedType, removal.count))
                }
                val typeName = atom.counterType ?: removal.counterType
                val entityName = container.get<CardComponent>()?.name ?: "Permanent"
                events.add(CountersRemovedEvent(removal.entityId, typeName, removal.count, entityName))
            }
            return CostPaymentExecution(newState, events, success = true)
        }
    }
}

/**
 * Result of [CostPaymentService.performPayment]: the post-payment state, the events it emitted, and
 * whether the payment actually completed ([success] is false only on a defensive failure such as a
 * mana solve that unexpectedly comes up short, in which case [state]/[events] are left untouched).
 */
data class CostPaymentExecution(
    val state: GameState,
    val events: List<GameEvent>,
    val success: Boolean
)
