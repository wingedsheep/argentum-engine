package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.XandeDarkMage
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Xande, Dark Mage. */
class XandeDarkMageScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(vararg cards: CardDefinition): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        cards.forEach { driver.registerCard(it) }
        return driver
    }

    // Resolve the whole stack (auto-resolving scry/library-order decisions) without advancing
    // past the current step, so end-of-turn cleanup never perturbs hand counts.
    fun GameTestDriver.resolveStackFully() {
        var guard = 0
        while ((stackSize > 0 || state.pendingDecision != null) && guard++ < 40) {
            if (state.pendingDecision != null) {
                autoResolveDecision()
            } else {
                passPriority(state.priorityPlayerId ?: player1)
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Xande, Dark Mage
    // ---------------------------------------------------------------------------------------------

    test("Xande gets +1/+1 for each noncreature, nonland card in your graveyard") {
        val driver = createDriver(XandeDarkMage)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)
        val active = driver.activePlayer!!

        val xande = driver.putCreatureOnBattlefield(active, "Xande, Dark Mage")

        // Empty graveyard: base 3/3.
        projector.getProjectedPower(driver.state, xande) shouldBe 3
        projector.getProjectedToughness(driver.state, xande) shouldBe 3

        // An instant (noncreature, nonland) counts: 4/4.
        driver.putCardInGraveyard(active, "Lightning Bolt")
        projector.getProjectedPower(driver.state, xande) shouldBe 4
        projector.getProjectedToughness(driver.state, xande) shouldBe 4

        // A creature card does NOT count.
        driver.putCardInGraveyard(active, "Centaur Courser")
        projector.getProjectedPower(driver.state, xande) shouldBe 4

        // A land card does NOT count.
        driver.putCardInGraveyard(active, "Swamp")
        projector.getProjectedPower(driver.state, xande) shouldBe 4

        // A second instant counts: 5/5.
        driver.putCardInGraveyard(active, "Doom Blade")
        projector.getProjectedPower(driver.state, xande) shouldBe 5
        projector.getProjectedToughness(driver.state, xande) shouldBe 5
    }
})
