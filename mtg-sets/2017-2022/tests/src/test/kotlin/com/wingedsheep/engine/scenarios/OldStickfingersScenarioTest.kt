package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.OldStickfingers
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Old Stickfingers — the cast trigger must read the X announced for the spell (it rides on the
 * SpellCastEvent, not the trigger's own X), walk the library until X creature cards are revealed,
 * bin those creatures, bottom the rest, and the resolved creature's P/T must count them.
 */
class OldStickfingersScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OldStickfingers))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("X = 2 mills the first two creature cards revealed and bottoms the rest") {
        val driver = newDriver()
        val active = driver.activePlayer!!

        // Top of library, top-most first: Forest, Grizzly Bears, Forest, Grizzly Bears, Swamp.
        val swamp = driver.putCardOnTopOfLibrary(active, "Swamp")
        val bears2 = driver.putCardOnTopOfLibrary(active, "Grizzly Bears")
        val forest2 = driver.putCardOnTopOfLibrary(active, "Forest")
        val bears1 = driver.putCardOnTopOfLibrary(active, "Grizzly Bears")
        val forest1 = driver.putCardOnTopOfLibrary(active, "Forest")

        val spell = driver.putCardInHand(active, "Old Stickfingers")
        driver.giveMana(active, Color.BLACK, 3)
        driver.giveMana(active, Color.GREEN, 1)
        driver.castXSpell(active, spell, xValue = 2).error shouldBe null

        // The cast trigger is above the creature spell; resolve it first.
        driver.bothPass()

        withClue("both revealed creature cards are in the graveyard") {
            driver.state.getGraveyard(active).toSet() shouldBe setOf(bears1, bears2)
        }
        val library = driver.state.getLibrary(active)
        withClue("the unrevealed Swamp is now the top card") {
            library.first() shouldBe swamp
        }
        withClue("the two revealed Forests are on the bottom") {
            library.takeLast(2).toSet() shouldBe setOf(forest1, forest2)
        }

        // Resolve Old Stickfingers itself: two creature cards in the graveyard -> 2/2.
        driver.bothPass()
        val stickfingers = driver.findPermanent(active, "Old Stickfingers")!!
        driver.state.projectedState.getPower(stickfingers) shouldBe 2
        driver.state.projectedState.getToughness(stickfingers) shouldBe 2
    }

    test("X = 0 reveals nothing; as a 0/0 it dies and then counts itself in the graveyard") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val bears = driver.putCardOnTopOfLibrary(active, "Grizzly Bears")

        val spell = driver.putCardInHand(active, "Old Stickfingers")
        driver.giveMana(active, Color.BLACK, 1)
        driver.giveMana(active, Color.GREEN, 1)
        driver.castXSpell(active, spell, xValue = 0).error shouldBe null

        driver.bothPass() // cast trigger: walks nothing
        driver.state.getLibrary(active).first() shouldBe bears
        driver.state.getGraveyard(active).isEmpty() shouldBe true

        driver.bothPass() // the creature resolves as a 0/0 and is put into the graveyard
        driver.findPermanent(active, "Old Stickfingers") shouldBe null
        driver.getGraveyardCardNames(active) shouldBe listOf("Old Stickfingers")
    }
})
