package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

class VeteranCavalierScenarioTest : ScenarioTestBase() {
    init {
        test("attacks without tapping and deals its two combat damage") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Veteran Cavalier", summoningSickness = false)
                .withActivePlayer(1)
                .build()

            val cavalier = game.findPermanent("Veteran Cavalier")!!
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Veteran Cavalier" to 2)).error shouldBe null
            game.state.getEntity(cavalier)?.has<TappedComponent>() shouldBe false
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 18
            game.state.getEntity(cavalier)?.has<TappedComponent>() shouldBe false
        }

        test("vigilance does not allow an already tapped creature to attack") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Veteran Cavalier", tapped = true, summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Veteran Cavalier" to 2)).error.shouldNotBeNull()
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
