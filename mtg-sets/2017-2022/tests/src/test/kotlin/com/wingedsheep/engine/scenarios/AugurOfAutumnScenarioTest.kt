package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.AugurOfAutumn
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Augur of Autumn: lands are always playable from the top of the library; creature spells only
 * while coven holds (three or more creatures you control with different powers).
 */
class AugurOfAutumnScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AugurOfAutumn)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("plays a land from the top of the library without coven") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Augur of Autumn")

        val forestOnTop = driver.putCardOnTopOfLibrary(you, "Forest")
        driver.playLand(you, forestOnTop).outcome shouldBe Outcome.Done
        driver.state.getBattlefield(you).contains(forestOnTop) shouldBe true
    }

    test("cannot cast a creature from the top when the powers are not three different values") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        // Powers 2, 2, 3 — only two different values.
        driver.putCreatureOnBattlefield(you, "Augur of Autumn")
        driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.putCreatureOnBattlefield(you, "Centaur Courser")

        val bearsOnTop = driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        driver.giveMana(you, Color.GREEN, 2)
        driver.castSpell(you, bearsOnTop).outcome shouldNotBe Outcome.Done
        driver.state.getLibrary(you).first() shouldBe bearsOnTop
    }

    test("casts a creature from the top with coven") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        // Powers 2, 3, 1 — coven.
        driver.putCreatureOnBattlefield(you, "Augur of Autumn")
        driver.putCreatureOnBattlefield(you, "Centaur Courser")
        driver.putCreatureOnBattlefield(you, "Llanowar Elves")

        val bearsOnTop = driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        driver.giveMana(you, Color.GREEN, 2)
        driver.castSpell(you, bearsOnTop).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getBattlefield(you).contains(bearsOnTop) shouldBe true
    }
})
