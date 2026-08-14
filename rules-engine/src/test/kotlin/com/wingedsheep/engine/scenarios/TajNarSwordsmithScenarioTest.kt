package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.TajNarSwordsmith
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Taj-Nar Swordsmith (MRD #27) — {3}{W} Creature — Cat Soldier, 2/3.
 *
 * "When this creature enters, you may pay {X}. If you do, search your library for an Equipment
 *  card with mana value X or less, put that card onto the battlefield, then shuffle."
 *
 * The interesting part is that the chosen X has to flow from the payment gate into the *library
 * filter* — `CardPredicate.ManaValueAtMostX` reads it off the resolution context — so the tests
 * pin both sides of the bound: an Equipment within X is offered, one above X is not.
 *
 * Bonesplitter is {1} (mana value 1); Fireshrieker is {3} (mana value 3).
 */
class TajNarSwordsmithScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TajNarSwordsmith))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /**
     * Cast the Swordsmith, answer its "pay {X}" chooser with [x], and resolve everything —
     * picking [pick] out of the library-search options when one matches. Returns the names the
     * search actually offered, so a test can assert on what was *not* findable.
     */
    fun GameTestDriver.castSwordsmith(player: EntityId, x: Int, pick: String? = null): List<String> {
        val card = putCardInHand(player, "Taj-Nar Swordsmith")
        giveMana(player, Color.WHITE, 4 + x)
        castSpell(player, card).isSuccess shouldBe true

        val offered = mutableListOf<String>()
        var guard = 0
        while ((stackSize > 0 || isPaused) && guard++ < 40) {
            when (val decision = pendingDecision) {
                is ChooseNumberDecision ->
                    submitDecision(player, NumberChosenResponse(decision.id, x))
                is SelectCardsDecision -> {
                    offered += decision.options.mapNotNull { getCardName(it) }
                    val chosen = decision.options.firstOrNull { getCardName(it) == pick }
                    if (chosen != null) submitCardSelection(player, listOf(chosen))
                    else submitCardSelection(player, emptyList())
                }
                null -> bothPass()
                else -> autoResolveDecision()
            }
        }
        return offered
    }

    test("paying X=3 finds an Equipment with mana value 3 and puts it onto the battlefield") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putCardOnTopOfLibrary(me, "Fireshrieker")

        val offered = driver.castSwordsmith(me, x = 3, pick = "Fireshrieker")

        offered.contains("Fireshrieker") shouldBe true
        (driver.findPermanent(me, "Fireshrieker") != null) shouldBe true
    }

    test("X=2 does not offer an Equipment with mana value 3") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putCardOnTopOfLibrary(me, "Fireshrieker")

        val offered = driver.castSwordsmith(me, x = 2, pick = "Fireshrieker")

        offered.contains("Fireshrieker") shouldBe false
        driver.findPermanent(me, "Fireshrieker") shouldBe null
    }

    test("X=1 still finds a mana value 1 Equipment") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putCardOnTopOfLibrary(me, "Bonesplitter")

        val offered = driver.castSwordsmith(me, x = 1, pick = "Bonesplitter")

        offered.contains("Bonesplitter") shouldBe true
        (driver.findPermanent(me, "Bonesplitter") != null) shouldBe true
    }

    test("declining the payment (X = 0) searches for nothing") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putCardOnTopOfLibrary(me, "Bonesplitter")

        driver.castSwordsmith(me, x = 0, pick = "Bonesplitter")

        driver.findPermanent(me, "Bonesplitter") shouldBe null
    }
})
