package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.AlBhedSalvagers
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Al Bhed Salvagers. */
class AlBhedSalvagersScenarioTest : FunSpec({

    fun createDriver(vararg cards: com.wingedsheep.sdk.model.CardDefinition): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        cards.forEach { driver.registerCard(it) }
        return driver
    }

    // -----------------------------------------------------------------------------------------
    // Al Bhed Salvagers
    // -----------------------------------------------------------------------------------------

    test("Al Bhed Salvagers drains when a creature you control dies") {
        val driver = createDriver(AlBhedSalvagers)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val youLifeBefore = driver.getLifeTotal(you)
        val oppLifeBefore = driver.getLifeTotal(opponent)

        driver.putCreatureOnBattlefield(you, "Al Bhed Salvagers")
        val fodder = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3

        // Opponent bolts your creature; it dies and triggers the drain.
        driver.giveMana(opponent, Color.RED, 1)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.passPriority(you)
        driver.castSpellWithTargets(opponent, bolt, listOf(ChosenTarget.Permanent(fodder))).error shouldBe null

        // Resolve the bolt, then the drain trigger (targeting the lone opponent).
        var safety = 0
        while (driver.stackSize > 0 && safety < 30) {
            val pending = driver.state.pendingDecision
            if (pending != null) {
                driver.submitTargetSelection(pending.playerId, listOf(opponent))
            } else {
                driver.bothPass()
            }
            safety++
        }

        driver.getLifeTotal(opponent) shouldBe (oppLifeBefore - 1)
        driver.getLifeTotal(you) shouldBe (youLifeBefore + 1)
    }
})
