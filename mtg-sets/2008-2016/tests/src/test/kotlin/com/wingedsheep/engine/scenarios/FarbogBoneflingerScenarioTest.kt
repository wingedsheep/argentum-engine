package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FarbogBoneflingerScenarioTest : FunSpec({

    test("when it enters, target creature gets -2/-2 until end of turn") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(driver.getOpponent(you), "Grizzly Bears")

        val boneflinger = driver.putCardInHand(you, "Farbog Boneflinger")
        driver.giveMana(you, Color.BLACK, 5)
        driver.castSpell(you, boneflinger).outcome shouldBe Outcome.Done

        var guard = 0
        while (guard++ < 20) {
            when (driver.pendingDecision) {
                is ChooseTargetsDecision -> driver.submitTargetSelection(you, listOf(bears))
                else -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
            }
        }

        driver.getGraveyard(driver.getOpponent(you)).contains(bears) shouldBe true
        driver.findPermanent(you, "Farbog Boneflinger") shouldNotBe null
    }
})
