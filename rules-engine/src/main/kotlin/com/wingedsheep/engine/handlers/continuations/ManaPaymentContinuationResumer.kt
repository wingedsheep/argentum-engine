package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

class ManaPaymentContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(CounterUnlessPaysContinuation::class, ::resumeCounterUnlessPays),
        resumer(CounterUnlessPaysLifeContinuation::class, ::resumeCounterUnlessPaysLife),
        resumer(CounterUnlessPaysManaSelectionContinuation::class, ::resumeCounterUnlessPaysManaSelection),
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
                return checkForMore(currentState, emptyList())
            }

            // Need to tap sources — show mana source selection UI
            val manaSolver = ManaSolver(services.cardRegistry)
            val sources = manaSolver.findAvailableManaSources(state, playerId)
            val sourceOptions = sources.map { source ->
                ManaSourceOption(
                    entityId = source.entityId,
                    name = source.name,
                    producesColors = source.producesColors,
                    producesColorless = source.producesColorless
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
                controllerId = continuation.controllerId
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
                val sourceMap = continuation.availableSources.associateBy { it.entityId }
                for (sourceId in response.selectedSources) {
                    val source = sourceMap[sourceId]
                        ?: return ExecutionResult.error(state, "Selected source $sourceId is not a valid mana source")

                    currentState = currentState.updateEntity(sourceId) { c ->
                        c.with(TappedComponent)
                    }
                    events.add(TappedEvent(sourceId, source.name))

                    if (source.producesColors.isNotEmpty()) {
                        currentPool = currentPool.add(source.producesColors.first())
                    } else if (source.producesColorless) {
                        currentPool = currentPool.addColorless(1)
                    }
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

        // Spell resolves normally — don't counter it
        return checkForMore(currentState, events)
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
                producesColorless = source.producesColorless
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
                val sourceMap = continuation.availableSources.associateBy { it.entityId }
                for (sourceId in response.selectedSources) {
                    val source = sourceMap[sourceId]
                        ?: return ExecutionResult.error(state, "Selected source $sourceId is not a valid mana source")

                    currentState = currentState.updateEntity(sourceId) { c ->
                        c.with(TappedComponent)
                    }
                    events.add(TappedEvent(sourceId, source.name))

                    if (source.producesColors.isNotEmpty()) {
                        currentPool = currentPool.add(source.producesColors.first())
                    } else if (source.producesColorless) {
                        currentPool = currentPool.addColorless(1)
                    }
                }
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
                producesColorless = source.producesColorless
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
                // Manual selection: tap the selected sources
                // Use the available sources stored in the continuation (already validated when decision was created)
                val sourceMap = continuation.availableSources.associateBy { it.entityId }

                for (sourceId in response.selectedSources) {
                    val source = sourceMap[sourceId]
                        ?: return ExecutionResult.error(state, "Selected source $sourceId is not a valid mana source")

                    currentState = currentState.updateEntity(sourceId) { c ->
                        c.with(TappedComponent)
                    }
                    events.add(TappedEvent(sourceId, source.name))

                    // Add mana from this source to the pool
                    // For simplicity, produce the first color or colorless
                    if (source.producesColors.isNotEmpty()) {
                        currentPool = currentPool.add(source.producesColors.first())
                    } else if (source.producesColorless) {
                        currentPool = currentPool.addColorless(1)
                    }
                }
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

        // Unwrap MayPayManaEffect to get inner effect
        val trigger = continuation.trigger
        val mayPayEffect = trigger.ability.effect as MayPayManaEffect
        val innerEffect = mayPayEffect.effect

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
}
