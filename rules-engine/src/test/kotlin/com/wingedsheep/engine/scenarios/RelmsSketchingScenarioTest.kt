package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.RelmsSketching
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Relm's Sketching. */
class RelmsSketchingScenarioTest : FunSpec({

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
    // Relm's Sketching
    // ---------------------------------------------------------------------------------------------

    test("Relm's Sketching creates a token copy of the targeted creature") {
        val driver = createDriver(RelmsSketching)
        driver.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        val original = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val spell = driver.putCardInHand(active, "Relm's Sketching")
        driver.giveMana(active, Color.BLUE, 4) // {2}{U}{U}

        driver.castSpell(active, spell, listOf(original))
        driver.resolveStackFully()

        val coursers = driver.state.getBattlefield()
            .filter { driver.getCardName(it) == "Centaur Courser" }
        coursers.size shouldBe 2

        val token = coursers.first { it != original }
        driver.state.getEntity(token)!!.has<TokenComponent>() shouldBe true
    }
})
