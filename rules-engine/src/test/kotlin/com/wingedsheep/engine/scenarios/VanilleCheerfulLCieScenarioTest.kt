package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.VanilleCheerfulLCie
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vanille, Cheerful l'Cie (FIN).
 *
 * "When Vanille enters, mill two cards, then return a permanent card from your graveyard to your
 * hand." (Its meld ability is intentionally omitted — see the card definition.)
 *
 * The test casts Vanille for real so its enters-the-battlefield trigger fires, then resolves the
 * mill and the resolution-time choice: a permanent card already in the graveyard (Grizzly Bears)
 * is offered and returned to hand. Two cards are milled off the top of the library on the way.
 */
class VanilleCheerfulLCieScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(VanilleCheerfulLCie))
        return driver
    }

    test("enters: mill two, then return a permanent card from your graveyard to your hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        // A permanent card sitting in your graveyard is the intended return target.
        val gyBear = driver.putCardInGraveyard(you, "Grizzly Bears")
        val libraryBefore = driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size
        val graveyardBefore = driver.getGraveyard(you).size

        // Cast Vanille so its ETB trigger fires.
        val vanille = driver.putCardInHand(you, "Vanille, Cheerful l'Cie")
        driver.giveMana(you, Color.GREEN, 4) // {3}{G}
        driver.castSpell(you, vanille).isSuccess shouldBe true

        // Resolve the creature spell and its ETB trigger (mill 2) until the return choice pauses.
        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        // Two cards were milled from the library into the graveyard.
        driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size shouldBe libraryBefore - 2

        // The ability offers a permanent card from your graveyard; Grizzly Bears is among the options.
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val choice = driver.pendingDecision as SelectCardsDecision
        withClue("Grizzly Bears is offered as a permanent card to return") {
            choice.playerId shouldBe you
            choice.options.contains(gyBear) shouldBe true
        }
        driver.submitCardSelection(you, listOf(gyBear))
        while (!driver.isPaused && driver.state.stack.isNotEmpty()) driver.bothPass()

        // Grizzly Bears moved from graveyard to hand; the two milled cards remain in the graveyard.
        withClue("Grizzly Bears is now in hand and no longer in the graveyard") {
            driver.getHand(you).contains(gyBear) shouldBe true
            driver.getGraveyard(you).contains(gyBear) shouldBe false
        }
        // Net graveyard: started with Grizzly Bears (graveyardBefore), +2 milled, −1 returned.
        driver.getGraveyard(you).size shouldBe graveyardBefore + 2 - 1
    }
})
