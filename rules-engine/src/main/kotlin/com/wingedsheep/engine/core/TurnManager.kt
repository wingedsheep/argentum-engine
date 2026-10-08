package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.effects.copy.copyExpiryEvents
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.sdk.scripting.OptionalSkipTurnWith
import com.wingedsheep.engine.replacement.ActiveReplacements
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.combat.rules.TappedBlockBypass
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.MustAttackPlayerComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.state.components.player.InAdditionalCombatPhaseComponent
import com.wingedsheep.engine.state.components.player.AdditionalUpkeepStepsComponent
import com.wingedsheep.engine.state.components.player.InAdditionalUpkeepStepComponent
import com.wingedsheep.engine.state.components.player.InAdditionalBeginningPhaseComponent
import com.wingedsheep.engine.state.components.player.AdditionalEndStepsComponent
import com.wingedsheep.engine.state.components.player.InAdditionalEndStepComponent
import com.wingedsheep.engine.state.components.player.BendsThisTurnComponent
import com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent
import com.wingedsheep.engine.state.components.player.LandsPlayedThisTurnComponent
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.state.components.player.CardsPutIntoExileThisTurnComponent
import com.wingedsheep.engine.state.components.player.EquipActivationsThisTurnComponent
import com.wingedsheep.engine.state.components.player.ExhaustAbilitiesActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.player.LoyaltyAbilitiesActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.player.ManaSpentOnSpellsThisTurnComponent
import com.wingedsheep.engine.state.components.player.LoseAtEndStepComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent
import com.wingedsheep.engine.state.components.player.SkipCombatPhasesComponent
import com.wingedsheep.engine.state.components.player.SkippedTurnPartsComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.state.components.player.SkipNextUntapStepComponent
import com.wingedsheep.engine.state.components.player.EndTheTurnRequestedComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.mechanics.stack.SpellCounterer
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.HijackScope
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.replacement.ReplacementEffectProcessor
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
 *
 * Delegates domain-specific logic to:
 * - [BeginningPhaseManager] — untap, upkeep, saga lore counters
 * - [DrawPhaseManager] — draw step, draw replacement effects
 * - [CleanupPhaseManager] — cleanup step, end-of-turn expiration
 */
