package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.ScorpionSentinel
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Scorpion Sentinel. */
class ScorpionSentinelScenarioTest : FunSpec({

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
    // Scorpion Sentinel
    // ---------------------------------------------------------------------------------------------

    test("Scorpion Sentinel gets +3/+0 only while you control seven or more lands") {
        val driver = createDriver(ScorpionSentinel)
        driver.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20)
        val active = driver.activePlayer!!

        val sentinel = driver.putCreatureOnBattlefield(active, "Scorpion Sentinel")

        // Six lands: base 1/4.
        repeat(6) { driver.putLandOnBattlefield(active, "Island") }
        projector.getProjectedPower(driver.state, sentinel) shouldBe 1
        projector.getProjectedToughness(driver.state, sentinel) shouldBe 4

        // Seventh land: +3/+0 → 4/4.
        driver.putLandOnBattlefield(active, "Island")
        projector.getProjectedPower(driver.state, sentinel) shouldBe 4
        projector.getProjectedToughness(driver.state, sentinel) shouldBe 4
    }
})
