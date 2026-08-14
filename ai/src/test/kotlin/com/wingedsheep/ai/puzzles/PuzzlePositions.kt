package com.wingedsheep.ai.puzzles

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

/**
 * Advancing a scenario to the exact window a puzzle probes.
 *
 * `TestGame.passUntilPhase` stops at the first priority window of a step regardless of *who* holds
 * priority, which is one window too early for half of these puzzles: a combat trick is cast in the
 * declare-blockers step **after** blocks are in, not in the window where the defender is still
 * being asked to declare them. These two helpers name that difference.
 */

internal fun ScenarioTestBase.TestGame.seatId(seat: Int): EntityId =
    if (seat == 1) player1Id else player2Id

/**
 * Advance until [seat] is being asked to declare attackers or blockers in [step] and has not yet
 * done so — the window a combat puzzle probes.
 */
fun ScenarioTestBase.TestGame.advanceToDeclaration(seat: Int, step: Step): ScenarioTestBase.TestGame =
    advanceUntil("$seat's ${step.displayName} declaration") {
        state.step == step && state.priorityPlayerId == seatId(seat) && needsDeclaration(seatId(seat))
    }

/**
 * Advance until [seat] holds ordinary priority in [step], with every combat declaration already
 * submitted — the window an instant is cast in.
 */
fun ScenarioTestBase.TestGame.advanceToPriority(seat: Int, step: Step): ScenarioTestBase.TestGame =
    advanceUntil("$seat's priority in ${step.displayName}") {
        state.step == step && state.priorityPlayerId == seatId(seat) && !needsDeclaration(seatId(seat))
    }

/**
 * Advance until [seat] holds priority with something still on the stack — the window a response is
 * cast in. The position is expected to have put the spell there already, by casting it from the
 * other seat.
 *
 * Separate from [advanceToPriority] because of the failure mode it has to rule out: passing one
 * time too many resolves the spell, and a puzzle that then asks *"do you counter this?"* about an
 * empty stack scores the AI on a decision that no longer exists. That check is why this cannot
 * just be `advanceUntil { priorityPlayerId == … }`.
 */
fun ScenarioTestBase.TestGame.advanceToStackResponse(seat: Int): ScenarioTestBase.TestGame =
    advanceUntil("$seat's response window") {
        check(state.stack.isNotEmpty()) {
            "The stack emptied before seat $seat was offered a response window — the position " +
                "resolved the spell it meant to ask about"
        }
        state.priorityPlayerId == seatId(seat)
    }

/** Whether [playerId] still owes this combat an attacker or blocker declaration. */
private fun ScenarioTestBase.TestGame.needsDeclaration(playerId: EntityId): Boolean = when {
    state.step == Step.DECLARE_ATTACKERS && playerId == state.activePlayerId ->
        state.getEntity(playerId)?.get<AttackersDeclaredThisCombatComponent>() == null
    state.step == Step.DECLARE_BLOCKERS && playerId != state.activePlayerId ->
        state.getEntity(playerId)?.get<BlockersDeclaredThisCombatComponent>() == null
    else -> false
}

/**
 * Pass priority — auto-submitting empty combat declarations, as `passUntilPhase` does — until
 * [stop] holds. Every failure mode is loud: a puzzle position that silently ends up in the wrong
 * window would score the AI on a decision it was never asked to make.
 */
private fun ScenarioTestBase.TestGame.advanceUntil(
    target: String,
    stop: ScenarioTestBase.TestGame.() -> Boolean,
): ScenarioTestBase.TestGame {
    var iterations = 0
    while (!stop()) {
        check(iterations++ < MAX_ADVANCE_ITERATIONS) {
            "Never reached $target — stalled at ${state.phase}/${state.step}"
        }
        val decision = state.pendingDecision
        check(decision == null) {
            "Puzzle setup paused on ${decision!!::class.simpleName} while advancing to $target. " +
                "Puzzle positions must be quiet; rebuild the board so nothing triggers."
        }
        val priorityPlayer = checkNotNull(state.priorityPlayerId) {
            "No priority player at ${state.phase}/${state.step} while advancing to $target"
        }
        val result = when {
            state.step == Step.DECLARE_ATTACKERS && priorityPlayer == state.activePlayerId &&
                needsDeclaration(priorityPlayer) -> execute(DeclareAttackers(priorityPlayer, emptyMap()))
            state.step == Step.DECLARE_BLOCKERS && priorityPlayer != state.activePlayerId &&
                needsDeclaration(priorityPlayer) -> execute(DeclareBlockers(priorityPlayer, emptyMap()))
            else -> execute(PassPriority(priorityPlayer))
        }
        check(result.error == null) { "Advancing to $target was rejected: ${result.error}" }
    }
    return this
}

private const val MAX_ADVANCE_ITERATIONS = 200
