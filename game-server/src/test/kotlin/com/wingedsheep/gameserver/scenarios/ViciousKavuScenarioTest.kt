package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Vicious Kavu:
 * "Whenever this creature attacks, it gets +2/+0 until end of turn."
 *
 * Base stats are 2/2.
 */
class ViciousKavuScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Vicious Kavu attack trigger") {

            test("gets +2/+0 when it attacks") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Vicious Kavu")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attackResult = game.declareAttackers(mapOf("Vicious Kavu" to 2))
                withClue("Declaring attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }
                game.resolveStack()

                val kavuId = game.findPermanent("Vicious Kavu")!!
                val projected = stateProjector.project(game.state)

                withClue("Power should be 2 base + 2 from attacking") {
                    projected.getPower(kavuId) shouldBe 4
                }
                withClue("Toughness stays at base 2 (+2/+0)") {
                    projected.getToughness(kavuId) shouldBe 2
                }
            }
        }
    }
}
