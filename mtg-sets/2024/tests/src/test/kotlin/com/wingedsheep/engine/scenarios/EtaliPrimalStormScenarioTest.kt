package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blc.cards.EtaliPrimalStorm
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Etali, Primal Storm — "Whenever Etali attacks, exile the top card of each player's library, then
 * you may cast any number of spells from among those cards without paying their mana costs."
 *
 * Pinned in a four-player game: *every* player's top card is exiled, offered and castable, not just
 * the attacked player's.
 */
class EtaliPrimalStormScenarioTest : FunSpec({

    test("in multiplayer the attack trigger exiles and offers every player's top card") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + EtaliPrimalStorm)
        val players = d.initMultiplayer(
            decks = List(4) { Deck.of("Mountain" to 40) },
            skipMulligans = true,
            startingPlayer = 0,
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = players[0]

        val etali = d.putCreatureOnBattlefield(me, "Etali, Primal Storm")
        d.removeSummoningSickness(etali)
        val tops = players.associateWith { d.putCardOnTopOfLibrary(it, "Grizzly Bears") }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, mapOf(etali to players[1])).error shouldBe null

        var guard = 0
        while (d.pendingDecision == null && d.stackSize > 0 && guard++ < 20) d.passPriority(d.state.priorityPlayerId!!)

        withClue("every player's top card is exiled") {
            players.forEach { p -> (tops.getValue(p) in d.getExile(p)) shouldBe true }
        }
        val decision = d.pendingDecision
        withClue("the cast prompt is raised: $decision") { (decision is SelectCardsDecision) shouldBe true }
        decision as SelectCardsDecision
        withClue("all four exiled spells are on offer") {
            decision.options.toSet() shouldBe tops.values.toSet()
        }

        // "Any number" is one pick per prompt; cast every one, resolving each spell as it lands.
        repeat(4) {
            val pick = d.pendingDecision as SelectCardsDecision
            d.submitCardSelection(me, listOf(pick.options.first())).error shouldBe null
        }
        guard = 0
        while (d.stackSize > 0 && guard++ < 40) {
            if (d.pendingDecision is SelectCardsDecision) {
                d.submitCardSelection(me, emptyList()).error shouldBe null
            } else {
                d.passPriority(d.state.priorityPlayerId!!)
            }
        }
        withClue("all four spells were cast and resolved under Etali's controller") {
            tops.values.forEach { card -> (card in d.getCreatures(me)) shouldBe true }
        }
    }
})
