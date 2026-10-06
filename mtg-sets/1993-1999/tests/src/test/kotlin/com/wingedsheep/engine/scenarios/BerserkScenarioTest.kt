package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Berserk
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Berserk (Limited Edition Alpha).
 *
 * Cast this spell only before the combat damage step.
 * Target creature gains trample and gets +X/+0 until end of turn, where X is its power. At the
 * beginning of the next end step, destroy that creature if it attacked this turn.
 */
class BerserkScenarioTest : FunSpec({

    data class Setup(val driver: GameTestDriver, val alice: EntityId, val bob: EntityId, val bears: EntityId)

    fun setup(): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Berserk)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bears = driver.putCreatureOnBattlefield(alice, "Grizzly Bears")
        driver.removeSummoningSickness(bears)
        return Setup(driver, alice, bob, bears)
    }

    test("doubles power, grants trample, and destroys the creature at end step after it attacked") {
        val (driver, alice, bob, bears) = setup()
        driver.giveMana(alice, Color.GREEN, 1)
        driver.castSpell(alice, driver.putCardInHand(alice, "Berserk"), listOf(bears)).error shouldBe null
        driver.bothPass()

        driver.state.projectedState.getPower(bears) shouldBe 4
        driver.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(alice, listOf(bears), bob)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(bob)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.getLifeTotal(bob) shouldBe 16
        withClue("the destruction waits for the end step") {
            driver.findPermanent(alice, "Grizzly Bears") shouldNotBe null
        }

        driver.passPriorityUntil(Step.CLEANUP)
        driver.findPermanent(alice, "Grizzly Bears") shouldBe null
        driver.assertInGraveyard(alice, "Grizzly Bears")
    }

    test("a creature that didn't attack survives the end step") {
        val (driver, alice, _, bears) = setup()
        driver.giveMana(alice, Color.GREEN, 1)
        driver.castSpell(alice, driver.putCardInHand(alice, "Berserk"), listOf(bears)).error shouldBe null
        driver.bothPass()
        driver.state.projectedState.getPower(bears) shouldBe 4

        driver.passPriorityUntil(Step.CLEANUP)
        driver.findPermanent(alice, "Grizzly Bears") shouldNotBe null
    }

    test("can't be cast after combat damage") {
        val (driver, alice, _, bears) = setup()
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.giveMana(alice, Color.GREEN, 1)
        driver.castSpell(alice, driver.putCardInHand(alice, "Berserk"), listOf(bears)).error shouldNotBe null
    }
})
