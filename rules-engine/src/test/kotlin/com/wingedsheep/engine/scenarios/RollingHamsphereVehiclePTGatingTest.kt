package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.JollyGerbils
import com.wingedsheep.mtg.sets.definitions.blc.cards.RollingHamsphere
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rolling Hamsphere has a permanent static "This Vehicle gets +1/+1 for each Hamster
 * you control." Vehicles are noncreature artifacts until crewed. Per CR 208.3, a
 * noncreature permanent has no power/toughness even if printed values appear on it,
 * so the +X/+X bonus must only apply while Rolling Hamsphere is currently a creature.
 *
 * Regression test for: after Crew's end-of-turn cleanup turned the Vehicle back into
 * a noncreature artifact, the Layer 7c modification kept firing and the projected P/T
 * stayed at 4+X / 4+X instead of reverting to the printed 4/4.
 */
class RollingHamsphereVehiclePTGatingTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(RollingHamsphere, JollyGerbils))
        return driver
    }

    test("uncrewed Rolling Hamsphere ignores its own +X/+X bonus while not a creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val hamsphere = driver.putPermanentOnBattlefield(you, "Rolling Hamsphere")
        driver.putCreatureOnBattlefield(you, "Jolly Gerbils")
        driver.putCreatureOnBattlefield(you, "Jolly Gerbils")

        // 2 Hamsters on the battlefield, but the Vehicle isn't a creature yet, so the
        // static "gets +1/+1 for each Hamster" doesn't modify its P/T. Printed 4/4.
        projector.getProjectedPower(driver.state, hamsphere) shouldBe 4
        projector.getProjectedToughness(driver.state, hamsphere) shouldBe 4
    }

    test("crewed Rolling Hamsphere picks up +X/+X, then drops it again at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val hamsphere = driver.putPermanentOnBattlefield(you, "Rolling Hamsphere")
        val gerbil1 = driver.putCreatureOnBattlefield(you, "Jolly Gerbils")
        val gerbil2 = driver.putCreatureOnBattlefield(you, "Jolly Gerbils")

        // Before crewing: not a creature → no buff.
        projector.getProjectedPower(driver.state, hamsphere) shouldBe 4
        projector.getProjectedToughness(driver.state, hamsphere) shouldBe 4

        // Crew it. Jolly Gerbils is 2/3, so two of them well over Crew 3.
        driver.submit(CrewVehicle(you, hamsphere, listOf(gerbil1, gerbil2))).isSuccess shouldBe true
        driver.bothPass() // resolve the Crew ability

        // Now a creature with 2 Hamsters present (the two Jolly Gerbils): 4+2 / 4+2.
        projector.getProjectedPower(driver.state, hamsphere) shouldBe 6
        projector.getProjectedToughness(driver.state, hamsphere) shouldBe 6

        // Run out the turn. The BecomeCreature floating effects (CREATURE type and
        // SetPowerToughness 4/4) end in cleanup; the static +X/+X must go dormant with them.
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()

        projector.getProjectedPower(driver.state, hamsphere) shouldBe 4
        projector.getProjectedToughness(driver.state, hamsphere) shouldBe 4
    }
})
