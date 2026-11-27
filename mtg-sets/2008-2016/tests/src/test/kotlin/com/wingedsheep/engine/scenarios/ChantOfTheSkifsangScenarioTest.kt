package com.wingedsheep.engine.scenarios

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

class ChantOfTheSkifsangScenarioTest : FunSpec({

    test("enchanted creature gets -13/-0") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(driver.getOpponent(you), "Grizzly Bears")

        val chant = driver.putCardInHand(you, "Chant of the Skifsang")
        driver.giveMana(you, Color.BLUE, 3)
        driver.castSpell(you, chant, listOf(bears)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.findPermanent(you, "Chant of the Skifsang") shouldNotBe null
        val projected = driver.state.projectedState
        projected.getPower(bears) shouldBe -11
        projected.getToughness(bears) shouldBe 2
    }
})
