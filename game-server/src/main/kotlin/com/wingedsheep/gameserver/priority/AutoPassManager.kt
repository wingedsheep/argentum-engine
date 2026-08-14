package com.wingedsheep.gameserver.priority

import com.wingedsheep.engine.legalactions.MeaningfulActionFilter
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.engine.view.asPriorityAction
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory

/**
 * The client-facing half of Arena-style priority passing: *should this player be prompted*, and
 * *where will the game stop next* if they don't act.
 *
 * The rules themselves live in [MeaningfulActionFilter] in the rules engine, because the AI and the
 * gym hold engine `LegalAction`s and never build a [LegalActionInfo]. This class adapts the DTO,
 * delegates the decision, and logs the reason. What stays here is presentation: the "Pass to
 * Combat" button label and the step lookahead that computes it.
 *
 * The rules, in short (full detail on [MeaningfulActionFilter]):
 *
 * 1. **Meaningful-action filter** — mana abilities, zero-target spells and unaffordable casts don't
 *    stop the game.
 * 2. **Opponent's-turn compression** — upkeep/draw pass; main phases, combat windows and the end
 *    step stop when you have an instant-speed response; declare blockers stops when you can block.
 * 3. **Your-turn optimization** — you stop at your main phases and at declare attackers.
 * 4. **Stack response** — your own object on top auto-passes; an opponent's non-permanent spell or
 *    ability stops.
 */
