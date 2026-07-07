package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.FrilledCaveWurm
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Frilled Cave-Wurm (LCI #57): {3}{U} 2/5 Creature — Salamander Wurm
 * "Descend 4 — This creature gets +2/+0 as long as there are four or more permanent
 * cards in your graveyard."
 *
 * Tests:
 *  - With fewer than 4 permanent cards in the graveyard → base stats 2/5.
 *  - With exactly 4 permanent cards in the graveyard → boosted stats 4/5.
 */
class FrilledCaveWurmScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FrilledCaveWurm))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Grizzly Bears" to 20),
            skipMulligans = true
        )
        return driver
    }

    test("base stats 2/5 when fewer than 4 permanent cards are in the graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wurm = driver.putCreatureOnBattlefield(player, "Frilled Cave-Wurm")

        // Put 3 permanent cards into the graveyard — one short of the Descend 4 threshold.
        repeat(3) { driver.putCardInGraveyard(player, "Grizzly Bears") }

        driver.state.projectedState.getPower(wurm) shouldBe 2
        driver.state.projectedState.getToughness(wurm) shouldBe 5
    }

    test("gets +2/+0 (becomes 4/5) when four or more permanent cards are in the graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wurm = driver.putCreatureOnBattlefield(player, "Frilled Cave-Wurm")

        // Put exactly 4 permanent cards (creatures) into the graveyard — meets the threshold.
        repeat(4) { driver.putCardInGraveyard(player, "Grizzly Bears") }

        driver.state.projectedState.getPower(wurm) shouldBe 4
        driver.state.projectedState.getToughness(wurm) shouldBe 5
    }
})
