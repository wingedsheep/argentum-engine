package com.wingedsheep.gameserver.priority

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory

/**
 * Arena-style auto-pass manager that implements intelligent priority passing.
 *
 * This follows the 4 Rules of Arena-style Instants:
 *
 * ## Rule 1: The "Meaningful Action" Filter
 * Filters out actions that shouldn't stop the game:
 * - Mana abilities are invisible (don't count for stopping)
 * - Zero-target spells are invisible (if requiresTargets but no validTargets)
 * - Counterspells only visible if stack is non-empty
 *
 * ## Rule 2: The "Opponent's Turn" Compression
 * When it's the opponent's turn:
 * - Upkeep/Draw: AUTO-PASS
 * - Main Phase: AUTO-PASS
 * - Combat (Start/Attackers): STOP (crucial window for tapping/killing attackers)
 * - End Step: ALWAYS STOP (golden rule for control players)
 *
 * ## Rule 3: The "My Turn" Optimization
 * When it's your own turn:
 * - Upkeep/Draw: AUTO-PASS
 * - Combat: STOP (for combat tricks like Giant Growth)
 *
 * ## Rule 4: The Stack Response (Arena-style)
 * - If YOUR spell/ability is on top of the stack: AUTO-PASS (let opponent respond)
 * - If OPPONENT's spell/ability is on top: STOP (you might want to respond)
 */
