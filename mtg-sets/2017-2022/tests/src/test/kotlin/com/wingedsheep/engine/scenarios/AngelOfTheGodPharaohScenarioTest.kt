package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AngelOfTheGodPharaohScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20, skipMulligans = true)
        return d
    }

    test("cycles for two generic mana, replacing itself with a card") {
        val d = driver()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val angel = d.putCardInHand(d.player1, "Angel of the God-Pharaoh")
        d.giveMana(d.player1, Color.BLUE, 2)
        val before = d.getHandSize(d.player1)
        d.submit(CycleCard(d.player1, angel)).error shouldBe null
        d.getGraveyard(d.player1).contains(angel) shouldBe true
        d.getHandSize(d.player1) shouldBe before
    }
})
