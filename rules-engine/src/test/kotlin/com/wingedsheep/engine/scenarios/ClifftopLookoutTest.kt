package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.ClifftopLookout
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Clifftop Lookout:
 * {2}{G} Creature — Frog Scout 1/2, Reach
 * When this creature enters, reveal cards from the top of your library until you
 * reveal a land card. Put that card onto the battlefield tapped and the rest on
 * the bottom of your library in a random order.
 *
 * Regression: previously the matched land ended up untapped (and not on the
 * battlefield) because GatherUntilMatch stores the match in BOTH `storeMatch`
 * and `storeRevealed`; moving the full revealed collection to the library
 * stripped the land off the battlefield after it had just been placed there.
 */
class ClifftopLookoutTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ClifftopLookout)
        return driver
    }

    test("revealed land enters the battlefield tapped, other revealed cards go to the bottom of the library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Library top (after these calls, in order from top): nonland, nonland, Forest, ...
        // Forest is the first land seen → it's the match.
        val deeperCard = driver.putCardOnTopOfLibrary(activePlayer, "Force of Nature")
        val targetLand = driver.putCardOnTopOfLibrary(activePlayer, "Forest")
        val nonlandTop2 = driver.putCardOnTopOfLibrary(activePlayer, "Grizzly Bears")
        val nonlandTop1 = driver.putCardOnTopOfLibrary(activePlayer, "Centaur Courser")

        val lookout = driver.putCardInHand(activePlayer, "Clifftop Lookout")
        driver.giveMana(activePlayer, Color.GREEN, 3)

        driver.castSpell(activePlayer, lookout)
        driver.bothPass() // resolve Lookout — enters battlefield, ETB trigger goes on stack
        driver.bothPass() // resolve the ETB trigger pipeline

        // No pause expected — Random ordering doesn't prompt for a reorder decision.
        driver.isPaused shouldBe false

        // The matched Forest is on the battlefield and tapped.
        val forestOnBattlefield = driver.findPermanent(activePlayer, "Forest")
        forestOnBattlefield shouldNotBe null
        forestOnBattlefield shouldBe targetLand
        driver.isTapped(targetLand) shouldBe true

        // The two revealed nonlands have been moved to the bottom of the library
        // (not on battlefield, not in graveyard, not in hand).
        val library = driver.state.getZone(ZoneKey(activePlayer, Zone.LIBRARY))
        library shouldNotBe emptyList<Any>()
        library.contains(targetLand) shouldBe false
        library.contains(nonlandTop1) shouldBe true
        library.contains(nonlandTop2) shouldBe true

        // The card that was below the matched land was never revealed and stays put.
        library.contains(deeperCard) shouldBe true

        // Sanity: the revealed nonlands are not on battlefield, in hand, or graveyard.
        driver.findPermanent(activePlayer, "Centaur Courser") shouldBe null
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        driver.getGraveyard(activePlayer).none { it == nonlandTop1 || it == nonlandTop2 } shouldBe true
        driver.getHand(activePlayer).none { it == nonlandTop1 || it == nonlandTop2 } shouldBe true

        // Lookout itself is on the battlefield.
        driver.findPermanent(activePlayer, "Clifftop Lookout") shouldNotBe null
    }

    test("when no land is revealed, no card enters the battlefield and all revealed cards return to the bottom") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Library is entirely nonlands — no match will be found.
        val lookout = driver.putCardInHand(activePlayer, "Clifftop Lookout")
        driver.giveMana(activePlayer, Color.GREEN, 3)

        val libraryBefore = driver.state.getZone(ZoneKey(activePlayer, Zone.LIBRARY)).toList()

        driver.castSpell(activePlayer, lookout)
        driver.bothPass()
        driver.bothPass()

        driver.isPaused shouldBe false

        // No land entered the battlefield.
        driver.findPermanent(activePlayer, "Forest") shouldBe null

        // Every card from the original library is still in the library.
        val libraryAfter = driver.state.getZone(ZoneKey(activePlayer, Zone.LIBRARY))
        libraryBefore.all { libraryAfter.contains(it) } shouldBe true
        libraryAfter.size shouldBe libraryBefore.size

        // Lookout itself is on the battlefield (the trigger resolved without aborting).
        driver.findPermanent(activePlayer, "Clifftop Lookout") shouldNotBe null
    }
})
