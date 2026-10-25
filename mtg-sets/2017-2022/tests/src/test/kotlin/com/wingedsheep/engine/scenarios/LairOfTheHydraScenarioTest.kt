package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.LairOfTheHydra
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Lair of the Hydra (AFR #259) — Land.
 *
 * "{X}{G}: Until end of turn, this land becomes an X/X green Hydra creature. It's still a land.
 *  X can't be 0."
 *
 * Pins the X-sized animation and the X ≥ 1 floor.
 */
class LairOfTheHydraScenarioTest : FunSpec({
    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.registerCard(LairOfTheHydra)
        it.initMirrorMatch(deck = Deck.of("Forest" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    val animateId = LairOfTheHydra.activatedAbilities[1].id

    test("X = 3 makes it a 3/3 green Hydra land until end of turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val lair = driver.putLandOnBattlefield(player, "Lair of the Hydra")

        driver.giveMana(player, Color.GREEN, 4)
        driver.submitSuccess(ActivateAbility(player, lair, animateId, xValue = 3))
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.hasType(lair, "LAND") shouldBe true
        projected.hasType(lair, "CREATURE") shouldBe true
        projected.hasSubtype(lair, "Hydra") shouldBe true
        projected.getPower(lair) shouldBe 3
        projected.getToughness(lair) shouldBe 3
        projected.getColors(lair) shouldBe setOf("GREEN")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        driver.state.projectedState.hasType(lair, "CREATURE") shouldBe false
    }

    test("X can't be 0") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val lair = driver.putLandOnBattlefield(player, "Lair of the Hydra")

        driver.giveMana(player, Color.GREEN, 1)
        driver.submitExpectFailure(ActivateAbility(player, lair, animateId, xValue = 0))
        driver.state.projectedState.hasType(lair, "CREATURE") shouldBe false
    }
})
