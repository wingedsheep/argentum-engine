package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.MustAttackPlayerComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.AdditionalCombatPhasesComponent
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.state.components.player.ManaSpentOnSpellsThisTurnComponent
import com.wingedsheep.engine.state.components.player.LoseAtEndStepComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.player.SkipCombatPhasesComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Manages turn-based game flow: phases, steps, and turn transitions.
 *
 * The turn structure follows MTG rules:
 * - Beginning Phase: Untap, Upkeep, Draw
 * - Precombat Main Phase
 * - Combat Phase: Begin Combat, Declare Attackers, Declare Blockers, Combat Damage, End Combat
 * - Postcombat Main Phase
 * - Ending Phase: End Step, Cleanup
 *
 * Delegates domain-specific logic to:
 * - [BeginningPhaseManager] — untap, upkeep, saga lore counters
 * - [DrawPhaseManager] — draw step, draw replacement effects
 * - [CleanupPhaseManager] — cleanup step, end-of-turn expiration
 */
class TurnManager(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val combatManager: CombatManager = CombatManager(cardRegistry),
    private val sbaChecker: StateBasedActionChecker = StateBasedActionChecker(cardRegistry = cardRegistry),
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val effectExecutor: ((GameState, Effect, EffectContext) -> ExecutionResult)? = null
) {

    val cleanupPhaseManager = CleanupPhaseManager(cardRegistry, decisionHandler)
    val drawPhaseManager = DrawPhaseManager(cardRegistry, decisionHandler, effectExecutor)
    val beginningPhaseManager = BeginningPhaseManager(cardRegistry, decisionHandler, cleanupPhaseManager)

    // ── Delegate methods for external callers ──

    /** Draw cards for a player. Delegates to [DrawPhaseManager]. */
    fun drawCards(state: GameState, playerId: EntityId, count: Int, skipPrompts: Boolean = false): ExecutionResult =
        drawPhaseManager.drawCards(state, playerId, count, skipPrompts)

    /** Check for prompt-on-draw abilities. Delegates to [DrawPhaseManager]. */
    internal fun checkPromptOnDraw(
        state: GameState,
        playerId: EntityId,
        drawCount: Int,
        isDrawStep: Boolean,
        declinedSourceIds: List<EntityId> = emptyList()
    ): ExecutionResult? =
        drawPhaseManager.checkPromptOnDraw(state, playerId, drawCount, isDrawStep, declinedSourceIds)

    // ── Turn lifecycle ──

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
            priorityPassedBy = emptySet(),
            spellsCastThisTurn = 0,
            playerSpellsCastThisTurn = emptyMap(),
            spellsCastThisTurnByPlayer = emptyMap(),
            pendingSpellCopies = emptyList(),
            spellWarpedThisTurn = false
        )

        // Reset cards-drawn-this-turn count for ALL players (not just active player)
        // because "each turn" means every turn transition resets the count
        for (pid in state.turnOrder) {
            newState = newState.updateEntity(pid) { container ->
                container.with(CardsDrawnThisTurnComponent(count = 0))
                    .with(ManaSpentOnSpellsThisTurnComponent(totalSpent = 0))
            }
        }

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
        if (currentStep == Step.POSTCOMBAT_MAIN) {
            val additionalPhases = state.getEntity(activePlayer)?.get<AdditionalCombatPhasesComponent>()
            if (additionalPhases != null && additionalPhases.count > 0) {
                var redirectedState = if (additionalPhases.count <= 1) {
                    state.updateEntity(activePlayer) { it.without<AdditionalCombatPhasesComponent>() }
                } else {
                    state.updateEntity(activePlayer) { container ->
                        container.with(AdditionalCombatPhasesComponent(additionalPhases.count - 1))
                    }
                }

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
                val untapResult = beginningPhaseManager.performUntapStep(newState)
                if (!untapResult.isSuccess) return untapResult
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
                val drawResult = drawPhaseManager.performDrawStep(newState)
                if (drawResult.isPaused) {
                    return ExecutionResult.paused(
                        drawResult.state,
                        drawResult.pendingDecision!!,
                        events + drawResult.events
                    )
                }
                if (!drawResult.isSuccess) return drawResult
                newState = drawResult.newState
                events.addAll(drawResult.events)
                // Check state-based actions after draw (Rule 704.3)
                val sbaResult = sbaChecker.checkAndApply(newState)
                if (sbaResult.isPaused) {
                    return ExecutionResult.paused(
                        sbaResult.state,
                        sbaResult.pendingDecision!!,
                        events + sbaResult.events
                    )
                }
                newState = sbaResult.newState
                events.addAll(sbaResult.events)
                if (newState.gameOver) {
                    newState = newState.copy(priorityPlayerId = null)
                }
            }

            Step.PRECOMBAT_MAIN -> {
                val sagaLoreResult = beginningPhaseManager.addLoreCountersToSagas(newState, activePlayer)
                newState = sagaLoreResult.newState
                events.addAll(sagaLoreResult.events)
                newState = newState.withPriority(activePlayer)
            }

            Step.POSTCOMBAT_MAIN -> {
                newState = newState.withPriority(activePlayer)
            }

            Step.BEGIN_COMBAT -> {
                val playerEntity = newState.getEntity(activePlayer)
                if (playerEntity?.has<SkipCombatPhasesComponent>() == true) {
                    newState = newState.updateEntity(activePlayer) { container ->
                        container.without<SkipCombatPhasesComponent>()
                    }
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
                if (!hasValidAttackers(newState, activePlayer)) {
                    return advanceStep(newState.copy(step = Step.DECLARE_ATTACKERS))
                }
                newState = newState.withPriority(activePlayer)
            }

            Step.DECLARE_BLOCKERS -> {
                if (!hasAttackingCreatures(newState)) {
                    return advanceStep(newState.copy(step = Step.DECLARE_BLOCKERS))
                }
                val defendingPlayer = newState.turnOrder.firstOrNull { it != activePlayer }
                    ?: activePlayer
                newState = newState.withPriority(defendingPlayer)
            }

            Step.FIRST_STRIKE_COMBAT_DAMAGE -> {
                if (!hasAttackingCreatures(newState) || !hasCombatFirstStrikeOrDoubleStrike(newState)) {
                    return advanceStep(newState.copy(step = Step.FIRST_STRIKE_COMBAT_DAMAGE))
                }
                val damageResult = combatManager.applyCombatDamage(newState, firstStrike = true)
                if (!damageResult.isSuccess) return damageResult
                newState = damageResult.newState
                events.addAll(damageResult.events)
                val sbaHelperResult = StepActionHelper.applySbasAndCheckGameOver(newState, activePlayer, sbaChecker, events)
                if (sbaHelperResult.isPaused) return sbaHelperResult
                newState = sbaHelperResult.newState
                // events already updated by helper
                if (!newState.gameOver) {
                    newState = newState.withPriority(activePlayer)
                }
                return ExecutionResult.success(newState, sbaHelperResult.events)
            }

            Step.COMBAT_DAMAGE -> {
                if (!hasAttackingCreatures(newState)) {
                    return advanceStep(newState.copy(step = Step.COMBAT_DAMAGE))
                }
                val damageResult = combatManager.applyCombatDamage(newState, firstStrike = false)
                if (!damageResult.isSuccess) return damageResult
                newState = damageResult.newState
                events.addAll(damageResult.events)
                val sbaHelperResult = StepActionHelper.applySbasAndCheckGameOver(newState, activePlayer, sbaChecker, events)
                if (sbaHelperResult.isPaused) return sbaHelperResult
                newState = sbaHelperResult.newState
                if (!newState.gameOver) {
                    newState = newState.withPriority(activePlayer)
                }
                return ExecutionResult.success(newState, sbaHelperResult.events)
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
                val loseComponent = newState.getEntity(activePlayer)?.get<LoseAtEndStepComponent>()
                if (loseComponent != null) {
                    if (loseComponent.turnsUntilLoss <= 0) {
                        newState = newState.updateEntity(activePlayer) { container ->
                            container.without<LoseAtEndStepComponent>()
                                .with(PlayerLostComponent(LossReason.CARD_EFFECT))
                        }
                        events.add(PlayerLostEvent(activePlayer, GameEndReason.CARD_EFFECT, loseComponent.message))
                        val sbaResult = sbaChecker.checkAndApply(newState)
                        if (sbaResult.isPaused) {
                            return ExecutionResult.paused(
                                sbaResult.state,
                                sbaResult.pendingDecision!!,
                                events + sbaResult.events
                            )
                        }
                        newState = sbaResult.newState
                        events.addAll(sbaResult.events)
                        if (newState.gameOver) {
                            newState = newState.copy(priorityPlayerId = null)
                            return ExecutionResult.success(newState, events)
                        }
                    } else {
                        newState = newState.updateEntity(activePlayer) { container ->
                            container.without<LoseAtEndStepComponent>()
                                .with(LoseAtEndStepComponent(loseComponent.turnsUntilLoss - 1, loseComponent.message))
                        }
                    }
                }
                newState = newState.withPriority(activePlayer)
            }

            Step.CLEANUP -> {
                val cleanupResult = cleanupPhaseManager.performCleanupStep(newState)
                if (!cleanupResult.isSuccess) return cleanupResult
                newState = cleanupResult.newState
                events.addAll(cleanupResult.events)

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
     * End the current turn and start the next player's turn.
     */
    fun endTurn(state: GameState): ExecutionResult {
        val currentPlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        // Get next player
        var nextPlayer = state.getNextPlayer(currentPlayer)

        // Clean up end-of-turn effects
        var cleanedState = cleanupPhaseManager.cleanupEndOfTurn(state)

        // Check if the next player should skip their turn (e.g., Last Chance effect)
        val nextPlayerEntity = cleanedState.getEntity(nextPlayer)
        if (nextPlayerEntity?.has<SkipNextTurnComponent>() == true) {
            cleanedState = cleanedState.updateEntity(nextPlayer) { container ->
                container.without<SkipNextTurnComponent>()
            }
            nextPlayer = cleanedState.getNextPlayer(nextPlayer)
        }

        // Start the new turn (sets step to UNTAP with no priority)
        val turnResult = startTurn(cleanedState, nextPlayer)
        if (!turnResult.isSuccess) return turnResult

        // Perform the untap step
        val untapResult = beginningPhaseManager.performUntapStep(turnResult.newState)
        if (!untapResult.isSuccess) return untapResult

        if (untapResult.isPaused) {
            return ExecutionResult.paused(
                untapResult.newState,
                untapResult.pendingDecision!!,
                turnResult.events + untapResult.events
            )
        }

        // Expire UntilYourNextTurn effects after the untap step completes.
        var postUntapState = cleanupPhaseManager.expireUntilYourNextTurnEffects(untapResult.newState, nextPlayer)
        postUntapState = cleanupPhaseManager.expireAffectedControllersNextUntapEffects(postUntapState, nextPlayer)

        // Advance to upkeep (this sets priority to the active player)
        val advanceResult = advanceStep(postUntapState)

        return ExecutionResult.success(
            advanceResult.newState,
            turnResult.events + untapResult.events + advanceResult.events
        )
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

    // ── Combat queries (thin delegates to CombatManager) ──

    fun canPlaySorcerySpeed(state: GameState, playerId: EntityId): Boolean {
        return state.step.allowsSorcerySpeed &&
            state.priorityPlayerId == playerId &&
            state.activePlayerId == playerId &&
            state.stack.isEmpty()
    }

    fun hasValidAttackers(state: GameState, playerId: EntityId): Boolean {
        val battlefield = state.getBattlefield()
        return battlefield.any { entityId ->
            combatManager.isValidAttacker(state, entityId, playerId) &&
                !combatManager.isRestrictedFromAllDefenders(state, entityId, playerId)
        }
    }

    fun getValidAttackers(state: GameState, playerId: EntityId): List<EntityId> {
        val battlefield = state.getBattlefield()
        return battlefield.filter { entityId ->
            combatManager.isValidAttacker(state, entityId, playerId) &&
                !combatManager.isRestrictedFromAllDefenders(state, entityId, playerId)
        }
    }

    fun getValidBlockers(state: GameState, playerId: EntityId): List<EntityId> {
        val battlefield = state.getBattlefield()
        val projected = state.projectedState

        return battlefield.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val controller = projected.getController(entityId)
            val projectedTypes = projected.getProjectedValues(entityId)?.types ?: emptySet()

            if ("CREATURE" !in projectedTypes || controller != playerId) {
                return@filter false
            }

            if (container.has<TappedComponent>()) {
                return@filter false
            }

            if (!combatManager.canCreatureBlockAnyAttacker(state, entityId, playerId)) {
                return@filter false
            }

            true
        }
    }

    fun getMandatoryAttackers(state: GameState, playerId: EntityId): List<EntityId> {
        return combatManager.getMandatoryAttackers(state, playerId)
    }

    fun getMandatoryBlockerAssignments(state: GameState, playerId: EntityId): Map<EntityId, List<EntityId>> {
        return combatManager.getMandatoryBlockerAssignments(state, playerId)
    }

    fun hasAttackingCreatures(state: GameState): Boolean {
        val battlefield = state.getBattlefield()
        return battlefield.any { entityId ->
            state.getEntity(entityId)?.has<AttackingComponent>() == true
        }
    }

    private fun hasCombatFirstStrikeOrDoubleStrike(state: GameState): Boolean {
        val projected = state.projectedState
        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            val isInCombat = container.has<AttackingComponent>() || container.has<BlockingComponent>()
            isInCombat && (projected.hasKeyword(entityId, Keyword.FIRST_STRIKE) || projected.hasKeyword(entityId, Keyword.DOUBLE_STRIKE))
        }
    }
}
