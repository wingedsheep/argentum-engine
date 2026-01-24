package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.MulliganHandler
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId

/**
 * The central action processor for the game engine.
 *
 * This is the main entry point for all game actions. It validates actions,
 * executes them against the game state, and returns the result.
 *
 * The processor is stateless - it's a pure function:
 * (GameState, GameAction) -> ExecutionResult(GameState, Events)
 */
class ActionProcessor(
    private val cardRegistry: CardRegistry? = null,
    private val combatManager: CombatManager = CombatManager(),
    private val turnManager: TurnManager = TurnManager(combatManager),
    private val stackResolver: StackResolver = StackResolver(),
    private val manaSolver: ManaSolver = ManaSolver(cardRegistry),
    private val costCalculator: CostCalculator = CostCalculator(cardRegistry),
    private val alternativePaymentHandler: AlternativePaymentHandler = AlternativePaymentHandler(),
    private val costHandler: CostHandler = CostHandler(),
    private val mulliganHandler: MulliganHandler = MulliganHandler()
) {

    /**
     * Process a game action and return the result.
     */
    fun process(state: GameState, action: GameAction): ExecutionResult {
        // Validate the action
        val validationError = validate(state, action)
        if (validationError != null) {
            return ExecutionResult.error(state, validationError)
        }

        // Execute the action
        return execute(state, action)
    }

    /**
     * Validate that an action is legal.
     * Returns an error message if invalid, null if valid.
     */
    private fun validate(state: GameState, action: GameAction): String? {
        // Check game is not over
        if (state.gameOver) {
            return "Game is already over"
        }

        // Check player exists
        if (!state.turnOrder.contains(action.playerId)) {
            return "Unknown player: ${action.playerId}"
        }

        return when (action) {
            is PassPriority -> validatePassPriority(state, action)
            is CastSpell -> validateCastSpell(state, action)
            is ActivateAbility -> validateActivateAbility(state, action)
            is PlayLand -> validatePlayLand(state, action)
            is DeclareAttackers -> validateDeclareAttackers(state, action)
            is DeclareBlockers -> validateDeclareBlockers(state, action)
            is OrderBlockers -> validateOrderBlockers(state, action)
            is MakeChoice -> validateMakeChoice(state, action)
            is SelectTargets -> validateSelectTargets(state, action)
            is ChooseManaColor -> validateChooseManaColor(state, action)
            is SubmitDecision -> validateSubmitDecision(state, action)
            is TakeMulligan -> validateTakeMulligan(state, action)
            is KeepHand -> validateKeepHand(state, action)
            is BottomCards -> validateBottomCards(state, action)
            is Concede -> null  // Always valid
        }
    }

    private fun validatePassPriority(state: GameState, action: PassPriority): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }
        return null
    }

    private fun validateCastSpell(state: GameState, action: CastSpell): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        // Check the card exists and is in hand
        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"

        // Check card is in a zone it can be cast from (typically hand)
        val handZone = ZoneKey(action.playerId, ZoneType.HAND)
        if (action.cardId !in state.getZone(handZone)) {
            return "Card is not in your hand"
        }

        // Check timing (sorcery speed vs instant speed)
        if (!cardComponent.typeLine.isInstant) {
            // Non-instants require sorcery timing
            if (!turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "You can only cast sorcery-speed spells during your main phase with an empty stack"
            }
        }

        // Check mana cost can be paid
        val xValue = action.xValue ?: 0

        // Calculate effective cost after applying cost reductions
        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)
        val effectiveCost = if (cardDef != null) {
            costCalculator.calculateEffectiveCost(state, cardDef, action.playerId)
        } else {
            cardComponent.manaCost
        }

        when (action.paymentStrategy) {
            is PaymentStrategy.AutoPay -> {
                // Check if auto-pay can find a solution
                if (!manaSolver.canPay(state, action.playerId, effectiveCost, xValue)) {
                    return "Not enough mana to cast this spell"
                }
            }
            is PaymentStrategy.FromPool -> {
                // Check if the mana pool has enough
                val poolComponent = state.getEntity(action.playerId)?.get<ManaPoolComponent>()
                    ?: ManaPoolComponent()
                val pool = ManaPool(
                    white = poolComponent.white,
                    blue = poolComponent.blue,
                    black = poolComponent.black,
                    red = poolComponent.red,
                    green = poolComponent.green,
                    colorless = poolComponent.colorless
                )
                if (!costHandler.canPayManaCost(pool, effectiveCost)) {
                    return "Insufficient mana in pool to cast this spell"
                }
            }
            is PaymentStrategy.Explicit -> {
                // Validate that the specified sources exist and can produce enough mana
                // For now, just check that all sources exist and are untapped
                for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                    val sourceContainer = state.getEntity(sourceId)
                        ?: return "Mana source not found: $sourceId"
                    if (sourceContainer.has<TappedComponent>()) {
                        return "Mana source is already tapped: $sourceId"
                    }
                }
                // TODO: Full validation that explicit sources produce enough mana
            }
        }

        // TODO: Check targets are valid

        return null
    }

    private fun validateActivateAbility(state: GameState, action: ActivateAbility): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        // Check the source exists
        val container = state.getEntity(action.sourceId)
            ?: return "Source not found: ${action.sourceId}"

        // Check player controls the source
        val controller = container.get<ControllerComponent>()?.playerId
        if (controller != action.playerId) {
            return "You don't control this permanent"
        }

        // TODO: Check ability exists and can be activated
        // TODO: Check costs can be paid

        return null
    }

    private fun validatePlayLand(state: GameState, action: PlayLand): String? {
        if (state.activePlayerId != action.playerId) {
            return "You can only play lands on your turn"
        }
        if (!state.step.isMainPhase) {
            return "You can only play lands during a main phase"
        }
        if (state.stack.isNotEmpty()) {
            return "You can only play lands when the stack is empty"
        }

        // Check land drop availability
        val landDrops = state.getEntity(action.playerId)?.get<LandDropsComponent>()
            ?: LandDropsComponent()
        if (!landDrops.canPlayLand) {
            return "You have already played a land this turn"
        }

        // Check card exists and is a land
        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"

        if (!cardComponent.typeLine.isLand) {
            return "You can only play land cards as lands"
        }

        // Check card is in hand
        val handZone = ZoneKey(action.playerId, ZoneType.HAND)
        if (action.cardId !in state.getZone(handZone)) {
            return "Land is not in your hand"
        }

        return null
    }

    private fun validateDeclareAttackers(state: GameState, action: DeclareAttackers): String? {
        if (state.activePlayerId != action.playerId) {
            return "You can only declare attackers on your turn"
        }
        if (state.step != com.wingedsheep.sdk.core.Step.DECLARE_ATTACKERS) {
            return "You can only declare attackers during the declare attackers step"
        }
        // Additional validation is done by CombatManager
        return null
    }

    private fun validateDeclareBlockers(state: GameState, action: DeclareBlockers): String? {
        if (state.activePlayerId == action.playerId) {
            return "You cannot declare blockers on your turn"
        }
        if (state.step != com.wingedsheep.sdk.core.Step.DECLARE_BLOCKERS) {
            return "You can only declare blockers during the declare blockers step"
        }
        // Additional validation is done by CombatManager
        return null
    }

    private fun validateOrderBlockers(state: GameState, action: OrderBlockers): String? {
        if (state.activePlayerId != action.playerId) {
            return "You can only order blockers on your turn"
        }
        if (state.step != com.wingedsheep.sdk.core.Step.DECLARE_BLOCKERS) {
            return "You can only order blockers during the declare blockers step"
        }
        return null
    }

    private fun validateMakeChoice(state: GameState, action: MakeChoice): String? {
        // TODO: Validate choice context
        return null
    }

    private fun validateSelectTargets(state: GameState, action: SelectTargets): String? {
        // TODO: Validate target selection
        return null
    }

    private fun validateChooseManaColor(state: GameState, action: ChooseManaColor): String? {
        // TODO: Validate mana color choice
        return null
    }

    private fun validateTakeMulligan(state: GameState, action: TakeMulligan): String? {
        val mullState = state.getEntity(action.playerId)?.get<MulliganStateComponent>()
            ?: return "Player mulligan state not found"

        if (mullState.hasKept) {
            return "You have already kept your hand"
        }

        if (!mullState.canMulligan) {
            return "Cannot mulligan - hand size would be 0"
        }

        return null
    }

    private fun validateKeepHand(state: GameState, action: KeepHand): String? {
        val mullState = state.getEntity(action.playerId)?.get<MulliganStateComponent>()
            ?: return "Player mulligan state not found"

        if (mullState.hasKept) {
            return "You have already kept your hand"
        }

        return null
    }

    private fun validateBottomCards(state: GameState, action: BottomCards): String? {
        val mullState = state.getEntity(action.playerId)?.get<MulliganStateComponent>()
            ?: return "Player mulligan state not found"

        if (!mullState.hasKept) {
            return "You have not kept your hand yet"
        }

        if (action.cardIds.size != mullState.cardsToBottom) {
            return "Must put exactly ${mullState.cardsToBottom} cards on bottom, got ${action.cardIds.size}"
        }

        // Validate cards are in hand
        val hand = state.getHand(action.playerId).toSet()
        val invalidCards = action.cardIds.filter { it !in hand }
        if (invalidCards.isNotEmpty()) {
            return "Cards not in hand: $invalidCards"
        }

        return null
    }

    /**
     * Execute a validated action.
     */
    private fun execute(state: GameState, action: GameAction): ExecutionResult {
        return when (action) {
            is PassPriority -> executePassPriority(state, action)
            is CastSpell -> executeCastSpell(state, action)
            is ActivateAbility -> executeActivateAbility(state, action)
            is PlayLand -> executePlayLand(state, action)
            is DeclareAttackers -> executeDeclareAttackers(state, action)
            is DeclareBlockers -> executeDeclareBlockers(state, action)
            is OrderBlockers -> executeOrderBlockers(state, action)
            is MakeChoice -> executeMakeChoice(state, action)
            is SelectTargets -> executeSelectTargets(state, action)
            is ChooseManaColor -> executeChooseManaColor(state, action)
            is SubmitDecision -> executeSubmitDecision(state, action)
            is TakeMulligan -> executeTakeMulligan(state, action)
            is KeepHand -> executeKeepHand(state, action)
            is BottomCards -> executeBottomCards(state, action)
            is Concede -> executeConcede(state, action)
        }
    }

    private fun executePassPriority(state: GameState, action: PassPriority): ExecutionResult {
        val newState = state.withPriorityPassed(action.playerId)

        // Check if all players passed
        if (newState.allPlayersPassed()) {
            // Either resolve top of stack or advance game
            return if (newState.stack.isNotEmpty()) {
                resolveTopOfStack(newState)
            } else {
                advanceGame(newState)
            }
        }

        // Pass to next player - use copy() to preserve priorityPassedBy
        // (withPriority would reset the passed flags)
        val nextPlayer = state.getNextPlayer(action.playerId)
        return ExecutionResult.success(
            newState.copy(priorityPlayerId = nextPlayer),
            listOf(PriorityChangedEvent(nextPlayer))
        )
    }

    private fun executeCastSpell(state: GameState, action: CastSpell): ExecutionResult {
        var currentState = state
        val events = mutableListOf<GameEvent>()

        // Get the card being cast
        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Card not found")

        val xValue = action.xValue ?: 0

        // Calculate effective cost after applying cost reductions
        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)
        var effectiveCost = if (cardDef != null) {
            costCalculator.calculateEffectiveCost(currentState, cardDef, action.playerId)
        } else {
            cardComponent.manaCost
        }

        // Apply alternative payment (Delve/Convoke) if specified
        if (action.alternativePayment != null && !action.alternativePayment.isEmpty && cardDef != null) {
            val altPaymentResult = alternativePaymentHandler.apply(
                currentState,
                effectiveCost,
                action.alternativePayment,
                action.playerId,
                cardDef
            )
            effectiveCost = altPaymentResult.reducedCost
            currentState = altPaymentResult.newState
            events.addAll(altPaymentResult.events)
        }

        // Handle mana payment based on strategy
        when (action.paymentStrategy) {
            is PaymentStrategy.FromPool -> {
                // Player has already floated mana - deduct from pool
                val poolComponent = currentState.getEntity(action.playerId)?.get<ManaPoolComponent>()
                    ?: ManaPoolComponent()
                val pool = ManaPool(
                    white = poolComponent.white,
                    blue = poolComponent.blue,
                    black = poolComponent.black,
                    red = poolComponent.red,
                    green = poolComponent.green,
                    colorless = poolComponent.colorless
                )

                val newPool = costHandler.payManaCost(pool, effectiveCost)
                    ?: return ExecutionResult.error(currentState, "Insufficient mana in pool")

                // Update the player's mana pool component
                currentState = currentState.updateEntity(action.playerId) { container ->
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

                // Emit mana spent event
                val spent = ManaSpentEvent(
                    playerId = action.playerId,
                    reason = "Cast ${cardComponent.name}",
                    white = poolComponent.white - newPool.white,
                    blue = poolComponent.blue - newPool.blue,
                    black = poolComponent.black - newPool.black,
                    red = poolComponent.red - newPool.red,
                    green = poolComponent.green - newPool.green,
                    colorless = poolComponent.colorless - newPool.colorless
                )
                events.add(spent)
            }

            is PaymentStrategy.AutoPay -> {
                // First try to pay from the mana pool
                val poolComponent = currentState.getEntity(action.playerId)?.get<ManaPoolComponent>()
                    ?: ManaPoolComponent()
                val pool = ManaPool(
                    white = poolComponent.white,
                    blue = poolComponent.blue,
                    black = poolComponent.black,
                    red = poolComponent.red,
                    green = poolComponent.green,
                    colorless = poolComponent.colorless
                )

                val newPool = costHandler.payManaCost(pool, effectiveCost)

                if (newPool != null) {
                    // Successfully paid from pool
                    currentState = currentState.updateEntity(action.playerId) { container ->
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

                    events.add(
                        ManaSpentEvent(
                            playerId = action.playerId,
                            reason = "Cast ${cardComponent.name}",
                            white = poolComponent.white - newPool.white,
                            blue = poolComponent.blue - newPool.blue,
                            black = poolComponent.black - newPool.black,
                            red = poolComponent.red - newPool.red,
                            green = poolComponent.green - newPool.green,
                            colorless = poolComponent.colorless - newPool.colorless
                        )
                    )
                } else {
                    // Pool didn't have enough - try tapping lands
                    val solution = manaSolver.solve(currentState, action.playerId, effectiveCost, xValue)
                        ?: return ExecutionResult.error(currentState, "Not enough mana to auto-pay")

                    // Tap each source and generate events
                    for (source in solution.sources) {
                        currentState = currentState.updateEntity(source.entityId) { container ->
                            container.with(TappedComponent)
                        }

                        // Emit tapped event for client animation
                        events.add(TappedEvent(source.entityId, source.name))
                    }

                    // Track mana spent for the event
                    var whiteSpent = 0
                    var blueSpent = 0
                    var blackSpent = 0
                    var redSpent = 0
                    var greenSpent = 0
                    var colorlessSpent = 0

                    for ((_, production) in solution.manaProduced) {
                        when (production.color) {
                            Color.WHITE -> whiteSpent++
                            Color.BLUE -> blueSpent++
                            Color.BLACK -> blackSpent++
                            Color.RED -> redSpent++
                            Color.GREEN -> greenSpent++
                            null -> colorlessSpent += production.colorless
                        }
                    }

                    events.add(
                        ManaSpentEvent(
                            playerId = action.playerId,
                            reason = "Cast ${cardComponent.name}",
                            white = whiteSpent,
                            blue = blueSpent,
                            black = blackSpent,
                            red = redSpent,
                            green = greenSpent,
                            colorless = colorlessSpent
                        )
                    )
                }
            }

            is PaymentStrategy.Explicit -> {
                // Tap the specified sources
                for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                    val sourceName = currentState.getEntity(sourceId)
                        ?.get<CardComponent>()?.name ?: "Unknown"

                    currentState = currentState.updateEntity(sourceId) { container ->
                        container.with(TappedComponent)
                    }

                    events.add(TappedEvent(sourceId, sourceName))
                }

                // For explicit payment, we trust that the client sent valid sources
                // TODO: More sophisticated tracking of exactly which mana was produced
            }
        }

        // Cast the spell using StackResolver
        val castResult = stackResolver.castSpell(
            currentState,
            action.cardId,
            action.playerId,
            action.targets,
            action.xValue
        )

        if (!castResult.isSuccess) {
            return castResult
        }

        // Combine our events with cast events
        val allEvents = events + castResult.events

        // After casting, active player gets priority
        return ExecutionResult.success(
            castResult.newState.withPriority(state.activePlayerId),
            allEvents
        )
    }

    private fun executeActivateAbility(state: GameState, action: ActivateAbility): ExecutionResult {
        // TODO: Look up the ability from the source
        // TODO: Pay costs
        // TODO: Put ability on stack

        // For now, return success with unchanged state
        return ExecutionResult.success(state)
    }

    private fun executePlayLand(state: GameState, action: PlayLand): ExecutionResult {
        val container = state.getEntity(action.cardId)
            ?: return ExecutionResult.error(state, "Card not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        var newState = state

        // Remove from hand
        val handZone = ZoneKey(action.playerId, ZoneType.HAND)
        newState = newState.removeFromZone(handZone, action.cardId)

        // Add to battlefield
        val battlefieldZone = ZoneKey(action.playerId, ZoneType.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, action.cardId)

        // Add controller component
        newState = newState.updateEntity(action.cardId) { c ->
            c.with(ControllerComponent(action.playerId))
        }

        // Use up a land drop
        newState = newState.updateEntity(action.playerId) { c ->
            val landDrops = c.get<LandDropsComponent>() ?: LandDropsComponent()
            c.with(landDrops.use())
        }

        return ExecutionResult.success(
            newState.tick(),
            listOf(
                ZoneChangeEvent(
                    action.cardId,
                    cardComponent.name,
                    ZoneType.HAND,
                    ZoneType.BATTLEFIELD,
                    action.playerId
                )
            )
        )
    }

    private fun executeDeclareAttackers(state: GameState, action: DeclareAttackers): ExecutionResult {
        return combatManager.declareAttackers(state, action.playerId, action.attackers)
    }

    private fun executeDeclareBlockers(state: GameState, action: DeclareBlockers): ExecutionResult {
        return combatManager.declareBlockers(state, action.playerId, action.blockers)
    }

    private fun executeOrderBlockers(state: GameState, action: OrderBlockers): ExecutionResult {
        val attackerId = action.attackerId

        // Validate the attacker exists and is actually blocked
        val attackerContainer = state.getEntity(attackerId)
            ?: return ExecutionResult.error(state, "Attacker not found: $attackerId")

        val blockedComponent = attackerContainer.get<com.wingedsheep.engine.state.components.combat.BlockedComponent>()
            ?: return ExecutionResult.error(state, "Creature is not blocked")

        // Validate all ordered blockers are actually blocking this attacker
        val actualBlockers = blockedComponent.blockerIds.toSet()
        val orderedBlockers = action.orderedBlockers.toSet()

        if (actualBlockers != orderedBlockers) {
            return ExecutionResult.error(
                state,
                "Ordered blockers must contain exactly the creatures blocking this attacker"
            )
        }

        // Store the damage assignment order on the attacker
        val newState = state.updateEntity(attackerId) { container ->
            container.with(
                com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent(
                    action.orderedBlockers
                )
            )
        }

        return ExecutionResult.success(
            newState,
            listOf(BlockerOrderDeclaredEvent(attackerId, action.orderedBlockers))
        )
    }

    private fun executeMakeChoice(state: GameState, action: MakeChoice): ExecutionResult {
        // TODO: Handle modal choices, card selections, etc.
        return ExecutionResult.success(state)
    }

    private fun executeSelectTargets(state: GameState, action: SelectTargets): ExecutionResult {
        // TODO: Handle target selection for triggered abilities
        return ExecutionResult.success(state)
    }

    private fun executeChooseManaColor(state: GameState, action: ChooseManaColor): ExecutionResult {
        // Add mana of the chosen color to the player's mana pool
        var newState = state.updateEntity(action.playerId) { c ->
            val manaPool = c.get<ManaPoolComponent>() ?: ManaPoolComponent()
            c.with(manaPool.add(action.color))
        }
        return ExecutionResult.success(newState)
    }

    private fun executeConcede(state: GameState, action: Concede): ExecutionResult {
        val opponent = state.getOpponent(action.playerId)
        return ExecutionResult.success(
            state.copy(gameOver = true, winnerId = opponent),
            listOf(
                PlayerLostEvent(action.playerId, GameEndReason.CONCESSION),
                GameEndedEvent(opponent, GameEndReason.CONCESSION)
            )
        )
    }

    private fun resolveTopOfStack(state: GameState): ExecutionResult {
        val result = stackResolver.resolveTop(state)

        if (!result.isSuccess) {
            return result
        }

        // After resolution, active player gets priority
        return ExecutionResult.success(
            result.newState.withPriority(state.activePlayerId),
            result.events
        )
    }

    private fun advanceGame(state: GameState): ExecutionResult {
        return turnManager.advanceStep(state)
    }

    // =========================================================================
    // Decision Handling
    // =========================================================================

    private fun validateSubmitDecision(state: GameState, action: SubmitDecision): String? {
        val pending = state.pendingDecision
            ?: return "No pending decision to respond to"

        if (pending.playerId != action.playerId) {
            return "You are not the player who needs to make this decision"
        }

        if (pending.id != action.response.decisionId) {
            return "Decision ID mismatch: expected ${pending.id}, got ${action.response.decisionId}"
        }

        // Validate the response matches the decision type
        return validateDecisionResponse(pending, action.response)
    }

    private fun validateDecisionResponse(decision: PendingDecision, response: DecisionResponse): String? {
        return when (decision) {
            is ChooseTargetsDecision -> {
                if (response !is TargetsResponse) {
                    return "Expected target selection response"
                }
                // Validate all selected targets are in the legal targets list
                for ((reqIndex, selectedIds) in response.selectedTargets) {
                    val legalForReq = decision.legalTargets[reqIndex] ?: emptyList()
                    for (id in selectedIds) {
                        if (id !in legalForReq) {
                            return "Invalid target: $id is not a legal choice for requirement $reqIndex"
                        }
                    }
                    // Check min/max constraints
                    val req = decision.targetRequirements.find { it.index == reqIndex }
                    if (req != null) {
                        if (selectedIds.size < req.minTargets) {
                            return "Not enough targets for requirement $reqIndex: need at least ${req.minTargets}"
                        }
                        if (selectedIds.size > req.maxTargets) {
                            return "Too many targets for requirement $reqIndex: maximum is ${req.maxTargets}"
                        }
                    }
                }
                null
            }
            is SelectCardsDecision -> {
                if (response !is CardsSelectedResponse) {
                    return "Expected card selection response"
                }
                // Validate all selected cards are in the options
                for (cardId in response.selectedCards) {
                    if (cardId !in decision.options) {
                        return "Invalid selection: $cardId is not a valid option"
                    }
                }
                if (response.selectedCards.size < decision.minSelections) {
                    return "Not enough cards selected: need at least ${decision.minSelections}"
                }
                if (response.selectedCards.size > decision.maxSelections) {
                    return "Too many cards selected: maximum is ${decision.maxSelections}"
                }
                null
            }
            is YesNoDecision -> {
                if (response !is YesNoResponse) {
                    return "Expected yes/no response"
                }
                null
            }
            is ChooseModeDecision -> {
                if (response !is ModesChosenResponse) {
                    return "Expected mode selection response"
                }
                // Validate all selected modes exist and are available
                for (modeIndex in response.selectedModes) {
                    val mode = decision.modes.find { it.index == modeIndex }
                    if (mode == null) {
                        return "Invalid mode index: $modeIndex"
                    }
                    if (!mode.available) {
                        return "Mode $modeIndex is not available"
                    }
                }
                if (response.selectedModes.size < decision.minModes) {
                    return "Not enough modes selected: need at least ${decision.minModes}"
                }
                if (response.selectedModes.size > decision.maxModes) {
                    return "Too many modes selected: maximum is ${decision.maxModes}"
                }
                null
            }
            is ChooseColorDecision -> {
                if (response !is ColorChosenResponse) {
                    return "Expected color choice response"
                }
                if (response.color !in decision.availableColors) {
                    return "Invalid color: ${response.color} is not available"
                }
                null
            }
            is ChooseNumberDecision -> {
                if (response !is NumberChosenResponse) {
                    return "Expected number choice response"
                }
                if (response.number < decision.minValue || response.number > decision.maxValue) {
                    return "Invalid number: must be between ${decision.minValue} and ${decision.maxValue}"
                }
                null
            }
            is DistributeDecision -> {
                if (response !is DistributionResponse) {
                    return "Expected distribution response"
                }
                val total = response.distribution.values.sum()
                if (total != decision.totalAmount) {
                    return "Distribution must total ${decision.totalAmount}, got $total"
                }
                for ((targetId, amount) in response.distribution) {
                    if (targetId !in decision.targets) {
                        return "Invalid target for distribution: $targetId"
                    }
                    if (amount < decision.minPerTarget) {
                        return "Each target must receive at least ${decision.minPerTarget}"
                    }
                }
                null
            }
            is OrderObjectsDecision -> {
                if (response !is OrderedResponse) {
                    return "Expected ordering response"
                }
                if (response.orderedObjects.toSet() != decision.objects.toSet()) {
                    return "Ordered objects must contain exactly the same objects as the decision"
                }
                null
            }
            is SplitPilesDecision -> {
                if (response !is PilesSplitResponse) {
                    return "Expected pile split response"
                }
                val allCards = response.piles.flatten().toSet()
                if (allCards != decision.cards.toSet()) {
                    return "Piles must contain exactly the same cards as the decision"
                }
                if (response.piles.size != decision.numberOfPiles) {
                    return "Must split into exactly ${decision.numberOfPiles} piles"
                }
                null
            }
            is ChooseOptionDecision -> {
                if (response !is OptionChosenResponse) {
                    return "Expected option choice response"
                }
                if (response.optionIndex < 0 || response.optionIndex >= decision.options.size) {
                    return "Invalid option index: ${response.optionIndex}"
                }
                null
            }
            is AssignDamageDecision -> {
                if (response !is DamageAssignmentResponse) {
                    return "Expected damage assignment response"
                }
                // Validate total damage doesn't exceed available power
                val totalDamage = response.assignments.values.sum()
                if (totalDamage > decision.availablePower) {
                    return "Total damage ($totalDamage) exceeds available power (${decision.availablePower})"
                }
                // Validate all targets are valid
                val validTargets = decision.orderedTargets.toSet() + listOfNotNull(decision.defenderId)
                for (targetId in response.assignments.keys) {
                    if (targetId !in validTargets) {
                        return "Invalid damage target: $targetId"
                    }
                }
                // Validate damage assignment order (each blocker must have lethal before next)
                var allPreviousHaveLethal = true
                for (blockerId in decision.orderedTargets) {
                    val assignedDamage = response.assignments[blockerId] ?: 0
                    val lethalRequired = decision.minimumAssignments[blockerId] ?: 0
                    val hasLethal = assignedDamage >= lethalRequired

                    if (!hasLethal && !allPreviousHaveLethal) {
                        // This is fine - we can assign 0 to later blockers
                    } else if (!hasLethal) {
                        allPreviousHaveLethal = false
                    }

                    // Check that no subsequent blocker has damage if this one doesn't have lethal
                    if (!hasLethal) {
                        val laterBlockers = decision.orderedTargets.dropWhile { it != blockerId }.drop(1)
                        for (laterBlocker in laterBlockers) {
                            if ((response.assignments[laterBlocker] ?: 0) > 0) {
                                return "Cannot assign damage to later blocker until earlier blockers have lethal damage"
                            }
                        }
                    }
                }
                // Validate trample damage only if all blockers have lethal
                val damageToDefender = response.assignments[decision.defenderId] ?: 0
                if (damageToDefender > 0) {
                    if (!decision.hasTrample) {
                        return "Cannot assign damage to defending player without trample"
                    }
                    if (!allPreviousHaveLethal) {
                        return "Cannot assign trample damage until all blockers have lethal damage"
                    }
                }
                null
            }
        }
    }

    private fun executeSubmitDecision(state: GameState, action: SubmitDecision): ExecutionResult {
        val pending = state.pendingDecision
            ?: return ExecutionResult.error(state, "No pending decision")

        // Clear the pending decision from state
        val clearedState = state.clearPendingDecision()

        // Emit event that decision was submitted
        val events = mutableListOf<GameEvent>(
            DecisionSubmittedEvent(pending.id, action.playerId)
        )

        // Process the decision response based on type
        // For now, just return success with cleared state
        // In a full implementation, this would resume the paused execution
        // by looking up the continuation context and calling the appropriate handler

        // TODO: Implement decision handlers that resume execution based on the context
        // For example:
        // - ChooseTargetsDecision during casting -> finish putting spell on stack
        // - SelectCardsDecision during discard -> actually discard the selected cards
        // - YesNoDecision for may abilities -> either execute or skip the effect

        return ExecutionResult.success(clearedState, events)
    }

    // =========================================================================
    // Mulligan Actions
    // =========================================================================

    private fun executeTakeMulligan(state: GameState, action: TakeMulligan): ExecutionResult {
        return when (val result = mulliganHandler.handleTakeMulligan(state, action)) {
            is EngineResult.Success -> checkMulliganCompletion(result.newState, result.events)
            is EngineResult.Failure -> ExecutionResult.error(result.originalState, result.message)
            is EngineResult.PausedForDecision -> ExecutionResult.paused(result.partialState, result.decision, result.events)
            is EngineResult.GameOver -> ExecutionResult.success(result.finalState.copy(gameOver = true, winnerId = result.winnerId), result.events)
        }
    }

    private fun executeKeepHand(state: GameState, action: KeepHand): ExecutionResult {
        return when (val result = mulliganHandler.handleKeepHand(state, action)) {
            is EngineResult.Success -> checkMulliganCompletion(result.newState, result.events)
            is EngineResult.Failure -> ExecutionResult.error(result.originalState, result.message)
            is EngineResult.PausedForDecision -> ExecutionResult.paused(result.partialState, result.decision, result.events)
            is EngineResult.GameOver -> ExecutionResult.success(result.finalState.copy(gameOver = true, winnerId = result.winnerId), result.events)
        }
    }

    private fun executeBottomCards(state: GameState, action: BottomCards): ExecutionResult {
        return when (val result = mulliganHandler.handleBottomCards(state, action)) {
            is EngineResult.Success -> checkMulliganCompletion(result.newState, result.events)
            is EngineResult.Failure -> ExecutionResult.error(result.originalState, result.message)
            is EngineResult.PausedForDecision -> ExecutionResult.paused(result.partialState, result.decision, result.events)
            is EngineResult.GameOver -> ExecutionResult.success(result.finalState.copy(gameOver = true, winnerId = result.winnerId), result.events)
        }
    }

    /**
     * Check if all mulligans are complete and advance the game to the first turn if so.
     */
    private fun checkMulliganCompletion(state: GameState, events: List<GameEvent>): ExecutionResult {
        // If still in mulligan phase or someone needs to bottom cards, just return success
        if (mulliganHandler.isInMulliganPhase(state) || mulliganHandler.needsBottomCards(state)) {
            return ExecutionResult.success(state, events)
        }

        // All mulligans complete - start the first turn
        // The game starts at UNTAP step, so advance to process the untap and get to upkeep
        val advanceResult = turnManager.advanceStep(state)
        return ExecutionResult.success(
            advanceResult.newState,
            events + advanceResult.events
        )
    }
}
