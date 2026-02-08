package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.MustAttackPlayerComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.player.AdditionalCombatPhasesComponent
import com.wingedsheep.engine.state.components.player.SkipCombatPhasesComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.state.components.player.SkipUntapComponent
import com.wingedsheep.engine.state.components.player.LoseAtEndStepComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration

/**
 * Manages turn-based game flow: phases, steps, and turn transitions.
 *
 * The turn structure follows MTG rules:
 * - Beginning Phase: Untap, Upkeep, Draw
 * - Precombat Main Phase
 * - Combat Phase: Begin Combat, Declare Attackers, Declare Blockers, Combat Damage, End Combat
 * - Postcombat Main Phase
 * - Ending Phase: End Step, Cleanup
 */
class TurnManager(
    private val combatManager: CombatManager = CombatManager(),
    private val sbaChecker: StateBasedActionChecker = StateBasedActionChecker(),
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val stateProjector: StateProjector = StateProjector()
) {

    /**
     * Start a new turn for a player.
     */
    fun startTurn(state: GameState, playerId: EntityId): ExecutionResult {
        // Turn number increments when the first player starts a new turn
        // It stays the same when the second player starts their turn within the same round
        val newTurnNumber = if (playerId == state.turnOrder.first()) {
            state.turnNumber + 1
        } else {
            state.turnNumber
        }

        var newState = state.copy(
            activePlayerId = playerId,
            turnNumber = newTurnNumber,
            phase = Phase.BEGINNING,
            step = Step.UNTAP,
            priorityPlayerId = null, // No priority during untap
            priorityPassedBy = emptySet()
        )

        // Activate MustAttackPlayerComponent if present (Taunt effect)
        val mustAttack = newState.getEntity(playerId)?.get<MustAttackPlayerComponent>()
        if (mustAttack != null && !mustAttack.activeThisTurn) {
            newState = newState.updateEntity(playerId) { container ->
                container.with(mustAttack.copy(activeThisTurn = true))
            }
        }

        return ExecutionResult.success(
            newState,
            listOf(TurnChangedEvent(newState.turnNumber, playerId))
        )
    }

    /**
     * Perform the untap step.
     * - Untap all permanents controlled by the active player
     * - Respects SkipUntapComponent which prevents certain permanents from untapping
     * - No priority is given during untap step
     */
    fun performUntapStep(state: GameState): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        val events = mutableListOf<GameEvent>()
        var newState = state

        // Check if the player has a SkipUntapComponent
        val skipUntap = newState.getEntity(activePlayer)?.get<SkipUntapComponent>()

        // Use projected state for controller checks (control-changing effects like Annex)
        val projected = stateProjector.project(state)

        // Find all tapped permanents controlled by the active player
        val permanentsToUntap = state.entities.filter { (entityId, container) ->
            projected.getController(entityId) == activePlayer &&
                container.has<TappedComponent>()
        }.keys.filter { entityId ->
            // If there's a skip untap component, check if this permanent should be skipped
            if (skipUntap != null) {
                val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                val typeLine = cardComponent?.typeLine
                val isCreature = typeLine?.isCreature == true
                val isLand = typeLine?.isLand == true

                // Skip this permanent if it matches the skip criteria
                val shouldSkip = (skipUntap.affectsCreatures && isCreature) ||
                    (skipUntap.affectsLands && isLand)
                !shouldSkip
            } else {
                true
            }
        }

        // Remove the SkipUntapComponent after processing (it's been consumed)
        if (skipUntap != null) {
            newState = newState.updateEntity(activePlayer) { container ->
                container.without<SkipUntapComponent>()
            }
        }

        // Filter out permanents with CANT_UNTAP keyword (e.g., Goblin Sharpshooter)
        val permanentsAfterCantUntap = permanentsToUntap.filter { entityId ->
            !projected.hasKeyword(entityId, Keyword.DOESNT_UNTAP)
        }

        // Check if any permanents have MAY_NOT_UNTAP keyword (e.g., Everglove Courier)
        val mayNotUntapPermanents = permanentsAfterCantUntap.filter { entityId ->
            projected.hasKeyword(entityId, Keyword.MAY_NOT_UNTAP)
        }

        if (mayNotUntapPermanents.isNotEmpty()) {
            // Ask the player which permanents to keep tapped
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = newState,
                playerId = activePlayer,
                sourceId = null,
                sourceName = null,
                prompt = "Choose permanents to keep tapped (you may choose not to untap them)",
                options = mayNotUntapPermanents,
                minSelections = 0,
                maxSelections = mayNotUntapPermanents.size,
                ordered = false,
                phase = DecisionPhase.STATE_BASED,
                useTargetingUI = true
            )

            val continuation = UntapChoiceContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                playerId = activePlayer,
                allPermanentsToUntap = permanentsAfterCantUntap
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                events + decisionResult.events
            )
        }

        // No MAY_NOT_UNTAP permanents - untap everything normally
        for (entityId in permanentsAfterCantUntap) {
            val cardName = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: "Permanent"
            newState = newState.updateEntity(entityId) { it.without<TappedComponent>() }
            events.add(UntappedEvent(entityId, cardName))
        }

        // Remove summoning sickness from all creatures the player controls (using projected state)
        val projectedAfterUntap = stateProjector.project(newState)
        val creaturesToRefresh = newState.entities.filter { (entityId, container) ->
            projectedAfterUntap.getController(entityId) == activePlayer &&
                container.has<SummoningSicknessComponent>()
        }.keys

        for (entityId in creaturesToRefresh) {
            newState = newState.updateEntity(entityId) { it.without<SummoningSicknessComponent>() }
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Perform the upkeep step.
     * - Triggers "at the beginning of your upkeep" abilities
     * - Players receive priority
     */
    fun performUpkeepStep(state: GameState): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        // Give priority to active player
        val newState = state.withPriority(activePlayer)

        return ExecutionResult.success(
            newState,
            listOf(StepChangedEvent(Step.UPKEEP))
        )
    }

    /**
     * Perform the draw step (active player draws a card).
     * - Skip draw on first turn for first player (standard rule)
     */
    fun performDrawStep(state: GameState): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        // Skip draw on first turn for first player
        val isFirstTurnFirstPlayer = state.turnNumber == 1 && activePlayer == state.turnOrder.first()
        if (isFirstTurnFirstPlayer) {
            return ExecutionResult.success(
                state.withPriority(activePlayer),
                listOf(StepChangedEvent(Step.DRAW))
            )
        }

        // Draw a card
        val drawResult = drawCards(state, activePlayer, 1)
        if (!drawResult.isSuccess) {
            return drawResult
        }

        // Give priority to active player
        val newState = drawResult.newState.withPriority(activePlayer)
        return ExecutionResult.success(newState, drawResult.events + StepChangedEvent(Step.DRAW))
    }

    /**
     * Draw cards for a player.
     */
    fun drawCards(state: GameState, playerId: EntityId, count: Int): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()
        val drawnCards = mutableListOf<EntityId>()

        val libraryKey = ZoneKey(playerId, Zone.LIBRARY)
        val handKey = ZoneKey(playerId, Zone.HAND)

        repeat(count) {
            val library = newState.getZone(libraryKey)
            if (library.isEmpty()) {
                // Player tries to draw from empty library - they lose (Rule 704.5c)
                newState = newState.updateEntity(playerId) { container ->
                    container.with(PlayerLostComponent(LossReason.EMPTY_LIBRARY))
                }
                events.add(DrawFailedEvent(playerId, "Library is empty"))
                return ExecutionResult.success(newState, events)
            }

            // Draw from top of library
            val cardId = library.first()
            newState = newState.removeFromZone(libraryKey, cardId)
            newState = newState.addToZone(handKey, cardId)
            drawnCards.add(cardId)
        }

        events.add(CardsDrawnEvent(playerId, count, drawnCards))
        return ExecutionResult.success(newState, events)
    }

    /**
     * Advance to the next step.
     * Handles automatic step-based actions and turn transitions.
     */
    fun advanceStep(state: GameState): ExecutionResult {
        val currentStep = state.step
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        // Check if we're wrapping to next turn
        if (currentStep == Step.CLEANUP) {
            return endTurn(state)
        }

        // Check for additional combat phases (Aggravated Assault, etc.)
        // When leaving POSTCOMBAT_MAIN, if additional combat phases are pending,
        // redirect to BEGIN_COMBAT instead of END step.
        if (currentStep == Step.POSTCOMBAT_MAIN) {
            val additionalPhases = state.getEntity(activePlayer)?.get<AdditionalCombatPhasesComponent>()
            if (additionalPhases != null && additionalPhases.count > 0) {
                // Decrement or remove the component
                var redirectedState = if (additionalPhases.count <= 1) {
                    state.updateEntity(activePlayer) { it.without<AdditionalCombatPhasesComponent>() }
                } else {
                    state.updateEntity(activePlayer) { container ->
                        container.with(AdditionalCombatPhasesComponent(additionalPhases.count - 1))
                    }
                }

                // Redirect to BEGIN_COMBAT
                redirectedState = redirectedState.copy(
                    step = Step.BEGIN_COMBAT,
                    phase = Phase.COMBAT,
                    priorityPassedBy = emptySet()
                )

                val events = mutableListOf<GameEvent>(
                    PhaseChangedEvent(Phase.COMBAT),
                    StepChangedEvent(Step.BEGIN_COMBAT)
                )

                redirectedState = redirectedState.withPriority(activePlayer)
                return ExecutionResult.success(redirectedState, events)
            }
        }

        val nextStep = currentStep.next()
        val nextPhase = nextStep.phase

        var newState = state.copy(
            step = nextStep,
            phase = nextPhase,
            priorityPassedBy = emptySet()
        )

        val events = mutableListOf<GameEvent>()

        // Emit phase change event if phase changed
        if (nextPhase != currentStep.phase) {
            events.add(PhaseChangedEvent(nextPhase))
        }

        events.add(StepChangedEvent(nextStep))

        // Perform automatic step actions
        when (nextStep) {
            Step.UNTAP -> {
                val untapResult = performUntapStep(newState)
                if (!untapResult.isSuccess) return untapResult
                // If paused for MAY_NOT_UNTAP decision, return paused result
                if (untapResult.isPaused) {
                    return ExecutionResult.paused(
                        untapResult.newState,
                        untapResult.pendingDecision!!,
                        events + untapResult.events
                    )
                }
                newState = untapResult.newState
                events.addAll(untapResult.events)
                // Immediately advance past untap (no priority)
                return advanceStep(newState.copy(step = Step.UNTAP))
            }

            Step.UPKEEP -> {
                newState = newState.withPriority(activePlayer)
            }

            Step.DRAW -> {
                val drawResult = performDrawStep(newState)
                if (!drawResult.isSuccess) return drawResult
                newState = drawResult.newState
                events.addAll(drawResult.events)
                // Check state-based actions after draw (Rule 704.3)
                // This handles the case where a player tried to draw from an empty library
                val sbaResult = sbaChecker.checkAndApply(newState)
                newState = sbaResult.newState
                events.addAll(sbaResult.events)
                // Only give priority if the game is not over
                if (newState.gameOver) {
                    newState = newState.copy(priorityPlayerId = null)
                }
            }

            Step.PRECOMBAT_MAIN,
            Step.POSTCOMBAT_MAIN -> {
                newState = newState.withPriority(activePlayer)
            }

            Step.BEGIN_COMBAT -> {
                // Check if the active player should skip combat phases (e.g., False Peace)
                val playerEntity = newState.getEntity(activePlayer)
                if (playerEntity?.has<SkipCombatPhasesComponent>() == true) {
                    // Remove the skip combat component (it's been used)
                    newState = newState.updateEntity(activePlayer) { container ->
                        container.without<SkipCombatPhasesComponent>()
                    }
                    // Skip directly to postcombat main phase
                    newState = newState.copy(
                        step = Step.POSTCOMBAT_MAIN,
                        phase = Phase.POSTCOMBAT_MAIN,
                        priorityPlayerId = activePlayer,
                        priorityPassedBy = emptySet()
                    )
                    events.add(PhaseChangedEvent(Phase.POSTCOMBAT_MAIN))
                    events.add(StepChangedEvent(Step.POSTCOMBAT_MAIN))
                    return ExecutionResult.success(newState, events)
                }
                newState = newState.withPriority(activePlayer)
            }

            Step.DECLARE_ATTACKERS -> {
                // Skip declare attackers if no valid attackers exist
                if (!hasValidAttackers(newState, activePlayer)) {
                    // Auto-advance past declare attackers (no creatures can attack)
                    return advanceStep(newState.copy(step = Step.DECLARE_ATTACKERS))
                }
                // Active player gets priority during declare attackers
                newState = newState.withPriority(activePlayer)
            }

            Step.DECLARE_BLOCKERS -> {
                // Skip declare blockers if no attackers were declared (CR 508.8)
                if (!hasAttackingCreatures(newState)) {
                    // Auto-advance past blockers step
                    return advanceStep(newState.copy(step = Step.DECLARE_BLOCKERS))
                }
                // Defending player gets priority during declare blockers
                val defendingPlayer = newState.turnOrder.firstOrNull { it != activePlayer }
                    ?: activePlayer
                newState = newState.withPriority(defendingPlayer)
            }

            Step.FIRST_STRIKE_COMBAT_DAMAGE -> {
                // Skip if no attackers
                if (!hasAttackingCreatures(newState)) {
                    return advanceStep(newState.copy(step = Step.FIRST_STRIKE_COMBAT_DAMAGE))
                }
                // Apply first strike combat damage
                val damageResult = combatManager.applyCombatDamage(newState, firstStrike = true)
                if (!damageResult.isSuccess) return damageResult
                newState = damageResult.newState
                events.addAll(damageResult.events)
                // Check state-based actions (creatures with lethal damage die)
                val sbaResult = sbaChecker.checkAndApply(newState)
                newState = sbaResult.newState
                events.addAll(sbaResult.events)
                // Only give priority if the game is not over
                if (newState.gameOver) {
                    newState = newState.copy(priorityPlayerId = null)
                } else {
                    newState = newState.withPriority(activePlayer)
                }
            }

            Step.COMBAT_DAMAGE -> {
                // Skip if no attackers (no combat damage to deal)
                if (!hasAttackingCreatures(newState)) {
                    return advanceStep(newState.copy(step = Step.COMBAT_DAMAGE))
                }
                // Apply regular combat damage
                val damageResult = combatManager.applyCombatDamage(newState, firstStrike = false)
                if (!damageResult.isSuccess) return damageResult
                newState = damageResult.newState
                events.addAll(damageResult.events)
                // Check state-based actions (creatures with lethal damage die)
                val sbaResult = sbaChecker.checkAndApply(newState)
                newState = sbaResult.newState
                events.addAll(sbaResult.events)
                // Only give priority if the game is not over
                if (newState.gameOver) {
                    newState = newState.copy(priorityPlayerId = null)
                } else {
                    newState = newState.withPriority(activePlayer)
                }
            }

            Step.END_COMBAT -> {
                // Clean up combat state (remove attacking/blocking components)
                val endCombatResult = combatManager.endCombat(newState)
                if (!endCombatResult.isSuccess) return endCombatResult
                newState = endCombatResult.newState
                events.addAll(endCombatResult.events)

                // Remove MustAttackPlayerComponent after combat (Taunt effect is consumed)
                val mustAttack = newState.getEntity(activePlayer)?.get<MustAttackPlayerComponent>()
                if (mustAttack != null && mustAttack.activeThisTurn) {
                    newState = newState.updateEntity(activePlayer) { container ->
                        container.without<MustAttackPlayerComponent>()
                    }
                }

                newState = newState.withPriority(activePlayer)
            }

            Step.END -> {
                // Check if the active player has LoseAtEndStepComponent (Last Chance effect)
                val loseComponent = newState.getEntity(activePlayer)?.get<LoseAtEndStepComponent>()
                if (loseComponent != null) {
                    if (loseComponent.turnsUntilLoss <= 0) {
                        // Time's up - remove the component and make the player lose
                        newState = newState.updateEntity(activePlayer) { container ->
                            container.without<LoseAtEndStepComponent>()
                                .with(PlayerLostComponent(LossReason.CARD_EFFECT))
                        }
                        events.add(PlayerLostEvent(activePlayer, GameEndReason.CARD_EFFECT, loseComponent.message))
                        // Check state-based actions to end the game
                        val sbaResult = sbaChecker.checkAndApply(newState)
                        newState = sbaResult.newState
                        events.addAll(sbaResult.events)
                        // Game is over, no priority
                        if (newState.gameOver) {
                            newState = newState.copy(priorityPlayerId = null)
                            return ExecutionResult.success(newState, events)
                        }
                    } else {
                        // Decrement the counter for next end step, preserving the message
                        newState = newState.updateEntity(activePlayer) { container ->
                            container.without<LoseAtEndStepComponent>()
                                .with(LoseAtEndStepComponent(loseComponent.turnsUntilLoss - 1, loseComponent.message))
                        }
                    }
                }
                newState = newState.withPriority(activePlayer)
            }

            Step.CLEANUP -> {
                // Perform cleanup actions
                val cleanupResult = performCleanupStep(newState)
                if (!cleanupResult.isSuccess) return cleanupResult
                newState = cleanupResult.newState
                events.addAll(cleanupResult.events)

                // Cleanup has no priority (normally) - auto-advance to next turn
                // unless there are triggered abilities or discard required
                if (newState.priorityPlayerId == null && newState.pendingDecision == null) {
                    val endTurnResult = endTurn(newState)
                    return ExecutionResult.success(
                        endTurnResult.newState,
                        events + endTurnResult.events
                    )
                }
            }
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Perform cleanup step actions.
     * - Discard down to maximum hand size (7)
     * - Remove damage from creatures
     * - Remove "until end of turn" effects
     */
    fun performCleanupStep(state: GameState): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Check if player needs to discard
        val handKey = ZoneKey(activePlayer, Zone.HAND)
        val hand = newState.getZone(handKey)
        val maxHandSize = 7
        val cardsToDiscard = hand.size - maxHandSize

        if (cardsToDiscard > 0) {
            // Player needs to discard - create a decision
            events.add(DiscardRequiredEvent(activePlayer, cardsToDiscard))

            // Create the card selection decision
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = newState,
                playerId = activePlayer,
                sourceId = null,
                sourceName = null,
                prompt = "Discard down to $maxHandSize cards (choose $cardsToDiscard to discard)",
                options = hand,
                minSelections = cardsToDiscard,
                maxSelections = cardsToDiscard,
                ordered = false,
                phase = DecisionPhase.STATE_BASED
            )

            // Push continuation to handle the response
            val continuation = HandSizeDiscardContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                playerId = activePlayer
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                events + decisionResult.events
            )
        }

        // Remove damage from all creatures
        val creaturesWithDamage = newState.entities.filter { (_, container) ->
            container.has<DamageComponent>() &&
                container.get<CardComponent>()?.typeLine?.isCreature == true
        }.keys

        for (entityId in creaturesWithDamage) {
            newState = newState.updateEntity(entityId) { it.without<DamageComponent>() }
        }

        // No priority during cleanup (normally)
        newState = newState.copy(priorityPlayerId = null)

        return ExecutionResult.success(newState, events)
    }

    /**
     * End the current turn and start the next player's turn.
     */
    fun endTurn(state: GameState): ExecutionResult {
        val currentPlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        // Get next player
        var nextPlayer = state.getNextPlayer(currentPlayer)

        // Clean up end-of-turn effects
        var cleanedState = cleanupEndOfTurn(state)

        // Check if the next player should skip their turn (e.g., Last Chance effect)
        val nextPlayerEntity = cleanedState.getEntity(nextPlayer)
        if (nextPlayerEntity?.has<SkipNextTurnComponent>() == true) {
            // Remove the skip turn component and skip to the following player's turn
            cleanedState = cleanedState.updateEntity(nextPlayer) { container ->
                container.without<SkipNextTurnComponent>()
            }
            // In a 2-player game, this gives the current player another turn
            nextPlayer = cleanedState.getNextPlayer(nextPlayer)
        }

        // Start the new turn (sets step to UNTAP with no priority)
        val turnResult = startTurn(cleanedState, nextPlayer)
        if (!turnResult.isSuccess) return turnResult

        // Perform the untap step
        val untapResult = performUntapStep(turnResult.newState)
        if (!untapResult.isSuccess) return untapResult

        // If paused for MAY_NOT_UNTAP decision, return paused result
        if (untapResult.isPaused) {
            return ExecutionResult.paused(
                untapResult.newState,
                untapResult.pendingDecision!!,
                turnResult.events + untapResult.events
            )
        }

        // Advance to upkeep (this sets priority to the active player)
        val advanceResult = advanceStep(untapResult.newState)

        return ExecutionResult.success(
            advanceResult.newState,
            turnResult.events + untapResult.events + advanceResult.events
        )
    }

    /**
     * Clean up end-of-turn effects.
     *
     * This is called at the end of each turn and handles:
     * 1. Expiring "until end of turn" floating effects (Giant Growth, etc.)
     * 2. Emptying mana pools
     * 3. Resetting per-turn trackers (land drops)
     */
    private fun cleanupEndOfTurn(state: GameState): GameState {
        var newState = state

        // 1. Expire floating effects with EndOfTurn duration
        val remainingEffects = newState.floatingEffects.filter { floatingEffect ->
            when (floatingEffect.duration) {
                is Duration.EndOfTurn -> false  // Remove it
                is Duration.EndOfCombat -> false  // Should already be removed, but clean up
                is Duration.UntilYourNextTurn -> true  // Keep until that player's next turn
                is Duration.UntilYourNextUpkeep -> true  // Keep until upkeep
                is Duration.Permanent -> true  // Never expires
                is Duration.WhileSourceOnBattlefield -> {
                    // Keep if source is still on battlefield
                    val sourceId = floatingEffect.sourceId
                    sourceId != null && newState.getBattlefield().contains(sourceId)
                }
                is Duration.WhileSourceTapped -> {
                    // Keep if source is still on battlefield AND tapped
                    val sourceId = floatingEffect.sourceId
                    sourceId != null && newState.getBattlefield().contains(sourceId) &&
                        newState.getEntity(sourceId)?.has<TappedComponent>() == true
                }
                is Duration.UntilPhase -> true  // Handle in phase transitions
                is Duration.UntilCondition -> true  // Handle condition checking elsewhere
            }
        }
        newState = newState.copy(floatingEffects = remainingEffects)

        // 2. Empty mana pools for all players
        for (playerId in newState.turnOrder) {
            newState = newState.updateEntity(playerId) { container ->
                val manaPool = container.get<ManaPoolComponent>()
                if (manaPool != null && !manaPool.isEmpty) {
                    container.with(manaPool.empty())
                } else {
                    container
                }
            }
        }

        // 3. Reset per-turn trackers (land drops reset at start of turn, but clean up here too)
        for (playerId in newState.turnOrder) {
            newState = newState.updateEntity(playerId) { container ->
                val landDrops = container.get<LandDropsComponent>()
                if (landDrops != null) {
                    container.with(landDrops.reset())
                } else {
                    container
                }
            }
        }

        // 4. Remove any unconsumed additional combat phase components
        for (playerId in newState.turnOrder) {
            newState = newState.updateEntity(playerId) { container ->
                if (container.has<AdditionalCombatPhasesComponent>()) {
                    container.without<AdditionalCombatPhasesComponent>()
                } else {
                    container
                }
            }
        }

        // 5. Expire granted triggered abilities with EndOfTurn duration
        if (newState.grantedTriggeredAbilities.isNotEmpty()) {
            val remainingGrants = newState.grantedTriggeredAbilities.filter { grant ->
                grant.duration !is Duration.EndOfTurn
            }
            newState = newState.copy(grantedTriggeredAbilities = remainingGrants)
        }

        return newState
    }

    /**
     * Skip to a specific step (used for testing or special effects).
     */
    fun skipToStep(state: GameState, step: Step): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        val newState = state.copy(
            step = step,
            phase = step.phase,
            priorityPlayerId = if (step.hasPriority) activePlayer else null,
            priorityPassedBy = emptySet()
        )

        return ExecutionResult.success(
            newState,
            listOf(PhaseChangedEvent(step.phase), StepChangedEvent(step))
        )
    }

    /**
     * Check if sorcery-speed actions are allowed.
     */
    fun canPlaySorcerySpeed(state: GameState, playerId: EntityId): Boolean {
        return state.step.allowsSorcerySpeed &&
            state.priorityPlayerId == playerId &&
            state.activePlayerId == playerId &&
            state.stack.isEmpty()
    }

    /**
     * Check if a player has any creatures that can legally attack.
     * A creature can attack if it's:
     * - A creature controlled by the player
     * - Untapped
     * - Doesn't have summoning sickness (unless it has haste)
     * - Doesn't have defender
     */
    fun hasValidAttackers(state: GameState, playerId: EntityId): Boolean {
        val battlefield = state.getBattlefield()

        // Project state once to get all keywords (including granted abilities)
        val projected = stateProjector.project(state)

        return battlefield.any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            val cardComponent = container.get<CardComponent>() ?: return@any false
            val controller = projected.getController(entityId)

            // Must be a creature controlled by the player
            if (!cardComponent.typeLine.isCreature || controller != playerId) {
                return@any false
            }

            // Must be untapped
            if (container.has<TappedComponent>()) {
                return@any false
            }

            // Check projected keywords for Haste/Defender
            val hasHaste = projected.hasKeyword(entityId, Keyword.HASTE)
            val hasDefender = projected.hasKeyword(entityId, Keyword.DEFENDER)

            // Must not have summoning sickness (unless it has haste)
            if (!hasHaste && container.has<SummoningSicknessComponent>()) {
                return@any false
            }

            // Must not have defender or "can't attack"
            if (hasDefender || projected.cantAttack(entityId)) {
                return@any false
            }

            true
        }
    }

    /**
     * Get all creatures that can legally attack for a player.
     * A creature can attack if it's:
     * - A creature controlled by the player
     * - Untapped
     * - Doesn't have summoning sickness (unless it has haste)
     * - Doesn't have defender
     */
    fun getValidAttackers(state: GameState, playerId: EntityId): List<EntityId> {
        val battlefield = state.getBattlefield()

        // Project state once to get all keywords (including granted abilities)
        val projected = stateProjector.project(state)

        return battlefield.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            val cardComponent = container.get<CardComponent>() ?: return@filter false
            val controller = projected.getController(entityId)

            // Must be a creature controlled by the player
            if (!cardComponent.typeLine.isCreature || controller != playerId) {
                return@filter false
            }

            // Must be untapped
            if (container.has<TappedComponent>()) {
                return@filter false
            }

            // Check projected keywords for Haste/Defender
            val hasHaste = projected.hasKeyword(entityId, Keyword.HASTE)
            val hasDefender = projected.hasKeyword(entityId, Keyword.DEFENDER)

            // Must not have summoning sickness (unless it has haste)
            if (!hasHaste && container.has<SummoningSicknessComponent>()) {
                return@filter false
            }

            // Must not have defender or "can't attack"
            if (hasDefender || projected.cantAttack(entityId)) {
                return@filter false
            }

            true
        }
    }

    /**
     * Get all creatures that can legally block for a player.
     * A creature can block if it's:
     * - A creature controlled by the player
     * - Untapped
     */
    fun getValidBlockers(state: GameState, playerId: EntityId): List<EntityId> {
        val battlefield = state.getBattlefield()
        val projected = stateProjector.project(state)

        return battlefield.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            val cardComponent = container.get<CardComponent>() ?: return@filter false
            val controller = projected.getController(entityId)

            // Must be a creature controlled by the player
            if (!cardComponent.typeLine.isCreature || controller != playerId) {
                return@filter false
            }

            // Must be untapped
            if (container.has<TappedComponent>()) {
                return@filter false
            }

            // Must be able to block at least one attacker
            if (!combatManager.canCreatureBlockAnyAttacker(state, entityId, playerId)) {
                return@filter false
            }

            true
        }
    }

    /**
     * Check if there are any creatures currently attacking.
     */
    fun hasAttackingCreatures(state: GameState): Boolean {
        return state.entities.any { (_, container) ->
            container.has<AttackingComponent>()
        }
    }
}
