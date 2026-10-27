package com.wingedsheep.gameserver.session

import com.wingedsheep.ai.engine.TrivialDecisions
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.pendingQuestionIsPrivate
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.core.Zone

/**
 * The answer a browser seat may submit for a question with exactly one legal answer, without
 * showing the player a dialog (#3340).
 *
 * The engine always asks, so that *whether* a player is asked never depends on hidden cards; the
 * shortcut belongs to the decider. The AI takes it through [TrivialDecisions], and this applies the
 * same definition to a human seat, so the two agree on what "forced" means.
 *
 * The server only classifies; the client chooses whether to use the answer (the player's
 * preference) and when: at once, or after a short delay that looks like a quick human answer.
 * Nothing here changes what the engine accepts: the client submits the response as an ordinary
 * decision.
 *
 * The delay exists because answering a forced question at machine speed tells the opponent it
 * was forced, and forcedness can depend on hidden cards (a "you may discard a creature card" with
 * none in hand). When everyone can already see the question has one answer, the pause protects
 * nothing; [isPublic] says when that is.
 */
object ForcedDecisionAutoAnswer {

    /**
     * The response to send [seatDecision]'s decider alongside it, or null when they must be asked.
     *
     * [engineDecision] is the engine's own decision and decides *whether* the question is forced:
     * the seat's copy has been enriched and re-keyed for transport, and a classification made from
     * a transformed view could see a choice as forced that is not. [seatDecision] is that same
     * decision as the seat receives it — its routing id and the seat's own card ids — so the answer
     * is built from it, ready to echo back unchanged.
     */
    fun responseFor(engineDecision: PendingDecision, seatDecision: PendingDecision): DecisionResponse? {
        if (showsCardsToTheDecider(engineDecision)) return null
        val engineAnswer = TrivialDecisions.responseFor(engineDecision) ?: return null
        val seatAnswer = TrivialDecisions.responseFor(seatDecision) ?: return null
        return seatAnswer.takeIf { it::class == engineAnswer::class }
    }

    /**
     * A question that is also how the decider *sees* cards. "Look at the top three cards; you may
     * reveal a creature card from among them" with no creature has only one answer, but the dialog
     * is the look: answering it unseen would throw away information the player is entitled to. A
     * library search is the exception: what was searched for is named in the notice the client
     * shows ("Fail to find"), and the rest of the library stays as hidden as it was.
     */
    private fun showsCardsToTheDecider(decision: PendingDecision): Boolean = when (decision) {
        is SelectCardsDecision -> decision.nonSelectableOptions.isNotEmpty() && !decision.librarySearch
        is ReorderLibraryDecision -> decision.cards.isNotEmpty()
        else -> false
    }

    /**
     * Whether every player — indeed every spectator — can tell that [decision] has only one legal
     * answer, so answering it at once gives nothing away. Flametongue Kavu entering an otherwise
     * empty battlefield must target itself, and the whole table can see that.
     *
     * Fails closed. A question is public only when the engine declared where its candidates came
     * from ([com.wingedsheep.engine.core.DecisionContext.candidateZones]) and everything in those
     * zones is visible to a spectator: no hand, library or sideboard, and no face-down object whose
     * identity a filter could have read. Only target and card-selection questions qualify, since
     * their forcedness is a function of the candidate pool alone; anything else — a forced "no", a
     * single remaining option — may be forced by a card nobody else can see. A private question
     * is never public, its existence being the secret.
     *
     * Read only for a question [responseFor] has already found forced.
     */
    fun isPublic(state: GameState, decision: PendingDecision, visibility: Visibility): Boolean {
        if (decision !is ChooseTargetsDecision && decision !is SelectCardsDecision) return false
        if (state.pendingQuestionIsPrivate) return false
        val zones = decision.context.candidateZones ?: return false
        // A spectator sees exactly what is public; any seat will do as the viewer.
        val viewer = state.turnOrder.firstOrNull() ?: return false
        return zones.all { zone ->
            state.turnOrder.all { owner ->
                visibility.isZoneVisibleTo(state, ZoneKey(owner, zone), viewer, isSpectator = true)
            } && objectsIn(state, zone).all { id ->
                visibility.isCardIdentityVisibleTo(state, zone, id, viewer, isSpectator = true)
            }
        }
    }

    private fun objectsIn(state: GameState, zone: Zone): List<com.wingedsheep.sdk.model.EntityId> =
        if (zone == Zone.STACK) state.stack
        else state.zones.filterKeys { it.zoneType == zone }.values.flatten()
}
