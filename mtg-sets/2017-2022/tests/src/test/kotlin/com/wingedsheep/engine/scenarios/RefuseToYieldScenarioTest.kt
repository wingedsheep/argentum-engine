package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Refuse to Yield (SNC #27) — {1}{W} Instant.
 *
 * "Target creature gets +2/+7 until end of turn. Untap it."
 */
class RefuseToYieldScenarioTest : ScenarioTestBase() {

    init {
        context("Refuse to Yield") {
            test("pumps the target +2/+7 and untaps it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withCardInHand(1, "Refuse to Yield")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Refuse to Yield", bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears is a 4/9 until end of turn") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 9
                }
                withClue("the target is untapped") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
                }
            }
        }
    }
}
