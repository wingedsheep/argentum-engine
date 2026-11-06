package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameLossSourceChoiceContinuation
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.mechanics.sba.creature.LethalDamageCheck
import com.wingedsheep.engine.mechanics.sba.creature.ZeroToughnessCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent

/**
 * A game loss replaced in this state-based-action pass ([GameLoss], a
 * [com.wingedsheep.sdk.scripting.ReplaceGameLossWith]) whose source is *also* about to leave the
 * battlefield in the same pass — Exquisite Archangel dealt lethal damage by the same spell that
 * took its controller to 0 life. Both happen as one simultaneous event (CR 704.3), so its
 * controller chooses which moves it: the replacement (exile) or the state-based action (the
 * graveyard). The Archangel's first ruling says exactly that.
 *
 * Runs after the loss checks and before the creature checks, so the replacement's rest is queued
 * but the source hasn't died yet. "Would leave" is a trial of the zero-toughness and lethal-damage
 * checks on this state — regeneration, umbra armor, indestructible and destruction replacements
 * included — so the question is asked only when the source really goes. The answer is recorded on
 * the rider ([GameLossSourceChoiceContinuation]); a source that survives the pass is never asked,
 * and the rest moves it as printed.
 */
class GameLossSourceChoiceCheck(
    private val zones: ZoneTransitionService,
    private val decisionHandler: DecisionHandler
) : StateBasedActionCheck {
    override val name = "Replaced game loss: source leaving in the same pass"
    override val order = SbaOrder.GAME_LOSS_SOURCE_CHOICE

    private val leavesThisPass = listOf(ZeroToughnessCheck(zones), LethalDamageCheck(zones))

    override fun check(state: GameState): ExecutionResult {
        val rider = state.pendingReplacementRiders.firstOrNull {
            it.replacesGameLoss && !it.sourceChoiceMade && it.hostId in state.getBattlefield()
        } ?: return ExecutionResult.success(state)

        var trial = state
        for (check in leavesThisPass) {
            val result = check.check(trial, state)
            if (result.outcome !is Outcome.Done) return ExecutionResult.success(state)
            trial = result.newState
        }
        if (rider.hostId in trial.getBattlefield()) return ExecutionResult.success(state)

        val name = state.getEntity(rider.hostId)?.get<CardComponent>()?.name ?: "this permanent"
        val decision = decisionHandler.createYesNoDecision(
            state = state,
            playerId = rider.controllerId,
            sourceId = rider.hostId,
            sourceName = name,
            prompt = "$name saves you from losing the game, but it is also leaving the battlefield. " +
                "Exile it, or put it into your graveyard?",
            yesText = "Exile",
            noText = "Graveyard",
            phase = DecisionPhase.STATE_BASED,
            answer = GameLossSourceChoiceContinuation(hostId = rider.hostId, subjectId = rider.subjectId),
        )
        return ExecutionResult.propagatePause(decision.state, decision.events)
    }
}
