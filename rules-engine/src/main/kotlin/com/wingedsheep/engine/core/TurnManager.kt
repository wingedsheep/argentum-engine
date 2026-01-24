package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ZoneType
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
class TurnManager {

    /**
     * Start a new turn for a player.
     */
    fun startTurn(state: GameState, playerId: EntityId): ExecutionResult {
        val isFirstTurn = state.turnNumber == 1 && playerId == state.turnOrder.first()
        val newTurnNumber = if (playerId == state.turnOrder.first()) state.turnNumber + 1 else state.turnNumber

        val newState = state.copy(
            activePlayerId = playerId,
            turnNumber = if (isFirstTurn) 1 else newTurnNumber,
            phase = Phase.BEGINNING,
            step = Step.UNTAP,
            priorityPlayerId = null, // No priority during untap
            priorityPassedBy = emptySet()
        )

        return ExecutionResult.success(
            newState,
            listOf(TurnChangedEvent(newState.turnNumber, playerId))
        )
    }

    /**
     * Perform the untap step.
     * - Untap all permanents controlled by the active player
     * - No priority is given during untap step
     */
    fun performUntapStep(state: GameState): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        val events = mutableListOf<GameEvent>()
        var newState = state

        // Find all tapped permanents controlled by the active player
        val permanentsToUntap = state.entities.filter { (_, container) ->
            container.get<ControllerComponent>()?.playerId == activePlayer &&
                container.has<TappedComponent>()
        }.keys

        // Untap them
        for (entityId in permanentsToUntap) {
            val cardName = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: "Permanent"
            newState = newState.updateEntity(entityId) { it.without<TappedComponent>() }
            events.add(UntappedEvent(entityId, cardName))
        }

        // Remove summoning sickness from all creatures the player controls
        val creaturesToRefresh = newState.entities.filter { (_, container) ->
            container.get<ControllerComponent>()?.playerId == activePlayer &&
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

        val libraryKey = ZoneKey(playerId, ZoneType.LIBRARY)
        val handKey = ZoneKey(playerId, ZoneType.HAND)

        repeat(count) {
            val library = newState.getZone(libraryKey)
            if (library.isEmpty()) {
                // Player tries to draw from empty library - they lose
                events.add(DrawFailedEvent(playerId, "Library is empty"))
                // Mark player as losing (will be handled by state-based actions)
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
            }

            Step.PRECOMBAT_MAIN,
            Step.POSTCOMBAT_MAIN -> {
                newState = newState.withPriority(activePlayer)
            }

            Step.BEGIN_COMBAT,
            Step.DECLARE_ATTACKERS,
            Step.DECLARE_BLOCKERS,
            Step.FIRST_STRIKE_COMBAT_DAMAGE,
            Step.COMBAT_DAMAGE,
            Step.END_COMBAT -> {
                newState = newState.withPriority(activePlayer)
            }

            Step.END -> {
                newState = newState.withPriority(activePlayer)
            }

            Step.CLEANUP -> {
                // Perform cleanup actions
                val cleanupResult = performCleanupStep(newState)
                if (!cleanupResult.isSuccess) return cleanupResult
                newState = cleanupResult.newState
                events.addAll(cleanupResult.events)
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
        val handKey = ZoneKey(activePlayer, ZoneType.HAND)
        val handSize = newState.getZone(handKey).size
        val maxHandSize = 7

        if (handSize > maxHandSize) {
            // Player needs to discard - this would require a decision
            // For now, mark that a decision is needed
            events.add(DiscardRequiredEvent(activePlayer, handSize - maxHandSize))
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
        val nextPlayer = state.getNextPlayer(currentPlayer)

        // Clean up end-of-turn effects
        val cleanedState = cleanupEndOfTurn(state)

        return startTurn(cleanedState, nextPlayer)
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
}
