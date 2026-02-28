package com.wingedsheep.gameserver.priority

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.sdk.core.Keyword
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
 * - End Step: STOP if you have instant-speed responses (golden rule for control players)
 *
 * ## Rule 3: The "My Turn" Optimization
 * When it's your own turn:
 * - Upkeep/Draw: AUTO-PASS
 * - Combat: STOP (for combat tricks like Giant Growth)
 *
 * ## Rule 4: The Stack Response
 * - If YOUR spell/ability is on top of the stack: AUTO-PASS (let opponent respond)
 * - If OPPONENT's permanent spell is on top and you have no responses: AUTO-PASS (auto-resolve)
 * - If OPPONENT's non-permanent spell or ability is on top: STOP (so you can see what they're doing)
 */
class AutoPassManager {

    private val logger = LoggerFactory.getLogger(AutoPassManager::class.java)

    companion object {
        /** Combat steps the engine auto-skips when no creatures are attacking (CR 508.8, 510.4, 511.4) */
        private val COMBAT_STEPS_SKIPPED_WITHOUT_ATTACKERS = setOf(
            Step.DECLARE_BLOCKERS,
            Step.FIRST_STRIKE_COMBAT_DAMAGE,
            Step.COMBAT_DAMAGE
        )
    }

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
        // If player doesn't have priority, can't pass
        if (state.priorityPlayerId != playerId) {
            return false
        }

        // If there's a pending decision, never auto-pass
        if (state.pendingDecision != null) {
            return false
        }

        // Get meaningful actions (Rule 1) - needed for stack response check
        val meaningfulActions = getMeaningfulActions(legalActions, state)

        // Rule 4: Stack Response - Check who controls the top of the stack
        // - If YOUR spell/ability is on top → AUTO-PASS (let opponent respond)
        // - If OPPONENT's spell/ability is on top → STOP (so you can see what they're doing)
        if (state.stack.isNotEmpty()) {
            val topOfStack = state.stack.last() // Stack is LIFO, last = top
            val topController = getStackItemController(state, topOfStack)

            if (topController == playerId) {
                // Our own spell/ability is on top - auto-pass to let opponent respond
                logger.debug("AUTO-PASS: Own spell/ability on top of stack")
                return true
            } else {
                // Opponent's item on top
                // In Stops mode: always stop on opponent's stack items (regardless of type)
                if (stopsMode) {
                    logger.debug("STOP (Stops mode): Opponent's spell/ability on stack")
                    return false
                }

                // Auto mode: check what type it is
                val container = state.getEntity(topOfStack)
                val isPermanentSpell = container?.get<SpellOnStackComponent>()?.let {
                    container.get<CardComponent>()?.isPermanent ?: false
                } ?: false

                if (isPermanentSpell) {
                    // Permanent spells (creatures, enchantments, artifacts, planeswalkers):
                    // Auto-pass if we have no meaningful instant-speed responses
                    val hasResponses = meaningfulActions.any {
                        it.actionType == "CastSpell" || it.actionType == "ActivateAbility" || it.actionType == "CycleCard" || it.actionType == "TypecycleCard"
                    }
                    if (!hasResponses) {
                        logger.debug("AUTO-PASS: Opponent's permanent spell on stack, no responses")
                        return true
                    }
                }

                // Non-permanent spells (instants/sorceries) and abilities: always stop
                logger.debug("STOP: Opponent's spell/ability on stack")
                return false
            }
        }

        // Determine if this is our turn or opponent's turn
        val isMyTurn = state.activePlayerId == playerId

        // Check per-step stop overrides (only when stack is empty, which it is at this point)
        val relevantStops = if (isMyTurn) myTurnStops else opponentTurnStops
        if (state.step in relevantStops) {
            logger.debug("STOP: Per-step stop override set for ${state.step}")
            return false
        }

        // Stops mode: stop at combat damage step when creatures are attacking you
        if (stopsMode && !isMyTurn && (state.step == Step.COMBAT_DAMAGE || state.step == Step.FIRST_STRIKE_COMBAT_DAMAGE)) {
            val hasAttackers = state.getBattlefield().any { entityId ->
                state.getEntity(entityId)?.get<AttackingComponent>() != null
            }
            if (hasAttackers) {
                logger.debug("STOP (Stops mode): Combat damage step with attackers")
                return false
            }
        }

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

