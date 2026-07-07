package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.EchoOfDusk
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Echo of Dusk (LCI #104): {1}{B} 2/2 Creature — Vampire Spirit
 * "Descend 4 — As long as there are four or more permanent cards in your graveyard,
 *  this creature gets +1/+1 and has lifelink."
 *
 * Tests:
 *  - With fewer than 4 permanent cards in the graveyard → base stats 2/2, no lifelink.
 *  - With exactly 4 permanent cards in the graveyard → boosted stats 3/3 and has lifelink.
 */
class EchoOfDuskScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EchoOfDusk))
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Grizzly Bears" to 20),
            skipMulligans = true
        )
        return driver
    }

    test("base stats 2/2 and no lifelink when fewer than 4 permanent cards are in the graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val echo = driver.putCreatureOnBattlefield(player, "Echo of Dusk")

        // Put 3 permanent cards into the graveyard — one short of the Descend 4 threshold.
        repeat(3) { driver.putCardInGraveyard(player, "Grizzly Bears") }

        driver.state.projectedState.getPower(echo) shouldBe 2
        driver.state.projectedState.getToughness(echo) shouldBe 2

        val projected = projector.project(driver.state)
        projected.hasKeyword(echo, Keyword.LIFELINK) shouldBe false
    }

    test("gets +1/+1 (becomes 3/3) and gains lifelink when four or more permanent cards are in the graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val echo = driver.putCreatureOnBattlefield(player, "Echo of Dusk")

        // Put exactly 4 permanent cards (creatures) into the graveyard — meets the Descend 4 threshold.
        repeat(4) { driver.putCardInGraveyard(player, "Grizzly Bears") }

        driver.state.projectedState.getPower(echo) shouldBe 3
        driver.state.projectedState.getToughness(echo) shouldBe 3

        val projected = projector.project(driver.state)
        projected.hasKeyword(echo, Keyword.LIFELINK) shouldBe true
    }
})