class TurnManager(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry,
    private val combatManager: CombatManager,
    private val sbaChecker: StateBasedActionChecker,
    private val spellCounterer: SpellCounterer,
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    replacementProcessor: ReplacementEffectProcessor,
    private val decisionHandler: DecisionHandler = DecisionHandler()
) {

    val cleanupPhaseManager = CleanupPhaseManager(cardRegistry, decisionHandler, conditionEvaluator = zones.predicateEvaluator.conditions)
    val drawPhaseManager = DrawPhaseManager(cardRegistry, decisionHandler, effectExecutor, replacementProcessor, predicateEvaluator = zones.predicateEvaluator)
    val beginningPhaseManager = BeginningPhaseManager(cardRegistry, decisionHandler, cleanupPhaseManager, zones.predicateEvaluator)

    // ── Delegate methods for external callers ──

    /** Draw cards for a player. Delegates to [DrawPhaseManager]. */
    fun drawCards(
        state: GameState,
        playerId: EntityId,
        count: Int,
        announce: Boolean = true
    ): ExecutionResult = drawPhaseManager.drawCards(state, playerId, count, announce)

    // ── Turn lifecycle ──

    /**
     * Start a new turn for a player.
     */
    fun startTurn(state: GameState, playerId: EntityId): ExecutionResult {
        // [GameState.turnNumber] counts player turns, so every turn that begins gets the next
        // number — a four-player pod's opening round is turns 1..4, and an extra turn (CR 500.7)
        // is a turn of its own. The game's *first* turn doesn't come through here (GameInitializer
        // seeds turnNumber = 1 directly), so this is only ever a transition into a later turn.
        //
        // This used to increment only for `turnOrder.first()`, making it a round counter. That made
        // `turnNumber + 1` mean "next round" rather than "next turn" for delayed triggers, and it
        // froze outright once the opening seat was eliminated — turnOrder keeps eliminated players,
        // so nothing ever matched the boundary again and a pod played on at a fixed turn number.
        val newTurnNumber = state.turnNumber + 1

        var newState = state.copy(
            activePlayerId = playerId,
            turnNumber = newTurnNumber,
            roundNumber = nextRoundNumber(state, playerId),
            phase = Phase.BEGINNING,
            step = Step.UNTAP,
            priorityPlayerId = null, // No priority during untap
            priorityPassedBy = emptySet(),
            // The untap-step day/night check reads the previous turn's active side. Snapshot every
            // member's count before the per-turn counters are reset; shared-team-turn formats need
            // each teammate's individual count, while ordinary formats produce a singleton map.
            previousTurnActivePlayerId = state.activePlayerId,
            previousTurnActiveTeamSpellCounts = state.activePlayerId
                ?.let(state::sharedTurnTeam)
                .orEmpty()
                .associateWith { state.playerSpellsCastThisTurn[it] ?: 0 },
            spellsCastThisTurn = 0,
            playerSpellsCastThisTurn = emptyMap(),
            spellsCastThisTurnByPlayer = emptyMap(),
            pendingSpellCopies = emptyList(),
            pendingUncounterableSpells = emptyList(),
            // "Next spell this turn has affinity" riders are turn-scoped — an unused grant (you
            // attacked with Don & Raph but cast no matching spell) must not leak into a later turn.
            pendingNextSpellAffinities = emptyList(),
            // "The next spell you cast this turn has improvise" riders (Archway of Innovation) too.
            pendingNextSpellKeywords = emptyList(),
            // "The next matching spell you cast this turn can be cast without paying its mana cost"
            // riders (World War Hulk I) are turn-scoped too — an unused free cast must not leak
            // into a later turn.
            pendingFreeCastSpells = emptyList(),
            // "Spells you cast this turn cost {N} less" discounts (Will / Rowan, Scion of …) end
            // with the turn that installed them; "until your next turn" ones (Ral, Leyline
            // Prodigy) survive and expire after their controller's next untap step.
            spellCostReductions = state.spellCostReductions.filter { it.duration != Duration.EndOfTurn },
            spellWarpedThisTurn = false,
            damageCantBePreventedThisTurn = false,
            // Kang the Conqueror's "during that turn, power-up abilities can't be activated" is
            // stamped against a future turn number, so it is *not* cleared at the boundary — only
            // stamps for turns that have already passed are dropped.
            powerUpRestrictedTurns = state.powerUpRestrictedTurns.filter { it >= newTurnNumber }.toSet(),
            nonlandPermanentLeftBattlefieldThisTurn = false,
            permanentsSacrificedThisTurn = 0,
            playersWhoCommittedCrimeThisTurn = emptySet(),
            playersDealtNoncombatDamageLastTurn = state.playersDealtNoncombatDamageThisTurn,
            playersDealtNoncombatDamageThisTurn = emptySet(),
            // "Since your last turn" ends with the outgoing active player's (or team's) own turn;
            // everyone else keeps accumulating across this boundary.
            playersDealtCombatDamageSinceTheirLastTurn = state.playersDealtCombatDamageSinceTheirLastTurn -
                state.activePlayerId?.let(state::sharedTurnTeam).orEmpty(),
            lastCastSpellColors = null,
            lastCardDrawnThisTurnByPlayer = emptyMap(),
            drawStepStartDrawCountByPlayer = emptyMap(),
            // Safety net mirroring CleanupPhaseManager.cleanupEndOfTurn: any end-of-turn /
            // end-of-combat counter-placement modifier still lingering at a turn boundary is
            // dropped. Longer-lived durations (UntilYourNextTurn, Permanent) survive.
            activeCounterPlacementModifiers = state.activeCounterPlacementModifiers.filter { modifier ->
                modifier.duration !is Duration.EndOfTurn &&
                    modifier.duration !is Duration.EndOfCombat
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
                    // Exhaust activations reset each turn (Elvish Refueler's "you haven't
                    // activated an exhaust ability this turn" gate).
                    .with(ExhaustAbilitiesActivatedThisTurnComponent(count = 0))
                    // Loyalty activations reset each turn (Kiora of Salt and Sand's gate).
                    .with(LoyaltyAbilitiesActivatedThisTurnComponent(count = 0))
                    // Cards discarded this turn reset for every player (Mayhem gate + Green Goblin count).
                    .with(CardsDiscardedThisTurnComponent(cardIds = emptyList()))
                    // Lands played this turn (with zone-of-origin) reset (Spider-Man 2099).
                    .with(LandsPlayedThisTurnComponent(fromZones = emptyList()))
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
        // The turn is the whole active team's in a shared team turn (CR 805.4), and a hijack
        // controls the team (CR 805.8), so every member's scheduled hijack engages — the second
        // head is never `playerId` here, and reading only that seat left them un-hijacked.
        for (member in newState.sharedTurnTeam(playerId)) {
            val scheduledHijack = newState.getEntity(member)?.get<PlayerTurnHijackedComponent>() ?: continue
            if (scheduledHijack.state == PlayerTurnHijackedComponent.HijackState.SCHEDULED &&
                scheduledHijack.scope == HijackScope.NextTurn
            ) {
                newState = newState.updateEntity(member) { container ->
                    container.with(
                        scheduledHijack.copy(state = PlayerTurnHijackedComponent.HijackState.ACTIVE)
                    )
                }
                events += TurnHijackedEvent(
                    controllerId = scheduledHijack.controllerId,
                    hijackedPlayerId = member,
                    sourceId = member,
                    sourceName = "Hijack engaged"
                )
            }
        }

        return ExecutionResult.success(ControlHistory.beginTurn(newState), events)
    }

    /**
     * The display-only [GameState.roundNumber] for a turn [next] is about to begin. A new round
     * starts when the turn wraps back around [GameState.turnOrder] — the incoming seat sits at or
     * before the outgoing one. Comparing seats rather than watching for `turnOrder.first()` keeps
     * it advancing after the opening seat is eliminated, and an extra turn for the same player
     * (CR 500.7) stays in the round it was taken in.
     */
    private fun nextRoundNumber(state: GameState, next: EntityId): Int {
        val previous = state.activePlayerId ?: return state.roundNumber + 1
        if (previous == next) return state.roundNumber
        val prevSeat = state.turnOrder.indexOf(previous)
        val nextSeat = state.turnOrder.indexOf(next)
        return if (prevSeat < 0 || nextSeat <= prevSeat) state.roundNumber + 1 else state.roundNumber
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
        var current = state
        // True once an entry has been drained *and skipped*: the queue has been mutated, so the
        // caller's own `state` is stale and returning null (its "nothing drained" signal) would
        // silently lose the consumption.
        var skippedAnEntry = false

        while (true) {
            val queued = current.getEntity(activePlayer)
                ?.get<AdditionalPhasesComponent>()?.phases.orEmpty()
            val next = queued.firstOrNull()
                ?: return if (skippedAnEntry) advanceStepFromEndedStep(current) else null
            val remaining = queued.drop(1)

            var redirectedState = if (remaining.isEmpty()) {
                current.updateEntity(activePlayer) { it.without<AdditionalPhasesComponent>() }
            } else {
                current.updateEntity(activePlayer) { it.with(AdditionalPhasesComponent(remaining)) }
            }

            val (step, phase) = when (next.kind) {
                ExtraPhaseKind.COMBAT -> Step.BEGIN_COMBAT to Phase.COMBAT
                ExtraPhaseKind.MAIN -> Step.POSTCOMBAT_MAIN to Phase.POSTCOMBAT_MAIN
                ExtraPhaseKind.BEGINNING -> Step.UNTAP to Phase.BEGINNING
            }

            // CR 500.11 — an inserted phase whose kind the active player is skipping every
            // instance of this turn (Fatespinner) is proceeded past as though it didn't exist.
            // The queue entry is still consumed: the phase *was* created, it just never happens,
            // and the next queued entry takes its place. Without this the natural combat phase
            // would be skipped while an Aggravated Assault combat phase sailed through.
            if (skipsTurnPart(current, step, activePlayer)) {
                current = redirectedState
                skippedAnEntry = true
                continue
            }

            val events = mutableListOf<GameEvent>()

            // A combat phase that ends here is over, whatever phase follows — even another combat
            // phase (CR 511.3): remove every creature from combat and end "until end of combat"
            // effects, as entering the natural postcombat main phase does.
            if (current.step == Step.END_COMBAT) {
                val closed = closeCombatPhase(redirectedState)
                if (closed.outcome !is Outcome.Done) return closed
                redirectedState = closed.newState
                events.addAll(closed.events)
            }

            redirectedState = redirectedState.updateEntity(activePlayer) { container ->
                val left = container.without<InAdditionalCombatPhaseComponent>()
                    .without<InAdditionalBeginningPhaseComponent>()
                when (next.kind) {
                    // Copy the entry's attacker restriction onto the marker so the declare-attackers
                    // legality check (AdditionalCombatPhaseAttackerRule) can enforce it for the
                    // duration of this inserted phase. `null` yields an ordinary unrestricted combat.
                    ExtraPhaseKind.COMBAT -> left.with(InAdditionalCombatPhaseComponent(next.attackerRestriction))
                    ExtraPhaseKind.MAIN -> left
                    ExtraPhaseKind.BEGINNING -> left.with(InAdditionalBeginningPhaseComponent)
                }
            }

            redirectedState = redirectedState.copy(step = step, phase = phase, priorityPassedBy = emptySet())
            events += PhaseChangedEvent(phase)
            if (step != Step.UNTAP || untapStepSkippers(redirectedState, activePlayer).size < redirectedState.sharedTurnTeam(activePlayer).size) {
                events += StepChangedEvent(step)
            }

            if (next.kind == ExtraPhaseKind.BEGINNING) {
                return runInsertedUntapStep(redirectedState.copy(priorityPlayerId = null), activePlayer, events)
            }
            return ExecutionResult.success(redirectedState.withPriority(activePlayer), events)
        }
    }

    /**
     * The untap step of an inserted beginning phase ([ExtraPhaseKind.BEGINNING]). It is a real untap
     * step — permanents phase and untap, "doesn't untap during its controller's next untap step"
     * effects are satisfied by it — but not a new turn, so "until your next turn" effects stay. No
     * player gets priority; the game moves straight on to the upkeep step.
     */
    private fun runInsertedUntapStep(
        state: GameState,
        activePlayer: EntityId,
        events: List<GameEvent>
    ): ExecutionResult {
        val skippers = untapStepSkippers(state, activePlayer)
        val pendingSkips = pendingUntapSkipsToConsume(state, activePlayer, skippers)
        val untapResult = beginningPhaseManager.performUntapStep(state)
        if (untapResult.error != null) return untapResult
        fun afterUntap(s: GameState) = consumeUntapStepSkips(
            cleanupPhaseManager.expireAffectedControllersNextUntapEffects(s, activePlayer, skippers),
            pendingSkips
        )
        if (untapResult.outcome is Outcome.Paused) {
            val consumed = untapResult.copy(state = afterUntap(untapResult.state))
            return parkRestOfTurn(consumed, state, AdvanceStepContinuation, events + untapResult.events)
        }
        val afterUntapStep = advanceStep(afterUntap(untapResult.newState))
        return afterUntapStep.copy(events = events + untapResult.events + afterUntapStep.events)
    }

    /**
     * The combat phase has ended: remove every creature from combat and drop "this combat" delayed
     * triggers (Goblin Flotilla). Deferred from the end of combat step so end-of-combat abilities
     * resolve while their attacking targets are still legal.
     */
    private fun closeCombatPhase(state: GameState): ExecutionResult {
        val endCombatResult = combatManager.endCombat(state)
        if (endCombatResult.outcome !is Outcome.Done) return endCombatResult
        val newState = endCombatResult.newState.copy(
            delayedTriggers = endCombatResult.newState.delayedTriggers.filter {
                it.expiry !is com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry.EndOfCombat
            }
        )
        return ExecutionResult.success(newState, endCombatResult.events)
    }

    /**
     * Leave an inserted phase whose queue is exhausted for the end step — the turn never falls back
     * into a main phase it didn't add. Clears the inserted-phase markers.
     */
    private fun proceedToEndStepAfterInsertedPhase(state: GameState, activePlayer: EntityId): ExecutionResult {
        val events = mutableListOf<GameEvent>()
        var redirectedState = state
        if (state.step == Step.END_COMBAT) {
            val closed = closeCombatPhase(redirectedState)
            if (closed.outcome !is Outcome.Done) return closed
            redirectedState = closed.newState
            events.addAll(closed.events)
        }
        redirectedState = redirectedState
            .updateEntity(activePlayer) {
                it.without<InAdditionalCombatPhaseComponent>().without<InAdditionalBeginningPhaseComponent>()
            }
            .copy(step = Step.END, phase = Phase.ENDING, priorityPassedBy = emptySet())
        val beforeCopyExpiry = redirectedState
        redirectedState = cleanupPhaseManager.performNextEndStepExpiry(redirectedState)
        events += copyExpiryEvents(beforeCopyExpiry, redirectedState)
        events += PhaseChangedEvent(Phase.ENDING)
        events += StepChangedEvent(Step.END)
        return ExecutionResult.success(redirectedState.withPriority(activePlayer), events)
    }

    /**
     * Advance to the next step.
     * Handles automatic step-based actions and turn transitions.
     */
    fun advanceStep(state: GameState): ExecutionResult =
        // CR 500.5 / 703.4q: as the ending step or phase closes, each player's unspent mana empties
        // (a turn-based action). This is the general per-step/phase emptying — the same action
        // end-of-turn cleanup performs — applying the Upwelling / Ozai / Last Agni Kai statics.
        // Firebending (END_OF_COMBAT) mana is preserved and handled by CombatManager.endCombat;
        // KEPT_UNTIL_END_OF_TURN mana is preserved until end-of-turn cleanup.
        advanceStepFromEndedStep(cleanupPhaseManager.emptyManaPools(state))

    /**
     * True when [step] belongs to a part of the turn [playerId] is skipping every instance of this
     * turn (`SkippedTurnPartsComponent`, written by a this-turn `SkipStepOrPhaseEffect`).
     *
     * One [com.wingedsheep.sdk.core.TurnPart] can cover several steps, which is the point: naming
     * `COMBAT_PHASE` skips all five combat steps one after another as this is consulted per step,
     * and `MAIN_PHASE` skips both main phases (CR 505.1).
     */
    /**
     * Whether [playerId]'s turn skips [step]. CR 805.8 — in a shared team turn a step one teammate
     * is made to skip is skipped by the team, so the marker is read off every member of the active
     * team; outside shared team turns [GameState.sharedTurnTeam] is just the player.
     */
    private fun skipsTurnPart(state: GameState, step: Step, playerId: EntityId): Boolean =
        state.sharedTurnTeam(playerId).any { member ->
            state.getEntity(member)?.get<SkippedTurnPartsComponent>()?.parts
                ?.any { it.covers(step) } == true
        }

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
            // Every member of the active team — a combat hijack controls the team (CR 805.8).
            for (member in state.sharedTurnTeam(activePlayer)) {
                val combatHijack = state.getEntity(member)?.get<PlayerTurnHijackedComponent>()
                if (combatHijack != null &&
                    combatHijack.state == PlayerTurnHijackedComponent.HijackState.ACTIVE &&
                    combatHijack.scope == HijackScope.NextCombatPhase
                ) {
                    state = state.updateEntity(member) { it.without<PlayerTurnHijackedComponent>() }
                }
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

            // Queue exhausted after the last inserted combat phase: end the extra-phase
            // progression at the end step (never a postcombat main).
            return proceedToEndStepAfterInsertedPhase(state, activePlayer)
        }

        // Leaving the draw step of an *inserted* beginning phase (Shadow of the Second Sun). Like an
        // inserted combat phase it must not fall through into the step that normally follows — a
        // precombat main phase — but drains the queue, or proceeds to the end step.
        if (currentStep == Step.DRAW &&
            state.getEntity(activePlayer)?.has<InAdditionalBeginningPhaseComponent>() == true
        ) {
            drainAdditionalPhase(state, activePlayer)?.let { return it }
            return proceedToEndStepAfterInsertedPhase(state, activePlayer)
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
                val beforeCopyExpiry = redirectedState
                redirectedState = cleanupPhaseManager.performNextEndStepExpiry(redirectedState)

                // Phase is unchanged (END and CLEANUP both live in the ending phase), so only the
                // step-changed event is emitted — that re-fires the end-step triggers.
                val events = mutableListOf<GameEvent>(StepChangedEvent(Step.END))
                events += copyExpiryEvents(beforeCopyExpiry, redirectedState)
                redirectedState = redirectedState.withPriority(activePlayer)
                return ExecutionResult.success(redirectedState, events)
            }
        }

        val nextStep = currentStep.next()

        // CR 500.11 / 614.10 — to skip a step or phase is to proceed past it as though it didn't
        // exist. That has to happen *here*, before the StepChangedEvent below is built: that event
        // is what `PassPriorityHandler` feeds to `detectPhaseStepTriggers`, so emitting it for a
        // skipped step would fire "at the beginning of ..." abilities for a step that never
        // happened, and the withPriority calls in the `when` would open a priority window in it.
        // The marker is turn-scoped (Fatespinner skips *each* instance this turn), so it is not
        // consumed here — end-of-turn cleanup drops it.
        if (skipsTurnPart(state, nextStep, activePlayer)) {
            var skipped = state.copy(
                step = nextStep,
                phase = nextStep.phase,
                priorityPassedBy = emptySet()
            )
            val skipEvents = mutableListOf<GameEvent>()
            // The postcombat main phase owns the deferred end-of-combat bookkeeping (see the
            // POSTCOMBAT_MAIN branch below), so skipping the phase must still run it or creatures
            // stay in combat for the rest of the turn.
            if (nextStep == Step.POSTCOMBAT_MAIN) {
                val endCombatResult = combatManager.endCombat(skipped)
                if (endCombatResult.outcome !is Outcome.Done) return endCombatResult
                skipped = endCombatResult.newState
                skipEvents.addAll(endCombatResult.events)
            }
            val onward = advanceStep(skipped)
            return onward.copy(events = skipEvents + onward.events)
        }

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

        if (nextStep != Step.UNTAP || untapStepSkippers(newState, activePlayer).size < newState.sharedTurnTeam(activePlayer).size) {
            events.add(StepChangedEvent(nextStep))
        }

        // Perform automatic step actions
        when (nextStep) {
            Step.UNTAP -> {
                val skippers = untapStepSkippers(newState, activePlayer)
                val pendingSkips = pendingUntapSkipsToConsume(newState, activePlayer, skippers)
                val untapResult = beginningPhaseManager.performUntapStep(newState)
                if (untapResult.error != null) return untapResult
                if (untapResult.outcome is Outcome.Paused) {
                    val consumed = untapResult.copy(state = consumeUntapStepSkips(untapResult.state, pendingSkips))
                    return parkRestOfTurn(consumed, newState, AdvanceStepContinuation, events + untapResult.events)
                }
                newState = consumeUntapStepSkips(untapResult.newState, pendingSkips)
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
                if (drawResult.outcome is Outcome.Paused) {
                    return ExecutionResult.propagatePause(
                        drawResult.state,
                        events + drawResult.events
                    )
                }
                if (drawResult.outcome !is Outcome.Done) return drawResult
                newState = drawResult.newState
                events.addAll(drawResult.events)
                // Check state-based actions after draw (Rule 704.3)
                val sbaResult = sbaChecker.checkAndApply(newState)
                if (sbaResult.outcome is Outcome.Paused) {
                    return ExecutionResult.propagatePause(
                        sbaResult.state,
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
                val endCombatResult = closeCombatPhase(newState)
                if (endCombatResult.outcome !is Outcome.Done) return endCombatResult
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
                // Every member of the active team — a combat hijack controls the team (CR 805.8).
                for (member in newState.sharedTurnTeam(activePlayer)) {
                    val combatHijack = newState.getEntity(member)?.get<PlayerTurnHijackedComponent>() ?: continue
                    if (combatHijack.state == PlayerTurnHijackedComponent.HijackState.SCHEDULED &&
                        combatHijack.scope == HijackScope.NextCombatPhase
                    ) {
                        newState = newState.updateEntity(member) { container ->
                            container.with(
                                combatHijack.copy(state = PlayerTurnHijackedComponent.HijackState.ACTIVE)
                            )
                        }
                        events.add(
                            TurnHijackedEvent(
                                controllerId = combatHijack.controllerId,
                                hijackedPlayerId = member,
                                sourceId = member,
                                sourceName = "Combat hijack engaged"
                            )
                        )
                    }
                }
                newState = newState.withPriority(activePlayer)
            }

            Step.DECLARE_ATTACKERS -> {
                // The step happens even when nothing can attack: CR 508.8 skips only the declare
                // blockers and combat damage steps after an empty declaration, so "at the beginning
                // of the declare attackers step" still triggers and players still get priority here
                // (CR 508.2). With no creature able to attack, the only legal declaration is the
                // empty one, so the turn-based action (CR 508.1) makes it instead of asking. The
                // stamped markers then let the active player pass, and auto-pass carries clients
                // through the window exactly as after a hand-made empty declaration.
                if (!hasValidAttackers(newState, activePlayer)) {
                    val declared = combatManager.declareAttackers(newState, activePlayer, emptyMap())
                    // An empty declaration has no costs to pause on. Should a requirement still
                    // reject it, leave the declaration to the player rather than wedge the turn.
                    if (declared.outcome is Outcome.Done) {
                        newState = declared.newState
                        events.addAll(declared.events)
                    }
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
                val firstDefender = CombatDefenders
                    .defendingPlayersInApnapOrder(newState).firstOrNull()
                    ?: newState.turnOrder.firstOrNull { it != activePlayer }
                    ?: activePlayer
                newState = newState.withPriority(firstDefender)
            }

            Step.FIRST_STRIKE_COMBAT_DAMAGE -> {
                if (!hasAttackingCreatures(newState) || !hasCombatFirstStrikeOrDoubleStrike(newState)) {
                    return advanceStep(newState.copy(step = Step.FIRST_STRIKE_COMBAT_DAMAGE))
                }
                newState = combatManager.stampFirstStrikeStepAssigners(newState)
                val damageResult = combatManager.applyCombatDamage(newState, firstStrike = true)
                if (damageResult.outcome !is Outcome.Done) return damageResult
                newState = damageResult.newState
                events.addAll(damageResult.events)
                val sbaHelperResult = StepActionHelper.applySbasAndCheckGameOver(newState, activePlayer, sbaChecker, events)
                if (sbaHelperResult.outcome is Outcome.Paused) return sbaHelperResult
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
                // CR 510.1 / 510.4: this step's damage is assigned from scratch. Anything a
                // first-strike assignment locked in belongs to the step that just ended — a
                // double striker re-divides among the blockers still blocking it.
                newState = combatManager.clearDamageAssignmentsForNewDamageStep(newState)
                val damageResult = combatManager.applyCombatDamage(newState, firstStrike = false)
                if (damageResult.outcome !is Outcome.Done) return damageResult
                newState = damageResult.newState
                events.addAll(damageResult.events)
                val sbaHelperResult = StepActionHelper.applySbasAndCheckGameOver(newState, activePlayer, sbaChecker, events)
                if (sbaHelperResult.outcome is Outcome.Paused) return sbaHelperResult
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

                // Remove MustAttackPlayerComponent after combat (Taunt effect is consumed). Read
                // off every member of the active team: a shared team turn is either head's turn.
                for (member in newState.sharedTurnTeam(activePlayer)) {
                    val mustAttack = newState.getEntity(member)?.get<MustAttackPlayerComponent>()
                    if (mustAttack != null && mustAttack.activeThisTurn) {
                        newState = newState.updateEntity(member) { container ->
                            container.without<MustAttackPlayerComponent>()
                        }
                    }
                }

                newState = newState.withPriority(activePlayer)
            }

            Step.END -> {
                // "Until the next end step" effects and copies wear off on entry to the end step,
                // alongside the paired "return it at the beginning of the next end step" triggers.
                val beforeCopyExpiry = newState
                newState = cleanupPhaseManager.performNextEndStepExpiry(newState)
                events += copyExpiryEvents(beforeCopyExpiry, newState)

                // "At the beginning of the next end step, you lose the game" (Final Fortune) is
                // keyed to whoever took the extra turn. In a shared team turn (CR 805.8) that is
                // either head, so every member of the active team is checked — not just the seat
                // that happens to be [activePlayer]; outside shared team turns the team is the
                // active player alone.
                for (member in newState.sharedTurnTeam(activePlayer)) {
                    val loseComponent = newState.getEntity(member)?.get<LoseAtEndStepComponent>() ?: continue
                    if (loseComponent.turnsUntilLoss <= 0) {
                        // "You can't lose the game" (Platinum Angel — CR 104.3, and team-wide in
                        // 2HG per CR 810.8a) stops this loss like every other: the delayed
                        // trigger resolves and does nothing, so the marker is still consumed.
                        if (com.wingedsheep.engine.mechanics.sba.player.playerCantLoseGame(newState, member, predicateEvaluator = zones.predicateEvaluator)) {
                            newState = newState.updateEntity(member) { it.without<LoseAtEndStepComponent>() }
                            continue
                        }
                        newState = newState.updateEntity(member) { container ->
                            container.without<LoseAtEndStepComponent>()
                                .with(PlayerLostComponent(LossReason.CARD_EFFECT))
                        }
                        events.add(PlayerLostEvent(member, GameEndReason.CARD_EFFECT, loseComponent.message))
                        val sbaResult = sbaChecker.checkAndApply(newState)
                        if (sbaResult.outcome is Outcome.Paused) {
                            return ExecutionResult.propagatePause(
                                sbaResult.state,
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
                        newState = newState.updateEntity(member) { container ->
                            container.without<LoseAtEndStepComponent>()
                                .with(LoseAtEndStepComponent(loseComponent.turnsUntilLoss - 1, loseComponent.message))
                        }
                    }
                }
                newState = newState.withPriority(activePlayer)
            }

            Step.CLEANUP -> {
                val cleanupResult = cleanupPhaseManager.performCleanupStep(newState)
                if (cleanupResult.error != null) return cleanupResult
                if (cleanupResult.outcome is Outcome.Paused) {
                    return parkRestOfTurn(cleanupResult, newState, AdvanceStepContinuation, events + cleanupResult.events)
                }
                newState = cleanupResult.newState
                events.addAll(cleanupResult.events)

                if (newState.priorityPlayerId == null && newState.pendingDecision == null) {
                    val endTurnResult = endTurn(newState)
                    return endTurnResult.copy(events = events + endTurnResult.events)
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

        cleanedState = cleanedState.copy(priorityPlayerId = null, priorityPassedBy = emptySet())
        val next = selectNextTurn(cleanedState, cleanedState.getNextTeam(currentPlayer))
        return next.copy(events = copyExpiryEvents(state, cleanedState) + next.events)
    }

    /** Walk skipped occurrences without allocating a recursive call per pending skip. */
    fun selectNextTurn(
        state: GameState,
        nextPlayer: EntityId,
        followUps: List<TurnStartFollowUp> = emptyList(),
        previousPlayerId: EntityId = requireNotNull(state.activePlayerId),
    ): ExecutionResult {
        var current = state
        var candidate = nextPlayer
        var previous = previousPlayerId
        var bypassedOrdinarySeats = false
        val events = mutableListOf<GameEvent>()
        while (true) {
            val team = current.sharedTurnTeam(candidate)
            if (team.any { (current.getEntity(it)?.get<SkipNextTurnComponent>()?.extraTurnBypasses ?: 0) > 0 }) {
                current = team.fold(current) { next, member ->
                    next.updateEntity(member) { container ->
                        val skip = container.get<SkipNextTurnComponent>()
                        if (skip == null || skip.extraTurnBypasses == 0) container
                        else if (skip.turns == 1) container.without<SkipNextTurnComponent>()
                        else container.with(skip.copy(turns = skip.turns - 1,
                            extraTurnBypasses = skip.extraTurnBypasses - 1))
                    }
                }
                bypassedOrdinarySeats = true
                candidate = current.getNextTeam(candidate)
                continue
            }
            // Synthetic bypasses insert an extra turn ahead of the ordinary seat walk; departed
            // seats on that walk have not reached their would-be turns yet.
            if (!bypassedOrdinarySeats) {
                val beforeCopyExpiry = current
                current = expireEffectsOfDepartedSeatsWhoseTurnWouldBeginNow(current, previous, candidate)
                events += copyExpiryEvents(beforeCopyExpiry, current)
            }
            bypassedOrdinarySeats = false
            val choices = turnStartChoices(current, team)
            val skips = team.any { current.getEntity(it)?.has<SkipNextTurnComponent>() == true }
            if (choices.isNotEmpty()) {
                // Shared-turn teammates collectively choose the replacement for their side's turn.
                val alternatives = choices.map { it.second }
                val choiceState = current
                val result = choiceState.suspendForDecision(
                    question = { id -> ChooseOptionDecision(id, choices.first().first,
                        "Your turn would begin. Choose whether to skip it.", DecisionContext(),
                        alternatives.map { option ->
                            val source = option.context.sourceId?.takeIf { it in choiceState.getBattlefield() }
                            val name = source?.let {
                                if (choiceState.projectedState.isFaceDown(it)) "face-down permanent"
                                else choiceState.getEntity(it)?.get<CardComponent>()?.name
                            }
                            "Skip this turn — ${name ?: "replacement"}: ${option.effect.description}"
                        } + if (skips) "Skip this turn using the pending skip effect" else "Begin this turn",
                        defaultSearch = if (skips) "Skip this turn using the pending skip effect" else "Begin this turn",
                        optionCardIds = alternatives.mapIndexedNotNull { index, option ->
                            option.context.sourceId?.takeIf { it in choiceState.getBattlefield() }?.let { index to listOf(it) }
                        }.toMap()) },
                    answer = TurnStartReplacementContinuation(candidate, alternatives, followUps)
                )
                return result.copy(events = events + result.events)
            }
            if (!skips) {
                val result = finishTurnSelection(current, candidate, followUps)
                return result.copy(events = events + result.events)
            }
            current = consumePendingTurnSkip(current, team)
            events.add(TurnSkippedEvent(candidate))
            previous = candidate
            candidate = current.getNextTeam(candidate)
        }
    }

    private fun turnStartChoices(state: GameState, team: List<EntityId>): List<Pair<EntityId, TurnStartFollowUp>> {
        val choices = mutableListOf<Pair<EntityId, TurnStartFollowUp>>()
        for (active in ActiveReplacements.all(state)) {
            val printed = active.effect as? OptionalSkipTurnWith ?: continue
            if (!active.granted && Zone.BATTLEFIELD !in printed.activeZones) continue
            if (!active.granted && (state.projectedState.hasLostAllAbilities(active.sourceId) ||
                    state.projectedState.isFaceDown(active.sourceId))) continue
            val text = if (active.granted) null else
                TextChanges.of(state, active.sourceId)
            val replacement = text?.let { printed.applyTextReplacement(it) as OptionalSkipTurnWith }
                ?: printed
            for (member in team) {
                val context = EffectContext(sourceId = active.sourceId, controllerId = active.controllerId,
                    triggeringPlayerId = member,
                    objectReferences = ObjectReferenceEnvironment(
                        captured = true, origin = state.objectRef(active.sourceId), source = state.objectRef(active.sourceId)))
                val players = context.resolvePlayerTargets(
                    EffectTarget.PlayerRef(replacement.appliesTo.player), state)
                if (member !in players || replacement.restrictions.any {
                        !zones.predicateEvaluator.conditions.evaluate(state, it, context)
                    }) continue
                choices.add(member to TurnStartFollowUp(replacement.effect, context))
            }
        }
        return choices
    }

    private fun consumePendingTurnSkip(state: GameState, team: List<EntityId>): GameState =
        team.fold(state) { current, member ->
            current.updateEntity(member) { container ->
                val remaining = container.get<SkipNextTurnComponent>()?.turns ?: 0
                if (remaining > 1) container.with(requireNotNull(container.get<SkipNextTurnComponent>()).copy(turns = remaining - 1))
                else container.without<SkipNextTurnComponent>()
            }
        }

    fun resumeTurnStartReplacement(state: GameState, frame: TurnStartReplacementContinuation,
        response: DecisionResponse): ExecutionResult {
        if (response !is OptionChosenResponse || response.optionIndex !in 0..frame.options.size)
            return ExecutionResult.error(state, "Expected a valid turn replacement choice")
        if (response.optionIndex == frame.options.size)
            return finishTurnSelection(state, frame.nextPlayerId, frame.followUps)
        val chosen = frame.options[response.optionIndex]
        val result = selectNextTurn(state, state.getNextTeam(frame.nextPlayerId), frame.followUps + chosen,
            previousPlayerId = frame.nextPlayerId)
        return result.copy(events = listOf(TurnSkippedEvent(frame.nextPlayerId, chosen.context.sourceId)) + result.events)
    }

    private fun finishTurnSelection(state: GameState, nextPlayer: EntityId,
        followUps: List<TurnStartFollowUp>): ExecutionResult {
        val nextTeam = state.sharedTurnTeam(nextPlayer)
        if (nextTeam.any { state.getEntity(it)?.has<SkipNextTurnComponent>() == true }) {
            val skipped = consumePendingTurnSkip(state, nextTeam)
            val result = selectNextTurn(skipped, skipped.getNextTeam(nextPlayer), followUps,
                previousPlayerId = nextPlayer)
            return result.copy(events = listOf(TurnSkippedEvent(nextPlayer)) + result.events)
        }
        val turnResult = startTurn(state, nextPlayer)
        if (turnResult.outcome !is Outcome.Done) return turnResult
        val result = finishTurnStart(turnResult.state, nextPlayer, followUps)
        return result.copy(events = turnResult.events + result.events)
    }

    /** Skip-then actions are the first work in the next actual turn (CR 614.10b). */
    fun finishTurnStart(state: GameState, playerId: EntityId,
        followUps: List<TurnStartFollowUp>): ExecutionResult {
        var current = state
        val events = mutableListOf<GameEvent>()
        for ((index, followUp) in followUps.withIndex()) {
            val result = effectExecutor(current, followUp.effect, followUp.context.withCurrentObjectReferences(current))
                .toExecutionResult()
            if (result.outcome is Outcome.Paused) return parkRestOfTurn(result, current,
                FinishTurnStartContinuation(playerId, followUps.drop(index + 1)), events + result.events)
            if (result.outcome !is Outcome.Done) return result.copy(events = events + result.events)
            current = result.state
            events.addAll(result.events)
            if (current.gameOver) return ExecutionResult.success(current, events)
        }
        val skippers = untapStepSkippers(current, playerId)
        val pendingSkips = pendingUntapSkipsToConsume(current, playerId, skippers)
        val untapResult = beginningPhaseManager.performUntapStep(current)
        if (untapResult.error != null) return untapResult
        if (untapResult.outcome is Outcome.Paused) {
            return parkRestOfTurn(untapResult, current, FinishUntapStepContinuation(playerId, skippers, pendingSkips), events + untapResult.events)
        }
        val finished = finishUntapStep(untapResult.newState, playerId, skippers, pendingSkips)
        return finished.copy(events = events + untapResult.events + finished.events)
    }

    /**
     * The rest of starting [activePlayer]'s turn once its untap step is over: effects that last
     * "until your next turn" or until that player's next untap step end, goad designations end,
     * and the game advances to the upkeep step. Runs inline from [endTurn], or from a
     * [FinishUntapStepContinuation] when the untap step stopped for a choice.
     */
    fun finishUntapStep(
        state: GameState,
        activePlayer: EntityId,
        skippedUntapStep: Set<EntityId>,
        pendingSkipsToConsume: Set<EntityId>,
    ): ExecutionResult {
        var postUntapState = cleanupPhaseManager.expireUntilYourNextTurnEffects(state, activePlayer)
        postUntapState = cleanupPhaseManager.expireAffectedControllersNextUntapEffects(
            postUntapState, activePlayer, skippedUntapStep
        )
        postUntapState = consumeUntapStepSkips(postUntapState, pendingSkipsToConsume)
        // CR 701.15a: goaded designation lasts "until the next turn of the
        // controller of that spell or ability"; same hook as the floating-effect
        // path above so all "until your next turn" semantics share one site.
        val (goadCleanedState, goadEvents) = cleanupPhaseManager.expireGoadedDesignationFor(postUntapState, activePlayer)
        postUntapState = goadCleanedState

        // Advance to upkeep (this sets priority to the active player)
        val advanceResult = advanceStep(postUntapState)
        return advanceResult.copy(events = copyExpiryEvents(state, postUntapState) + goadEvents + advanceResult.events)
    }

    /** A standing skip affecting any member skips the shared team's step. */
    private fun standingUntapSkip(state: GameState, activePlayer: EntityId): Boolean =
        state.sharedTurnTeam(activePlayer).any { skipsUntapStep(state, cardRegistry, zones.predicateEvaluator, it) }

    /** Players whose untap-dependent durations must wait for an actual untap step. */
    private fun untapStepSkippers(state: GameState, activePlayer: EntityId): Set<EntityId> {
        val team = state.sharedTurnTeam(activePlayer)
        if (standingUntapSkip(state, activePlayer)) return team.toSet()
        return team.filterTo(HashSet()) { state.getEntity(it)?.has<SkipNextUntapStepComponent>() == true }
    }

    /** Capture before phasing/untapping: a source phasing in cannot retroactively skip this step. */
    private fun pendingUntapSkipsToConsume(state: GameState, activePlayer: EntityId, skippers: Set<EntityId>): Set<EntityId> =
        if (standingUntapSkip(state, activePlayer)) emptySet() else skippers

    /** Consume only the one-shot replacements selected before the step's actions. */
    private fun consumeUntapStepSkips(state: GameState, skippers: Set<EntityId>): GameState {
        return skippers.fold(state) { s, player ->
            s.updateEntity(player) { container ->
                val remaining = (container.get<SkipNextUntapStepComponent>()?.steps ?: 1) - 1
                if (remaining > 0) container.with(SkipNextUntapStepComponent(remaining))
                else container.without<SkipNextUntapStepComponent>()
            }
        }
    }

    /**
     * A turn-based action of the current step stopped for a choice. Park [rest] beneath every
     * frame that action pushed, measured from [before], so the choice and anything it chains into
     * finish first. After that, the turn carries on exactly as the unpaused path would have.
     */
    private fun parkRestOfTurn(
        paused: ExecutionResult,
        before: GameState,
        rest: AutomaticContinuation,
        events: List<GameEvent>
    ): ExecutionResult {
        val stack = paused.state.continuationStack
        val at = before.continuationStack.size
        val parked = paused.state.copy(
            continuationStack = stack.subList(0, at) + rest + stack.subList(at, stack.size)
        )
        return ExecutionResult.propagatePause(parked, events)
    }

    /**
     * CR 800.4m: when the turn passes from [from]'s team to [to]'s team, every player who has left
     * the game and sits between them in seat order — plus any departed member of [to]'s own team,
     * whose shared turn is beginning (CR 805.4) — is a player whose next turn "would have begun"
     * right now. Their "until your next turn" / "until your next upkeep" / "until the end of your
     * next turn" effects, their goads and their may-play permissions end here, the same hooks a
     * living player's untap step runs for them. Nothing to do while nobody has left.
     */
    private fun expireEffectsOfDepartedSeatsWhoseTurnWouldBeginNow(
        state: GameState,
        from: EntityId,
        to: EntityId
    ): GameState {
        val order = state.turnOrder
        val departed = order.filter { state.getEntity(it)?.has<PlayerLostComponent>() == true }
        if (departed.isEmpty()) return state

        val fromTeam = state.teamOf(from).toHashSet()
        val toTeam = state.teamOf(to).toHashSet()
        val startIdx = order.indexOf(from)
        if (startIdx < 0) return state
        val passedOver = mutableListOf<EntityId>()
        // Walk the seats after the finished team until the next team is reached.
        for (step in 1 until order.size) {
            val seat = order[(startIdx + step) % order.size]
            if (seat in fromTeam) continue
            if (seat in toTeam) break
            if (seat in departed) passedOver += seat
        }
        // A departed member of the team now taking its turn: their turn is beginning too.
        passedOver += toTeam.filter { it in departed && it !in fromTeam }

        var s = state
        for (leaver in passedOver.distinct()) {
            s = cleanupPhaseManager.expireUntilYourNextTurnEffects(s, leaver)
            s = cleanupPhaseManager.expireUntilYourNextUpkeepEffects(s, leaver)
            s = cleanupPhaseManager.expireGoadedDesignationFor(s, leaver).first
            // "Until the end of your next turn" is keyed to a turn the leaver will never take
            // (a turn-number floor plus a controller guard); CR 800.4m ends it here as well.
            s = s.copy(
                floatingEffects = s.floatingEffects.filterNot { fe ->
                    fe.duration is com.wingedsheep.sdk.scripting.Duration.EndOfYourNextTurn &&
                        fe.controllerId == leaver
                },
                mayPlayPermissions = s.mayPlayPermissions.filterNot { permission ->
                    !permission.permanent && permission.expiresAfterTurn != null &&
                        (permission.expiryControllerId ?: permission.controllerId) == leaver
                }
            )
        }
        return s
    }

    /**
     * Carry out an "end the turn" effect (CR 724.1). Invoked by [Settler] once the spell or ability
     * that requested it (via [EndTheTurnRequestedComponent]) has finished resolving.
     *
     * In order (CR 724.1):
     *  - **a.** triggered abilities waiting to be put on the stack cease to exist. The waiting queue
     *    is cleared, and a [TurnEndedByEffectEvent] tells the settle boundary to ignore every
     *    earlier event;
     *  - **b.** every spell and ability still on the stack is exiled (spells go to exile, abilities
     *    cease to exist), and the source of the effect is exiled too;
     *  - **c.** state-based actions are checked once, and no triggered abilities are put on the stack;
     *  - **d.** all creatures are removed from combat, and the game skips straight to the cleanup
     *    step, which runs as normal: the active player discards down to their maximum hand size,
     *    marked damage wears off, and "until end of turn" / "this turn" effects end. Then the turn
     *    ends into the next turn.
     *
     * Abilities that trigger during this process go on the stack at the next settle, which is after
     * the next turn has begun. CR 724.1f would put them on the stack in an extra cleanup step
     * instead, and the engine doesn't model that.
     *
     * The cleanup and the turn end reuse the machinery of a natural cleanup step. If the active
     * player is over their maximum hand size, this returns paused for the discard, with an
     * [AdvanceStepContinuation] beneath it that ends the turn once the discard resolves.
     */
    fun performEndTheTurn(state: GameState): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        val request = state.getEntity(activePlayer)?.get<EndTheTurnRequestedComponent>()
        var newState = state.updateEntity(activePlayer) { it.without<EndTheTurnRequestedComponent>() }
            .copy(pendingTriggers = emptyList())
        val events = mutableListOf<GameEvent>(TurnEndedByEffectEvent(activePlayer))

        // CR 724.1c: state-based actions are checked. (Creatures destroyed by a preceding board
        // wipe are already handled by that effect; this catches any other pending SBA.)
        val sbaResult = sbaChecker.checkAndApply(newState)
        if (sbaResult.outcome is Outcome.Paused) {
            return ExecutionResult.propagatePause(sbaResult.newState, events + sbaResult.events)
        }
        newState = sbaResult.newState
        events.addAll(sbaResult.events)
        if (newState.gameOver) {
            return ExecutionResult.success(newState.copy(priorityPlayerId = null), events)
        }

        // CR 724.1b: exile every remaining spell and ability on the stack. Snapshot the ids first
        // because exiling mutates the stack.
        for (entityId in newState.stack.toList()) {
            if (entityId !in newState.stack) continue
            val onStack = newState.getEntity(entityId) ?: continue
            val result = if (onStack.has<SpellOnStackComponent>()) {
                spellCounterer.exileSpell(newState, entityId, makePlotted = false)
            } else {
                // Triggered / activated abilities on the stack simply cease to exist.
                spellCounterer.counterAbility(newState, entityId)
            }
            if (result.outcome is Outcome.Done) {
                newState = result.newState
                events.addAll(result.events)
            }
        }

        // CR 724.1b: the source spell/ability is exiled along with the rest of the stack. After its
        // resolution a spell source has been put into its owner's graveyard — move it to exile.
        request?.sourceId?.let { sourceId ->
            val (movedState, moveEvents) = moveSourceToExile(newState, sourceId)
            newState = movedState
            events.addAll(moveEvents)
        }

        // CR 724.1d: remove all creatures from combat.
        if (newState.phase == Phase.COMBAT) {
            val endCombatResult = combatManager.endCombat(newState)
            if (endCombatResult.outcome is Outcome.Done) {
                newState = endCombatResult.newState
                events.addAll(endCombatResult.events)
            }
        }

        // CR 724.1d: skip straight to the cleanup step (bypassing the end step and any queued
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
        if (cleanupResult.outcome is Outcome.Paused) {
            // Over max hand size: pause for the discard. The HandSizeDiscardContinuation finishes the
            // cleanup turn-based actions, then the parked frame advances CLEANUP → next turn.
            return parkRestOfTurn(cleanupResult, newState, AdvanceStepContinuation, events + cleanupResult.events)
        }
        if (cleanupResult.outcome is Outcome.Rejected) return cleanupResult
        newState = cleanupResult.newState
        events.addAll(cleanupResult.events)

        val endTurnResult = endTurn(newState)
        if (endTurnResult.outcome is Outcome.Paused) {
            return ExecutionResult.propagatePause(
                endTurnResult.newState,
                events + endTurnResult.events
            )
        }
        return ExecutionResult.success(endTurnResult.newState, events + endTurnResult.events)
    }

    /**
     * Move an "end the turn" source from its owner's graveyard to exile (CR 724.1b). Best-effort:
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
            ownerId = ownerId,
            oldObject = state.objectRef(sourceId),
            newObject = movedState.objectRef(sourceId)
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
            // CR 805.5a — either teammate may act while their team holds priority, on their
            // team's turn. Both gates collapse to plain equality outside a shared-turns format.
            state.hasPriority(playerId) &&
            state.isActiveTurnFor(playerId) &&
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

            if (TappedBlockBypass.tappedPreventsBlocking(state, entityId, cardRegistry, zones.predicateEvaluator)) {
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
