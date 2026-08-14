package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ManaSourceOption
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.ReopenManaPaymentDecisionContinuation
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * CR 605.3a — "A player may activate an activated mana ability whenever they have priority,
 * **whenever they are casting a spell or activating an ability that requires a mana payment, or
 * whenever a rule or effect asks for a mana payment**, even if it's in the middle of casting or
 * resolving a spell or activating or resolving an ability."
 *
 * A [SelectManaSourcesDecision] is exactly that third clause: the engine has stopped the game to
 * ask one player for mana (ward, "you may pay {B}", a pay-to-attack tax, a draw replacement, …).
 * While it is open, that player holds no priority, so the ordinary `priorityPlayerId` gate in
 * `ActivateAbilityHandler` would reject every mana ability — leaving the pre-computed
 * [SelectManaSourcesDecision.availableSources] menu as the only way to produce mana. That menu is
 * deliberately narrow: [ManaSolver.findAvailableManaSources] only models `{T}`-shaped abilities, so
 * anything with a discard/Forage/sacrifice-something-else sub-cost, or a cost with no `{T}` at all
 * (Ashnod's Altar), was simply unreachable during a payment.
 *
 * This object is the single definition of "a mana payment is being asked for right now". Both the
 * engine's authority check (`ActivateAbilityHandler.validate`) and the server's offer
 * (`GameSession.getLegalActions`) read it, so the two can't drift.
 *
 * Activating a mana ability inside the window must leave the window itself untouched: the mana goes
 * to the pool, the decision is re-raised (refreshed — see [refresh]), and the player pays with the
 * floating mana when they confirm. Every payment resumer already spends the pool before tapping
 * anything, so no resumer needed to change.
 */
object ManaPaymentWindow {

    /**
     * The open mana-payment decision [playerId] is being asked to pay, or `null` if the game isn't
     * currently asking them for mana.
     *
     * [actorId] is the seat submitting the action, which may be driving another player's turn
     * (Mindslaver-style control). The window belongs to the *paying* player; the actor only needs
     * to be whoever currently drives them.
     */
    fun openFor(state: GameState, actorId: EntityId): SelectManaSourcesDecision? {
        val decision = state.pendingDecision as? SelectManaSourcesDecision ?: return null
        return decision.takeIf { state.actorFor(it.playerId) == actorId }
    }

    /**
     * Builds a mana-payment window for [cost] — the source menu, the auto-pay suggestion, and the
     * decision itself. The caller pushes its own continuation with the returned `decisionId` and
     * pauses; [floatSelectedMana] applies whatever the player submits.
     *
     * Sources carrying a secondary tap sub-cost (Springleaf Drum) are left out. Resolving those
     * needs a nested "which permanent do you tap?" prompt, which only the ward resumer implements —
     * and since CR 605.3a now lets the player activate any mana ability while the window is open,
     * leaving them off the menu costs nothing: the player taps the Drum themselves and the mana is
     * waiting in their pool.
     */
    fun buildDecision(
        state: GameState,
        playerId: EntityId,
        cost: com.wingedsheep.sdk.core.ManaCost,
        decisionId: String,
        prompt: String,
        context: com.wingedsheep.engine.core.DecisionContext,
        canDecline: Boolean,
        cardRegistry: CardRegistry
    ): SelectManaSourcesDecision {
        val solver = ManaSolver(cardRegistry)
        val options = solver.findAvailableManaSources(state, playerId)
            .filter { it.tapPermanentsSubCost == null }
            .map { source ->
                ManaSourceOption(
                    entityId = source.entityId,
                    name = source.name,
                    producesColors = source.producesColors,
                    producesColorless = source.producesColorless,
                    requiresSacrifice = source.requiresSacrifice
                )
            }
        val remaining = remainingAfterFloating(state, playerId, cost)
        val suggestion = if (remaining.isEmpty()) emptyList()
            else solver.solve(state, playerId, remaining)?.sources?.map { it.entityId }.orEmpty()

        return SelectManaSourcesDecision(
            id = decisionId,
            playerId = playerId,
            prompt = prompt,
            context = context,
            availableSources = options,
            requiredCost = cost.toString(),
            autoPaySuggestion = suggestion.filter { id -> options.any { it.entityId == id } },
            canDecline = canDecline
        )
    }

    /** Outcome of applying a [ManaSourcesSelectedResponse] to a window opened by [buildDecision]. */
    data class FloatResult(
        val state: GameState,
        val events: List<GameEvent>,
        /** False when the player refused, or the submission couldn't produce the mana. */
        val paid: Boolean
    )

    /**
     * Taps (or sacrifices) whatever the player submitted and puts the mana in their pool, leaving
     * the caller's own payment code to spend it. Floating mana the player already had — including
     * anything they made with a mana ability inside the window — is left alone and counts toward
     * the cost.
     *
     * Returns `paid = false` for a refusal, or when the submission doesn't add up; the caller runs
     * its "didn't pay" branch either way.
     */
    fun floatSelectedMana(
        state: GameState,
        playerId: EntityId,
        cost: com.wingedsheep.sdk.core.ManaCost,
        response: ManaSourcesSelectedResponse,
        availableSources: List<ManaSourceOption>,
        services: com.wingedsheep.engine.core.EngineServices
    ): FloatResult {
        val remaining = remainingAfterFloating(state, playerId, cost)
        if (response.isDecline(remaining.isEmpty())) return FloatResult(state, emptyList(), paid = false)
        if (remaining.isEmpty()) return FloatResult(state, emptyList(), paid = true)

        var current = state
        val events = mutableListOf<GameEvent>()
        var produced = ManaPool()

        if (response.autoPay) {
            val solution = ManaSolver(services.cardRegistry).solve(current, playerId, remaining)
                ?: return FloatResult(state, emptyList(), paid = false)
            val (afterTaps, tapEvents) = services.manaAbilitySideEffectExecutor
                .tapSourcesWithSideEffects(current, solution, playerId)
            current = afterTaps
            events.addAll(tapEvents)
            for ((_, p) in solution.manaProduced) {
                produced = if (p.color != null) produced.add(p.color, p.amount) else produced.addColorless(p.colorless)
            }
            // Bonus mana from mana auras / "whenever you tap for mana" riders isn't in
            // manaProduced; credit it or the cost comes up short (mirrors CostPaymentService.payMana).
            for (source in solution.sources) {
                val bonusColor = source.bonusManaColor
                if (source.bonusManaPerTap > 0 && bonusColor != null) {
                    produced = produced.add(bonusColor, source.bonusManaPerTap)
                }
            }
        } else {
            val byId = availableSources.associateBy { it.entityId }
            for (sourceId in response.selectedSources) {
                val source = byId[sourceId] ?: return FloatResult(state, emptyList(), paid = false)
                val tapped = tapOrSacrifice(current, sourceId, source, playerId)
                current = tapped.first
                events.addAll(tapped.second)
                produced = when {
                    source.producesColors.isNotEmpty() -> produced.add(source.producesColors.first())
                    source.producesColorless -> produced.addColorless(1)
                    else -> produced
                }
            }
        }

        return FloatResult(current.addToManaPool(playerId, produced), events, paid = true)
    }

    /**
     * Pays a selected source's activation cost: a `{T}, Sacrifice this` source (a Treasure) is
     * sacrificed, everything else is tapped. A [com.wingedsheep.engine.core.TappedEvent] fires
     * either way so "becomes tapped" triggers see the tap sub-cost.
     */
    private fun tapOrSacrifice(
        state: GameState,
        sourceId: EntityId,
        source: ManaSourceOption,
        fallbackControllerId: EntityId
    ): Pair<GameState, List<GameEvent>> {
        if (!source.requiresSacrifice) {
            val (tapped, event) = com.wingedsheep.engine.core.tap(state, sourceId)
            return tapped to listOfNotNull(event)
        }
        val controller = state.getEntity(sourceId)
            ?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId
            ?: fallbackControllerId
        val events = mutableListOf<GameEvent>(
            com.wingedsheep.engine.core.TappedEvent(sourceId, source.name)
        )
        val preState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
            .trackPermanentSacrifice(state, listOf(sourceId), controller)
        val transition = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
            .moveToZone(preState, sourceId, com.wingedsheep.sdk.core.Zone.GRAVEYARD)
        events.add(com.wingedsheep.engine.core.PermanentsSacrificedEvent(controller, listOf(sourceId)))
        events.addAll(transition.events)
        return transition.state to events
    }

    /** [cost] minus [playerId]'s floating mana. */
    private fun remainingAfterFloating(
        state: GameState,
        playerId: EntityId,
        cost: com.wingedsheep.sdk.core.ManaCost
    ): com.wingedsheep.sdk.core.ManaCost {
        val pool = state.getEntity(playerId)
            ?.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
            ?: return cost
        return ManaPool(pool.white, pool.blue, pool.black, pool.red, pool.green, pool.colorless)
            .payPartial(cost).remainingCost
    }

    /** Adds [produced] to [playerId]'s pool, preserving restricted mana and provenance. */
    private fun GameState.addToManaPool(playerId: EntityId, produced: ManaPool): GameState =
        updateEntity(playerId) { container ->
            val pool = container.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
                ?: com.wingedsheep.engine.state.components.player.ManaPoolComponent()
            container.with(
                pool.copy(
                    white = pool.white + produced.white,
                    blue = pool.blue + produced.blue,
                    black = pool.black + produced.black,
                    red = pool.red + produced.red,
                    green = pool.green + produced.green,
                    colorless = pool.colorless + produced.colorless
                )
            )
        }

    /**
     * Sets the window aside so a mana ability can resolve against a decision-free state, and
     * queues its restoration.
     *
     * The [ReopenManaPaymentDecisionContinuation] is pushed *above* the payment continuation that
     * is already on the stack, so if the mana ability raises a decision of its own (choosing a
     * color for Birds of Paradise, a Fertile Ground tap bonus) that decision nests on top and the
     * window is re-raised only once the ability has fully resolved.
     */
    fun suspend(state: GameState, decision: SelectManaSourcesDecision): GameState =
        state.clearPendingDecision()
            .pushContinuation(ReopenManaPaymentDecisionContinuation(decision.id, decision))

    /**
     * Re-raises the window that [suspend] set aside, popping its continuation frame.
     *
     * Returns `null` when the frame isn't on top — the mana ability paused for a nested decision,
     * so the frame stays put and the auto-resumer will re-raise the window later.
     */
    fun resumeIfPending(
        state: GameState,
        events: List<GameEvent>,
        cardRegistry: CardRegistry
    ): ExecutionResult? {
        val frame = state.peekContinuation() as? ReopenManaPaymentDecisionContinuation ?: return null
        val (_, popped) = state.popContinuation()
        return reopen(popped, frame.decision, events, cardRegistry)
    }

    /** Re-raises [decision], refreshed against the post-activation board. */
    fun reopen(
        state: GameState,
        decision: SelectManaSourcesDecision,
        events: List<GameEvent>,
        cardRegistry: CardRegistry
    ): ExecutionResult {
        val refreshed = refresh(state, decision, cardRegistry)
        return ExecutionResult.paused(state.withPendingDecision(refreshed), refreshed, events)
    }

    /**
     * Recomputes the menu against the current board so a source the player just tapped by hand
     * stops being offered, and the auto-pay suggestion covers only what the floating mana doesn't.
     *
     * The refreshed [SelectManaSourcesDecision.availableSources] is always a subset of the original
     * — activating a mana ability consumes sources, it never creates them — which matters because
     * the payment continuation still validates the player's final submission against the list it
     * captured when the window first opened.
     */
    fun refresh(
        state: GameState,
        decision: SelectManaSourcesDecision,
        cardRegistry: CardRegistry
    ): SelectManaSourcesDecision {
        val solver = ManaSolver(cardRegistry)
        val stillAvailable = solver.findAvailableManaSources(state, decision.playerId)
            .map { source ->
                ManaSourceOption(
                    entityId = source.entityId,
                    name = source.name,
                    producesColors = source.producesColors,
                    producesColorless = source.producesColorless,
                    requiresSacrifice = source.requiresSacrifice,
                    requiresTappingAnotherPermanent = source.tapPermanentsSubCost != null
                )
            }
            .associateBy { it.entityId }

        // Keep the original entries (the continuation validates against those) but drop the ones
        // the board no longer offers.
        val availableSources = decision.availableSources.filter { it.entityId in stillAvailable }

        val remaining = remainingCost(state, decision)
        val autoPaySuggestion = when {
            remaining == null || remaining.isEmpty() -> emptyList()
            else -> solver.solve(state, decision.playerId, remaining)?.sources?.map { it.entityId }
                ?: emptyList()
        }

        return decision.copy(
            availableSources = availableSources,
            autoPaySuggestion = autoPaySuggestion.filter { id -> availableSources.any { it.entityId == id } }
        )
    }

    /**
     * Whether [playerId]'s floating mana already covers [cost] in full.
     *
     * Payment resumers use this to tell "I refuse to pay" apart from "I already floated the mana
     * myself" — both submit an empty source selection.
     */
    fun floatingManaCovers(
        state: GameState,
        playerId: EntityId,
        cost: com.wingedsheep.sdk.core.ManaCost
    ): Boolean {
        val pool = state.getEntity(playerId)
            ?.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
            ?: return false
        return ManaPool(pool.white, pool.blue, pool.black, pool.red, pool.green, pool.colorless)
            .payPartial(cost)
            .remainingCost
            .isEmpty()
    }

    /** [SelectManaSourcesDecision.requiredCost] minus what the player already has floating. */
    private fun remainingCost(
        state: GameState,
        decision: SelectManaSourcesDecision
    ): com.wingedsheep.sdk.core.ManaCost? {
        val cost = runCatching { com.wingedsheep.sdk.core.ManaCost.parse(decision.requiredCost) }
            .getOrNull() ?: return null
        val pool = state.getEntity(decision.playerId)
            ?.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
            ?: return cost
        return ManaPool(pool.white, pool.blue, pool.black, pool.red, pool.green, pool.colorless)
            .payPartial(cost)
            .remainingCost
    }
}
