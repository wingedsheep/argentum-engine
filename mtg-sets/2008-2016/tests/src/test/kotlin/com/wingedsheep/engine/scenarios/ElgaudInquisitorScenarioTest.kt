package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ElgaudInquisitorScenarioTest : FunSpec({

    test("when it dies, its controller creates a 1/1 white Spirit token with flying") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = driver.activePlayer!!
        val inquisitor = driver.putCreatureOnBattlefield(you, "Elgaud Inquisitor")

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(inquisitor)).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        driver.getGraveyard(you).contains(inquisitor) shouldBe true
        val token = driver.getCreatures(you).single()
        val projected = driver.state.projectedState
        projected.getPower(token) shouldBe 1
        projected.getToughness(token) shouldBe 1
        projected.hasKeyword(token, Keyword.FLYING) shouldBe true
    }
})
