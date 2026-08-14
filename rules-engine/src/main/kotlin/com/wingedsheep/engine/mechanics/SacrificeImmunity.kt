package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.GrantsSacrificeImmunityComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * "Spells and abilities your opponents control can't cause you to sacrifice permanents"
 * (Sigarda, Host of Herons) — the single read point for
 * [com.wingedsheep.sdk.scripting.OpponentsCantMakeYouSacrifice].
 *
 * A "can't" beats the instruction (CR 101.2), so a protected player's sacrifice simply doesn't
 * happen and an optional sacrifice offered by such a source can't be chosen. Every sacrifice
 * site — the edict executor, the plain and targeted sacrifice executors, and the ward—sacrifice
 * cost — consults [appliesTo] before moving anything to the graveyard, which is what keeps the
 * grant from leaking into the many other ways a permanent leaves the battlefield (lethal damage,
 * 0 toughness, the legend rule, destruction). None of those are sacrifices.
 *
 * The restriction is scoped to *opponents*: a player's own spells and abilities can still make
 * them sacrifice, and [GameState.getOpponents] is team-aware, so a teammate's effect doesn't
 * count as an opponent's either.
 */
object SacrificeImmunity {

    /**
     * True when [sacrificingPlayerId] can't be made to sacrifice by a spell or ability controlled
     * by [effectControllerId].
     *
     * [effectControllerId] must be the controller of the *overall* spell or ability, not the
     * player a per-player iteration is currently bound to — read it as
     * `context.effectControllerId ?: context.controllerId` so a `ForEachPlayer` wrapper (Killing
     * Wave) still reports the caster. A null controller (a synthesized source with no controller)
     * is treated as not-an-opponent, leaving the sacrifice to proceed.
     */
    fun appliesTo(
        state: GameState,
        sacrificingPlayerId: EntityId,
        effectControllerId: EntityId?
    ): Boolean {
        if (effectControllerId == null || effectControllerId == sacrificingPlayerId) return false
        if (effectControllerId !in state.getOpponents(sacrificingPlayerId)) return false
        return ControllerGrants.grantedTo<GrantsSacrificeImmunityComponent>(state, sacrificingPlayerId)
    }
}
