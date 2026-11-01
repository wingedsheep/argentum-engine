package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.GrantsDiscardImmunityComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsSacrificeImmunityComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CompelledAction

/**
 * "Spells and abilities your opponents control can't cause you to discard cards or sacrifice
 * permanents" (Sigarda, Host of Herons; Tamiyo, Collector of Tales) — the single read point for
 * [com.wingedsheep.sdk.scripting.OpponentsCantCauseYouTo].
 *
 * A "can't" beats the instruction (CR 101.2), so a protected player's discard or sacrifice simply
 * doesn't happen and an optional one offered by such a source can't be chosen. Every site where a
 * resolving spell or ability makes a player sacrifice (the edict executor, the plain and targeted
 * sacrifice executors, ward and "unless" costs) or discard (the discard move of a resolving
 * pipeline, `discardCards` with a cause, ward and "unless" costs) consults [appliesTo] first.
 * That is what keeps the grant from leaking into the game rules: lethal damage, 0 toughness, the
 * legend rule, destruction and cleanup's hand-size discard aren't a spell or ability compelling
 * anything.
 *
 * The restriction is scoped to *opponents*: a player's own spells and abilities can still make
 * them discard or sacrifice, and [GameState.getOpponents] is team-aware, so a teammate's effect
 * doesn't count as an opponent's either.
 */
object CompelledActionImmunity {

    /**
     * True when [playerId] can't be made to perform [action] by a spell or ability controlled by
     * [effectControllerId].
     *
     * [effectControllerId] must be the controller of the *overall* spell or ability, not the
     * player a per-player iteration is currently bound to — read it as
     * `context.effectControllerId ?: context.controllerId` so a `ForEachPlayer` wrapper (Killing
     * Wave) still reports the caster. A null controller (a synthesized source with no controller)
     * is treated as not-an-opponent, leaving the action to proceed.
     */
    fun appliesTo(
        state: GameState,
        playerId: EntityId,
        effectControllerId: EntityId?,
        action: CompelledAction,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        if (effectControllerId == null || effectControllerId == playerId) return false
        if (effectControllerId !in state.getOpponents(playerId)) return false
        return when (action) {
            CompelledAction.SACRIFICE ->
                ControllerGrants.grantedTo<GrantsSacrificeImmunityComponent>(state, playerId, predicateEvaluator = predicateEvaluator)
            CompelledAction.DISCARD ->
                ControllerGrants.grantedTo<GrantsDiscardImmunityComponent>(state, playerId, predicateEvaluator = predicateEvaluator)
        }
    }
}
