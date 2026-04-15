package com.wingedsheep.engine.gym

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState

/**
 * Strategy for selecting actions and responding to decisions.
 *
 * Implement this to plug custom agents (neural networks, MCTS, scripted bots)
 * into [GameEnvironment.playGame].
 */
interface ActionSelector {
    /**
     * Choose an action from the legal action list.
     *
     * @param state Current game state.
     * @param legalActions All legal actions for this player (pre-filtered to affordable).
     * @return The [GameAction] to submit.
     */
    fun selectAction(state: GameState, legalActions: List<LegalAction>): GameAction

    /**
     * Respond to a pending decision (target selection, yes/no, scry order, etc.).
     *
     * @param state Current game state.
     * @param decision The decision the engine is waiting for.
     * @return A [DecisionResponse] matching the decision type.
     */
    fun respondToDecision(state: GameState, decision: PendingDecision): DecisionResponse
}

/**
 * An [ActionSelector] that picks actions uniformly at random.
 *
 * Useful as a baseline opponent or for random rollouts in MCTS.
 */
class RandomActionSelector(
    private val random: java.util.Random = java.util.Random()
) : ActionSelector {

    override fun selectAction(state: GameState, legalActions: List<LegalAction>): GameAction {
        val affordable = legalActions.filter { it.affordable }
        return affordable[random.nextInt(affordable.size)].action
    }

    override fun respondToDecision(state: GameState, decision: PendingDecision): DecisionResponse {
        // Delegate to the default decision responder (handled by GameEnvironment)
        throw UnsupportedOperationException(
            "RandomActionSelector does not handle decisions — " +
                "GameEnvironment.playGame uses the built-in DecisionResponder as fallback"
        )
    }
}
