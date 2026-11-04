package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AvenOfEnduringHopeScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20, skipMulligans = true)
        return d
    }

    test("entering gains its controller three life even if the bird leaves before resolution") {
        val d = driver()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bird = d.putCardInHand(d.player1, "Aven of Enduring Hope")
        d.giveMana(d.player1, Color.WHITE, 5)
        d.castSpell(d.player1, bird).error shouldBe null
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 20
        d.moveToGraveyard(bird)
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 23
        d.getLifeTotal(d.player2) shouldBe 20
    }
})
