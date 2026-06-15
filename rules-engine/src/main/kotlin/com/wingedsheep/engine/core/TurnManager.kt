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
import com.wingedsheep.engine.state.components.player.AdditionalUpkeepStepsComponent
import com.wingedsheep.engine.state.components.player.InAdditionalUpkeepStepComponent
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.state.components.player.EquipActivationsThisTurnComponent
import com.wingedsheep.engine.state.components.player.ManaSpentOnSpellsThisTurnComponent
import com.wingedsheep.engine.state.components.player.LoseAtEndStepComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent
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
    private val combatManager: CombatManager = CombatManager(
        cardRegistry,
        com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor.noOp(cardRegistry)
    ),
    private val sbaChecker: StateBasedActionChecker = StateBasedActionChecker(cardRegistry = cardRegistry),
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val effectExecutor: ((GameState, Effect, EffectContext) -> EffectResult)? = null
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
            pendingUncounterableSpells = emptyList(),
            spellWarpedThisTurn = false,
            damageCantBePreventedThisTurn = false,
            nonlandPermanentLeftBattlefieldThisTurn = false,
            permanentsSacrificedThisTurn = 0,
            playersWhoCommittedCrimeThisTurn = emptySet(),
            lastCastSpellColors = null,
            lastCardDrawnThisTurnByPlayer = emptyMap(),
            drawStepStartDrawCountByPlayer = emptyMap(),
            // Safety net mirroring CleanupPhaseManager.cleanupEndOfTurn: any end-of-turn /
            // end-of-combat counter-placement modifier still lingering at a turn boundary is
            // dropped. Longer-lived durations (UntilYourNextTurn, Permanent) survive.
            activeCounterPlacementModifiers = state.activeCounterPlacementModifiers.filter { modifier ->
                modifier.duration !is com.wingedsheep.sdk.scripting.Duration.EndOfTurn &&
                    modifier.duration !is com.wingedsheep.sdk.scripting.Duration.EndOfCombat
            }
        )

        // Reset cards-drawn-this-turn count for ALL players (not just active player)
        // because "each turn" means every turn transition resets the count
        for (pid in state.turnOrder) {
            newState = newState.updateEntity(pid) { container ->
                container.with(CardsDrawnThisTurnComponent(count = 0))
                    .with(ManaSpentOnSpellsThisTurnComponent(totalSpent = 0))
                    .with(EquipActivationsThisTurnComponent(count = 0))
            }
        }

        // Increment the turn-taken counter for every player on the active team. CR 500.11 / 614.10a
        // make a skipped turn "proceed past as though it didn't exist", so a skipped turn should not
        // count — the increment lives here, downstream of the SkipNextTurn consumption path. In a
        // shared team turn (CR 805.4) both teammates are taking the turn, so both counters advance;
        // in a non-team game (and in Team vs. Team, CR 808.4) this is just the active player.
        for (member in newState.sharedTurnTeam(playerId)) {
            newState = newState.updateEntity(member) { container ->
                val prev = container.get<PlayerTurnsTakenComponent>() ?: PlayerTurnsTakenComponent()
                container.with(prev.increment())
            }
        }

        // Activate MustAttackPlayerComponent if present (Taunt effect)
        val mustAttack = newState.getEntity(playerId)?.get<MustAttackPlayerComponent>()
        if (mustAttack != null && !mustAttack.activeThisTurn) {
            newState = newState.updateEntity(playerId) { container ->
                container.with(mustAttack.copy(activeThisTurn = true))
            }
        }

        val events = mutableListOf<GameEvent>(TurnChangedEvent(newState.turnNumber, playerId))

        // Activate a Mindslaver-style hijack scheduled on this player. Per Scryfall ruling,
        // a scheduled hijack waits through any skipped turns and engages on the next turn
        // the affected player actually takes.
        val scheduledHijack = newState.getEntity(playerId)?.get<PlayerTurnHijackedComponent>()
        if (scheduledHijack != null && scheduledHijack.state == PlayerTurnHijackedComponent.HijackState.SCHEDULED) {
            newState = newState.updateEntity(playerId) { container ->
                container.with(
                    scheduledHijack.copy(state = PlayerTurnHijackedComponent.HijackState.ACTIVE)
                )
            }
            events += TurnHijackedEvent(
                controllerId = scheduledHijack.controllerId,
                hijackedPlayerId = playerId,
                sourceId = playerId,
                sourceName = "Hijack engaged"
            )
        }

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

        // Leaving an inserted additional upkeep step (Obeka, Splitter of Seconds). Per CR 500.10
        // the extra beginning phase has only its upkeep step (its untap and draw steps are
        // skipped), and the game then returns to the phase after which the steps were added — the
        // postcombat main phase. The active player gets priority there; when they pass, the
        // POSTCOMBAT_MAIN drain below inserts the next remaining additional upkeep step (if any).
        if (currentStep == Step.UPKEEP &&
            state.getEntity(activePlayer)?.has<InAdditionalUpkeepStepComponent>() == true
        ) {
            var redirectedState = state.updateEntity(activePlayer) { container ->
                container.without<InAdditionalUpkeepStepComponent>()
            }
            redirectedState = redirectedState.copy(
                step = Step.POSTCOMBAT_MAIN,
                phase = Phase.POSTCOMBAT_MAIN,
                priorityPassedBy = emptySet()
            )
            val events = mutableListOf<GameEvent>(
                PhaseChangedEvent(Phase.POSTCOMBAT_MAIN),
                StepChangedEvent(Step.POSTCOMBAT_MAIN)
            )
            redirectedState = redirectedState.withPriority(activePlayer)
            return ExecutionResult.success(redirectedState, events)
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

            // No more additional combat phases — now drain any additional upkeep steps
            // (Obeka, Splitter of Seconds). Per the card's rulings, extra combat phases (created
            // earlier) happen before the extra beginning phases, which is why this check follows
            // the combat-phase check above. Each remaining count inserts one fresh beginning phase
            // whose only step is the upkeep step (untap and draw skipped); the
            // InAdditionalUpkeepStepComponent marker makes the redirect at the top of advanceStep
            // send the game back here after that upkeep step, draining the next one until the
            // count is exhausted, after which the turn proceeds to the postcombat main phase.
            val additionalUpkeeps = state.getEntity(activePlayer)?.get<AdditionalUpkeepStepsComponent>()
            if (additionalUpkeeps != null && additionalUpkeeps.count > 0) {
                var redirectedState = if (additionalUpkeeps.count <= 1) {
                    state.updateEntity(activePlayer) { it.without<AdditionalUpkeepStepsComponent>() }
                } else {
                    state.updateEntity(activePlayer) { container ->
                        container.with(AdditionalUpkeepStepsComponent(additionalUpkeeps.count - 1))
                    }
                }

                redirectedState = redirectedState
                    .updateEntity(activePlayer) { it.with(InAdditionalUpkeepStepComponent) }
                    .copy(
                        step = Step.UPKEEP,
                        phase = Phase.BEGINNING,
                        priorityPassedBy = emptySet()
                    )

                val events = mutableListOf<GameEvent>(
                    PhaseChangedEvent(Phase.BEGINNING),
                    StepChangedEvent(Step.UPKEEP)
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
                // Immediately advance past untap (no priority). Carry the untap-step events
                // (untaps, and phase-ins from Rule 702.26) forward on the result so the caller's
                // trigger detection sees them — e.g. King of the Oathbreakers' "phases in" trigger,
                // which fires when its controller's untap step phases it back in.
                val afterUntap = advanceStep(newState.copy(step = Step.UNTAP))
                return afterUntap.copy(events = events + afterUntap.events)
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
                // The combat phase has now ended: remove every creature from combat (clear
                // attacking/blocking and related components). Deferred from the end of combat step
                // so end-of-combat abilities resolve while their attacking targets are still legal.
                val endCombatResult = combatManager.endCombat(newState)
                if (!endCombatResult.isSuccess) return endCombatResult
                newState = endCombatResult.newState
                events.addAll(endCombatResult.events)

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
                        phase = Phase.POSTCOMBAT_MAIN
                    ).withPriority(activePlayer)
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
                // Each defending player declares blockers for the attackers aimed at them,
                // in APNAP order (CR 509.1 / 101.4). Hand priority to the first defender; as
                // each declares and passes, the priority round walks to the next defender
                // (a defending player who hasn't declared can't pass — see PassPriorityHandler),
                // and the step only advances to combat damage once every defender has declared.
                val firstDefender = com.wingedsheep.engine.mechanics.combat.CombatDefenders
                    .defendingPlayersInApnapOrder(newState).firstOrNull()
                    ?: newState.turnOrder.firstOrNull { it != activePlayer }
                    ?: activePlayer
                newState = newState.withPriority(firstDefender)
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
                // Creatures stay in combat until the combat phase *ends* (cleaned up on entry to
                // POSTCOMBAT_MAIN below), not when the end of combat step begins. This keeps them
                // flagged as attacking while players hold priority here, so abilities that target
                // an attacking creature only during the end of combat step (e.g. Desert) still
                // have legal targets.

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

        // Clean up end-of-turn effects
        var cleanedState = cleanupPhaseManager.cleanupEndOfTurn(state)

        // The turn passes to the next *team* (CR 805.4) — both teammates share one turn, so we
        // advance past the whole active team, not to a teammate. In a non-team game getNextTeam is
        // identical to getNextPlayer.
        var nextPlayer = cleanedState.getNextTeam(currentPlayer)

        // Team-wide skip (CR 805.8): if any member of the side taking the next turn has a skip
        // marker, that turn is skipped. Clear the marker from every such member and move on to the
        // team after. With shared team turns this is the whole next team; in Team vs. Team / non-team
        // games it is just the next player (sharedTurnTeam is a singleton there), so a skip is
        // individual.
        val nextTeam = cleanedState.sharedTurnTeam(nextPlayer)
        if (nextTeam.any { cleanedState.getEntity(it)?.has<SkipNextTurnComponent>() == true }) {
            for (member in nextTeam) {
                cleanedState = cleanedState.updateEntity(member) { it.without<SkipNextTurnComponent>() }
            }
            nextPlayer = cleanedState.getNextTeam(nextPlayer)
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
        // CR 701.15a: goaded designation lasts "until the next turn of the
        // controller of that spell or ability"; same hook as the floating-effect
        // path above so all "until your next turn" semantics share one site.
        val (goadCleanedState, goadEvents) = cleanupPhaseManager.expireGoadedDesignationFor(postUntapState, nextPlayer)
        postUntapState = goadCleanedState

        // Advance to upkeep (this sets priority to the active player)
        val advanceResult = advanceStep(postUntapState)

        return ExecutionResult.success(
            advanceResult.newState,
            turnResult.events + untapResult.events + goadEvents + advanceResult.events
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
            state.isActiveTurnFor(playerId) && // CR 805.5a — either teammate may act on the team's turn
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