        // Special handling for DECLARE_BLOCKERS on your own turn.
        // After the opponent declares blockers, you may want to cast combat tricks
        // (like Giant Growth) before damage. On opponent's turn, this is handled
        // by shouldAutoPassOnOpponentTurn which only stops if you have blockers.
        if (state.step == Step.DECLARE_BLOCKERS && isMyTurn) {
            val hasInstantSpeedResponses = meaningfulActions.any { action ->
                (action.actionType == "CastSpell" || action.actionType == "ActivateAbility" || action.actionType == "CycleCard" || action.actionType == "TypecycleCard") &&
                (!action.requiresTargets || !action.validTargets.isNullOrEmpty())
            }

            if (hasInstantSpeedResponses) {
                logger.debug("STOP: Declare blockers step (have instant-speed responses)")
                return false
            }
        }

        // On opponent's declare attackers, auto-pass if they didn't attack — nothing to respond to
        if (!isMyTurn && state.step == Step.DECLARE_ATTACKERS) {
            val hasAttackers = state.getBattlefield().any { entityId ->
                state.getEntity(entityId)?.get<AttackingComponent>() != null
            }
            if (!hasAttackers) {
                logger.debug("AUTO-PASS: Opponent's declare attackers (no attackers declared)")
                return true
            }
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
            // Exception: mana abilities with sacrifice costs (e.g., Skirk Prospector)
            // are meaningful because sacrificing a creature is a significant game action
            if (action.isManaAbility) {
                val hasSacrificeCost = action.additionalCostInfo?.costType == "SacrificePermanent"
                if (!hasSacrificeCost) {
                    return@filter false
                }
            }

            // DeclareAttackers is meaningful if there are valid attackers
            if (action.actionType == "DeclareAttackers") {
                val hasValidAttackers = !action.validAttackers.isNullOrEmpty()
                return@filter hasValidAttackers
            }

            // DeclareBlockers is meaningful only if there are valid blockers
            if (action.actionType == "DeclareBlockers") {
                val hasValidBlockers = !action.validBlockers.isNullOrEmpty()
                return@filter hasValidBlockers
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

            // CastSpell is meaningful only if the player can afford it
            // (Regular spells are only added when affordable, but companion actions
            // for cycling/morph cards can be added with isAffordable=false)
            if (action.actionType == "CastSpell") {
                return@filter action.isAffordable
            }

            // CastFaceDown (morph) is meaningful only if the player can afford it
            if (action.actionType == "CastFaceDown") {
                return@filter action.isAffordable
            }

            // Cycling/typecycling is meaningful only if the player can afford it
            if (action.actionType == "CycleCard" || action.actionType == "TypecycleCard") {
                return@filter action.isAffordable
            }

            // Other actions are meaningful by default
            true
        }
    }

    /**
     * Rule 3: Determine auto-pass behavior on your own turn (TRUE Arena-style).
     *
     * Arena is VERY aggressive about auto-passing on your own turn. You only stop at:
     * - Main phases (to play lands and spells)
     * - Declare attackers (to declare attacks)
     *
     * EVERYTHING ELSE auto-passes, even if you have instant-speed actions available.
     * If you want to act at other times, you need Full Control or manual stops.
     *
     * This matches how Arena actually works - it speeds through your turn so you can
     * play your main phase cards and attack, then quickly passes to opponent's turn.
     */
    private fun shouldAutoPassOnMyTurn(
        step: Step,
        meaningfulActions: List<LegalActionInfo>
    ): Boolean {
        return when (step) {
            // Main Phases - STOP (this is where you play lands and cast spells)
            Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN -> {
                logger.debug("STOP: My main phase")
                false
            }

            // Declare Attackers - STOP only if we still need to declare attacks
            // Once attackers are confirmed, auto-pass to let opponent declare blockers.
            Step.DECLARE_ATTACKERS -> {
                val needsToDeclare = meaningfulActions.any { it.actionType == "DeclareAttackers" }
                if (needsToDeclare) {
                    logger.debug("STOP: My declare attackers step (need to declare)")
                    false
                } else {
                    logger.debug("AUTO-PASS: My declare attackers step (already declared)")
                    true
                }
            }

            // EVERYTHING ELSE - AUTO-PASS (Arena-style aggressive passing)
            // This includes: upkeep, draw, begin combat, declare blockers (after opponent blocks),
            // first strike damage, combat damage, end combat, end step, cleanup
            Step.UPKEEP, Step.DRAW -> {
                logger.debug("AUTO-PASS: My upkeep/draw step")
                true
            }

            Step.BEGIN_COMBAT -> {
                logger.debug("AUTO-PASS: My begin combat")
                true
            }

            Step.DECLARE_BLOCKERS -> {
                val hasResponses = meaningfulActions.any { action ->
                    (action.actionType == "CastSpell" || action.actionType == "ActivateAbility" || action.actionType == "CycleCard" || action.actionType == "TypecycleCard") &&
                    (!action.requiresTargets || !action.validTargets.isNullOrEmpty())
                }
                if (hasResponses) {
                    logger.debug("STOP: My declare blockers step (have instant-speed responses)")
                    false
                } else {
                    logger.debug("AUTO-PASS: My declare blockers step (no responses, moving to damage)")
                    true
                }
            }

            Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT -> {
                logger.debug("AUTO-PASS: My combat damage/end combat")
                true
            }

            Step.END -> {
                logger.debug("AUTO-PASS: My end step")
                true
            }

            Step.CLEANUP, Step.UNTAP -> {
                logger.debug("AUTO-PASS: Cleanup/Untap")
                true
            }
        }
    }

