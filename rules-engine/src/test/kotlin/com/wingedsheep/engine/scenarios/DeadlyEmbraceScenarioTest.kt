package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.DeadlyEmbrace
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Deadly Embrace. */
class DeadlyEmbraceScenarioTest : FunSpec({

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
    // Deadly Embrace
    // ---------------------------------------------------------------------------------------------

    test("Deadly Embrace destroys the creature and draws for it (counted as died this turn)") {
        val driver = createDriver(DeadlyEmbrace)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val victim = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val spell = driver.putCardInHand(active, "Deadly Embrace")
        driver.giveMana(active, Color.BLACK, 5) // {3}{B}{B}

        val handBefore = driver.getHandSize(active) // includes Deadly Embrace

        driver.castSpell(active, spell, listOf(victim))
        driver.resolveStackFully()

        // The creature is destroyed and counts as having died this turn → draw exactly 1.
        // Hand: handBefore −1 (spell cast) +1 (draw) = handBefore. Drawing 0 (wrong timing)
        // would leave handBefore − 1.
        driver.state.getBattlefield().contains(victim) shouldBe false
        driver.getHandSize(active) shouldBe handBefore
    }
})
