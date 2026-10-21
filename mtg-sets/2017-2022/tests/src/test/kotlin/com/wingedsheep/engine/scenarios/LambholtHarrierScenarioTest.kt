package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Lambholt Harrier (MID #145) — {1}{R} 2/2. "{3}{R}: Target creature can't block this turn."
 */
class LambholtHarrierScenarioTest : ScenarioTestBase() {

    init {
        context("Lambholt Harrier") {

            test("target creature can't block this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lambholt Harrier", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.cantBlock(bears) shouldBe false

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Lambholt Harrier")!!,
                        abilityId = cardRegistry.getCard("Lambholt Harrier")!!.activatedAbilities.first().id,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.state.projectedState.cantBlock(bears) shouldBe true
            }
        }
    }
}
