package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.AdelbertSteiner
import com.wingedsheep.mtg.sets.definitions.fin.cards.LionHeart
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Adelbert Steiner. */
class AdelbertSteinerScenarioTest : FunSpec({

    fun createDriver(vararg cards: com.wingedsheep.sdk.model.CardDefinition): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        cards.forEach { driver.registerCard(it) }
        return driver
    }

    // -----------------------------------------------------------------------------------------
    // Adelbert Steiner
    // -----------------------------------------------------------------------------------------

    test("Adelbert Steiner gets +1/+1 for each Equipment you control") {
        val driver = createDriver(AdelbertSteiner, LionHeart)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        val active = driver.activePlayer!!

        val steiner = driver.putCreatureOnBattlefield(active, "Adelbert Steiner")

        // No Equipment: base 2/1.
        driver.state.projectedState.getPower(steiner) shouldBe 2
        driver.state.projectedState.getToughness(steiner) shouldBe 1

        // One Equipment (unattached) you control: 3/2.
        driver.putPermanentOnBattlefield(active, "Lion Heart")
        driver.state.projectedState.getPower(steiner) shouldBe 3
        driver.state.projectedState.getToughness(steiner) shouldBe 2

        // Two Equipment: 4/3.
        driver.putPermanentOnBattlefield(active, "Lion Heart")
        driver.state.projectedState.getPower(steiner) shouldBe 4
        driver.state.projectedState.getToughness(steiner) shouldBe 3
    }
})
