package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.life.LifePaymentService
import com.wingedsheep.engine.handlers.effects.zones.ForceExileMultiZoneExecutor
import com.wingedsheep.engine.handlers.effects.zones.ForceSacrificeExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.library.MillAmountModifier
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect

class SacrificeAndPayContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(SacrificeContinuation::class, ::resumeSacrifice),
        resumer(ExileMultiZoneContinuation::class, ::resumeExileMultiZone),
        resumer(PayOrSufferContinuation::class, ::resumePayOrSuffer),
        resumer(PayOrSufferManaSelectionContinuation::class, ::resumePayOrSufferManaSelection),
        resumer(PayOrSufferChoiceContinuation::class, ::resumePayOrSufferChoice),
        resumer(AnyPlayerMayPayContinuation::class, ::resumeAnyPlayerMayPay),
        resumer(UntapChoiceContinuation::class, ::resumeUntapChoice)
    )

    fun resumeSacrifice(
        state: GameState,
        continuation: SacrificeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for sacrifice")
        }

        val playerId = continuation.playerId
        val selectedPermanents = response.selectedCards

        // Move selected permanents from battlefield to owner's graveyard
        var newState = state
        val events = mutableListOf<GameEvent>()

        // Capture characteristics BEFORE the zone change so sibling effects can read the
        // sacrificed permanents' supertypes / subtypes / controllers / token-ness (Rise of the
        // Witch-king "if you sacrificed a creature this way…" rider; Exploit's `EmitExploitedEventEffect`
        // reading `wasToken` for Skull Skaab's "exploits a nontoken creature"). The GameState overload
        // records token-ness ([TokenComponent], not a projected value) too.
        val snapshots = if (selectedPermanents.isNotEmpty()) {
            com.wingedsheep.engine.state.components.stack.captureEntitySnapshots(
                selectedPermanents, newState
            )
        } else {
            emptyList()
        }

        if (selectedPermanents.isNotEmpty()) {
            val permanentNames = selectedPermanents.map { id ->
                newState.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
            }
            events.add(PermanentsSacrificedEvent(playerId, selectedPermanents, permanentNames))
            newState = ZoneTransitionService.trackPermanentSacrifice(newState, selectedPermanents, playerId)
        }

        for (permanentId in selectedPermanents) {
            val transitionResult = ZoneTransitionService.moveToZone(
                newState, permanentId, Zone.GRAVEYARD
            )
            newState = transitionResult.state
            events.addAll(transitionResult.events)
        }

        newState = withSacrificeSnapshots(newState, snapshots)

        // If there are remaining players (from "each opponent" effects), process them
        if (continuation.remainingPlayers.isNotEmpty() && continuation.filter != null) {
            val executor = ForceSacrificeExecutor()
            val result = executor.processPlayers(
                newState, continuation.remainingPlayers, continuation.filter,
                continuation.count, continuation.sourceId
            )
            val resultStateWithSnaps =
                withSacrificeSnapshots(result.state, result.updatedSacrificedPermanents)
            val allEvents = events + result.events
            return if (result.isPaused) {
                // Another player needs a decision — return paused with combined events
                ExecutionResult.paused(resultStateWithSnaps, result.pendingDecision!!, allEvents)
            } else {
                checkForMore(resultStateWithSnaps, allEvents)
            }
        }

        return checkForMore(newState, events)
    }

    /**
     * Publish [snapshots] of just-sacrificed permanents onto the enclosing [EffectContinuation]s so
     * a sibling effect resolving after this paused sacrifice can read
     * `context.sacrificedPermanents` (Rise of the Witch-king's "if you sacrificed a creature this
     * way…" rider). Mirrors `CreatureTypeChoiceContinuationResumer`'s "walk the stack and patch the
     * captured effectContext" pattern.
     */
    private fun withSacrificeSnapshots(
        state: GameState,
        snapshots: List<com.wingedsheep.engine.state.components.stack.EntitySnapshot>
    ): GameState {
        if (snapshots.isEmpty()) return state
        return state.copy(continuationStack = state.continuationStack.map { frame ->
            if (frame is EffectContinuation) {
                frame.copy(
                    effectContext = frame.effectContext.copy(
                        sacrificedPermanents = frame.effectContext.sacrificedPermanents + snapshots
                    )
                )
            } else {
                frame
            }
        })
    }

    fun resumeExileMultiZone(
        state: GameState,
        continuation: ExileMultiZoneContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for multi-zone exile")
        }

        val result = ForceExileMultiZoneExecutor.exileEntities(
            state, continuation.playerId, response.selectedCards
        )

        return checkForMore(result.state, result.events.toList())
    }

    fun resumePayOrSuffer(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val playerId = continuation.playerId
        val sourceId = continuation.sourceId
        val sourceName = continuation.sourceName

        return when (continuation.costType) {
            PayOrSufferCostType.DISCARD -> {
                if (continuation.random) {
                    resumePayOrSufferRandomDiscard(state, continuation, response, checkForMore)
                } else {
                    resumePayOrSufferDiscard(state, continuation, response, checkForMore)
                }
            }
            PayOrSufferCostType.SACRIFICE -> resumePayOrSufferSacrifice(state, continuation, response, checkForMore)
            PayOrSufferCostType.PAY_LIFE -> resumePayOrSufferPayLife(state, continuation, response, checkForMore)
            PayOrSufferCostType.MILL -> resumePayOrSufferMill(state, continuation, response, checkForMore)
            PayOrSufferCostType.MANA -> resumePayOrSufferMana(state, continuation, response, checkForMore)
            PayOrSufferCostType.EXILE -> resumePayOrSufferExile(state, continuation, response, checkForMore)
            PayOrSufferCostType.TAP -> resumePayOrSufferTap(state, continuation, response, checkForMore)
            PayOrSufferCostType.PUT_COUNTERS -> resumePayOrSufferPutCounters(state, continuation, response, checkForMore)
            PayOrSufferCostType.REMOVE_COUNTERS, PayOrSufferCostType.CHOICE -> ExecutionResult.error(state, "Choice cost type should be handled by PayOrSufferChoiceContinuation, not PayOrSufferContinuation")
        }
    }

    /**
     * Resume after player picks which cost to pay from a multi-option PayOrSufferEffect.
     */
    fun resumePayOrSufferChoice(
        state: GameState,
        continuation: PayOrSufferChoiceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for pay or suffer choice")
        }

        val chosenIndex = response.optionIndex

        // Last option is always the suffer effect
        if (chosenIndex >= continuation.options.size) {
            // Player chose the suffer option — runs under the ability's controller (see
            // executePayOrSufferConsequence), not the player who declined the costs.
            val context = EffectContext(
                sourceId = continuation.sourceId,
                controllerId = continuation.abilityControllerId ?: continuation.playerId,
                targets = continuation.targets,
                pipeline = PipelineState(
                    namedTargets = continuation.namedTargets,
                    storedCollections = continuation.storedCollections,
                    // Rebind the enclosing ForEach loop's entity: the consequence may refer back to it
                    // (Tidal Flats' "creatures you control blocking *that creature*"), and a null here
                    // matches nothing at all rather than failing loudly.
                    iterationTarget = continuation.iterationEntityId
                ),
                triggeringEntityId = continuation.triggeringEntityId,
                triggeringPlayerId = continuation.triggeringPlayerId
            )
            val result = services.effectExecutorRegistry.execute(state, continuation.sufferEffect, context).toExecutionResult()
            return if (result.isPaused) result else checkForMore(result.state, result.events.toList())
        }

        // Player chose a cost option — create a single-cost PayOrSufferEffect and execute it
        val chosenCost = continuation.options[chosenIndex]
        val singleCostEffect = PayOrSufferEffect(
            cost = chosenCost,
            suffer = continuation.sufferEffect,
            consequenceDescription = continuation.consequenceDescription
        )
        val context = EffectContext(
            sourceId = continuation.sourceId,
            controllerId = continuation.playerId,
            targets = continuation.targets,
            pipeline = PipelineState(
                namedTargets = continuation.namedTargets,
                storedCollections = continuation.storedCollections,
                // Rebind the enclosing ForEach loop's entity: the consequence may refer back to it
                // (Tidal Flats' "creatures you control blocking *that creature*"), and a null here
                // matches nothing at all rather than failing loudly.
                iterationTarget = continuation.iterationEntityId
            ),
            triggeringEntityId = continuation.triggeringEntityId,
            triggeringPlayerId = continuation.triggeringPlayerId
        )
        val result = services.effectExecutorRegistry.execute(state, singleCostEffect, context).toExecutionResult()
        return if (result.isPaused) result else checkForMore(result.state, result.events.toList())
    }

    /**
     * Handle discard cost selection for pay or suffer.
     */
    private fun resumePayOrSufferDiscard(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for pay or suffer discard")
        }

        val playerId = continuation.playerId
        val sourceId = continuation.sourceId
        val selectedCards = response.selectedCards

        // If player didn't select enough cards, execute the suffer effect
        if (selectedCards.size < continuation.requiredCount) {
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        // Player paid the cost — discard the selected cards through the shared discard path, so a
        // card-intrinsic discard replacement (madness, CR 702.35a) applies here too.
        val result = ZoneTransitionService.discardCards(state, playerId, selectedCards)
        return checkForMore(result.state, result.events)
    }

    /**
     * Handle random discard yes/no choice for pay or suffer.
     */
    private fun resumePayOrSufferRandomDiscard(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for pay or suffer random discard")
        }

        if (!response.choice) {
            // Player declined - execute suffer effect
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        // Player chose to pay - execute random discard
        val result = com.wingedsheep.engine.handlers.effects.player.PayOrSufferExecutor.executeRandomDiscard(
            state,
            continuation.playerId,
            continuation.filter,
            continuation.requiredCount,
            continuation.sourceId
        )
        return checkForMore(result.state, result.events.toList())
    }

    /**
     * Handle sacrifice cost selection for pay or suffer.
     */
    private fun resumePayOrSufferSacrifice(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for pay or suffer sacrifice")
        }

        val playerId = continuation.playerId
        val selectedPermanents = response.selectedCards

        // If player didn't select enough permanents, execute the suffer effect
        if (selectedPermanents.size < continuation.requiredCount) {
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        // Player paid the cost - sacrifice the selected permanents
        var newState = state
        val events = mutableListOf<GameEvent>()

        val permanentNames = selectedPermanents.map { id ->
            newState.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
        }
        events.add(PermanentsSacrificedEvent(playerId, selectedPermanents, permanentNames))
        newState = ZoneTransitionService.trackPermanentSacrifice(newState, selectedPermanents, playerId)

        for (permanentId in selectedPermanents) {
            val transitionResult = ZoneTransitionService.moveToZone(
                newState, permanentId, Zone.GRAVEYARD
            )
            newState = transitionResult.state
            events.addAll(transitionResult.events)
        }

        return checkForMore(newState, events)
    }

    /**
     * Handle tap-permanent selection for pay or suffer.
     * Tapping each selected permanent emits a TappedEvent so "becomes tapped" triggers fire.
     */
    private fun resumePayOrSufferTap(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for pay or suffer tap")
        }

        val selectedPermanents = response.selectedCards

        // If player didn't select enough untapped permanents, execute the suffer effect.
        if (selectedPermanents.size < continuation.requiredCount) {
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        // Player paid the cost - tap each selected permanent.
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (permanentId in selectedPermanents) {
            val (tappedState, tapEvent) = tap(newState, permanentId)
            newState = tappedState
            tapEvent?.let(events::add)
        }

        return checkForMore(newState, events)
    }

    /**
     * Handle the put-counters payment for pay or suffer (Tourach's Chant, Thelon's Chant).
     *
     * Selecting nothing is a decline, exactly as it is for the sacrifice and tap payments.
     */
    private fun resumePayOrSufferPutCounters(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for pay or suffer put counters")
        }

        val selected = response.selectedCards
        if (selected.size < continuation.requiredCount) {
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        val counterName = continuation.counterType
            ?: return ExecutionResult.error(state, "Put-counters payment has no counter type")
        val counterType = com.wingedsheep.engine.handlers.effects.permanent.counters
            .resolveCounterType(counterName)

        // Counters put on to pay a cost are an ordinary counter placement (CR 121.6), so this runs
        // the same four-step chokepoint as CostHandler's PutCountersOnSelf and AddCountersExecutor:
        // the "can't have counters put on it" gate (Solemnity), the placement replacements
        // (Hardened Scales, Doubling Season), the first-placement-this-turn marker, and a
        // CountersAddedEvent that names its placer. Skipping any of them is silent — a placer-less
        // event makes every placer-restricted trigger decline, and nothing fails.
        val placerId = continuation.playerId
        var newState = state
        val events = mutableListOf<GameEvent>()
        for (permanentId in selected) {
            val container = newState.getEntity(permanentId) ?: continue
            if (!newState.projectedState.canReceiveCounters(permanentId)) continue
            val counters = container.get<CountersComponent>() ?: CountersComponent()
            val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                newState, permanentId, counterType, continuation.requiredCounters, placerId = placerId
            )
            val firstThisTurn = DamageUtils.isFirstCounterThisTurn(newState, permanentId)
            newState = newState.updateEntity(permanentId) { c ->
                c.with(counters.withAdded(counterType, modifiedCount))
            }.let {
                DamageUtils.markCounterPlacedOnCreature(
                    it, placerId, permanentId,
                    com.wingedsheep.engine.handlers.effects.permanent.counters
                        .counterTypeToString(counterType)
                )
            }
            events.add(
                CountersAddedEvent(
                    permanentId,
                    counterName,
                    modifiedCount,
                    container.get<CardComponent>()?.name ?: "Permanent",
                    firstThisTurn,
                    placedBy = placerId,
                )
            )
        }

        return checkForMore(newState, events)
    }

    /**
     * Handle pay life yes/no choice for pay or suffer.
     */
    private fun resumePayOrSufferPayLife(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for pay or suffer pay life")
        }

        if (!response.choice) {
            // Player declined - execute suffer effect
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        // Player chose to pay life
        val (newState, events) = LifePaymentService
            .pay(state, continuation.playerId, continuation.requiredCount)
            ?: return ExecutionResult.error(state, "Player has no life total")

        return checkForMore(newState, events)
    }

    /**
     * Resume the [CostAtom.Mill] payment for pay-or-suffer (Deep Spawn).
     *
     * Affordability was settled before the prompt — CR 701.17b forbids paying a mill cost deeper
     * than the library — so a yes here always pays. Mill *replacement* effects apply now, which is
     * why the count goes through [MillAmountModifier] rather than being taken literally.
     */
    private fun resumePayOrSufferMill(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for pay or suffer mill")
        }

        if (!response.choice) {
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        val playerId = continuation.playerId
        val count = MillAmountModifier.apply(state, playerId, continuation.requiredCount)
        val milled = state.getZone(ZoneKey(playerId, Zone.LIBRARY)).take(count)
        val result = ZoneTransitionService.moveToZoneBatch(state, milled, Zone.GRAVEYARD)

        return checkForMore(result.state, result.events)
    }

    /**
     * Handle exile cost selection for pay or suffer.
     */
    private fun resumePayOrSufferExile(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for pay or suffer exile")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // If player didn't select enough cards, execute the suffer effect
        if (selectedCards.size < continuation.requiredCount) {
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        // Player paid the cost - exile the selected cards
        val sourceZone = continuation.zone ?: Zone.HAND
        val fromZone = ZoneKey(playerId, sourceZone)
        val exileZone = ZoneKey(playerId, Zone.EXILE)
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (cardId in selectedCards) {
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.removeFromZone(fromZone, cardId)
            newState = newState.addToZone(exileZone, cardId)
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = sourceZone,
                    toZone = Zone.EXILE,
                    ownerId = playerId
                )
            )
        }

        return checkForMore(newState, events)
    }

    /**
     * Raises the mana-source window for a "pay or suffer" cost the payer just agreed to.
     */
    private fun openManaSourceWindow(
        state: GameState,
        continuation: PayOrSufferContinuation,
        manaCost: ManaCost
    ): ExecutionResult {
        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = ManaPaymentWindow.buildDecision(
            state = state,
            playerId = continuation.playerId,
            cost = manaCost,
            decisionId = decisionId,
            prompt = "Pay $manaCost",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            canDecline = true,
            cardRegistry = services.cardRegistry
        )
        val frame = PayOrSufferManaSelectionContinuation(
            decisionId = decisionId,
            inner = continuation,
            manaCost = manaCost,
            availableSources = decision.availableSources
        )
        return ExecutionResult.paused(
            state.withPendingDecision(decision).pushContinuation(frame),
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.playerId,
                    decisionType = "SELECT_MANA_SOURCES",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Applies the payer's source picks, then re-enters the payment with the mana floating so the
     * ordinary "pay from pool" path finishes it. Declining is the same outcome as answering "no".
     */
    private fun resumePayOrSufferManaSelection(
        state: GameState,
        continuation: PayOrSufferManaSelectionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is ManaSourcesSelectedResponse) {
            return ExecutionResult.error(state, "Expected mana sources selected response for pay or suffer")
        }
        val inner = continuation.inner
        val floated = ManaPaymentWindow.floatSelectedMana(
            state, inner.playerId, continuation.manaCost, response, continuation.availableSources, services
        )
        if (!floated.paid) return executePayOrSufferConsequence(floated.state, inner, checkForMore)

        // Settle directly rather than re-entering the yes-branch: that would notice a short
        // submission and raise the window a second time, which is a loop with a stubborn client.
        // Coming up short here is simply a failure to pay, so the consequence runs.
        return settleManaPayment(floated.state, inner, continuation.manaCost, floated.events, checkForMore)
    }

    /**
     * Handle mana cost yes/no choice for pay or suffer.
     */
    private fun resumePayOrSufferMana(
        state: GameState,
        continuation: PayOrSufferContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for pay or suffer mana")
        }

        if (!response.choice) {
            return executePayOrSufferConsequence(state, continuation, checkForMore)
        }

        val manaCost = continuation.manaCost
            ?: return ExecutionResult.error(state, "No mana cost stored in continuation")
        val playerId = continuation.playerId

        // A second step: which sources to tap. Handing the cost straight to the auto-tap solver
        // gave the payer no say, and no chance to activate a mana ability for a cost the solver
        // can't auto-tap — a Treasure, an Ashnod's Altar — which `canPay` counts as affordable, so
        // "yes" could be accepted and then silently drop through to the suffer effect.
        // CR 605.3a; see [ManaPaymentWindow].
        if (!ManaPaymentWindow.floatingManaCovers(state, playerId, manaCost)) {
            return openManaSourceWindow(state, continuation, manaCost)
        }
        return settleManaPayment(state, continuation, manaCost, emptyList(), checkForMore)
    }

    /**
     * Spends [manaCost] from the payer's pool and runs the paid branch. Whatever they were going to
     * tap is already tapped and floating by the time this runs — either they had the mana, or the
     * source window put it there — so coming up short means the cost simply wasn't paid.
     */
    private fun settleManaPayment(
        state: GameState,
        continuation: PayOrSufferContinuation,
        manaCost: ManaCost,
        priorEvents: List<GameEvent>,
        checkForMore: CheckForMore
    ): ExecutionResult {
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

        val currentPool = manaPool
        var currentState = state
        val events = priorEvents.toMutableList()

        // No solver fallback here: everything the payer meant to tap is already in the pool —
        // either they had the mana floating, or the source window put it there. Auto-tapping the
        // shortfall would silently overrule what they picked.
        val newPool = currentPool.pay(manaCost)
            ?: return executePayOrSufferConsequence(state, continuation, checkForMore)

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

        return checkForMore(currentState, events)
    }

    private fun executePayOrSufferConsequence(
        state: GameState,
        continuation: PayOrSufferContinuation,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val sourceId = continuation.sourceId
        val sufferEffect = continuation.sufferEffect

        // Create context for executing the suffer effect, preserving targets AND the original
        // trigger context. Without the latter, effects like LoseLifeEffect(1, PlayerRef(TriggeringPlayer))
        // (Nafs Asp's bite when the damaged player declines to pay {1}) silently fizzle.
        //
        // The suffer effect belongs to the triggered ability, so it resolves under the ability's
        // controller — not [playerId], which is the player who declined to pay. The two diverge
        // only when the cost was routed to a non-controller (Meathook Massacre II: an opponent
        // declines to pay 3 life, and *you* — the ability's controller — return the dead creature
        // under your control). `EffectTarget.Controller` in the consequence must mean the ability's
        // controller for that theft to work. The auto-suffer path already runs under the original
        // (ability-controller) context, so this keeps both paths consistent. Falls back to the payer
        // for the common case where the payer is the controller.
        val context = EffectContext(
            sourceId = sourceId,
            controllerId = continuation.abilityControllerId ?: continuation.playerId,
            targets = continuation.targets,
            pipeline = PipelineState(
                namedTargets = continuation.namedTargets,
                // Carried across the pause so a collection-reading suffer effect still resolves —
                // Wand of Ith discards "the card revealed this way".
                storedCollections = continuation.storedCollections,
                // Rebind the enclosing ForEach loop's entity: the consequence may refer back to it
                // (Tidal Flats' "creatures you control blocking *that creature*"), and a null here
                // matches nothing at all rather than failing loudly.
                iterationTarget = continuation.iterationEntityId
            ),
            triggeringEntityId = continuation.triggeringEntityId,
            triggeringPlayerId = continuation.triggeringPlayerId
        )

        // Execute the suffer effect using the registry
        val result = services.effectExecutorRegistry.execute(state, sufferEffect, context).toExecutionResult()

        return if (result.isPaused) {
            result
        } else {
            checkForMore(result.state, result.events.toList())
        }
    }

    /**
     * Resume after a player decides whether to pay for "any player may [cost]" effects.
     *
     * Dispatches on the cost type: [PayCost.Sacrifice] expects a card selection, [PayCost.PayLife]
     * a yes/no. If the current player pays, [AnyPlayerMayPayContinuation.consequence] runs. If they
     * decline, the next eligible player is asked; once none remain,
     * [AnyPlayerMayPayContinuation.consequenceIfNonePaid] runs.
     */
    fun resumeAnyPlayerMayPay(
        state: GameState,
        continuation: AnyPlayerMayPayContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val playerId = continuation.currentPlayerId

        when ((continuation.cost as? PayCost.Atom)?.atom) {
            is CostAtom.Sacrifice -> {
                if (response !is CardsSelectedResponse) {
                    return ExecutionResult.error(state, "Expected card selection response for any player may pay")
                }
                val selectedPermanents = response.selectedCards
                if (selectedPermanents.size < continuation.requiredCount) {
                    return askNextPlayerForAnyPlayerMayPay(state, continuation, checkForMore)
                }
                // No domain re-check here: `DecisionValidators.validateSelectCards` already rejects
                // anything outside the decision's `options`, and those options come from
                // [anyPlayerSacrificeCandidates] on both the initial and the next-player path — so an
                // `excludeSelf` source can never reach this payment.
                var newState = state
                val events = mutableListOf<GameEvent>()
                events.add(PermanentsSacrificedEvent(playerId, selectedPermanents))
                newState = ZoneTransitionService.trackPermanentSacrifice(newState, selectedPermanents, playerId)
                for (permanentId in selectedPermanents) {
                    val transitionResult = ZoneTransitionService.moveToZone(newState, permanentId, Zone.GRAVEYARD)
                    newState = transitionResult.state
                    events.addAll(transitionResult.events)
                }
                return runAnyPlayerMayPayConsequence(newState, continuation, continuation.consequence, events, checkForMore)
            }

            is CostAtom.PayLife -> {
                if (response !is YesNoResponse) {
                    return ExecutionResult.error(state, "Expected yes/no response for any player may pay life")
                }
                if (!response.choice) {
                    return askNextPlayerForAnyPlayerMayPay(state, continuation, checkForMore)
                }

                val (newState, paymentEvents) = LifePaymentService
                    .pay(state, playerId, continuation.requiredCount)
                    ?: return ExecutionResult.error(state, "Player has no life total")
                val events = paymentEvents.toMutableList()
                return runAnyPlayerMayPayConsequence(newState, continuation, continuation.consequence, events, checkForMore)
            }

            else -> return ExecutionResult.error(
                state,
                "Unsupported cost type for AnyPlayerMayPay resume: ${continuation.cost::class.simpleName}"
            )
        }
    }

    /**
     * Execute one of the AnyPlayerMayPay consequence branches (may be null = nothing), carrying the
     * pipeline's stored collections so the effect can reference cards gathered earlier this resolution.
     */
    private fun runAnyPlayerMayPayConsequence(
        state: GameState,
        continuation: AnyPlayerMayPayContinuation,
        consequence: com.wingedsheep.sdk.scripting.effects.Effect?,
        priorEvents: List<GameEvent>,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (consequence == null) return checkForMore(state, priorEvents)
        val context = EffectContext(
            sourceId = continuation.sourceId,
            controllerId = continuation.controllerId,
            pipeline = PipelineState(
                storedCollections = continuation.storedCollections,
                // The enclosing per-permanent loop's current entity, so a consequence written as
                // `EffectTarget.Self` still means that permanent after the pay-or-decline pause
                // (Cleansing: "for each land, destroy that land unless any player pays 1 life").
                iterationTarget = continuation.iterationTarget
            ),
            triggeringEntityId = continuation.triggeringEntityId,
            triggeringPlayerId = continuation.triggeringPlayerId
        )
        val result = services.effectExecutorRegistry.execute(state, consequence, context).toExecutionResult()
        val allEvents = priorEvents + result.events
        return if (result.isPaused) result else checkForMore(result.state, allEvents)
    }

    /**
     * The permanents [playerId] may sacrifice to pay an "any player may sacrifice …" [cost] whose
     * source is [sourceId].
     *
     * The same rule the initial prompt uses (`AnyPlayerMayPayExecutor`), applied here so the
     * continuation can't widen or narrow the domain between players:
     *
     * - the atom's own `excludeSelf` — and nothing else — decides whether the source is in the pool;
     * - control comes from *projected* state via [BattlefieldFilterUtils], not from a raw
     *   `ZoneKey(playerId, BATTLEFIELD)` scan, so a permanent whose controller was changed by a
     *   continuous effect is offered to the player who actually controls it.
     */
    private fun anyPlayerSacrificeCandidates(
        state: GameState,
        playerId: EntityId,
        cost: CostAtom.Sacrifice,
        sourceId: EntityId
    ): List<EntityId> =
        BattlefieldFilterUtils.findMatchingOnBattlefield(
            state,
            cost.filter.youControl(),
            PredicateContext(controllerId = playerId),
            excludeSelfId = if (cost.excludeSelf) sourceId else null
        )

    /**
     * Find and ask the next eligible player for "any player may [cost]" effects.
     * If no player can pay, runs the "none paid" consequence (e.g., reanimate the discarded card).
     */
    private fun askNextPlayerForAnyPlayerMayPay(
        state: GameState,
        continuation: AnyPlayerMayPayContinuation,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val cost = continuation.cost
        val decisionHandler = DecisionHandler()

        for ((index, nextPlayerId) in continuation.remainingPlayers.withIndex()) {
            val remainingAfter = continuation.remainingPlayers.drop(index + 1)
            when (val atom = (cost as? PayCost.Atom)?.atom) {
                is CostAtom.Sacrifice -> {
                    val validPermanents = anyPlayerSacrificeCandidates(
                        state, nextPlayerId, atom, continuation.sourceId
                    )
                    if (validPermanents.size >= atom.count) {
                        val prompt = "You may sacrifice ${atom.count} ${atom.filter.description}s to cause ${continuation.sourceName} to be sacrificed, or skip"
                        val decisionResult = decisionHandler.createCardSelectionDecision(
                            state = state,
                            playerId = nextPlayerId,
                            sourceId = continuation.sourceId,
                            sourceName = continuation.sourceName,
                            prompt = prompt,
                            options = validPermanents,
                            minSelections = 0,
                            maxSelections = atom.count,
                            ordered = false,
                            phase = DecisionPhase.RESOLUTION,
                            useTargetingUI = true
                        )
                        val newContinuation = continuation.copy(
                            decisionId = decisionResult.pendingDecision!!.id,
                            currentPlayerId = nextPlayerId,
                            remainingPlayers = remainingAfter
                        )
                        val stateWithContinuation = decisionResult.state.pushContinuation(newContinuation)
                        return ExecutionResult.paused(
                            stateWithContinuation,
                            decisionResult.pendingDecision,
                            decisionResult.events
                        )
                    }
                }

                is CostAtom.PayLife -> {
                    val life = state.lifeTotal(nextPlayerId) // CR 810.9a — team's shared total
                    if (life >= atom.amount) {
                        val decisionId = java.util.UUID.randomUUID().toString()
                        val prompt = "Pay ${atom.amount} life to prevent ${continuation.sourceName}'s effect?"
                        val decision = YesNoDecision(
                            id = decisionId,
                            playerId = nextPlayerId,
                            prompt = prompt,
                            context = DecisionContext(
                                sourceId = continuation.sourceId,
                                sourceName = continuation.sourceName,
                                phase = DecisionPhase.RESOLUTION
                            ),
                            yesText = "Pay ${atom.amount} life",
                            noText = "Don't pay"
                        )
                        val newContinuation = continuation.copy(
                            decisionId = decisionId,
                            currentPlayerId = nextPlayerId,
                            remainingPlayers = remainingAfter
                        )
                        val stateWithContinuation = state.withPendingDecision(decision).pushContinuation(newContinuation)
                        return ExecutionResult.paused(
                            stateWithContinuation,
                            decision,
                            listOf(
                                DecisionRequestedEvent(
                                    decisionId = decisionId,
                                    playerId = nextPlayerId,
                                    decisionType = "YES_NO",
                                    prompt = prompt
                                )
                            )
                        )
                    }
                }

                else -> {}
            }
        }

        // No player paid - run the "none paid" branch.
        return runAnyPlayerMayPayConsequence(state, continuation, continuation.consequenceIfNonePaid, emptyList(), checkForMore)
    }

    fun resumeUntapChoice(
        state: GameState,
        continuation: UntapChoiceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for untap choice")
        }

        val keepTapped = response.selectedCards.toSet()
        val toUntap = continuation.allPermanentsToUntap.filter { it !in keepTapped }

        // Enforce any untap-count caps (Damping Field). The decision's minSelections already forces
        // the player to keep enough tapped when the matching pool is homogeneous; this is defence in
        // depth against a selection that satisfies the count but leaves a filter over its cap (which
        // can happen when the keep-tapped pool also holds optional MAY_NOT_UNTAP permanents outside
        // the filter). The decision stays pending, so the rejection re-prompts; spell out the exact
        // shortfall so the player can correct it rather than guess.
        for (limit in continuation.untapLimits) {
            val untappingMatching = limit.matchingPermanents.count { it in toUntap }
            if (untappingMatching > limit.max) {
                val mustKeepMore = untappingMatching - limit.max
                return ExecutionResult.error(
                    state,
                    "At most ${limit.max} of the restricted permanents may untap; " +
                        "keep $mustKeepMore more of them tapped"
                )
            }
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Untap the permanents that the player did NOT choose to keep tapped.
        // Stun counters replace each untap event per Rule 122.1d; the granted
        // "remove a +1/+1 counter to untap" replacement applies on this (active
        // player's) untap step, so pass projected state.
        for (entityId in toUntap) {
            val (afterUntap, untapEvents) = untapOrConsumeStun(newState, entityId, newState.projectedState)
            newState = afterUntap
            events.addAll(untapEvents)
        }

        // Remove WhileSourceTapped (and the power-gated variant) floating effects whose
        // source is no longer tapped. The power-comparison half of the variant duration is
        // gated per-frame by StateProjector; cleanup only enforces the tapped condition.
        newState = newState.copy(
            floatingEffects = newState.floatingEffects.filter { floatingEffect ->
                when (floatingEffect.duration) {
                    is Duration.WhileSourceTapped,
                    is Duration.WhileSourceTappedAndAffectedPowerAtMostSource -> {
                        val sourceId = floatingEffect.sourceId
                        sourceId != null && newState.getBattlefield().contains(sourceId) &&
                            newState.getEntity(sourceId)?.has<TappedComponent>() == true
                    }
                    else -> true
                }
            }
        )

        // Remove summoning sickness from all creatures the active player controls
        val activePlayer = continuation.playerId
        val projected = newState.projectedState
        val creaturesToRefresh = newState.entities.filter { (entityId, container) ->
            projected.getController(entityId) == activePlayer &&
                container.has<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
        }.keys

        for (entityId in creaturesToRefresh) {
            newState = newState.updateEntity(entityId) {
                it.without<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
