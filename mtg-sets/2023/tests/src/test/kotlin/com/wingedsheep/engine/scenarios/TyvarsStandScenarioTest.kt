package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.TyvarsStand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tyvar's Stand — {X}{G} Instant.
 * "Target creature you control gets +X/+X and gains hexproof and indestructible until end of turn."
 */
class TyvarsStandScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TyvarsStand)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && pendingDecision == null && guard++ < 8) bothPass()
    }

    test("X=2 gives +2/+2, hexproof and indestructible") {
        val driver = newDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val stand = driver.putCardInHand(driver.player1, "Tyvar's Stand")
        driver.giveMana(driver.player1, Color.GREEN, 3)

        driver.castXSpell(driver.player1, stand, xValue = 2, targets = listOf(bears)).error shouldBe null
        driver.resolveStack()

        val projected = driver.state.projectedState
        projected.getPower(bears) shouldBe 4
        projected.getToughness(bears) shouldBe 4
        projected.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
        projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
    }

    test("X=0 still grants indestructible: the creature survives lethal damage") {
        val driver = newDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val stand = driver.putCardInHand(driver.player1, "Tyvar's Stand")
        driver.giveMana(driver.player1, Color.GREEN, 1)

        driver.castXSpell(driver.player1, stand, xValue = 0, targets = listOf(bears)).error shouldBe null
        driver.resolveStack()

        driver.state.projectedState.getPower(bears) shouldBe 2

        val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.giveMana(driver.player1, Color.RED, 1)
        driver.castSpell(driver.player1, bolt, listOf(bears)).error shouldBe null
        driver.resolveStack()

        withClue("indestructible keeps the 2/2 alive through 3 damage") {
            driver.findPermanent(driver.player1, "Grizzly Bears") shouldBe bears
        }
    }

    test("cannot target a creature you don't control") {
        val driver = newDriver()
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val stand = driver.putCardInHand(driver.player1, "Tyvar's Stand")
        driver.giveMana(driver.player1, Color.GREEN, 2)

        driver.castXSpell(driver.player1, stand, xValue = 1, targets = listOf(theirs)).error shouldNotBe null
    }
})
