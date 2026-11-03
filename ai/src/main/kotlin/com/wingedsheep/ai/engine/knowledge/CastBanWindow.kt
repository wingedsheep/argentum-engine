package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect

/**
 * "Does casting this cast ban now deny an opponent a whole turn of spells?" — the one window an
 * [IntentTag.CAST_BAN] spell is worth its card in, read for
 * [com.wingedsheep.ai.engine.AiProfile.castBansDenyTheirTurn].
 *
 * A ban moves no permanent, so the board score sees only a card leaving our hand: Silence scored
 * below passing in every window, Sphinx's Decree never left the hand. What the ban buys is spells
 * the opponent does not cast, which a one-ply leaf cannot see. This names the window where that is
 * most of a turn's worth, so [HoldPolicy] can reward it and `Strategist` can refund the hand charge:
 *
 *  - **"This turn"** ([Duration.EndOfTurn], Silence, Orim's Chant): only cast in an opponent's
 *    upkeep or draw step, before their first main phase — their whole turn of casting still ahead.
 *  - **"During that player's next turn"** (Sphinx's Decree): whenever it is cast, it covers the
 *    opponent's next turn whole.
 *  - **"Until your next turn"** (Inventive Iteration's shape): only when cast on our own turn,
 *    which leaves the opponent's next turn inside it.
 *
 * And only while an opponent the ban reaches holds a card: a ban on an empty hand denies nothing
 * the AI can see. A self-binding ban, an untargeted shape it does not know, or a trigger's ban (a
 * creature's entry is priced by its body) declines.
 */
object CastBanWindow {

    /** Whether casting [definition]'s spell now, as [playerId], bans an opponent for a whole turn. */
    fun deniesATurn(state: GameState, playerId: EntityId, definition: CardDefinition): Boolean {
        val spell = definition.script.spellEffect ?: return false
        val bans = EffectWalker.leaves(spell).filterIsInstance<CantCastSpellsEffect>()
            .filter { CardIntentAnalyzer.bansAnOpponent(it.target) }
        if (bans.isEmpty()) return false
        val ourTurn = state.isActiveTurnFor(playerId)
        val covers = bans.any { ban ->
            when (ban.duration) {
                Duration.EndOfTurn -> !ourTurn && state.step in THEIR_BEGINNING_PHASE
                Duration.DuringAffectedPlayersNextTurn -> true
                Duration.UntilYourNextTurn -> ourTurn
                else -> false
            }
        }
        if (!covers) return false
        return state.turnOrder.any { it != playerId && state.getZone(it, Zone.HAND).isNotEmpty() }
    }

    /** The steps of an opponent's turn before their first main phase. */
    private val THEIR_BEGINNING_PHASE = setOf(Step.UPKEEP, Step.DRAW)
}
