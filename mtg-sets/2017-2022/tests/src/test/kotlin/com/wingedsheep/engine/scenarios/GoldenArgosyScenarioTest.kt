package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.Cloudshift
import com.wingedsheep.mtg.sets.definitions.dmu.cards.GoldenArgosy
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Golden Argosy — {4} Legendary Artifact — Vehicle, 3/6, Crew 1.
 *
 * Whenever it attacks, exile each creature that crewed it this turn; return them tapped under
 * their owner's control at the beginning of the next end step.
 */
class GoldenArgosyScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GoldenArgosy)
        driver.registerCard(Shatter)
        driver.registerCard(Cloudshift)
        driver.initMirrorMatch(Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("attacking exiles the crewers and returns them tapped at the next end step") {
        val driver = newDriver()
        val argosy = driver.putPermanentOnBattlefield(driver.player1, "Golden Argosy")
        val crewer = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val bystander = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(argosy)

        driver.submitSuccess(CrewVehicle(driver.player1, argosy, listOf(crewer)))
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(argosy), driver.player2)
        driver.bothPass()

        driver.getExile(driver.player1) shouldContain crewer
        driver.getPermanents(driver.player1) shouldContain bystander

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.getPermanents(driver.player1) shouldContain crewer
        driver.isTapped(crewer) shouldBe true
    }

    test("the crewers are still exiled when the Argosy is destroyed before its trigger resolves") {
        val driver = newDriver()
        val argosy = driver.putPermanentOnBattlefield(driver.player1, "Golden Argosy")
        val crewer = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(argosy)
        val shatter = driver.putCardInHand(driver.player2, "Shatter")

        driver.submitSuccess(CrewVehicle(driver.player1, argosy, listOf(crewer)))
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(argosy), driver.player2)
        // The attack trigger is on the stack; the opponent destroys the Argosy in response.
        driver.passPriority(driver.player1)
        driver.giveMana(driver.player2, Color.RED, 2)
        driver.castSpell(driver.player2, shatter, listOf(argosy))
        driver.bothPass() // Shatter resolves
        driver.getGraveyard(driver.player1) shouldContain argosy
        driver.bothPass() // attack trigger resolves

        driver.getExile(driver.player1) shouldContain crewer

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.getPermanents(driver.player1) shouldContain crewer
        driver.isTapped(crewer) shouldBe true
    }

    test("a blinked Argosy is a new object: creatures that crewed the old one stay put") {
        val driver = newDriver()
        val argosy = driver.putPermanentOnBattlefield(driver.player1, "Golden Argosy")
        val firstCrewer = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val secondCrewer = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val cloudshift = driver.putCardInHand(driver.player1, "Cloudshift")

        driver.submitSuccess(CrewVehicle(driver.player1, argosy, listOf(firstCrewer)))
        driver.bothPass()

        // Crewed, it is a creature, so Cloudshift can blink it.
        driver.giveMana(driver.player1, Color.WHITE, 1)
        driver.castSpell(driver.player1, cloudshift, listOf(argosy))
        driver.bothPass()

        driver.removeSummoningSickness(argosy)
        driver.submitSuccess(CrewVehicle(driver.player1, argosy, listOf(secondCrewer)))
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(argosy), driver.player2)
        driver.bothPass()

        driver.getExile(driver.player1) shouldContainExactlyInAnyOrder listOf(secondCrewer)
        driver.getExile(driver.player1) shouldNotContain firstCrewer
        driver.getPermanents(driver.player1) shouldContain firstCrewer
    }
})
