package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thopter Mechanic (BRO #68) — whenever you draw your second card each turn, put a +1/+1 counter
 * on this creature. When this creature dies, create a 1/1 colorless Thopter artifact creature
 * token with flying.
 */
class ThopterMechanicScenarioTest : ScenarioTestBase() {

    init {
        test("drawing the second card of the turn adds a +1/+1 counter") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Thopter Mechanic")
                .withCardInHand(1, "Divination")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Divination").error shouldBe null
            game.resolveStack()

            val mechanic = game.findPermanent("Thopter Mechanic")!!
            game.state.getEntity(mechanic)!!.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }

        test("dying creates a 1/1 flying Thopter token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Thopter Mechanic")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mechanic = game.findPermanent("Thopter Mechanic")!!
            game.castSpell(1, "Lightning Bolt", mechanic).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Thopter Mechanic") shouldBe true
            game.findPermanent("Thopter Token") shouldNotBe null
        }
    }
}
