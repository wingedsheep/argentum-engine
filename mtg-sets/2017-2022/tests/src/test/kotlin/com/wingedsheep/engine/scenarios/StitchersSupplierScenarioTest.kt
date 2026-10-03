package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m19.cards.StitchersSupplier
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Stitcher's Supplier (M19) — When this creature enters or dies, mill three cards.
 */
class StitchersSupplierScenarioTest : FunSpec({

    test("mills three when it enters and three more when it dies") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + StitchersSupplier)
            initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = d.activePlayer!!
        val libraryStart = d.state.getLibrary(you).size

        val supplier = d.putCardInHand(you, "Stitcher's Supplier")
        d.giveMana(you, Color.BLACK, 1)
        d.castSpell(you, supplier).error shouldBe null
        d.bothPass() // resolve the creature spell
        d.bothPass() // resolve the enters trigger

        d.state.getLibrary(you).size shouldBe libraryStart - 3
        d.getGraveyard(you).size shouldBe 3

        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.giveMana(you, Color.RED, 1)
        d.castSpell(you, bolt, listOf(supplier)).error shouldBe null
        d.bothPass() // resolve Bolt; Supplier dies
        d.bothPass() // resolve the dies trigger

        d.getGraveyard(you) shouldContain supplier
        d.state.getLibrary(you).size shouldBe libraryStart - 6
        // 6 milled + Supplier + Bolt
        d.getGraveyard(you).size shouldBe 8
    }
})
