package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.GiftsUngiven
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gifts Ungiven — {3}{U} Instant (Champions of Kamigawa #62)
 *
 * "Search your library for up to four cards with different names and reveal them. Target
 *  opponent chooses two of those cards. Put the chosen cards into your graveyard and the rest
 *  into your hand. Then shuffle."
 *
 * Covers the split (the *target opponent* decides, not the caster), the "with different names"
 * restriction, and the ruling that finding two or fewer sends them all to the graveyard.
 */
class GiftsUngivenScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + GiftsUngiven)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castGifts() {
        val gifts = putCardInHand(player1, "Gifts Ungiven")
        giveMana(player1, Color.BLUE, 1)
        giveColorlessMana(player1, 3)
        castSpell(player1, gifts, targets = listOf(player2)).error shouldBe null
        bothPass()
    }

    test("target opponent chooses two of four found cards for the graveyard; the rest go to hand") {
        val d = driver()
        val courser = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        val force = d.putCardOnTopOfLibrary(d.player1, "Force of Nature")
        val guide = d.putCardOnTopOfLibrary(d.player1, "Goblin Guide")
        val lions = d.putCardOnTopOfLibrary(d.player1, "Savannah Lions")

        d.castGifts()

        withClue("the caster searches") { d.pendingDecision?.playerId shouldBe d.player1 }
        d.submitCardSelection(d.player1, listOf(courser, force, guide, lions)).error shouldBe null

        withClue("the target opponent makes the split") { d.pendingDecision?.playerId shouldBe d.player2 }
        d.submitCardSelection(d.player2, listOf(force, lions)).error shouldBe null

        withClue("the chosen cards go to the caster's graveyard") {
            (force in d.getGraveyard(d.player1)) shouldBe true
            (lions in d.getGraveyard(d.player1)) shouldBe true
        }
        withClue("the rest go to the caster's hand") {
            (courser in d.getHand(d.player1)) shouldBe true
            (guide in d.getHand(d.player1)) shouldBe true
        }
    }

    test("\"with different names\" refuses a second copy of a name") {
        val d = driver()
        val courser1 = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")
        val courser2 = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")

        d.castGifts()

        // Either the selection is rejected outright or the duplicate is dropped — in no case
        // may both copies leave the library.
        d.submitCardSelection(d.player1, listOf(courser1, courser2))
        withClue("at most one Centaur Courser may be found") {
            val library = d.state.getLibrary(d.player1)
            ((courser1 in library) || (courser2 in library)) shouldBe true
        }
    }

    test("finding only one card sends it to the graveyard without an opponent choice") {
        val d = driver()
        val courser = d.putCardOnTopOfLibrary(d.player1, "Centaur Courser")

        d.castGifts()

        d.submitCardSelection(d.player1, listOf(courser)).error shouldBe null

        withClue("the lone card must go to the graveyard (2017-03-14 ruling)") {
            (courser in d.getGraveyard(d.player1)) shouldBe true
            (courser in d.getHand(d.player1)) shouldBe false
        }
    }
})
