package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.PitilessCarnage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Pitiless Carnage (OTJ #98) — {3}{B} Sorcery.
 *
 * "Sacrifice any number of permanents you control, then draw that many cards. Plot {1}{B}{B}"
 */
class PitilessCarnageScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(PitilessCarnage)
        d.initMirrorMatch(
            deck = Deck.of("Swamp" to 30, "Grizzly Bears" to 30),
            skipMulligans = true,
            startingPlayer = 0
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("sacrifices two chosen permanents and draws that many cards") {
        val d = newDriver()
        val p1 = d.player1

        val carnage = d.putCardInHand(p1, "Pitiless Carnage")
        val bearA = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val bearB = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.putCreatureOnBattlefield(p1, "Grizzly Bears") // a third, left alone
        d.giveMana(p1, Color.BLACK, 1)
        d.giveColorlessMana(p1, 3)

        val handBefore = d.getHand(p1).size

        d.submit(CastSpell(playerId = p1, cardId = carnage))
        d.bothPass() // resolve -> sacrifice-selection prompt

        val sel = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(
            p1,
            CardsSelectedResponse(decisionId = sel.id, selectedCards = listOf(bearA, bearB))
        )
        d.bothPass()

        d.isPaused shouldBe false
        d.getGraveyard(p1).contains(bearA) shouldBe true
        d.getGraveyard(p1).contains(bearB) shouldBe true
        // Hand: -1 cast Carnage, +2 drawn = handBefore + 1.
        d.getHand(p1).size shouldBe handBefore + 1
    }

    test("sacrificing zero permanents draws zero cards") {
        val d = newDriver()
        val p1 = d.player1

        val carnage = d.putCardInHand(p1, "Pitiless Carnage")
        d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.giveMana(p1, Color.BLACK, 1)
        d.giveColorlessMana(p1, 3)

        val handBefore = d.getHand(p1).size

        d.submit(CastSpell(playerId = p1, cardId = carnage))
        d.bothPass() // resolve -> sacrifice-selection prompt (may select none)

        if (d.pendingDecision is SelectCardsDecision) {
            val sel = d.pendingDecision as SelectCardsDecision
            d.submitDecision(p1, CardsSelectedResponse(decisionId = sel.id, selectedCards = emptyList()))
            d.bothPass()
        }

        d.isPaused shouldBe false
        // Hand: -1 cast Carnage, +0 drawn = handBefore - 1.
        d.getHand(p1).size shouldBe handBefore - 1
    }
})
