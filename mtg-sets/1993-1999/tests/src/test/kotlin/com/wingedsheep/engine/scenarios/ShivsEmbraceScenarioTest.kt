package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.usg.cards.ShivsEmbrace
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Shiv's Embrace (USG #216) — "Enchanted creature gets +2/+2 and has flying. {R}: Enchanted
 * creature gets +1/+0 until end of turn."
 */
class ShivsEmbraceScenarioTest : ScenarioTestBase() {

    init {
        context("Shiv's Embrace") {
            test("enchanted creature gets +2/+2, flying, and firebreathing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Shiv's Embrace", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 4
                projected.hasKeyword(bears, Keyword.FLYING) shouldBe true

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Shiv's Embrace")!!,
                        abilityId = ShivsEmbrace.activatedAbilities.first().id,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 5
                game.state.projectedState.getToughness(bears) shouldBe 4
            }
        }
    }
}
