package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.TerisianMindbreaker
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Terisian Mindbreaker (BRO #83) — {7} Artifact Creature — Juggernaut, 6/4.
 *
 *   "Whenever this creature attacks, defending player mills half their library, rounded up."
 *
 * The mill count is a zone-count amount read off `Player.DefendingPlayer` at resolution — the
 * first card to aim a library count at the defending player — and rounds an odd library up.
 */
class TerisianMindbreakerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TerisianMindbreaker)
        return driver
    }

    test("attack trigger mills half the defending player's library, rounded up") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val mindbreaker = driver.putCreatureOnBattlefield(attacker, "Terisian Mindbreaker")
        driver.removeSummoningSickness(mindbreaker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val defenderLibraryBefore = driver.state.getLibrary(defender).size
        val attackerLibraryBefore = driver.state.getLibrary(attacker).size
        // 40-card deck minus a 7-card opening hand: an odd library pins the rounding direction.
        (defenderLibraryBefore % 2) shouldBe 1

        driver.declareAttackers(attacker, listOf(mindbreaker), defender)
        driver.bothPass()

        val milled = (defenderLibraryBefore + 1) / 2
        driver.state.getLibrary(defender).size shouldBe (defenderLibraryBefore - milled)
        driver.getGraveyard(defender).size shouldBe milled
        driver.state.getLibrary(attacker).size shouldBe attackerLibraryBefore
    }
})
