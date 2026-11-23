package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rtr.cards.SphinxOfTheChimes
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * "Discard two nonland cards with the same name" (Sphinx of the Chimes) is a selection cost the AI
 * fills itself. Ranking the hand by value interleaves two pairs, so taking the first two would
 * name two different cards and be refused; the AI pays with one name's pair.
 */
class SameNameDiscardCostAiTest : FunSpec({

    test("the AI pays Sphinx of the Chimes' cost with a same-name pair, and the activation resolves") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + SphinxOfTheChimes)
            initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        d.putCreatureOnBattlefield(d.player1, "Sphinx of the Chimes")
        listOf("Lightning Bolt", "Savannah Lions", "Lightning Bolt", "Savannah Lions")
            .forEach { d.putCardInHand(d.player1, it) }

        val offer = d.legalActions(d.player1)
            .first { it.actionType == "ActivateAbility" && it.additionalCostInfo?.costType == "DiscardCard" }
        offer.additionalCostInfo!!.discardSameName shouldBe true
        val paid = SelectionCostPayment.withPlainCosts(
            d.state, offer, offer.action, IntentCatalog.of(d.cardRegistry),
        ) as ActivateAbility

        val discarded = paid.costPayment!!.discardedCards
        discarded shouldHaveSize 2
        discarded.map { d.state.getEntity(it)!!.get<CardComponent>()!!.name }.toSet() shouldHaveSize 1
        val result = d.submit(paid)
        withClue("activation failed: ${result.error}") { result.outcome shouldBe Outcome.Done }
    }
})
