package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AvenReedstalkerScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20, skipMulligans = true)
        return d
    }

    test("flash allows casting during the opposing upkeep") {
        val d = driver()
        d.passPriorityUntil(Step.UPKEEP, d.player2)
        val bird = d.putCardInHand(d.player1, "Aven Reedstalker")
        d.giveMana(d.player1, Color.BLUE, 4)
        d.passPriority(d.player2)
        d.castSpell(d.player1, bird).error shouldBe null
        d.bothPass()
        d.state.projectedState.getPower(bird) shouldBe 2
        d.state.projectedState.getToughness(bird) shouldBe 3
        d.state.projectedState.hasKeyword(bird, Keyword.FLYING) shouldBe true
    }
})
