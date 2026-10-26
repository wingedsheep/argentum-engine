package com.wingedsheep.gameserver.session

import com.wingedsheep.ai.engine.TrivialDecisions
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision

/**
 * The answer a browser seat may submit for a question with exactly one legal answer, without
 * showing the player a dialog (#3340).
 *
 * The engine always asks, so that *whether* a player is asked never depends on hidden cards; the
 * shortcut belongs to the decider. The AI takes it through [TrivialDecisions], and this applies the
 * same definition to a human seat, so the two agree on what "forced" means.
 *
 * The server only classifies; the client chooses whether to use the answer (an opt-out preference)
 * and when (after a short delay that looks like a quick human answer). Nothing here changes what
 * the engine accepts: the client submits the response as an ordinary decision.
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
}
