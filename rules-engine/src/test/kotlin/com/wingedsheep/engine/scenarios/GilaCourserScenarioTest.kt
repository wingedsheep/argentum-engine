package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.GilaCourser
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gila Courser (OTJ).
 *
 * Oracle: "Whenever this creature attacks while saddled, exile the top card of your library.
 * Until the end of your next turn, you may play that card. Saddle 1"
 *
 * Composes the "attacks while saddled" trigger gate (Triggers.Attacks + Conditions.SourceIsSaddled)
 * with the impulse-exile body (GatherCards → MoveCollection(EXILE) → GrantMayPlayFromExile,
 * MayPlayExpiry.UntilEndOfNextTurn). No new SDK surface.
 */
class GilaCourserScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GilaCourser)
        driver.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("attacking while saddled exiles the top card and grants permission to play it") {
        val driver = newDriver()
        val courser = driver.putCreatureOnBattlefield(driver.player1, "Gila Courser")
        // Saddle 1 needs total power >= 1; one Grizzly Bears (power 2) suffices.
        val saddler = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(courser)

        val exileBefore = driver.getExile(driver.player1).size

        driver.submitSuccess(SaddleMount(driver.player1, courser, listOf(saddler)))
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(courser), driver.player2)
        driver.bothPass() // resolve the attack trigger

        // The top card was exiled and is playable.
        driver.getExile(driver.player1).size shouldBe exileBefore + 1
        val exiled = driver.getExile(driver.player1).last()
        driver.state.mayPlayPermissions.any { exiled in it.cardIds } shouldBe true
    }

    test("attacking while not saddled does not exile a card") {
        val driver = newDriver()
        val courser = driver.putCreatureOnBattlefield(driver.player1, "Gila Courser")
        driver.removeSummoningSickness(courser)

        val exileBefore = driver.getExile(driver.player1).size

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(courser), driver.player2)
        driver.bothPass()

        driver.getExile(driver.player1).size shouldBe exileBefore
    }
})