class AutoPassManager(
    /**
     * Used to resolve the actually-cast face of a split-layout spell on the stack (CR 709/715).
     * A null registry disables that resolution and falls back to the base card's type line — fine
     * for unit tests that only exercise single-face spells, but production must supply it so an
     * Omen/Adventure instant face isn't mistaken for the permanent (creature) face.
     */
    private val cardRegistry: CardRegistry? = null
) {

    private val logger = LoggerFactory.getLogger(AutoPassManager::class.java)

    /**
     * Determines if the player with priority should automatically pass.
     *
     * @param state The current game state
     * @param playerId The player who has priority
     * @param legalActions All legal actions available to the player
     * @return true if the player should auto-pass, false if they should be prompted
     */
    fun shouldAutoPass(
        state: GameState,
        playerId: EntityId,
        legalActions: List<LegalActionInfo>,
        myTurnStops: Set<Step> = emptySet(),
        opponentTurnStops: Set<Step> = emptySet(),
        stopsMode: Boolean = false
    ): Boolean {
        val verdict = MeaningfulActionFilter.autoPassVerdict(
            state = state,
            playerId = playerId,
            legalActions = legalActions.map { it.asPriorityAction() },
            myTurnStops = myTurnStops,
            opponentTurnStops = opponentTurnStops,
            stopsMode = stopsMode,
            cardRegistry = cardRegistry,
        )
        logger.debug(verdict.reason)
        return verdict.autoPass
    }

    /**
     * Filter legal actions to only the "meaningful" ones that should stop the game.
     */
    fun getMeaningfulActions(legalActions: List<LegalActionInfo>): List<LegalActionInfo> =
        legalActions.filter { MeaningfulActionFilter.isMeaningful(it.asPriorityAction()) }

    /**
     * Calculates the next step/phase where the game will stop for this player.
     * This is used to show on the Pass button (e.g., "Pass to Combat", "To my turn").
     *
     * @param state The current game state
     * @param playerId The player who has priority
     * @param hasMeaningfulActions Whether the player has meaningful actions available
     * @return A user-friendly string describing the next stop point, or null if unknown
     */
    fun getNextStopPoint(
        state: GameState,
        playerId: EntityId,
        hasMeaningfulActions: Boolean,
        myTurnStops: Set<Step> = emptySet(),
        opponentTurnStops: Set<Step> = emptySet(),
        stopsMode: Boolean = false
    ): String? {
        // If there's something on the stack, passing will resolve it
        if (state.stack.isNotEmpty()) {
            return "Resolve"
        }

        val currentStep = state.step
        // Team-aware (see [MeaningfulActionFilter]): a Two-Headed Giant teammate shares the turn.
        val isMyTurn = state.isActiveTurnFor(playerId)

        // Special combat damage labels when there are attacking creatures
        val hasAttackers = state.getBattlefield().any { entityId ->
            state.getEntity(entityId)?.get<AttackingComponent>() != null
        }

        if (hasAttackers && currentStep == Step.DECLARE_ATTACKERS && isMyTurn) {
            return "To Blockers"
        }

        if (hasAttackers && currentStep == Step.DECLARE_BLOCKERS) {
            return if (hasCombatFirstStrike(state)) {
                "Resolve first strike damage"
            } else {
                "Resolve combat damage"
            }
        }

        if (hasAttackers && currentStep == Step.FIRST_STRIKE_COMBAT_DAMAGE) {
            return "Resolve combat damage"
        }

        // At postcombat main on my turn, passing effectively ends the turn
        if (isMyTurn && currentStep == Step.POSTCOMBAT_MAIN) {
            return "End Turn"
        }

        // Simulate advancing through steps to find where we'll stop
        var step = currentStep
        var onMyTurn = isMyTurn
        var iterations = 0
        val maxIterations = 20 // Prevent infinite loops

        while (iterations < maxIterations) {
            iterations++

            // Advance to next step
            val nextStep = step.next()
            val turnChanged = nextStep == Step.UNTAP && step == Step.CLEANUP

            if (turnChanged) {
                onMyTurn = !onMyTurn
            }
            step = nextStep

            // Skip combat steps that the engine auto-skips when there are no attackers (CR 508.8)
            if (!hasAttackers && step in MeaningfulActionFilter.COMBAT_STEPS_SKIPPED_WITHOUT_ATTACKERS) {
                continue
            }

            // Check if we'd stop at this step
            if (wouldStopAtStep(step, onMyTurn, hasMeaningfulActions, myTurnStops, opponentTurnStops, stopsMode)) {
                return formatStopPoint(step, onMyTurn, isMyTurn)
            }
        }

        return null
    }

    /**
     * Determines if the player would stop at a given step (assuming no stack and no pending decision).
     */
    private fun wouldStopAtStep(step: Step, isMyTurn: Boolean, hasMeaningfulActions: Boolean, myTurnStops: Set<Step> = emptySet(), opponentTurnStops: Set<Step> = emptySet(), stopsMode: Boolean = false): Boolean {
        // Check per-step stop overrides first
        val relevantStops = if (isMyTurn) myTurnStops else opponentTurnStops
        if (step in relevantStops) return true

        // Stops mode: stop at combat damage when being attacked (opponent's turn)
        if (stopsMode && !isMyTurn && (step == Step.COMBAT_DAMAGE || step == Step.FIRST_STRIKE_COMBAT_DAMAGE)) {
            return true
        }

        return if (isMyTurn) {
            !shouldAutoPassOnMyTurnForStep(step, hasMeaningfulActions)
        } else {
            !shouldAutoPassOnOpponentTurnForStep(step, hasMeaningfulActions)
        }
    }

    /**
     * Simplified version of the my-turn rules for calculating the next stop point.
     * Arena-style: Only stop at main phases, declare attackers, and first strike damage
     * (when player has meaningful actions) on your own turn.
     */
    private fun shouldAutoPassOnMyTurnForStep(step: Step, hasMeaningfulActions: Boolean): Boolean {
        return when (step) {
            // Only stop at main phases and declare attackers
            Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN -> false
            Step.DECLARE_ATTACKERS -> false
            // Stop at first strike damage if we have responses
            Step.FIRST_STRIKE_COMBAT_DAMAGE -> !hasMeaningfulActions
            // Everything else auto-passes
            else -> true
        }
    }

    /**
     * Simplified version of the opponent's-turn rules for calculating the next stop point.
     * Arena-style: Very aggressive auto-passing, only stop at declare blockers, declare attackers
     * (if have responses), and end step.
     */
    private fun shouldAutoPassOnOpponentTurnForStep(step: Step, hasMeaningfulActions: Boolean): Boolean {
        return when (step) {
            // Auto-pass through most phases
            Step.UPKEEP, Step.DRAW -> true
            Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN -> !hasMeaningfulActions
            Step.BEGIN_COMBAT -> true
            Step.DECLARE_ATTACKERS -> !hasMeaningfulActions // Stop if we have responses
            Step.DECLARE_BLOCKERS -> !hasMeaningfulActions // Stop only if we have blockers/responses
            Step.FIRST_STRIKE_COMBAT_DAMAGE -> !hasMeaningfulActions // Stop if we have responses after first strike
            Step.COMBAT_DAMAGE -> true
            Step.END_COMBAT -> !hasMeaningfulActions // Stop if we have responses (e.g. Desert's end-of-combat ping)
            Step.END -> !hasMeaningfulActions // Stop if we have responses
            Step.CLEANUP, Step.UNTAP -> true
        }
    }

    /**
     * Format the stop point as a complete button label.
     */
    private fun formatStopPoint(step: Step, willBeMyTurn: Boolean, currentlyMyTurn: Boolean): String {
        // If the turn is changing, show "To my turn" or "To opponent's turn"
        if (willBeMyTurn != currentlyMyTurn) {
            return if (willBeMyTurn) "To my turn" else "To opponent's turn"
        }

        // On opponent's turn, use neutral "Pass" — the player is just yielding priority,
        // not driving the turn forward. Destination-specific labels like "Pass to Attackers"
        // feel misleading when it's not your turn.
        if (!currentlyMyTurn) {
            return "Pass"
        }

        // END on own turn is "End Turn" (a distinct action feel)
        if (step == Step.END) {
            return "End Turn"
        }

        // Own turn: show "Pass to <step>" to indicate where the game is heading
        return when (step) {
            Step.UNTAP -> "Pass to Untap"
            Step.UPKEEP -> "Pass to Upkeep"
            Step.DRAW -> "Pass to Draw"
            Step.PRECOMBAT_MAIN -> "Pass to Main"
            Step.BEGIN_COMBAT -> "Pass to Combat"
            Step.DECLARE_ATTACKERS -> "Pass to Attackers"
            Step.DECLARE_BLOCKERS -> "Pass to Blockers"
            Step.FIRST_STRIKE_COMBAT_DAMAGE -> "Pass to First Strike"
            Step.COMBAT_DAMAGE -> "Pass to Damage"
            Step.END_COMBAT -> "Pass to End Combat"
            Step.POSTCOMBAT_MAIN -> "Pass to Main 2"
            Step.END -> "Pass to End Step"
            Step.CLEANUP -> "Pass to Cleanup"
        }
    }

    /**
     * Check if any attacker or blocker in combat has first strike or double strike.
     */
    private fun hasCombatFirstStrike(state: GameState): Boolean {
        val projected = state.projectedState
        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            val isInCombat = container.get<AttackingComponent>() != null || container.get<BlockingComponent>() != null
            isInCombat && (projected.hasKeyword(entityId, Keyword.FIRST_STRIKE) || projected.hasKeyword(entityId, Keyword.DOUBLE_STRIKE))
        }
    }
}