class AutoPassManager {

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
        legalActions: List<LegalActionInfo>
    ): Boolean {
        // If player doesn't have priority, can't pass
        if (state.priorityPlayerId != playerId) {
            return false
        }

        // If there's a pending decision, never auto-pass
        if (state.pendingDecision != null) {
            return false
        }

        // Rule 4: Stack Response - Check who controls the top of the stack
        // - If YOUR spell/ability is on top → AUTO-PASS (let opponent respond)
        // - If OPPONENT's spell/ability is on top → STOP (you might want to respond)
        if (state.stack.isNotEmpty()) {
            val topOfStack = state.stack.last() // Stack is LIFO, last = top
            val topController = state.getEntity(topOfStack)?.get<ControllerComponent>()?.playerId

            if (topController == playerId) {
                // Our own spell/ability is on top - auto-pass to let opponent respond
                logger.debug("AUTO-PASS: Own spell/ability on top of stack")
                return true
            } else {
                // Opponent's spell/ability is on top - stop to consider response
                logger.debug("STOP: Opponent's spell/ability on stack")
                return false
            }
        }

        // Get meaningful actions (Rule 1)
        val meaningfulActions = getMeaningfulActions(legalActions, state)

        // Determine if this is our turn or opponent's turn
        val isMyTurn = state.activePlayerId == playerId

        // Never auto-pass the active player's own main phases
        if (isMyTurn && (state.step == Step.PRECOMBAT_MAIN || state.step == Step.POSTCOMBAT_MAIN)) {
            logger.debug("STOP: My main phase (always stop)")
            return false
        }

        // If no meaningful actions, auto-pass
        if (meaningfulActions.isEmpty()) {
            logger.debug("AUTO-PASS: No meaningful actions available")
            return true
        }

        return if (isMyTurn) {
            shouldAutoPassOnMyTurn(state.step, meaningfulActions)
        } else {
            shouldAutoPassOnOpponentTurn(state.step, meaningfulActions)
        }
    }

    /**
     * Rule 1: Filter legal actions to only "meaningful" actions that should stop the game.
     */
    private fun getMeaningfulActions(
        legalActions: List<LegalActionInfo>,
        state: GameState
    ): List<LegalActionInfo> {
        return legalActions.filter { action ->
            // PassPriority is never meaningful (it's the default)
            if (action.actionType == "PassPriority") {
                return@filter false
            }

            // Mana abilities are invisible - they don't stop the game
            if (action.isManaAbility) {
                return@filter false
            }

            // DeclareAttackers and DeclareBlockers are always meaningful when available
            if (action.actionType == "DeclareAttackers" || action.actionType == "DeclareBlockers") {
                return@filter true
            }

            // PlayLand is always meaningful when available
            if (action.actionType == "PlayLand") {
                return@filter true
            }

            // For spells that require targets, check if they have valid targets
            if (action.requiresTargets) {
                val hasValidTargets = !action.validTargets.isNullOrEmpty()
                if (!hasValidTargets) {
                    // Zero-target spell - invisible
                    return@filter false
                }
            }

            // ActivateAbility (non-mana) is meaningful
            if (action.actionType == "ActivateAbility") {
                return@filter true
            }

            // CastSpell is meaningful if it passes the above checks
            if (action.actionType == "CastSpell") {
                return@filter true
            }

            // Other actions are meaningful by default
            true
        }
    }

    /**
     * Rule 3: Determine auto-pass behavior on your own turn.
     *
     * - Upkeep/Draw: AUTO-PASS (rarely cast instants in own upkeep)
     * - Main Phases: DON'T auto-pass (want to play spells)
     * - Combat: STOP (for combat tricks)
     * - End Step: DON'T auto-pass (might want to use abilities)
     */
    private fun shouldAutoPassOnMyTurn(
        step: Step,
        meaningfulActions: List<LegalActionInfo>
    ): Boolean {
        return when (step) {
            // Beginning Phase - auto-pass (rarely need to act)
            Step.UPKEEP, Step.DRAW -> {
                logger.debug("AUTO-PASS: My upkeep/draw step")
                true
            }

            // Main Phases - DON'T auto-pass (want to play spells/lands)
            Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN -> {
                logger.debug("STOP: My main phase with meaningful actions")
                false
            }

            // Combat Steps - STOP during declare blockers (for combat tricks after blocks)
            // Begin Combat and Declare Attackers are handled by combat UI
            Step.BEGIN_COMBAT -> {
                // Usually auto-pass to get to attackers quickly
                logger.debug("AUTO-PASS: My begin combat")
                true
            }
            Step.DECLARE_ATTACKERS -> {
                // This step requires declaring attackers - not an auto-pass scenario
                logger.debug("STOP: My declare attackers step")
                false
            }
            Step.DECLARE_BLOCKERS -> {
                // STOP after blocks to use combat tricks
                logger.debug("STOP: My declare blockers step (for combat tricks)")
                false
            }
            Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT -> {
                if (meaningfulActions.isNotEmpty()) {
                    logger.debug("STOP: My combat damage/end combat with meaningful actions")
                    false
                } else {
                    logger.debug("AUTO-PASS: My combat damage/end combat")
                    true
                }
            }

            // End Step - DON'T auto-pass (might want to use abilities)
            Step.END -> {
                logger.debug("STOP: My end step with meaningful actions")
                false
            }

            // Cleanup - no priority normally
            Step.CLEANUP, Step.UNTAP -> {
                logger.debug("AUTO-PASS: Cleanup/Untap (no priority)")
                true
            }
        }
    }

    /**
     * Rule 2: Determine auto-pass behavior on opponent's turn.
     *
     * - Upkeep/Draw: AUTO-PASS (unless manual stop)
     * - Main Phase: AUTO-PASS (wait for stack or combat)
     * - Combat (Start/Attackers): STOP (crucial window!)
     * - Declare Blockers: STOP (for removal/pump before damage)
     * - Combat Damage: AUTO-PASS
     * - End Step: ALWAYS STOP (golden rule)
     */
    private fun shouldAutoPassOnOpponentTurn(
        step: Step,
        meaningfulActions: List<LegalActionInfo>
    ): Boolean {
        return when (step) {
            // Beginning Phase - auto-pass
            Step.UPKEEP, Step.DRAW -> {
                logger.debug("AUTO-PASS: Opponent's upkeep/draw")
                true
            }

            // Main Phases - auto-pass (wait for combat or end step or stack)
            Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN -> {
                logger.debug("AUTO-PASS: Opponent's main phase")
                true
            }

            // Combat - STOP during begin combat and declare attackers IF we have meaningful actions
            // This is the crucial window to tap creatures or kill attackers
            // But if we can't do anything, auto-pass to speed up the game
            Step.BEGIN_COMBAT -> {
                if (meaningfulActions.isEmpty()) {
                    logger.debug("AUTO-PASS: Opponent's begin combat (no meaningful actions)")
                    true
                } else {
                    logger.debug("STOP: Opponent's begin combat (crucial window)")
                    false
                }
            }
            Step.DECLARE_ATTACKERS -> {
                if (meaningfulActions.isEmpty()) {
                    logger.debug("AUTO-PASS: Opponent's declare attackers (no meaningful actions)")
                    true
                } else {
                    logger.debug("STOP: Opponent's declare attackers (crucial window)")
                    false
                }
            }
            Step.DECLARE_BLOCKERS -> {
                // We're the defending player, so we declared blockers
                // STOP for removal/pump before damage
                logger.debug("STOP: My declare blockers step")
                false
            }
            Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT -> {
                if (meaningfulActions.isNotEmpty()) {
                    logger.debug("STOP: Opponent's combat damage/end combat with meaningful actions")
                    false
                } else {
                    logger.debug("AUTO-PASS: Opponent's combat damage/end combat")
                    true
                }
            }

            // End Step - ALWAYS STOP (golden rule)
            // This is when control players cast "end of turn" draw spells
            Step.END -> {
                logger.debug("STOP: Opponent's end step (golden rule)")
                false
            }

            // Cleanup - no priority normally
            Step.CLEANUP, Step.UNTAP -> {
                logger.debug("AUTO-PASS: Cleanup/Untap (no priority)")
                true
            }
        }
    }

    /**
     * Check if the player has any instant-speed responses available.
     * Used for stack response checking.
     */
    fun hasInstantSpeedResponses(legalActions: List<LegalActionInfo>): Boolean {
        return legalActions.any { action ->
            // Exclude PassPriority
            action.actionType != "PassPriority" &&
            // Exclude mana abilities
            !action.isManaAbility &&
            // Must be a spell or ability (not land play or combat action)
            (action.actionType == "CastSpell" || action.actionType == "ActivateAbility") &&
            // Must have valid targets if required
            (!action.requiresTargets || !action.validTargets.isNullOrEmpty())
        }
    }

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
        hasMeaningfulActions: Boolean
    ): String? {
        // If there's something on the stack, passing will resolve it
        if (state.stack.isNotEmpty()) {
            return "Resolve"
        }

        val currentStep = state.step
        val isMyTurn = state.activePlayerId == playerId

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

            // Check if we'd stop at this step
            if (wouldStopAtStep(step, onMyTurn, hasMeaningfulActions)) {
                return formatStopPoint(step, onMyTurn, isMyTurn)
            }
        }

        return null
    }

    /**
     * Determines if the player would stop at a given step (assuming no stack and no pending decision).
     */
    private fun wouldStopAtStep(step: Step, isMyTurn: Boolean, hasMeaningfulActions: Boolean): Boolean {
        return if (isMyTurn) {
            !shouldAutoPassOnMyTurnForStep(step, hasMeaningfulActions)
        } else {
            !shouldAutoPassOnOpponentTurnForStep(step, hasMeaningfulActions)
        }
    }

    /**
     * Simplified version of shouldAutoPassOnMyTurn that doesn't need the full action list.
     */
    private fun shouldAutoPassOnMyTurnForStep(step: Step, hasMeaningfulActions: Boolean): Boolean {
        return when (step) {
            Step.UPKEEP, Step.DRAW -> true
            Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN -> false // Always stop on my main phases
            Step.BEGIN_COMBAT -> true
            Step.DECLARE_ATTACKERS -> false
            Step.DECLARE_BLOCKERS -> false
            Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT -> !hasMeaningfulActions
            Step.END -> false
            Step.CLEANUP, Step.UNTAP -> true
        }
    }

    /**
     * Simplified version of shouldAutoPassOnOpponentTurn that doesn't need the full action list.
     */
    private fun shouldAutoPassOnOpponentTurnForStep(step: Step, hasMeaningfulActions: Boolean): Boolean {
        return when (step) {
            Step.UPKEEP, Step.DRAW -> true
            Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN -> true
            Step.BEGIN_COMBAT -> !hasMeaningfulActions // Auto-pass if no actions
            Step.DECLARE_ATTACKERS -> !hasMeaningfulActions // Auto-pass if no actions
            Step.DECLARE_BLOCKERS -> false
            Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT -> !hasMeaningfulActions
            Step.END -> false // Golden rule
            Step.CLEANUP, Step.UNTAP -> true
        }
    }

    /**
     * Format the stop point as a user-friendly string.
     */
    private fun formatStopPoint(step: Step, willBeMyTurn: Boolean, currentlyMyTurn: Boolean): String {
        // If the turn is changing, show "My turn" or "Opponent's turn"
        if (willBeMyTurn != currentlyMyTurn) {
            return if (willBeMyTurn) "My turn" else "Opponent's turn"
        }

        // Otherwise show the step/phase name
        return when (step) {
            Step.UNTAP -> "Untap"
            Step.UPKEEP -> "Upkeep"
            Step.DRAW -> "Draw"
            Step.PRECOMBAT_MAIN -> "Main"
            Step.BEGIN_COMBAT -> "Combat"
            Step.DECLARE_ATTACKERS -> "Attackers"
            Step.DECLARE_BLOCKERS -> "Blockers"
            Step.FIRST_STRIKE_COMBAT_DAMAGE -> "First Strike"
            Step.COMBAT_DAMAGE -> "Damage"
            Step.END_COMBAT -> "End Combat"
            Step.POSTCOMBAT_MAIN -> "Main 2"
            Step.END -> "End Step"
            Step.CLEANUP -> "Cleanup"
        }
    }
}
