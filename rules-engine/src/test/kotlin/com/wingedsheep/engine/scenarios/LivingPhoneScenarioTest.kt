package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.LivingPhone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Living Phone (DSK #20) — {2}{W} Artifact Creature — Toy 2/1.
 *
 * "When this creature dies, look at the top five cards of your library. You may reveal a creature
 * card with power 2 or less from among them and put it into your hand. Put the rest on the bottom
 * of your library in a random order."
 *
 * Exercises the look-at-top-five → optional filtered reveal-to-hand → rest-to-bottom-in-random-order
 * pipeline ([com.wingedsheep.sdk.dsl.LibraryPatterns.lookAtTopRevealMatchingToHand]) on a dies
 * trigger, including the "you may" decline path.
 */
class LivingPhoneScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + LivingPhone)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun librarySize(driver: GameTestDriver, playerId: EntityId): Int =
        driver.state.getZone(ZoneKey(playerId, Zone.LIBRARY)).size

    test("dies: reveals a power-2-or-less creature to hand, rest to bottom of library") {
        val driver = newDriver()
        val player = driver.player1

        val phone = driver.putCreatureOnBattlefield(player, "Living Phone")
        // Top of library: a power-2 creature on top (matches), Forests beneath (don't match).
        val bear = driver.putCardOnTopOfLibrary(player, "Grizzly Bears") // 2/2 — power 2

        val libBefore = librarySize(driver, player) // includes the freshly-stacked Grizzly Bears

        // Bolt the 2/1 Living Phone to kill it and fire the dies trigger.
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, bolt, targets = listOf(phone)).isSuccess shouldBe true
        driver.bothPass() // resolve the bolt — Living Phone dies, queuing the dies trigger
        driver.bothPass() // resolve the dies trigger — pauses at the optional reveal

        driver.isPaused shouldBe true
        val pick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.options shouldContain bear

        // Reveal the Grizzly Bears and put it into hand.
        driver.submitDecision(player, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(bear)))
        while (!driver.isPaused && driver.state.stack.isNotEmpty()) driver.bothPass()

        // Grizzly Bears is in hand; the other four looked-at cards went to the bottom.
        driver.getHand(player) shouldContain bear
        // Library: started at libBefore, looked at 5 (removed), 1 to hand + 4 to bottom = libBefore - 1.
        librarySize(driver, player) shouldBe libBefore - 1
    }

    test("dies: the reveal is optional — declining leaves the hand untouched") {
        val driver = newDriver()
        val player = driver.player1

        val phone = driver.putCreatureOnBattlefield(player, "Living Phone")
        driver.putCardOnTopOfLibrary(player, "Grizzly Bears")
        val libBefore = librarySize(driver, player)

        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        val handAfterBolt = driver.getHandSize(player) - 1 // hand once the bolt is cast away
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, bolt, targets = listOf(phone)).isSuccess shouldBe true
        driver.bothPass()
        driver.bothPass()

        driver.isPaused shouldBe true
        val pick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        // Decline: choose nothing — all five looked-at cards go to the bottom.
        driver.submitDecision(player, CardsSelectedResponse(decisionId = pick.id, selectedCards = emptyList()))
        while (!driver.isPaused && driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getHandSize(player) shouldBe handAfterBolt
        // All five looked-at cards returned to the bottom — library size unchanged.
        librarySize(driver, player) shouldBe libBefore
    }
})