    /**
     * Rule 2: Determine auto-pass behavior on opponent's turn (TRUE Arena-style).
     *
     * Arena is very aggressive about auto-passing on opponent's turn too.
     * You only stop when you actually need to make a blocking decision.
     * Having instants in hand doesn't mean you stop at every phase.
     *
     * - Upkeep/Draw/Main: AUTO-PASS
     * - Begin Combat: AUTO-PASS
     * - Declare Attackers (after declaration): STOP if you have instant-speed responses
     * - Declare Blockers: STOP if you have blockers or instant-speed actions
     * - Combat Damage: AUTO-PASS
     * - End Step: AUTO-PASS (use per-step stop override to hold here)
     */
    private fun shouldAutoPassOnOpponentTurn(
        step: Step,
        meaningfulActions: List<LegalActionInfo>
    ): Boolean {
        // Check if we have blockers available
        val hasBlockers = meaningfulActions.any { it.actionType == "DeclareBlockers" && !it.validBlockers.isNullOrEmpty() }

        // Check if we have instant-speed responses (spells/abilities, not blockers)
        val hasInstantSpeedResponses = meaningfulActions.any { action ->
            (action.actionType == "CastSpell" || action.actionType == "ActivateAbility" || action.actionType == "CycleCard" || action.actionType == "TypecycleCard") &&
            (!action.requiresTargets || !action.validTargets.isNullOrEmpty())
        }

        return when (step) {
            // Beginning Phase - auto-pass
            Step.UPKEEP, Step.DRAW -> {
                logger.debug("AUTO-PASS: Opponent's upkeep/draw")
                true
            }

            // Main Phases - auto-pass (wait for end step if you want to cast instants)
            Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN -> {
                logger.debug("AUTO-PASS: Opponent's main phase")
                true
            }

            // Begin Combat - AUTO-PASS (Arena-style)
            Step.BEGIN_COMBAT -> {
                logger.debug("AUTO-PASS: Opponent's begin combat (Arena-style)")
                true
            }

            // Declare Attackers (after declaration) - STOP if we have instant-speed responses.
            // This is the priority window after attackers are declared (CR 507.4) where
            // the defending player can cast instants/activate abilities before blockers.
            Step.DECLARE_ATTACKERS -> {
                if (hasInstantSpeedResponses) {
                    logger.debug("STOP: Opponent's declare attackers (have instant-speed responses)")
                    false
                } else {
                    logger.debug("AUTO-PASS: Opponent's declare attackers (no responses)")
                    true
                }
            }

            // Declare Blockers - STOP if we have creatures that can block OR
            // instant-speed actions we can actually perform (combat tricks, cycling with mana, etc.)
            // The server only sends legal actions, so having them in meaningfulActions
            // means the player can actually pay for them.
            Step.DECLARE_BLOCKERS -> {
                if (hasBlockers || hasInstantSpeedResponses) {
                    logger.debug("STOP: Opponent's declare blockers (have ${if (hasBlockers) "blockers" else "instant-speed responses"})")
                    false
                } else {
                    logger.debug("AUTO-PASS: Opponent's declare blockers (no blockers or responses)")
                    true
                }
            }

            // Combat Damage steps - AUTO-PASS
            Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT -> {
                logger.debug("AUTO-PASS: Opponent's combat damage/end combat")
                true
            }

            // End Step - STOP if we have instant-speed responses
            Step.END -> {
                if (hasInstantSpeedResponses) {
                    logger.debug("STOP: Opponent's end step (have instant-speed responses)")
                    false
                } else {
                    logger.debug("AUTO-PASS: Opponent's end step (no responses)")
                    true
                }
            }

            // Cleanup - no priority normally
            Step.CLEANUP, Step.UNTAP -> {
                logger.debug("AUTO-PASS: Cleanup/Untap")
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
            // Exclude mana abilities (unless they have a sacrifice cost)
            (!action.isManaAbility || action.additionalCostInfo?.costType == "SacrificePermanent") &&
            // Must be a spell or ability (not land play or combat action)
            (action.actionType == "CastSpell" || action.actionType == "ActivateAbility") &&
            // Must have valid targets if required
            (!action.requiresTargets || !action.validTargets.isNullOrEmpty())
        }
    }

    /**
     * Get the controller of a stack item.
     * Checks various component types since abilities use different components than spells.
     */
    private fun getStackItemController(state: GameState, entityId: EntityId): EntityId? {
        val container = state.getEntity(entityId) ?: return null

        // Check for activated ability
        container.get<ActivatedAbilityOnStackComponent>()?.let {
            return it.controllerId
        }

        // Check for triggered ability
        container.get<TriggeredAbilityOnStackComponent>()?.let {
            return it.controllerId
        }

        // Check for spell (uses casterId)
        container.get<SpellOnStackComponent>()?.let {
            return it.casterId
        }

        // Fall back to ControllerComponent (for permanents that somehow end up here)
        container.get<ControllerComponent>()?.let {
            return it.playerId
        }

        return null
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
        hasMeaningfulActions: Boolean,
        stateProjector: StateProjector? = null,
        myTurnStops: Set<Step> = emptySet(),
        opponentTurnStops: Set<Step> = emptySet(),
        stopsMode: Boolean = false
    ): String? {
        // If there's something on the stack, passing will resolve it
        if (state.stack.isNotEmpty()) {
            return "Resolve"
        }

        val currentStep = state.step
        val isMyTurn = state.activePlayerId == playerId

        // Special combat damage labels when there are attacking creatures
        val hasAttackers = state.getBattlefield().any { entityId ->
            state.getEntity(entityId)?.get<AttackingComponent>() != null
        }

        if (hasAttackers && currentStep == Step.DECLARE_ATTACKERS && isMyTurn) {
            return "To Blockers"
        }

        if (hasAttackers && currentStep == Step.DECLARE_BLOCKERS) {
            return if (hasCombatFirstStrike(state, stateProjector)) {
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
            if (!hasAttackers && step in COMBAT_STEPS_SKIPPED_WITHOUT_ATTACKERS) {
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
     * Simplified version of shouldAutoPassOnMyTurn for calculating next stop point.
     * Arena-style: Only stop at main phases and declare attackers on your own turn.
     */
    private fun shouldAutoPassOnMyTurnForStep(step: Step, hasMeaningfulActions: Boolean): Boolean {
        return when (step) {
            // Only stop at main phases and declare attackers
            Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN -> false
            Step.DECLARE_ATTACKERS -> false
            // Everything else auto-passes
            else -> true
        }
    }

    /**
     * Simplified version of shouldAutoPassOnOpponentTurn for calculating next stop point.
     * Arena-style: Very aggressive auto-passing, only stop at declare blockers, declare attackers
     * (if have responses), and end step.
     */
    private fun shouldAutoPassOnOpponentTurnForStep(step: Step, hasMeaningfulActions: Boolean): Boolean {
        return when (step) {
            // Auto-pass through most phases
            Step.UPKEEP, Step.DRAW -> true
            Step.PRECOMBAT_MAIN, Step.POSTCOMBAT_MAIN -> true
            Step.BEGIN_COMBAT -> true
            Step.DECLARE_ATTACKERS -> !hasMeaningfulActions // Stop if we have responses
            Step.DECLARE_BLOCKERS -> !hasMeaningfulActions // Stop only if we have blockers/responses
            Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT -> true
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

        // END on own turn is "End Turn" (a distinct action feel)
        if (step == Step.END && currentlyMyTurn) {
            return "End Turn"
        }

        // Otherwise show "Pass to <step>"
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
    private fun hasCombatFirstStrike(state: GameState, stateProjector: StateProjector?): Boolean {
        if (stateProjector == null) return false

        val projected = stateProjector.project(state)
        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            val isInCombat = container.get<AttackingComponent>() != null || container.get<BlockingComponent>() != null
            isInCombat && (projected.hasKeyword(entityId, Keyword.FIRST_STRIKE) || projected.hasKeyword(entityId, Keyword.DOUBLE_STRIKE))
        }
    }
}
