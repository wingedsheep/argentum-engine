package com.wingedsheep.engine.handlers.continuations
import com.wingedsheep.sdk.dsl.Patterns

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.composite.asOptionalManaPayment

class ManaPaymentContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(CounterUnlessPaysContinuation::class, ::resumeCounterUnlessPays),
        resumer(CounterUnlessPaysLifeContinuation::class, ::resumeCounterUnlessPaysLife),
        resumer(CounterUnlessDiscardContinuation::class, ::resumeCounterUnlessDiscard),
        resumer(CounterUnlessSacrificeContinuation::class, ::resumeCounterUnlessSacrifice),
        resumer(CounterUnlessPaysManaSelectionContinuation::class, ::resumeCounterUnlessPaysManaSelection),
        resumer(WardTapPermanentsSubCostContinuation::class, ::resumeWardTapPermanentsSubCost),
        resumer(ChangeSpellTargetContinuation::class, ::resumeChangeSpellTarget),
        resumer(MayPayManaContinuation::class, ::resumeMayPayMana),
        resumer(MayPayManaSelectionContinuation::class, ::resumeMayPayManaSelection),
        resumer(MayPayManaTriggerContinuation::class, ::resumeMayPayManaTrigger),
        resumer(MayPayXContinuation::class, ::resumeMayPayX),
        resumer(ManaSourceSelectionContinuation::class, ::resumeManaSourceSelection)
    )

    fun resumeCounterUnlessPays(
        state: GameState,
        continuation: CounterUnlessPaysContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for counter unless pays")
        }

        if (response.choice) {
            // Player chose to pay — show mana source selection
            val playerId = continuation.payingPlayerId
            val playerEntity = state.getEntity(playerId)
                ?: return ExecutionResult.error(state, "Paying player not found")
            val manaPoolComponent = playerEntity.get<ManaPoolComponent>()
                ?: return ExecutionResult.error(state, "Player has no mana pool")
            val manaPool = ManaPool(
                manaPoolComponent.white, manaPoolComponent.blue, manaPoolComponent.black,
                manaPoolComponent.red, manaPoolComponent.green, manaPoolComponent.colorless
            )
            val partialResult = manaPool.payPartial(continuation.manaCost)

            if (partialResult.remainingCost.isEmpty()) {
                // Floating mana covers the cost — pay immediately
                val newPool = manaPool.pay(continuation.manaCost)
                    ?: return ExecutionResult.error(state, "Cannot pay mana cost from floating mana")
                val currentState = state.updateEntity(playerId) { container ->
                    container.with(
                        ManaPoolComponent(
                            white = newPool.white, blue = newPool.blue, black = newPool.black,
                            red = newPool.red, green = newPool.green, colorless = newPool.colorless
                        )
                    )
                }
                return runOnPaidThenCheckForMore(
                    currentState,
                    emptyList(),
                    continuation.onPaid,
                    continuation.controllerId ?: continuation.payingPlayerId,
                    continuation.sourceId,
                    checkForMore
                )
            }

            // Need to tap sources — show mana source selection UI
            val manaSolver = ManaSolver(services.cardRegistry)
            val sources = manaSolver.findAvailableManaSources(state, playerId)
            val sourceOptions = sources.map { source ->
                ManaSourceOption(
                    entityId = source.entityId,
                    name = source.name,
                    producesColors = source.producesColors,
                    producesColorless = source.producesColorless,
                    requiresSacrifice = source.requiresSacrifice,
                    requiresTappingAnotherPermanent = source.tapPermanentsSubCost != null
                )
            }

            val solution = manaSolver.solve(state, playerId, partialResult.remainingCost)
            val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()

            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = SelectManaSourcesDecision(
                id = decisionId,
                playerId = playerId,
                prompt = "Pay ${continuation.manaCost}",
                context = DecisionContext(
                    sourceId = continuation.sourceId,
                    sourceName = continuation.sourceName ?: "Counter unless pays",
                    phase = DecisionPhase.RESOLUTION
                ),
                availableSources = sourceOptions,
                requiredCost = continuation.manaCost.toString(),
                autoPaySuggestion = autoPaySuggestion
            )

            val manaSelectionContinuation = CounterUnlessPaysManaSelectionContinuation(
                decisionId = decisionId,
                payingPlayerId = playerId,
                spellEntityId = continuation.spellEntityId,
                manaCost = continuation.manaCost,
                availableSources = sourceOptions,
                autoPaySuggestion = autoPaySuggestion,
                exileOnCounter = continuation.exileOnCounter,
                controllerId = continuation.controllerId,
                onPaid = continuation.onPaid,
                sourceId = continuation.sourceId
            )

            val stateWithDecision = state.withPendingDecision(decision)
            val stateWithContinuation = stateWithDecision.pushContinuation(manaSelectionContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = playerId,
                        decisionType = "SELECT_MANA_SOURCES",
                        prompt = decision.prompt
                    )
                )
            )
        } else {
            // Player chose not to pay — counter the spell
            val counterResult = if (continuation.exileOnCounter) {
                services.stackResolver.counterSpellToExile(
                    state, continuation.spellEntityId,
                    grantFreeCast = false,
                    controllerId = continuation.controllerId ?: continuation.payingPlayerId
                )
            } else {
                services.stackResolver.counterSpell(state, continuation.spellEntityId)
            }
            return checkForMore(counterResult.newState, counterResult.events)
        }
    }

    /**
     * Resume after the controller decides whether to pay a life cost
     * for a ward—pay-life trigger.
     *
     * Yes → deduct life (LifeChangedEvent / markLifeLostThisTurn) and let the spell resolve.
     * No  → counter the spell (or counter-to-exile if exileOnCounter).
     */
    fun resumeCounterUnlessPaysLife(
        state: GameState,
        continuation: CounterUnlessPaysLifeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for counter unless pays life")
        }

        if (response.choice) {
            val playerId = continuation.payingPlayerId
            val currentLife = state.getEntity(playerId)
                ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life
                ?: return ExecutionResult.error(state, "Paying player has no life total")

            // If the player can't actually pay (e.g., life dropped between trigger and decision),
            // counter the spell. Players can pay life that takes them below 0.
            // (CR 119.4 — paying life is a cost, not damage; players can pay life they have.)
            // Here we require the player has at least lifeCost life remaining.
            if (currentLife < continuation.lifeCost) {
                val counterResult = if (continuation.exileOnCounter) {
                    services.stackResolver.counterSpellToExile(
                        state, continuation.spellEntityId,
                        grantFreeCast = false,
                        controllerId = continuation.controllerId ?: continuation.payingPlayerId
                    )
                } else {
                    services.stackResolver.counterSpell(state, continuation.spellEntityId)
                }
                return checkForMore(counterResult.newState, counterResult.events)
            }

            val newLife = currentLife - continuation.lifeCost
            var newState = state.updateEntity(playerId) { container ->
                container.with(
                    com.wingedsheep.engine.state.components.identity.LifeTotalComponent(newLife)
                )
            }
            newState = com.wingedsheep.engine.handlers.effects.DamageUtils
                .markLifeLostThisTurn(newState, playerId)

            val events = listOf<GameEvent>(
                LifeChangedEvent(playerId, currentLife, newLife, LifeChangeReason.PAYMENT)
            )
            return checkForMore(newState, events)
        } else {
            val counterResult = if (continuation.exileOnCounter) {
                services.stackResolver.counterSpellToExile(
                    state, continuation.spellEntityId,
                    grantFreeCast = false,
                    controllerId = continuation.controllerId ?: continuation.payingPlayerId
                )
            } else {
                services.stackResolver.counterSpell(state, continuation.spellEntityId)
            }
            return checkForMore(counterResult.newState, counterResult.events)
        }
    }

    /**
     * Resume after the controller decides whether to discard cards for a
     * ward—discard trigger.
     *
     * Yes → run the standard discard pipeline (random or player's choice) against
     *       the caster's hand and let the spell resolve.
     * No  → counter the spell (or counter-to-exile if exileOnCounter).
     *
     * If the hand has fewer than `count` cards by the time we resume, treat that as
     * an inability to pay and counter the spell.
     */
    fun resumeCounterUnlessDiscard(
        state: GameState,
        continuation: CounterUnlessDiscardContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for counter unless discard")
        }

        if (!response.choice) {
            val counterResult = if (continuation.exileOnCounter) {
                services.stackResolver.counterSpellToExile(
                    state, continuation.spellEntityId,
                    grantFreeCast = false,
                    controllerId = continuation.controllerId ?: continuation.payingPlayerId
                )
            } else {
                services.stackResolver.counterSpell(state, continuation.spellEntityId)
            }
            return checkForMore(counterResult.newState, counterResult.events)
        }

        // Yes — re-verify the player can actually discard the required number of cards.
        if (state.getHand(continuation.payingPlayerId).size < continuation.count) {
            val counterResult = if (continuation.exileOnCounter) {
                services.stackResolver.counterSpellToExile(
                    state, continuation.spellEntityId,
                    grantFreeCast = false,
                    controllerId = continuation.controllerId ?: continuation.payingPlayerId
                )
            } else {
                services.stackResolver.counterSpell(state, continuation.spellEntityId)
            }
            return checkForMore(counterResult.newState, counterResult.events)
        }

        val discardEffect = if (continuation.random) {
            com.wingedsheep.sdk.dsl.Patterns.Hand.discardRandom(continuation.count)
        } else {
            com.wingedsheep.sdk.dsl.Patterns.Hand.discardCards(continuation.count)
        }

        val discardContext = com.wingedsheep.engine.handlers.EffectContext(
            sourceId = continuation.controllerId,
            controllerId = continuation.payingPlayerId,
        )

        val discardResult = services.effectExecutorRegistry
            .execute(state, discardEffect, discardContext)
            .toExecutionResult()
        if (discardResult.error != null) return discardResult
        if (discardResult.isPaused) return discardResult

        return checkForMore(discardResult.newState, discardResult.events.toList())
    }

    /**
     * Resume after the controller chooses which permanent(s) to sacrifice for a
     * ward—sacrifice trigger (e.g. Ygra's "Ward—Sacrifice a Food").
     *
     * Selected [count] qualifying permanents → sacrifice them and let the spell resolve.
     * Declined (fewer than [count] selected) → counter the spell.
     *
     * The selection is re-validated against projected state at resume time, so a permanent
     * that lost its qualifying subtype between the prompt and the response no longer counts.
     * Sacrifices emit [PermanentsSacrificedEvent] and run through [ZoneTransitionService] so
     * Food-death triggers (Ygra growing) still fire off the sacrificed permanent.
     */
    fun resumeCounterUnlessSacrifice(
        state: GameState,
        continuation: CounterUnlessSacrificeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for counter unless sacrifice")
        }

        // Re-validate the selection against projected state — only permanents the paying
        // player controls that still match the ward fodder filter count toward payment.
        val valid = BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, continuation.filter.youControl(), PredicateContext(controllerId = continuation.payingPlayerId)
        ).toSet()
        val selectedPermanents = response.selectedCards.filter { it in valid }

        // Declined / underpaid → counter the spell.
        if (selectedPermanents.size < continuation.count) {
            val counterResult = if (continuation.exileOnCounter) {
                services.stackResolver.counterSpellToExile(
                    state, continuation.spellEntityId,
                    grantFreeCast = false,
                    controllerId = continuation.controllerId ?: continuation.payingPlayerId
                )
            } else {
                services.stackResolver.counterSpell(state, continuation.spellEntityId)
            }
            return checkForMore(counterResult.newState, counterResult.events)
        }

        // Paid — sacrifice the chosen permanents and let the spell resolve.
        var newState = state
        val events = mutableListOf<GameEvent>()

        val permanentNames = selectedPermanents.map { id ->
            newState.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
        }
        events.add(PermanentsSacrificedEvent(continuation.payingPlayerId, selectedPermanents, permanentNames))
        newState = ZoneTransitionService.trackFoodSacrifice(newState, selectedPermanents, continuation.payingPlayerId)

        for (permanentId in selectedPermanents) {
            val transitionResult = ZoneTransitionService.moveToZone(newState, permanentId, Zone.GRAVEYARD)
            newState = transitionResult.state
            events.addAll(transitionResult.events)
        }

        return checkForMore(newState, events)
    }

    /**
     * Resume after the controller selects mana sources to pay a "counter unless pays" cost.
     */
    fun resumeCounterUnlessPaysManaSelection(
        state: GameState,
        continuation: CounterUnlessPaysManaSelectionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is ManaSourcesSelectedResponse) {
            return ExecutionResult.error(state, "Expected mana sources selected response")
        }

        // If the player declined (no sources selected and not auto-pay), counter the spell
        if (!response.autoPay && response.selectedSources.isEmpty()) {
            val counterResult = if (continuation.exileOnCounter) {
                services.stackResolver.counterSpellToExile(
                    state, continuation.spellEntityId,
                    grantFreeCast = false,
                    controllerId = continuation.controllerId ?: continuation.payingPlayerId
                )
            } else {
                services.stackResolver.counterSpell(state, continuation.spellEntityId)
            }
            return checkForMore(counterResult.newState, counterResult.events)
        }

        val playerId = continuation.payingPlayerId
        val playerEntity = state.getEntity(playerId)
            ?: return ExecutionResult.error(state, "Paying player not found")

        val manaPoolComponent = playerEntity.get<ManaPoolComponent>()
            ?: return ExecutionResult.error(state, "Player has no mana pool")

        val manaPool = ManaPool(
            manaPoolComponent.white, manaPoolComponent.blue, manaPoolComponent.black,
            manaPoolComponent.red, manaPoolComponent.green, manaPoolComponent.colorless
        )

        val partialResult = manaPool.payPartial(continuation.manaCost)
        val remainingCost = partialResult.remainingCost
        var currentPool = manaPool
        var currentState = state
        val events = mutableListOf<GameEvent>()

        if (!remainingCost.isEmpty()) {
            if (response.autoPay) {
                val manaSolver = ManaSolver(services.cardRegistry)
                val solution = manaSolver.solve(currentState, playerId, remainingCost)
                    ?: return ExecutionResult.error(state, "Cannot pay mana cost with auto-pay")

                for (source in solution.sources) {
                    currentState = currentState.updateEntity(source.entityId) { c ->
                        c.with(TappedComponent)
                    }
                    events.add(TappedEvent(source.entityId, source.name))
                }
                for ((_, production) in solution.manaProduced) {
                    currentPool = if (production.color != null) {
                        currentPool.add(production.color, production.amount)
                    } else {
                        currentPool.addColorless(production.colorless)
                    }
                }
            } else {
                // Split off sources that carry a tap-permanents sub-cost (Springleaf Drum) —
                // those require a follow-up creature-selection prompt before the source can
                // be tapped. All other selections (lands, treasures, etc.) are tapped now.
                val sourceMap = continuation.availableSources.associateBy { it.entityId }
                val (subCostSources, otherSources) = response.selectedSources.partition { id ->
                    sourceMap[id]?.requiresTappingAnotherPermanent == true
                }

                val manual = applyManualSourceSelection(
                    currentState,
                    currentPool,
                    continuation.availableSources,
                    otherSources,
                    fallbackControllerId = playerId
                ) ?: return ExecutionResult.error(state, "Selected source is not a valid mana source")
                currentState = manual.state
                currentPool = manual.pool
                events.addAll(manual.events)

                if (subCostSources.isNotEmpty()) {
                    // Persist the pool from the first-phase taps so the sub-cost
                    // continuation reads it off the player's mana pool component on resume.
                    currentState = currentState.updateEntity(playerId) { container ->
                        container.with(
                            ManaPoolComponent(
                                white = currentPool.white, blue = currentPool.blue, black = currentPool.black,
                                red = currentPool.red, green = currentPool.green, colorless = currentPool.colorless
                            )
                        )
                    }
                    return promptForTapPermanentsSubCost(
                        currentState,
                        events,
                        payingPlayerId = playerId,
                        spellEntityId = continuation.spellEntityId,
                        manaCost = continuation.manaCost,
                        exileOnCounter = continuation.exileOnCounter,
                        controllerId = continuation.controllerId,
                        pendingSubCostSources = subCostSources,
                        availableSources = continuation.availableSources,
                        onPaid = continuation.onPaid,
                        sourceId = continuation.sourceId
                    )
                }
            }
        }

        val newPool = currentPool.pay(continuation.manaCost)
        if (newPool == null) {
            // Payment failed — counter the spell
            val counterResult = if (continuation.exileOnCounter) {
                services.stackResolver.counterSpellToExile(
                    state, continuation.spellEntityId,
                    grantFreeCast = false,
                    controllerId = continuation.controllerId ?: continuation.payingPlayerId
                )
            } else {
                services.stackResolver.counterSpell(state, continuation.spellEntityId)
            }
            return checkForMore(counterResult.newState, counterResult.events)
        }

        currentState = currentState.updateEntity(playerId) { container ->
            container.with(
                ManaPoolComponent(
                    white = newPool.white, blue = newPool.blue, black = newPool.black,
                    red = newPool.red, green = newPool.green, colorless = newPool.colorless
                )
            )
        }

        // Spell resolves normally — don't counter it.
        return runOnPaidThenCheckForMore(
            currentState,
            events,
            continuation.onPaid,
            continuation.controllerId ?: continuation.payingPlayerId,
            continuation.sourceId,
            checkForMore
        )
    }

    /**
     * Run the optional "If they do, …" rider that fires only when the spell's controller
     * paid the counter-unless cost, then continue the pipeline.
     *
     * The rider executes with [riderController] as `controllerId` (the controller of the
     * counter effect, i.e. "you" in "you create a Lander token"). If the rider pauses
     * for a sub-decision we surface that
     * pause directly; on error we also propagate it. Otherwise events from the prior
     * payment phase are concatenated with the rider's events.
     */
    private fun runOnPaidThenCheckForMore(
        state: GameState,
        priorEvents: List<GameEvent>,
        onPaid: com.wingedsheep.sdk.scripting.effects.Effect?,
        riderController: EntityId,
        sourceId: EntityId?,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (onPaid == null) return checkForMore(state, priorEvents)

        val riderContext = com.wingedsheep.engine.handlers.EffectContext(
            sourceId = sourceId,
            controllerId = riderController,
        )
        val riderResult = services.effectExecutorRegistry
            .execute(state, onPaid, riderContext)
            .toExecutionResult()
        if (riderResult.error != null) return riderResult
        if (riderResult.isPaused) {
            return ExecutionResult.paused(
                riderResult.state,
                riderResult.pendingDecision!!,
                priorEvents + riderResult.events
            )
        }
        return checkForMore(riderResult.state, priorEvents + riderResult.events)
    }

    /**
     * Resume after the controller chooses a new target for a spell/ability.
     * Handles both creature (permanent) and player targets.
     */
    fun resumeChangeSpellTarget(
        state: GameState,
        continuation: ChangeSpellTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for change spell target")
        }

        val selectedEntityId = response.selectedCards.firstOrNull()
            ?: return ExecutionResult.error(state, "No target selected for change spell target")

        // Get the spell entity and update its target
        val spellEntity = state.getEntity(continuation.spellEntityId)
            ?: return checkForMore(state, emptyList()) // Spell no longer on stack

        val targetsComponent = spellEntity.get<TargetsComponent>()
            ?: return checkForMore(state, emptyList())

        // Determine the appropriate ChosenTarget type based on the selected entity
        val newTarget = if (state.turnOrder.contains(selectedEntityId)) {
            ChosenTarget.Player(selectedEntityId)
        } else {
            ChosenTarget.Permanent(selectedEntityId)
        }

        val newTargets = listOf(newTarget)
        val updatedState = state.updateEntity(continuation.spellEntityId) { container ->
            container.with(TargetsComponent(newTargets, targetsComponent.targetRequirements))
        }

        return checkForMore(updatedState, emptyList())
    }

    /**
     * Resume after the controller decides whether to pay a mana cost for "you may pay {cost}" effects.
     */
    fun resumeMayPayMana(
        state: GameState,
        continuation: MayPayManaContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may pay mana")
        }

        if (!response.choice) {
            // Player declined to pay — nothing happens
            return checkForMore(state, emptyList())
        }

        // Player chose to pay — check if floating mana covers the cost
        val playerId = continuation.playerId
        val playerEntity = state.getEntity(playerId)
            ?: return ExecutionResult.error(state, "Paying player not found")

        val manaPoolComponent = playerEntity.get<ManaPoolComponent>()
            ?: return ExecutionResult.error(state, "Player has no mana pool")

        val manaPool = ManaPool(
            manaPoolComponent.white, manaPoolComponent.blue, manaPoolComponent.black,
            manaPoolComponent.red, manaPoolComponent.green, manaPoolComponent.colorless
        )
        val partialResult = manaPool.payPartial(continuation.manaCost)

        if (partialResult.remainingCost.isEmpty()) {
            // Floating mana covers the cost — pay immediately and execute inner effect
            val newPool = manaPool.pay(continuation.manaCost)
                ?: return ExecutionResult.error(state, "Cannot pay mana cost from floating mana")
            var currentState = state.updateEntity(playerId) { container ->
                container.with(
                    ManaPoolComponent(
                        white = newPool.white, blue = newPool.blue, black = newPool.black,
                        red = newPool.red, green = newPool.green, colorless = newPool.colorless
                    )
                )
            }

            val effectResult = services.effectExecutorRegistry.execute(currentState, continuation.effect, continuation.effectContext).toExecutionResult()
            if (effectResult.error != null) return effectResult
            if (effectResult.isPaused) return effectResult
            return checkForMore(effectResult.state, effectResult.events)
        }

        // Need to tap sources — show mana source selection UI
        val manaSolver = ManaSolver(services.cardRegistry)
        val sources = manaSolver.findAvailableManaSources(state, playerId)
        val sourceOptions = sources.map { source ->
            ManaSourceOption(
                entityId = source.entityId,
                name = source.name,
                producesColors = source.producesColors,
                producesColorless = source.producesColorless,
                requiresSacrifice = source.requiresSacrifice,
                requiresTappingAnotherPermanent = source.tapPermanentsSubCost != null
            )
        }

        val solution = manaSolver.solve(state, playerId, partialResult.remainingCost)
        val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectManaSourcesDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Pay ${continuation.manaCost}",
            context = DecisionContext(
                sourceId = continuation.effectContext.sourceId,
                sourceName = continuation.sourceName ?: "You may pay",
                phase = DecisionPhase.RESOLUTION
            ),
            availableSources = sourceOptions,
            requiredCost = continuation.manaCost.toString(),
            autoPaySuggestion = autoPaySuggestion,
            canDecline = true
        )

        val manaSelectionContinuation = MayPayManaSelectionContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceName = continuation.sourceName,
            manaCost = continuation.manaCost,
            effect = continuation.effect,
            effectContext = continuation.effectContext,
            availableSources = sourceOptions,
            autoPaySuggestion = autoPaySuggestion
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(manaSelectionContinuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "SELECT_MANA_SOURCES",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after the controller selects mana sources for a "you may pay" ability
     * (non-targeted). Taps sources, deducts mana, executes the inner effect.
     */
    fun resumeMayPayManaSelection(
        state: GameState,
        continuation: MayPayManaSelectionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is ManaSourcesSelectedResponse) {
            return ExecutionResult.error(state, "Expected mana sources selected response")
        }

        // Declined — do nothing
        if (!response.autoPay && response.selectedSources.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        val playerId = continuation.playerId
        val playerEntity = state.getEntity(playerId)
            ?: return ExecutionResult.error(state, "Paying player not found")

        val manaPoolComponent = playerEntity.get<ManaPoolComponent>()
            ?: return ExecutionResult.error(state, "Player has no mana pool")

        val manaPool = ManaPool(
            manaPoolComponent.white, manaPoolComponent.blue, manaPoolComponent.black,
            manaPoolComponent.red, manaPoolComponent.green, manaPoolComponent.colorless
        )

        val partialResult = manaPool.payPartial(continuation.manaCost)
        val remainingCost = partialResult.remainingCost
        var currentPool = manaPool
        var currentState = state
        val events = mutableListOf<GameEvent>()

        if (!remainingCost.isEmpty()) {
            if (response.autoPay) {
                val manaSolver = ManaSolver(services.cardRegistry)
                val solution = manaSolver.solve(currentState, playerId, remainingCost)
                    ?: return ExecutionResult.error(state, "Cannot pay mana cost with auto-pay")

                for (source in solution.sources) {
                    currentState = currentState.updateEntity(source.entityId) { c ->
                        c.with(TappedComponent)
                    }
                    events.add(TappedEvent(source.entityId, source.name))
                }
                for ((_, production) in solution.manaProduced) {
                    currentPool = if (production.color != null) {
                        currentPool.add(production.color, production.amount)
                    } else {
                        currentPool.addColorless(production.colorless)
                    }
                }
            } else {
                val manual = applyManualSourceSelection(
                    currentState,
                    currentPool,
                    continuation.availableSources,
                    response.selectedSources,
                    fallbackControllerId = playerId
                ) ?: return ExecutionResult.error(state, "Selected source is not a valid mana source")
                currentState = manual.state
                currentPool = manual.pool
                events.addAll(manual.events)
            }
        }

        val newPool = currentPool.pay(continuation.manaCost)
            ?: return ExecutionResult.error(state, "Cannot pay mana cost after tapping sources")

        currentState = currentState.updateEntity(playerId) { container ->
            container.with(
                ManaPoolComponent(
                    white = newPool.white, blue = newPool.blue, black = newPool.black,
                    red = newPool.red, green = newPool.green, colorless = newPool.colorless
                )
            )
        }

        // Execute the inner effect
        val effectResult = services.effectExecutorRegistry.execute(currentState, continuation.effect, continuation.effectContext).toExecutionResult()
        if (effectResult.error != null) return effectResult
        if (effectResult.isPaused) return effectResult

        val allEvents = events + effectResult.events
        return checkForMore(effectResult.state, allEvents)
    }

    /**
     * Resume after the controller decides whether to pay a mana cost for a triggered
     * ability that also requires targets (e.g., Lightning Rift).
     *
     * If the player says yes, shows mana source selection. If no, skips the trigger.
     */
    fun resumeMayPayManaTrigger(
        state: GameState,
        continuation: MayPayManaTriggerContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may pay mana trigger")
        }

        if (!response.choice) {
            // Player declined to pay — skip the trigger entirely
            return checkForMore(state, emptyList())
        }

        // Player chose to pay — show mana source selection
        val playerId = continuation.trigger.controllerId
        val manaSolver = ManaSolver(services.cardRegistry)

        // Find available sources for the UI
        val sources = manaSolver.findAvailableManaSources(state, playerId)
        val sourceOptions = sources.map { source ->
            ManaSourceOption(
                entityId = source.entityId,
                name = source.name,
                producesColors = source.producesColors,
                producesColorless = source.producesColorless,
                requiresSacrifice = source.requiresSacrifice,
                requiresTappingAnotherPermanent = source.tapPermanentsSubCost != null
            )
        }

        // Get auto-pay suggestion
        val solution = manaSolver.solve(state, playerId, continuation.manaCost)
        val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()

        // Create mana source selection decision
        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectManaSourcesDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Pay ${continuation.manaCost}",
            context = DecisionContext(
                sourceId = continuation.trigger.sourceId,
                sourceName = continuation.trigger.sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            availableSources = sourceOptions,
            requiredCost = continuation.manaCost.toString(),
            autoPaySuggestion = autoPaySuggestion
        )

        val manaSourceContinuation = ManaSourceSelectionContinuation(
            decisionId = decisionId,
            trigger = continuation.trigger,
            targetRequirement = continuation.targetRequirement,
            manaCost = continuation.manaCost,
            availableSources = sourceOptions,
            autoPaySuggestion = autoPaySuggestion
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(manaSourceContinuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "SELECT_MANA_SOURCES",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after the controller chooses an X value for "you may pay {X}" effects.
     * If X > 0, auto-tap X mana and execute the inner effect with the chosen X.
     */
    fun resumeMayPayX(
        state: GameState,
        continuation: MayPayXContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is NumberChosenResponse) {
            return ExecutionResult.error(state, "Expected number chosen response for may pay X")
        }

        val chosenX = response.number
        if (chosenX <= 0) {
            // Player declined to pay — nothing happens
            return checkForMore(state, emptyList())
        }

        // Player chose to pay X mana — auto-tap sources
        val playerId = continuation.playerId
        val playerEntity = state.getEntity(playerId)
            ?: return ExecutionResult.error(state, "Paying player not found")

        val manaPoolComponent = playerEntity.get<ManaPoolComponent>()
            ?: return ExecutionResult.error(state, "Player has no mana pool")

        val manaPool = ManaPool(
            manaPoolComponent.white,
            manaPoolComponent.blue,
            manaPoolComponent.black,
            manaPoolComponent.red,
            manaPoolComponent.green,
            manaPoolComponent.colorless
        )

        // Create a ManaCost of {X} generic mana
        val xCost = com.wingedsheep.sdk.core.ManaCost(
            List(chosenX) { com.wingedsheep.sdk.core.ManaSymbol.generic(1) }
        )

        // Try to pay from floating mana first, then tap sources for the rest
        val partialResult = manaPool.payPartial(xCost)
        val remainingCost = partialResult.remainingCost
        var currentPool = manaPool
        var currentState = state
        val events = mutableListOf<GameEvent>()

        if (!remainingCost.isEmpty()) {
            val manaSolver = ManaSolver(services.cardRegistry)
            val solution = manaSolver.solve(currentState, playerId, remainingCost)
                ?: return ExecutionResult.error(state, "Cannot pay mana cost")

            for (source in solution.sources) {
                currentState = currentState.updateEntity(source.entityId) { c ->
                    c.with(TappedComponent)
                }
                events.add(TappedEvent(source.entityId, source.name))
            }

            for ((_, production) in solution.manaProduced) {
                currentPool = if (production.color != null) {
                    currentPool.add(production.color)
                } else {
                    currentPool.addColorless(production.colorless)
                }
            }
        }

        // Deduct the cost from the pool
        val newPool = currentPool.pay(xCost)
            ?: return ExecutionResult.error(state, "Cannot pay mana cost after auto-tap")

        currentState = currentState.updateEntity(playerId) { container ->
            container.with(
                ManaPoolComponent(
                    white = newPool.white,
                    blue = newPool.blue,
                    black = newPool.black,
                    red = newPool.red,
                    green = newPool.green,
                    colorless = newPool.colorless
                )
            )
        }

        // Execute the inner effect with the chosen X value
        val context = continuation.effectContext.copy(xValue = chosenX)
        val effectResult = services.effectExecutorRegistry.execute(currentState, continuation.effect, context).toExecutionResult()

        if (effectResult.error != null) {
            return effectResult
        }
        if (effectResult.isPaused) return effectResult

        val allEvents = events + effectResult.events
        return checkForMore(effectResult.state, allEvents)
    }

    /**
     * Resume after the controller selects mana sources to pay a cost for a triggered
     * ability that also requires targets.
     *
     * Taps the selected sources, deducts mana, unwraps MayPayManaEffect, and proceeds
     * to target selection with the inner effect.
     */
    fun resumeManaSourceSelection(
        state: GameState,
        continuation: ManaSourceSelectionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is ManaSourcesSelectedResponse) {
            return ExecutionResult.error(state, "Expected mana sources selected response")
        }

        val playerId = continuation.trigger.controllerId
        val playerEntity = state.getEntity(playerId)
            ?: return ExecutionResult.error(state, "Paying player not found")

        val manaPoolComponent = playerEntity.get<ManaPoolComponent>()
            ?: return ExecutionResult.error(state, "Player has no mana pool")

        val manaPool = ManaPool(
            manaPoolComponent.white,
            manaPoolComponent.blue,
            manaPoolComponent.black,
            manaPoolComponent.red,
            manaPoolComponent.green,
            manaPoolComponent.colorless
        )

        // Try to pay from floating mana first
        val partialResult = manaPool.payPartial(continuation.manaCost)
        val remainingCost = partialResult.remainingCost
        var currentPool = manaPool
        var currentState = state
        val events = mutableListOf<GameEvent>()

        if (!remainingCost.isEmpty()) {
            if (response.autoPay) {
                // Auto-tap: use ManaSolver
                val manaSolver = ManaSolver(services.cardRegistry)
                val solution = manaSolver.solve(currentState, playerId, remainingCost)
                    ?: return ExecutionResult.error(state, "Cannot pay mana cost with auto-pay")

                for (source in solution.sources) {
                    currentState = currentState.updateEntity(source.entityId) { c ->
                        c.with(TappedComponent)
                    }
                    events.add(TappedEvent(source.entityId, source.name))
                }

                for ((_, production) in solution.manaProduced) {
                    currentPool = if (production.color != null) {
                        currentPool.add(production.color, production.amount)
                    } else {
                        currentPool.addColorless(production.colorless)
                    }
                }
            } else {
                val manual = applyManualSourceSelection(
                    currentState,
                    currentPool,
                    continuation.availableSources,
                    response.selectedSources,
                    fallbackControllerId = playerId
                ) ?: return ExecutionResult.error(state, "Selected source is not a valid mana source")
                currentState = manual.state
                currentPool = manual.pool
                events.addAll(manual.events)
            }
        }

        // Deduct the cost from the pool
        val newPool = currentPool.pay(continuation.manaCost)
            ?: return ExecutionResult.error(state, "Cannot pay mana cost after tapping sources")

        currentState = currentState.updateEntity(playerId) { container ->
            container.with(
                ManaPoolComponent(
                    white = newPool.white,
                    blue = newPool.blue,
                    black = newPool.black,
                    red = newPool.red,
                    green = newPool.green,
                    colorless = newPool.colorless
                )
            )
        }

        // Unwrap the optional-mana-payment gate to get its inner effect (mana already paid).
        val trigger = continuation.trigger
        val innerEffect = trigger.ability.effect.asOptionalManaPayment()!!.then

        // Create a modified trigger with the inner effect (mana already paid)
        val unwrappedAbility = trigger.ability.copy(effect = innerEffect)
        val unwrappedTrigger = trigger.copy(ability = unwrappedAbility)

        // Proceed to target selection
        val result = services.triggerProcessor.processTargetedTrigger(currentState, unwrappedTrigger, continuation.targetRequirement)

        if (result.isPaused) {
            // Target selection is needed - return paused with accumulated events
            return ExecutionResult.paused(
                result.state,
                result.pendingDecision!!,
                events + result.events
            )
        }

        if (!result.isSuccess) {
            return result
        }

        // Target was auto-selected - check for more continuations
        return checkForMore(result.newState, events + result.events.toList())
    }

    /**
     * Applies a player's manual mana-source selection: taps each source (sacrificing
     * those with [ManaSourceOption.requiresSacrifice] — e.g. Treasure tokens) and
     * adds the produced mana to [pool]. For tap+sacrifice sources we route the zone
     * move through [ZoneTransitionService] so dies/leaves triggers and Food trackers
     * stay consistent with [com.wingedsheep.engine.handlers.CostHandler]'s payment of
     * the same cost.
     *
     * For "any color" producers (treasures' `AddAnyColorMana`) the resumer picks the
     * first listed color; for paying generic costs (ward {N}, may-pay {N}) any color
     * suffices.
     *
     * @return null when [selectedSourceIds] references a source not in [availableSources].
     */
    private data class ManualSourceTapResult(
        val state: GameState,
        val pool: ManaPool,
        val events: List<GameEvent>
    )

    private fun applyManualSourceSelection(
        state: GameState,
        pool: ManaPool,
        availableSources: List<ManaSourceOption>,
        selectedSourceIds: List<EntityId>,
        fallbackControllerId: EntityId
    ): ManualSourceTapResult? {
        val sourceMap = availableSources.associateBy { it.entityId }
        var currentState = state
        var currentPool = pool
        val events = mutableListOf<GameEvent>()

        for (sourceId in selectedSourceIds) {
            val source = sourceMap[sourceId] ?: return null

            if (source.requiresSacrifice) {
                val sourceController = currentState.getEntity(sourceId)
                    ?.get<ControllerComponent>()?.playerId
                    ?: fallbackControllerId
                // Emit a TappedEvent for parity with the tap sub-cost on the ability
                // (some triggers care about "becomes tapped"), then sacrifice. The
                // permanent is about to leave the battlefield so we skip setting
                // TappedComponent on it.
                events.add(TappedEvent(sourceId, source.name))
                val preState = ZoneTransitionService
                    .trackFoodSacrifice(currentState, listOf(sourceId), sourceController)
                val transition = ZoneTransitionService.moveToZone(
                    preState, sourceId, Zone.GRAVEYARD
                )
                currentState = transition.state
                events.add(PermanentsSacrificedEvent(sourceController, listOf(sourceId)))
                events.addAll(transition.events)
            } else {
                currentState = currentState.updateEntity(sourceId) { c -> c.with(TappedComponent) }
                events.add(TappedEvent(sourceId, source.name))
            }

            if (source.producesColors.isNotEmpty()) {
                currentPool = currentPool.add(source.producesColors.first())
            } else if (source.producesColorless) {
                currentPool = currentPool.addColorless(1)
            }
        }

        return ManualSourceTapResult(currentState, currentPool, events)
    }

    /**
     * Resolves the [com.wingedsheep.engine.mechanics.mana.TapPermanentsSubCost] for [sourceId]
     * by re-querying the ManaSolver. Returns null when [sourceId] is no longer a sub-cost
     * source — this can happen if the source left the battlefield between menu render and
     * resume, in which case the resumer must counter the spell.
     */
    private fun lookupSubCost(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId
    ): com.wingedsheep.engine.mechanics.mana.TapPermanentsSubCost? {
        val manaSolver = ManaSolver(services.cardRegistry)
        return manaSolver.findAvailableManaSources(state, playerId)
            .firstOrNull { it.entityId == sourceId }
            ?.tapPermanentsSubCost
    }

    /**
     * Builds a [SelectCardsDecision] for the head of [pendingSubCostSources] and pauses
     * with a [WardTapPermanentsSubCostContinuation] queued for the response. The decision
     * lists every untapped permanent the [payingPlayerId] controls that matches the
     * sub-cost filter (and is not the source itself).
     */
    private fun promptForTapPermanentsSubCost(
        state: GameState,
        priorEvents: List<GameEvent>,
        payingPlayerId: EntityId,
        spellEntityId: EntityId,
        manaCost: com.wingedsheep.sdk.core.ManaCost,
        exileOnCounter: Boolean,
        controllerId: EntityId?,
        pendingSubCostSources: List<EntityId>,
        availableSources: List<ManaSourceOption>,
        onPaid: com.wingedsheep.sdk.scripting.effects.Effect? = null,
        sourceId: EntityId? = null
    ): ExecutionResult {
        val headSourceId = pendingSubCostSources.first()
        val sourceName = availableSources.firstOrNull { it.entityId == headSourceId }?.name
            ?: state.getEntity(headSourceId)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name
            ?: "Mana source"

        val subCost = lookupSubCost(state, payingPlayerId, headSourceId)
            ?: return ExecutionResult.error(state, "Selected mana source is no longer available")

        val projected = state.projectedState
        val predicateEvaluator = PredicateEvaluator()
        val predicateContext = PredicateContext(controllerId = payingPlayerId)
        val options = projected.getBattlefieldControlledBy(payingPlayerId)
            .filter { candidate ->
                candidate != headSourceId &&
                    state.getEntity(candidate)?.has<TappedComponent>() == false &&
                    predicateEvaluator.matches(state, projected, candidate, subCost.filter, predicateContext)
            }

        if (options.size < subCost.count) {
            return ExecutionResult.error(state, "Not enough valid permanents to satisfy $sourceName's tap cost")
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = payingPlayerId,
            prompt = "Tap an untapped ${subCost.filter.description} you control for $sourceName",
            context = DecisionContext(
                sourceId = headSourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = options,
            minSelections = subCost.count,
            maxSelections = subCost.count,
            useTargetingUI = true
        )

        val continuation = WardTapPermanentsSubCostContinuation(
            decisionId = decisionId,
            payingPlayerId = payingPlayerId,
            spellEntityId = spellEntityId,
            manaCost = manaCost,
            exileOnCounter = exileOnCounter,
            controllerId = controllerId,
            pendingSubCostSources = pendingSubCostSources,
            availableSources = availableSources,
            onPaid = onPaid,
            sourceId = sourceId
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            priorEvents + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = payingPlayerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after the player picks which permanent to tap for the secondary tap-permanents
     * sub-cost of the head [WardTapPermanentsSubCostContinuation.pendingSubCostSources].
     * Taps the source + the chosen permanent, adds the source's mana to the player's
     * pool, then either prompts for the next sub-cost or attempts to pay the ward cost.
     */
    fun resumeWardTapPermanentsSubCost(
        state: GameState,
        continuation: WardTapPermanentsSubCostContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for tap-permanents sub-cost")
        }

        val headSourceId = continuation.pendingSubCostSources.first()
        val sourceOption = continuation.availableSources.firstOrNull { it.entityId == headSourceId }
            ?: return ExecutionResult.error(state, "Mana source not found in continuation availableSources")

        val subCost = lookupSubCost(state, continuation.payingPlayerId, headSourceId)
            ?: return ExecutionResult.error(state, "Selected mana source is no longer available")

        if (response.selectedCards.size != subCost.count) {
            return ExecutionResult.error(state, "Expected ${subCost.count} target(s) for ${sourceOption.name}'s tap cost")
        }

        // Validate each chosen permanent still matches the filter and is untapped.
        val projected = state.projectedState
        val predicateEvaluator = PredicateEvaluator()
        val predicateContext = PredicateContext(controllerId = continuation.payingPlayerId)
        for (chosen in response.selectedCards) {
            if (chosen == headSourceId) {
                return ExecutionResult.error(state, "Cannot use the source itself to pay its own tap-permanents sub-cost")
            }
            val container = state.getEntity(chosen)
                ?: return ExecutionResult.error(state, "Chosen permanent not found")
            if (container.has<TappedComponent>()) {
                return ExecutionResult.error(state, "Chosen permanent is already tapped")
            }
            if (!predicateEvaluator.matches(state, projected, chosen, subCost.filter, predicateContext)) {
                return ExecutionResult.error(state, "Chosen permanent does not match ${sourceOption.name}'s tap requirement")
            }
        }

        // Tap the source and each chosen permanent, then credit the source's mana to the pool.
        var currentState = state
        val events = mutableListOf<GameEvent>()
        currentState = currentState.updateEntity(headSourceId) { c -> c.with(TappedComponent) }
        events.add(TappedEvent(headSourceId, sourceOption.name))
        for (chosen in response.selectedCards) {
            val chosenName = currentState.getEntity(chosen)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name
                ?: "Permanent"
            currentState = currentState.updateEntity(chosen) { c -> c.with(TappedComponent) }
            events.add(TappedEvent(chosen, chosenName))
        }

        // Read current pool, add the source's mana, persist.
        val playerEntity = currentState.getEntity(continuation.payingPlayerId)
            ?: return ExecutionResult.error(state, "Paying player not found")
        val poolComponent = playerEntity.get<ManaPoolComponent>()
            ?: return ExecutionResult.error(state, "Player has no mana pool")
        var pool = ManaPool(
            poolComponent.white, poolComponent.blue, poolComponent.black,
            poolComponent.red, poolComponent.green, poolComponent.colorless
        )
        pool = if (sourceOption.producesColors.isNotEmpty()) {
            pool.add(sourceOption.producesColors.first())
        } else if (sourceOption.producesColorless) {
            pool.addColorless(1)
        } else {
            pool
        }
        currentState = currentState.updateEntity(continuation.payingPlayerId) { container ->
            container.with(
                ManaPoolComponent(
                    white = pool.white, blue = pool.blue, black = pool.black,
                    red = pool.red, green = pool.green, colorless = pool.colorless
                )
            )
        }

        // If more sub-cost sources remain, prompt the next one.
        val remaining = continuation.pendingSubCostSources.drop(1)
        if (remaining.isNotEmpty()) {
            return promptForTapPermanentsSubCost(
                currentState,
                events,
                payingPlayerId = continuation.payingPlayerId,
                spellEntityId = continuation.spellEntityId,
                manaCost = continuation.manaCost,
                exileOnCounter = continuation.exileOnCounter,
                controllerId = continuation.controllerId,
                pendingSubCostSources = remaining,
                availableSources = continuation.availableSources,
                onPaid = continuation.onPaid,
                sourceId = continuation.sourceId
            )
        }

        // No more sub-costs — attempt to pay the ward cost. On failure, counter the spell.
        val newPool = pool.pay(continuation.manaCost)
        if (newPool == null) {
            val counterResult = if (continuation.exileOnCounter) {
                services.stackResolver.counterSpellToExile(
                    currentState,
                    continuation.spellEntityId,
                    grantFreeCast = false,
                    controllerId = continuation.controllerId ?: continuation.payingPlayerId
                )
            } else {
                services.stackResolver.counterSpell(currentState, continuation.spellEntityId)
            }
            return checkForMore(counterResult.newState, events + counterResult.events)
        }
        currentState = currentState.updateEntity(continuation.payingPlayerId) { container ->
            container.with(
                ManaPoolComponent(
                    white = newPool.white, blue = newPool.blue, black = newPool.black,
                    red = newPool.red, green = newPool.green, colorless = newPool.colorless
                )
            )
        }
        // Payment fully resolved through the sub-cost source — fire the "If they do, …"
        // rider (no-op when null), with the counter's controller as "you".
        return runOnPaidThenCheckForMore(
            currentState,
            events,
            continuation.onPaid,
            continuation.controllerId ?: continuation.payingPlayerId,
            continuation.sourceId,
            checkForMore
        )
    }
}
