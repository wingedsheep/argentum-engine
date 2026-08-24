package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * The live game's answer to "has this stopped being a game?".
 *
 * A wedged game is not a hypothetical here. The AI picks its move by scoring the position each
 * candidate leads to, and a free ability that resolves back onto the board it started from can
 * outscore passing — from which it outscores passing again, identically, forever
 * ([com.wingedsheep.ai.engine.StateProgress] is the AI-side defence against exactly that, and the
 * reason it exists is that the AI once activated Aphetto Alchemist until the game had to be
 * abandoned). That defence is a heuristic over a *shape* of loop, and new shapes keep arriving —
 * two permanents untapping each other, a decision the engine re-raises, a mandatory trigger that
 * puts the board back. Every one of them ends the same way if nothing else is watching: the
 * WebSocket ping-pong never stops, the session is never swept, the replay log grows without limit,
 * and a tournament round blocks on a match that will never finish.
 *
 * So this is the backstop underneath every one of those: not "is this move good" but "is this game
 * still going anywhere". The rules make the same call — CR 104.4b: a game that enters a loop of
 * mandatory actions with no way to stop **is a draw** — and when the loop contains an *optional*
 * action instead, CR 732.3 requires the looping player to make a different choice, which is the
 * requirement the AI-side guard implements. A draw is therefore the honest outcome when the AI-side
 * guard misses: the position genuinely cannot be resolved by anyone playing it.
 *
 * ## The two shapes, and why one counter can't see both
 *
 * - **Actions apply, the game doesn't move.** Each activation is legal and changes *something*
 *   (a tap, a stack entity, an untap) but the position comes back. [onActionApplied] watches this
 *   with the same clock the arena harness uses: actions since the turn last changed hands.
 * - **Nothing applies at all.** The AI's chosen action is rejected, every safe fallback is
 *   rejected too, and the server re-broadcasts the state so the AI can try again — with the same
 *   state, so it chooses the same rejected action. No action is ever applied, so the counter above
 *   never moves and cannot see this. [onActionRejected] counts those instead, per seat, and gives
 *   up on a seat that has run out of legal moves it is willing to make.
 *
 * Both counters are reset by progress, so a long, healthy game never approaches either.
 *
 * ## Thresholds
 *
 * Deliberately far looser than the arena's ([com.wingedsheep.ai.arena] stops a game after 300
 * actions without a turn handover). A false stall there discards one data point; here it ends a
 * game somebody is playing, so every threshold below sits an order of magnitude above the largest
 * real game we have measured — ~1,650 actions across ~32 player turns for a whole game of random
 * play (`CompactReplaySizeBenchmark`), a few hundred for a normal one.
 *
 * Thread confinement: one instance per [GameSession], only ever touched under that session's state
 * lock. No synchronization of its own.
 *
 * Every threshold is a constructor parameter defaulting to the shipped policy, so a test can reach
 * a backstop in a handful of actions instead of tens of thousands — the numbers below are chosen to
 * be unreachable by anything but a broken game, which also makes them unreachable by a test.
 */
