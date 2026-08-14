package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.MatoyaArchonElder
import com.wingedsheep.mtg.sets.definitions.inv.cards.Opt
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Matoya, Archon Elder. */
class MatoyaArchonElderScenarioTest : FunSpec({

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
    // Matoya, Archon Elder
    // ---------------------------------------------------------------------------------------------

    test("Matoya draws a card whenever you scry") {
        val driver = createDriver(MatoyaArchonElder, Opt)
        driver.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!

        driver.putCreatureOnBattlefield(active, "Matoya, Archon Elder")
        val opt = driver.putCardInHand(active, "Opt")
        driver.giveMana(active, Color.BLUE, 1)

        val handBefore = driver.getHandSize(active) // includes Opt

        driver.castSpell(active, opt)
        driver.resolveStackFully()

        // Opt: −1 (cast) +1 (its own draw) +1 (Matoya's scry-triggered draw) = handBefore + 1.
        driver.getHandSize(active) shouldBe handBefore + 1
    }
})
