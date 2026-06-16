package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.HighwayRobbery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Highway Robbery (OTJ #129) — {1}{R} Sorcery.
 *
 * "You may discard a card or sacrifice a land. If you do, draw two cards.
 *  Plot {1}{R}"
 */
class HighwayRobberyScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(HighwayRobbery)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40, "Grizzly Bears" to 20), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("discard mode: discards a card then draws two") {
        val d = newDriver()
        val p1 = d.player1

        val robbery = d.putCardInHand(p1, "Highway Robbery")
        val fodder = d.putCardInHand(p1, "Grizzly Bears") // the card to discard
        d.giveMana(p1, Color.RED, 1)
        d.giveColorlessMana(p1, 1)

        val handBefore = d.state.getHand(p1).size

        d.submit(CastSpell(playerId = p1, cardId = robbery))

        // Cast-time mode selection: choose "discard a card".
        val modeChoice = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        modeChoice.options shouldContain "Done" // minChooseCount = 0 → may decline
        d.submitDecision(p1, OptionChosenResponse(modeChoice.id, 0))

        d.bothPass() // resolve the spell -> discard prompt

        val discardSel = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(p1, CardsSelectedResponse(decisionId = discardSel.id, selectedCards = listOf(fodder)))
        d.bothPass()

        d.isPaused shouldBe false
        d.getGraveyard(p1).contains(fodder) shouldBe true
        // Hand: started handBefore, -1 cast Robbery, -1 discarded fodder, +2 drawn = handBefore.
        d.state.getHand(p1).size shouldBe handBefore
    }

    test("sacrifice-a-land mode: sacrifices a land then draws two") {
        val d = newDriver()
        val p1 = d.player1

        val robbery = d.putCardInHand(p1, "Highway Robbery")
        val land = d.putLandOnBattlefield(p1, "Mountain") // a land to sacrifice
        d.giveMana(p1, Color.RED, 1)
        d.giveColorlessMana(p1, 1)

        val handBefore = d.state.getHand(p1).size

        d.submit(CastSpell(playerId = p1, cardId = robbery))

        val modeChoice = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(p1, OptionChosenResponse(modeChoice.id, 1)) // sacrifice a land

        d.bothPass() // resolve -> may pause for sacrifice selection

        if (d.pendingDecision is SelectCardsDecision) {
            val sacSel = d.pendingDecision as SelectCardsDecision
            d.submitDecision(p1, CardsSelectedResponse(decisionId = sacSel.id, selectedCards = listOf(land)))
            d.bothPass()
        }

        d.isPaused shouldBe false
        d.getGraveyard(p1).contains(land) shouldBe true
        // Hand: started handBefore, -1 cast Robbery, +2 drawn = handBefore + 1.
        d.state.getHand(p1).size shouldBe handBefore + 1
    }
})
