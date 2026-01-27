package com.wingedsheep.gameserver.priority

import com.wingedsheep.engine.state.GameState
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
 * ## Rule 4: The Stack Response (Absolute Rule)
 * If the stack is NOT empty and you have a legal response: ALWAYS STOP
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

        // Rule 4: Stack Response - ALWAYS STOP if stack is non-empty and we have responses
        if (state.stack.isNotEmpty()) {
            val meaningfulActions = getMeaningfulActions(legalActions, state)
            if (meaningfulActions.isNotEmpty()) {
                logger.debug("STOP: Stack non-empty with meaningful actions available")
                return false
            }
            // No meaningful responses - auto-pass
            logger.debug("AUTO-PASS: Stack non-empty but no meaningful responses")
            return true
        }

        // Get meaningful actions (Rule 1)
        val meaningfulActions = getMeaningfulActions(legalActions, state)

        // If no meaningful actions, auto-pass
        if (meaningfulActions.isEmpty()) {
            logger.debug("AUTO-PASS: No meaningful actions available")
            return true
        }

        // Determine if this is our turn or opponent's turn
        val isMyTurn = state.activePlayerId == playerId

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

            // Main Phase - auto-pass (wait for combat or stack)
            Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN -> {
                logger.debug("AUTO-PASS: Opponent's main phase")
                true
            }

            // Combat - STOP during begin combat and declare attackers
            // This is the crucial window to tap creatures or kill attackers
            Step.BEGIN_COMBAT -> {
                logger.debug("STOP: Opponent's begin combat (crucial window)")
                false
            }
            Step.DECLARE_ATTACKERS -> {
                logger.debug("STOP: Opponent's declare attackers (crucial window)")
                false
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
}
