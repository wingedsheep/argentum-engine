package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Witch's Web (VOW #227) — {1}{G} Instant.
 *
 *   Target creature gets +3/+3 and gains reach until end of turn. Untap it.
 *
 * Exercises the combined pump + keyword grant + untap on a single target.
 */
class WitchsWebScenarioTest : ScenarioTestBase() {

    init {
        context("Witch's Web — pump + reach + untap") {

            test("target creature gets +3/+3, gains reach, and is untapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Witch's Web")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true) // 2/2 tapped target
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Grizzly Bears does not start with reach") {
                    game.state.projectedState.hasKeyword(bears, Keyword.REACH) shouldBe false
                }
                withClue("Grizzly Bears starts tapped") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }

                game.castSpell(1, "Witch's Web", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears gets +3/+3 (becomes 5/5)") {
                    game.state.projectedState.getPower(bears) shouldBe 5
                    game.state.projectedState.getToughness(bears) shouldBe 5
                }
                withClue("Grizzly Bears gains reach") {
                    game.state.projectedState.hasKeyword(bears, Keyword.REACH) shouldBe true
                }
                withClue("Grizzly Bears is untapped") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
                }
            }
        }
    }
}
