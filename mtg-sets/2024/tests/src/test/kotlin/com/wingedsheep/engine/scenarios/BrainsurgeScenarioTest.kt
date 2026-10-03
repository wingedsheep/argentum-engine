package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.Brainsurge
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Brainsurge (MH3 #53) — {2}{U} Instant.
 *   "Draw four cards, then put two cards from your hand on top of your library in any order."
 */
class BrainsurgeScenarioTest : FunSpec({

    test("draws four, then puts two chosen hand cards on top in the chosen order") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + Brainsurge)
            initMirrorMatch(deck = Deck.of("Island" to 60), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.activePlayer!!
        // A card already in hand before the draw is a legal choice (2024-06-07 ruling).
        val oldCard = d.putCardInHand(me, "Grizzly Bears")
        val spell = d.putCardInHand(me, "Brainsurge")
        val handBefore = d.state.getHand(me).size
        val libraryBefore = d.state.getLibrary(me).size

        d.giveMana(me, Color.BLUE, 3)
        d.castSpell(me, spell).error shouldBe null
        d.bothPass()

        val select = d.pendingDecision as SelectCardsDecision
        select.minSelections shouldBe 2
        select.maxSelections shouldBe 2
        // Hand after casting (−1) and drawing four (+4).
        select.options.size shouldBe handBefore - 1 + 4
        val drawn = d.state.getHand(me).first { it != oldCard }
        d.submitCardSelection(me, listOf(oldCard, drawn))

        d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        d.submitOrderedResponse(me, listOf(oldCard, drawn))

        d.state.getHand(me).size shouldBe handBefore - 1 + 4 - 2
        d.state.getLibrary(me).size shouldBe libraryBefore - 4 + 2
        d.state.getLibrary(me).take(2).toSet() shouldBe setOf(oldCard, drawn)
        d.state.getLibrary(me).first() shouldBe oldCard
        d.getGraveyardCardNames(me) shouldBe listOf("Brainsurge")
    }
})