class GameStallGuard(
    private val maxActionsPerTurn: Int = MAX_ACTIONS_PER_TURN,
    private val maxPlayerTurns: Int = MAX_PLAYER_TURNS,
    private val maxActions: Int = MAX_ACTIONS,
    private val maxConsecutiveRejections: Int = MAX_CONSECUTIVE_REJECTIONS,
) {

    /** Total actions applied to this game. */
    private var actions = 0

    /** Who was the active player when the turn last changed hands, and at what action count. */
    private var lastActivePlayer: EntityId? = null
    private var actionsAtHandover = 0

    /** Consecutive rejected actions per seat since that seat last had one applied. */
    private val rejections = mutableMapOf<EntityId, Int>()

    /**
     * Sticky: once a game is called stalled it stays stalled, and it is only reported once.
     *
     * Written under the session's state lock but read off it by the game-over path, which builds the
     * player-facing message — hence volatile.
     */
    @Volatile
    var stall: Stall? = null
        private set

    /**
     * Why a game was abandoned. [code] is for the log (dense, greppable, mirrors the arena's draw
     * taxonomy); [playerMessage] is shown in the game-over overlay, so it says what happened
     * without blaming the player who happened to be holding priority.
     */
    data class Stall(val code: String, val playerMessage: String)

    /**
     * Called after every action the engine has actually applied, with the state it produced.
     * Returns the verdict when this game has to be abandoned, or null to carry on.
     */
    fun onActionApplied(state: GameState): Stall? {
        actions++
        // A turn changing hands is the one signal that is cheap, monotone, and impossible to fake
        // from inside a loop. `GameState.turnNumber` counts player turns, so it advances on every
        // turn regardless of seat count or who has already been eliminated — but the *handover* is
        // what resets the per-turn budget, and reading the active player catches a turn that ends
        // without the counter being what moved.
        if (state.activePlayerId != lastActivePlayer) {
            lastActivePlayer = state.activePlayerId
            actionsAtHandover = actions
        }
        // A seat that just had an action applied is plainly able to act.
        rejections.clear()

        stall?.let { return null } // already reported

        val verdict = when {
            actions - actionsAtHandover > maxActionsPerTurn -> Stall(
                code = "wedged(turn=${state.turnNumber},step=${state.step.name},actions=$actions)",
                playerMessage = "This game was ended as a draw: it repeated more than " +
                    "$maxActionsPerTurn actions in a single turn without the turn ever ending, " +
                    "which means it had entered a loop that could not be broken (CR 104.4b).",
            )

            state.turnNumber > maxPlayerTurns -> Stall(
                code = "turnLimit(turn=${state.turnNumber},actions=$actions)",
                playerMessage = "This game was ended as a draw: it passed $maxPlayerTurns turns " +
                    "with neither side able to finish it.",
            )

            actions > maxActions -> Stall(
                code = "actionLimit(turn=${state.turnNumber},actions=$actions)",
                playerMessage = "This game was ended as a draw: it passed $maxActions actions " +
                    "without reaching a result.",
            )

            else -> null
        }
        return verdict?.also { stall = it }
    }

    /**
     * Called when [playerId]'s chosen action was rejected and no safe fallback could be applied
     * either — nothing reached the game, so [onActionApplied] cannot see this at all.
     *
     * Returns true when this seat has failed often enough in a row that it should be treated as
     * unable to continue. The caller concedes it rather than ending the whole game: one seat with
     * no move it will make is that seat's problem, and conceding is a resolution every format,
     * lobby and stats path already understands.
     */
    fun onActionRejected(playerId: EntityId): Boolean {
        val count = (rejections[playerId] ?: 0) + 1
        rejections[playerId] = count
        return count >= maxConsecutiveRejections
    }

    /** Total actions applied, for logging. */
    fun actionCount(): Int = actions

    companion object {
        /**
         * Actions inside one player turn before the game is called a loop.
         *
         * A whole game of random play is ~1,650 actions over ~32 player turns — ~50 per turn, and
         * that is the noisy end of the range. A real combo turn is in the low hundreds. 2,000 is
         * far enough above both that reaching it means the turn is not going to end.
         */
        const val MAX_ACTIONS_PER_TURN = 2_000

        /**
         * Player turns (not rounds) before a game nobody can close out is called a draw. A duel
         * gets 200 turns each, a four-player pod 100.
         *
         * This is the *other* loop: a soft lock where turns keep passing — two players who can each
         * neutralise the other's every threat — so the per-turn budget above never fires. Tournament
         * Magic reaches the same verdict by a clock rather than a turn count; we have no clock,
         * because a human's thinking time is not the engine's problem and must never end their game.
         */
        const val MAX_PLAYER_TURNS = 400

        /**
         * Total actions in one game, as the backstop under both of the above: 400 turns of 1,999
         * actions each would satisfy them and still be pathological.
         *
         * Deliberately larger than [com.wingedsheep.gameserver.replay.ReplayRecordingPolicy.MAX_RECORDED_ACTIONS]:
         * if a game somehow gets that long we would rather truncate the *record* than end the game
         * somebody is playing, so the recording gives up first and the game keeps going.
         */
        const val MAX_ACTIONS = 50_000

        /**
         * Consecutive rejected-with-no-fallback actions from one seat before it is conceded.
         *
         * Only the AI can reach this: a human's rejected action is an error message and they pick
         * something else, while the AI is handed the same state and deterministically re-chooses
         * the same illegal move. Small on purpose — every iteration is a round trip that produces
         * nothing, and there is no reason to expect the eleventh to differ from the tenth.
         */
        const val MAX_CONSECUTIVE_REJECTIONS = 10
    }
}
