package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eoe.cards.HemosymbicMite
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Hemosymbic Mite (EOE #190).
 *
 * Hemosymbic Mite {G}
 * Creature — Mite 1/1
 * Whenever this creature becomes tapped, another target creature you control gets +X/+X until
 * end of turn, where X is this creature's power.
 *
 * Regression: the "another target creature" clause must exclude the Mite itself — it cannot be
 * its own target.
 */
class HemosymbicMiteTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HemosymbicMite)
        return driver
    }

    test("Hemosymbic Mite cannot target itself with its becomes-tapped trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        val mite = driver.putCreatureOnBattlefield(activePlayer, "Hemosymbic Mite")
        driver.removeSummoningSickness(mite)
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attacking taps the Mite, firing its "becomes tapped" trigger, which pauses for a target.
        driver.declareAttackers(activePlayer, listOf(mite), opponent)

        // The Mite must NOT be a legal target ("another"); the other creature you control must be.
        val decision = driver.pendingDecision as? ChooseTargetsDecision
            ?: error("Expected a ChooseTargetsDecision for the becomes-tapped trigger")
        val legal = decision.legalTargets[0] ?: emptyList()
        legal shouldNotContain mite
        legal shouldContain bears
    }

    test("Hemosymbic Mite buffs another creature you control by its power") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        val mite = driver.putCreatureOnBattlefield(activePlayer, "Hemosymbic Mite")
        driver.removeSummoningSickness(mite)
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(mite), opponent)

        // Target the other creature; X = Mite's power (1), so +1/+1 (Grizzly Bears 2/2 -> 3/3).
        driver.submitTargetSelection(activePlayer, listOf(bears)).isSuccess shouldBe true
        driver.bothPass() // resolve the trigger

        projector.getProjectedPower(driver.state, bears) shouldBe 3
        projector.getProjectedToughness(driver.state, bears) shouldBe 3
    }
})
