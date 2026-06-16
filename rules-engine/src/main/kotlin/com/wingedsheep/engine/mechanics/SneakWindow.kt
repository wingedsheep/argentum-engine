package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

/**
 * Sneak timing + cost helper (CR 702.190).
 *
 * "Sneak [cost]" means: *"Any time you could cast an instant during your declare blockers
 * step, you may cast this spell by paying [cost] and returning an unblocked creature you
 * control to its owner's hand rather than paying this spell's mana cost."* (CR 702.190a)
 *
 * This object centralizes the two facts every sneak code path needs so the gnarly combat-state
 * reads live in exactly one place (the legal-action enumerator, the cast handler's validate and
 * execute all consult it):
 *  - [isWindowOpen] — is it legal to *announce* a sneak cast right now, and
 *  - [unblockedAttackers] — which creatures can be returned to pay the sneak cost.
 *
 * Both are pure reads against [GameState]; the controller of each attacker is taken from the
 * projected state (battlefield reads must honor control-changing effects, CR 613).
 */
object SneakWindow {

    /**
     * The window is open for [playerId] when it is the declare blockers step of *their* combat
     * (they are the active player, CR 702.190a "your declare blockers step"), the declare-blockers
     * turn-based action has happened (CR 509.1h assigns blocked/unblocked status only then — before
     * it no attacker is "unblocked"), and they control at least one unblocked attacker to return.
     */
    fun isWindowOpen(state: GameState, playerId: EntityId): Boolean =
        state.step == Step.DECLARE_BLOCKERS &&
            state.activePlayerId == playerId &&
            blockersDeclared(state, playerId) &&
            unblockedAttackers(state, playerId).isNotEmpty()

    /** The defending player has performed the declare-blockers turn-based action this combat. */
    private fun blockersDeclared(state: GameState, playerId: EntityId): Boolean =
        state.getOpponents(playerId).any { defender ->
            state.getEntity(defender)?.get<BlockersDeclaredThisCombatComponent>() != null
        }

    /**
     * Unblocked attackers [playerId] controls — the legal pool for the "return an unblocked
     * creature you control to its owner's hand" portion of a sneak cost. A creature qualifies
     * when it has an [AttackingComponent], no [BlockedComponent], and its projected controller
     * is [playerId]. Blocked status is sticky (CR 509.1h: a creature remains blocked even if
     * all the creatures blocking it are removed from combat), which is exactly what
     * [BlockedComponent] encodes — it survives its blockers leaving combat
     * (see [com.wingedsheep.engine.mechanics.combat.CombatRemovalHelper]), so it must be read
     * here instead of scanning the current blockers.
     */
    fun unblockedAttackers(state: GameState, playerId: EntityId): List<EntityId> {
        val projected = state.projectedState
        return state.getBattlefield().filter { entityId ->
            val entity = state.getEntity(entityId) ?: return@filter false
            entity.get<AttackingComponent>() != null &&
                entity.get<BlockedComponent>() == null &&
                projected.getController(entityId) == playerId
        }
    }
}
