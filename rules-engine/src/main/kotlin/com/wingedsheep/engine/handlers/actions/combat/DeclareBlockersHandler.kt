package com.wingedsheep.engine.handlers.actions.combat

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.mechanics.combat.SharedTeamBlockChoices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Step
import kotlin.reflect.KClass

/**
 * Handler for the DeclareBlockers action.
 *
 * Delegates to CombatManager for the actual block declaration. Block triggers are put on the
 * stack by the settle boundary.
 */
class DeclareBlockersHandler(
    private val combatManager: CombatManager
) : ActionHandler<DeclareBlockers> {
    override val actionType: KClass<DeclareBlockers> = DeclareBlockers::class

    override fun validate(state: GameState, action: DeclareBlockers): String? {
        // CR 805.10a — the active team is the attacking team; no member of it blocks.
        if (state.isActiveTurnFor(action.playerId)) {
            return "You cannot declare blockers on your turn"
        }
        if (state.step != Step.DECLARE_BLOCKERS) {
            return "You can only declare blockers during the declare blockers step"
        }
        if (state.getEntity(action.playerId)?.has<BlockersDeclaredThisCombatComponent>() == true) {
            return "Blockers have already been declared"
        }
        // Additional validation is done by CombatManager.
        return SharedTeamBlockChoices.validateInput(state, action.playerId, action.blockers)
    }

    override fun execute(state: GameState, action: DeclareBlockers): ExecutionResult {
        // Block triggers ("whenever this creature blocks") are the settle boundary's job, including
        // those that wait out a block tax's payment question.
        val result = if (com.wingedsheep.engine.mechanics.combat.RandomizedBlockerPiles.isActive(state)) {
            if (action.blockers.isNotEmpty()) return ExecutionResult.error(state, "Choose piles instead of specific blockers")
            combatManager.beginBlockerPiles(state, action.playerId)
        } else SharedTeamBlockChoices.submit(state, action.playerId, action.blockers) { combinedState, combined ->
            combatManager.declareBlockers(combinedState, action.playerId, combined)
        } ?: combatManager.declareBlockers(state, action.playerId, action.blockers)
        if (result.error != null || result.pendingDecision != null ||
            result.events.any { it is com.wingedsheep.engine.core.TeamBlockerChoiceEvent }) return result
        return ExecutionResult.success(handToNextUndeclaredDefender(result.newState), result.events)
    }

    /**
     * CR 802.4 / 509.1: with more than one defending player, each declares blockers in APNAP order
     * and nobody receives priority until every declaration is in. The declare-blockers round is
     * driven by the priority baton, so after one defender declares, the baton goes straight to the
     * next defender who still owes a declaration — not on a lap of the table, where a seat that is
     * not being attacked would get a real priority window (and could Giant Growth the next
     * defender's would-be blocker) before that defender has declared. Once every defender has
     * declared the baton stays with the last declarer, exactly as before.
     */
    private fun handToNextUndeclaredDefender(state: GameState): GameState {
        val next = CombatDefenders.defendingPlayersInApnapOrder(state).firstOrNull { defender ->
            state.getEntity(defender)?.has<BlockersDeclaredThisCombatComponent>() != true
        } ?: return state
        return state.withPriority(next)
    }

    companion object {
        fun create(services: EngineServices): DeclareBlockersHandler {
            return DeclareBlockersHandler(services.combatManager)
        }
    }
}
