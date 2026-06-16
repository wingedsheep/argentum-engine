package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.SheriffOfSafePassage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Sheriff of Safe Passage (OTJ #29).
 *
 * {2}{W} 0/0 Human Knight.
 * Enters with a +1/+1 counter plus an additional +1/+1 counter for each *other creature you
 * control*. Plot {1}{W}.
 *
 * The interesting authoring detail is "each OTHER creature YOU control": the count must include
 * the controller's other creatures but exclude the Sheriff itself and the opponent's creatures.
 */
class SheriffOfSafePassageScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SheriffOfSafePassage))
        return driver
    }

    test("enters as a 1/1 with no other creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sheriff = driver.putCardInHand(activePlayer, "Sheriff of Safe Passage")
        driver.giveMana(activePlayer, Color.WHITE, 3)

        driver.castSpell(activePlayer, sheriff).isSuccess shouldBe true
        driver.bothPass()

        // Base 0/0 + 1 fixed counter = 1/1.
        projector.getProjectedPower(driver.state, sheriff) shouldBe 1
        projector.getProjectedToughness(driver.state, sheriff) shouldBe 1
    }

    test("counts each other creature the controller controls") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two of the controller's other creatures.
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        val sheriff = driver.putCardInHand(activePlayer, "Sheriff of Safe Passage")
        driver.giveMana(activePlayer, Color.WHITE, 3)

        driver.castSpell(activePlayer, sheriff).isSuccess shouldBe true
        driver.bothPass()

        // 1 fixed + 2 other creatures = 3 counters → 3/3.
        projector.getProjectedPower(driver.state, sheriff) shouldBe 3
        projector.getProjectedToughness(driver.state, sheriff) shouldBe 3
    }

    test("does not count the opponent's creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // One of mine, two of the opponent's.
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val sheriff = driver.putCardInHand(activePlayer, "Sheriff of Safe Passage")
        driver.giveMana(activePlayer, Color.WHITE, 3)

        driver.castSpell(activePlayer, sheriff).isSuccess shouldBe true
        driver.bothPass()

        // 1 fixed + 1 of my other creatures (opponent's two are ignored) = 2 → 2/2.
        projector.getProjectedPower(driver.state, sheriff) shouldBe 2
        projector.getProjectedToughness(driver.state, sheriff) shouldBe 2
    }
})
