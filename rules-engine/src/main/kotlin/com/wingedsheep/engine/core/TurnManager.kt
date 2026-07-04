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
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.state.components.player.InAdditionalCombatPhaseComponent
import com.wingedsheep.engine.state.components.player.AdditionalUpkeepStepsComponent
import com.wingedsheep.engine.state.components.player.InAdditionalUpkeepStepComponent
import com.wingedsheep.engine.state.components.player.AdditionalEndStepsComponent
import com.wingedsheep.engine.state.components.player.InAdditionalEndStepComponent
import com.wingedsheep.engine.state.components.player.BendsThisTurnComponent
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.state.components.player.CardsPutIntoExileThisTurnComponent
import com.wingedsheep.engine.state.components.player.EquipActivationsThisTurnComponent
import com.wingedsheep.engine.state.components.player.ManaSpentOnSpellsThisTurnComponent
import com.wingedsheep.engine.state.components.player.LoseAtEndStepComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent
import com.wingedsheep.engine.state.components.player.SkipCombatPhasesComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.state.components.player.EndTheTurnRequestedComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.HijackScope

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
            // "Next spell this turn has affinity" riders are turn-scoped — an unused grant (you
            // attacked with Don & Raph but cast no matching spell) must not leak into a later turn.
            pendingNextSpellAffinities = emptyList(),
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
                    .with(CardsPutIntoExileThisTurnComponent(count = 0))
                    .with(ManaSpentOnSpellsThisTurnComponent(totalSpent = 0))
                    .with(EquipActivationsThisTurnComponent(count = 0))
                    // Distinct bends reset each turn for every player ("this turn" is per game-turn).
                    .with(BendsThisTurnComponent(types = emptySet()))
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

        // Activate a Mindslaver-style *turn*-scoped hijack scheduled on this player. Per Scryfall
        // ruling, a scheduled hijack waits through any skipped turns and engages on the next turn
        // the affected player actually takes. Combat-phase-scoped hijacks (Secret of Bloodbending)
        // are ignored here — they engage at beginning of combat (see advanceStep) instead.
        val scheduledHijack = newState.getEntity(playerId)?.get<PlayerTurnHijackedComponent>()
        if (scheduledHijack != null &&
            scheduledHijack.state == PlayerTurnHijackedComponent.HijackState.SCHEDULED &&
            scheduledHijack.scope == HijackScope.NextTurn
        ) {
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
     * Pop the next entry from the active player's [AdditionalPhasesComponent] queue (CR 500.8) and
     * redirect the turn into that phase, or return `null` if the queue is empty. A COMBAT entry
     * re-enters the combat phase at its begin-combat step and sets [InAdditionalCombatPhaseComponent]
     * so the end-of-combat advance drains the queue again instead of falling into a postcombat main
     * phase; a MAIN entry re-enters a fresh postcombat main phase and clears that marker. The active
     * player gets priority in the new phase.
     */
    private fun drainAdditionalPhase(state: GameState, activePlayer: EntityId): ExecutionResult? {
        val component = state.getEntity(activePlayer)?.get<AdditionalPhasesComponent>() ?: return null
        val next = component.phases.firstOrNull() ?: return null
        val remaining = component.phases.drop(1)

        var redirectedState = if (remaining.isEmpty()) {
            state.updateEntity(activePlayer) { it.without<AdditionalPhasesComponent>() }
        } else {
            state.updateEntity(activePlayer) { it.with(AdditionalPhasesComponent(remaining)) }
        }

        val (step, phase) = when (next.kind) {
            ExtraPhaseKind.COMBAT -> {
                // Copy the entry's attacker restriction onto the marker so the declare-attackers
                // legality check (AdditionalCombatPhaseAttackerRule) can enforce it for the
                // duration of this inserted phase. `null` yields an ordinary unrestricted combat.
                redirectedState = redirectedState
                    .updateEntity(activePlayer) {
                        it.with(InAdditionalCombatPhaseComponent(next.attackerRestriction))
                    }
                Step.BEGIN_COMBAT to Phase.COMBAT
            }
            ExtraPhaseKind.MAIN -> {
                redirectedState = redirectedState
                    .updateEntity(activePlayer) { it.without<InAdditionalCombatPhaseComponent>() }
                Step.POSTCOMBAT_MAIN to Phase.POSTCOMBAT_MAIN
            }
        }

        redirectedState = redirectedState
            .copy(step = step, phase = phase, priorityPassedBy = emptySet())
            .withPriority(activePlayer)

        val events = mutableListOf<GameEvent>(
            PhaseChangedEvent(phase),
            StepChangedEvent(step)
        )
        return ExecutionResult.success(redirectedState, events)
    }

    /**
     * Advance to the next step.
     * Handles automatic step-based actions and turn transitions.
     */
    fun advanceStep(state: GameState): ExecutionResult =
        // CR 500.5 / 703.4q: as the ending step or phase closes, each player's unspent mana empties
        // (a turn-based action). This is the general per-step/phase emptying — the same action
        // end-of-turn cleanup performs — applying the Upwelling / Ozai / Last Agni Kai statics.
        // Firebending (END_OF_COMBAT) mana is preserved and handled by CombatManager.endCombat.
        advanceStepFromEndedStep(cleanupPhaseManager.emptyManaPools(state))

    private fun advanceStepFromEndedStep(incomingState: GameState): ExecutionResult {
        val currentStep = incomingState.step
        val activePlayer = incomingState.activePlayerId
            ?: return ExecutionResult.error(incomingState, "No active player")

        // End a combat-phase-scoped hijack (Secret of Bloodbending) as its combat phase closes:
        // the affected player is leaving their end-of-combat step, so input authority reverts.
        // Done here (rather than on entry to postcombat main) so it also fires when an *additional*
        // combat phase follows — "their next combat phase" is a single phase, never the extra ones.
        var state = incomingState
        if (currentStep == Step.END_COMBAT) {
            val combatHijack = state.getEntity(activePlayer)?.get<PlayerTurnHijackedComponent>()
            if (combatHijack != null &&
                combatHijack.state == PlayerTurnHijackedComponent.HijackState.ACTIVE &&
                combatHijack.scope == HijackScope.NextCombatPhase
            ) {
                state = state.updateEntity(activePlayer) { it.without<PlayerTurnHijackedComponent>() }
            }
        }

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

        // Leaving an *inserted* extra combat phase (Aurelia / Fear of Missing Out / the combat half
        // of Aggravated Assault). The InAdditionalCombatPhaseComponent marker distinguishes these
        // from the natural combat phase: an extra combat phase must NOT fall through into a
        // postcombat main phase (Step.next() of END_COMBAT) — a trailing main phase only exists when
        // an explicit AddMainPhaseEffect queued one. So we drain the queue here instead: the next
        // queued phase begins, or if the queue is empty the turn proceeds to the end step.
        if (currentStep == Step.END_COMBAT &&
            state.getEntity(activePlayer)?.has<InAdditionalCombatPhaseComponent>() == true
        ) {
            drainAdditionalPhase(state, activePlayer)?.let { return it }

            // Queue exhausted after the last inserted combat phase: clear the marker and end the
            // extra-phase progression at the end step (never a postcombat main).
            var redirectedState = state
                .updateEntity(activePlayer) { it.without<InAdditionalCombatPhaseComponent>() }
                .copy(step = Step.END, phase = Phase.ENDING, priorityPassedBy = emptySet())
            redirectedState = cleanupPhaseManager.performNextEndStepExpiry(redirectedState)
            val events = mutableListOf<GameEvent>(
                PhaseChangedEvent(Phase.ENDING),
                StepChangedEvent(Step.END)
            )
            redirectedState = redirectedState.withPriority(activePlayer)
            return ExecutionResult.success(redirectedState, events)
        }

        // Check for additional phases queued after the postcombat main phase (Aggravated Assault,
        // Aurelia, Fear of Missing Out, …). CR 500.8: extra phases are inserted after the specified
        // phase; the engine inserts them after the postcombat main phase, draining the queue one
        // entry at a time (combat phases occur before any extra upkeep steps — see below).
        if (currentStep == Step.POSTCOMBAT_MAIN) {
            drainAdditionalPhase(state, activePlayer)?.let { return it }

            // No more additional combat/main phases — now drain any additional upkeep steps
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

        // Drain any additional end steps (Y'shtola Rhul). Per CR 500.9 an "additional end step
        // after this step" is inserted directly after the current end step; each is a full end step
        // (CR 513) where the active player gets priority and "at the beginning of the end step"
        // abilities trigger again. So instead of advancing from the end step to the cleanup step, we
        // re-enter a fresh end step, decrementing the count, until it's exhausted. The
        // InAdditionalEndStepComponent marker (set on the first redirect, cleared at end-of-turn
        // cleanup) lets IsFirstEndStepOfTurn distinguish these extra steps from the natural one, so
        // the rider that created them doesn't loop.
        if (currentStep == Step.END) {
            val additionalEndSteps = state.getEntity(activePlayer)?.get<AdditionalEndStepsComponent>()
            if (additionalEndSteps != null && additionalEndSteps.count > 0) {
                var redirectedState = if (additionalEndSteps.count <= 1) {
                    state.updateEntity(activePlayer) { it.without<AdditionalEndStepsComponent>() }
                } else {
                    state.updateEntity(activePlayer) { container ->
                        container.with(AdditionalEndStepsComponent(additionalEndSteps.count - 1))
                    }
                }

                redirectedState = redirectedState
                    .updateEntity(activePlayer) { it.with(InAdditionalEndStepComponent) }
                    .copy(
                        step = Step.END,
                        phase = Phase.ENDING,
                        priorityPassedBy = emptySet()
                    )

                // An "until the next end step" effect created during the previous end step wears
                // off now, on entry to this additional one (CR 500.9).
                redirectedState = cleanupPhaseManager.performNextEndStepExpiry(redirectedState)

                // Phase is unchanged (END and CLEANUP both live in the ending phase), so only the
                // step-changed event is emitted — that re-fires the end-step triggers.
                val events = mutableListOf<GameEvent>(StepChangedEvent(Step.END))
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
                // Expire "until your next upkeep" effects controlled by the active player at the
                // beginning of their upkeep (before upkeep triggers resolve, so a re-applying
                // upkeep trigger like Erhnam Djinn's re-grants afterward). Xenic Poltergeist's
                // dynamic animate reverts here.
                newState = cleanupPhaseManager.expireUntilYourNextUpkeepEffects(newState, activePlayer)
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
                // Engage a combat-phase-scoped hijack (Secret of Bloodbending) scheduled on the
                // active player: their combat phase is now beginning, so input authority moves to
                // the hijacker for the duration of this phase. A hijack scheduled while combat was
                // skipped stays SCHEDULED and waits for a combat phase they actually reach here.
                val combatHijack = newState.getEntity(activePlayer)?.get<PlayerTurnHijackedComponent>()
                if (combatHijack != null &&
                    combatHijack.state == PlayerTurnHijackedComponent.HijackState.SCHEDULED &&
                    combatHijack.scope == HijackScope.NextCombatPhase
                ) {
                    newState = newState.updateEntity(activePlayer) { container ->
                        container.with(
                            combatHijack.copy(state = PlayerTurnHijackedComponent.HijackState.ACTIVE)
                        )
                    }
                    events.add(
                        TurnHijackedEvent(
                            controllerId = combatHijack.controllerId,
                            hijackedPlayerId = activePlayer,
                            sourceId = activePlayer,
                            sourceName = "Combat hijack engaged"
                        )
                    )
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
                // "Until the next end step" effects and copies wear off on entry to the end step,
                // alongside the paired "return it at the beginning of the next end step" triggers.
                newState = cleanupPhaseManager.performNextEndStepExpiry(newState)

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
            // Consume one skipped turn per affected member: decrement the remaining count and
            // remove the component once it reaches zero (Ral Zarek can stack several). One turn
            // is skipped per endTurn call; multi-turn skips persist across successive turns.
            for (member in nextTeam) {
                cleanedState = cleanedState.updateEntity(member) { container ->
                    val remaining = container.get<SkipNextTurnComponent>()?.turns ?: 0
                    if (remaining > 1) container.with(SkipNextTurnComponent(remaining - 1))
                    else container.without<SkipNextTurnComponent>()
                }
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
     * Carry out an "end the turn" effect (CR 720). Invoked by
     * [com.wingedsheep.engine.handlers.actions.priority.PassPriorityHandler] once the spell or
     * ability that requested it (via [EndTheTurnRequestedComponent]) has finished resolving.
     *
     * In order (CR 720.1):
     *  - **a.** every spell and ability still on the stack is exiled — spells go to exile, abilities
     *    cease to exist — and the source of the effect is exiled too;
     *  - **c.** state-based actions are checked once; no triggered abilities are put on the stack —
     *    the caller drops the triggers detected from the resolution/board-wipe events and diverts
     *    here instead of processing them;
     *  - **b.** all creatures and players are removed from combat;
     *  - **d.** the game skips straight to the cleanup step, which runs as normal (CR 720.2): the
     *    active player discards down to their maximum hand size, marked damage wears off, and
     *    "until end of turn" / "this turn" effects end — then the turn ends into the next turn.
     *
     * The cleanup + turn-end reuse the same machinery as a natural cleanup step, so if the active
     * player is over their maximum hand size this returns paused for the discard, and the game
     * advances CLEANUP → next turn once the discard resolves (mirrors [advanceStep]'s CLEANUP path).
     */
    fun performEndTheTurn(state: GameState): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        val request = state.getEntity(activePlayer)?.get<EndTheTurnRequestedComponent>()
        var newState = state.updateEntity(activePlayer) { it.without<EndTheTurnRequestedComponent>() }
        val events = mutableListOf<GameEvent>()

        // CR 720.1c: state-based actions are checked. (Creatures destroyed by a preceding board
        // wipe are already handled by that effect; this catches any other pending SBA.)
        val sbaResult = sbaChecker.checkAndApply(newState)
        if (sbaResult.isPaused) {
            return ExecutionResult.paused(sbaResult.newState, sbaResult.pendingDecision!!, events + sbaResult.events)
        }
        newState = sbaResult.newState
        events.addAll(sbaResult.events)
        if (newState.gameOver) {
            return ExecutionResult.success(newState.copy(priorityPlayerId = null), events)
        }

        // CR 720.1a: exile every remaining spell and ability on the stack. Snapshot the ids first
        // because exiling mutates the stack.
        val resolver = StackResolver(cardRegistry = cardRegistry)
        for (entityId in newState.stack.toList()) {
            if (entityId !in newState.stack) continue
            val onStack = newState.getEntity(entityId) ?: continue
            val result = if (onStack.has<SpellOnStackComponent>()) {
                resolver.exileSpell(newState, entityId, makePlotted = false)
            } else {
                // Triggered / activated abilities on the stack simply cease to exist.
                resolver.counterAbility(newState, entityId)
            }
            if (result.isSuccess) {
                newState = result.newState
                events.addAll(result.events)
            }
        }

        // CR 720.1a: the source spell/ability is exiled along with the rest of the stack. After its
        // resolution a spell source has been put into its owner's graveyard — move it to exile.
        request?.sourceId?.let { sourceId ->
            val (movedState, moveEvents) = moveSourceToExile(newState, sourceId)
            newState = movedState
            events.addAll(moveEvents)
        }

        // CR 720.1b: remove all creatures and players from combat.
        if (newState.phase == Phase.COMBAT) {
            val endCombatResult = combatManager.endCombat(newState)
            if (endCombatResult.isSuccess) {
                newState = endCombatResult.newState
                events.addAll(endCombatResult.events)
            }
        }

        // CR 720.1d / 720.2: skip straight to the cleanup step (bypassing the end step and any queued
        // additional end steps), run its turn-based actions, then end the turn. Mirrors the
        // Step.CLEANUP branch of [advanceStep] so hand-size discard and end-of-turn cleanup behave
        // identically.
        newState = newState.copy(
            step = Step.CLEANUP,
            phase = Phase.ENDING,
            priorityPlayerId = null,
            priorityPassedBy = emptySet()
        )
        events.add(PhaseChangedEvent(Phase.ENDING))
        events.add(StepChangedEvent(Step.CLEANUP))

        val cleanupResult = cleanupPhaseManager.performCleanupStep(newState)
        if (cleanupResult.isPaused) {
            // Over max hand size: pause for the discard. The HandSizeDiscardContinuation finishes the
            // cleanup turn-based actions, then the game advances CLEANUP → next turn (advanceStep).
            return ExecutionResult.paused(
                cleanupResult.newState,
                cleanupResult.pendingDecision!!,
                events + cleanupResult.events
            )
        }
        if (cleanupResult.error != null) {
            return ExecutionResult.error(cleanupResult.newState, cleanupResult.error!!)
        }
        newState = cleanupResult.newState
        events.addAll(cleanupResult.events)

        val endTurnResult = endTurn(newState)
        if (endTurnResult.isPaused) {
            return ExecutionResult.paused(
                endTurnResult.newState,
                endTurnResult.pendingDecision!!,
                events + endTurnResult.events
            )
        }
        return ExecutionResult.success(endTurnResult.newState, events + endTurnResult.events)
    }

    /**
     * Move an "end the turn" source from its owner's graveyard to exile (CR 720.1a). Best-effort:
     * if the source is not currently in a graveyard (e.g. it was a token or an ability that already
     * ceased to exist) it is left untouched.
     */
    private fun moveSourceToExile(state: GameState, sourceId: EntityId): Pair<GameState, List<GameEvent>> {
        val currentKey = state.zones.entries.firstOrNull { (_, ids) -> sourceId in ids }?.key
            ?: return state to emptyList()
        if (currentKey.zoneType != Zone.GRAVEYARD) return state to emptyList()
        val card = state.getEntity(sourceId)?.get<CardComponent>()
        val ownerId = card?.ownerId ?: currentKey.ownerId
        val exileKey = ZoneKey(ownerId, Zone.EXILE)
        val movedState = state.moveToZone(sourceId, currentKey, exileKey)
        val event = ZoneChangeEvent(
            entityId = sourceId,
            entityName = card?.name ?: "",
            fromZone = Zone.GRAVEYARD,
            toZone = Zone.EXILE,
            ownerId = ownerId
        )
        return movedState to listOf(event)
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
