package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Lookout's Dispersal (XLN #62) — {2}{U} Instant.
 *
 *   This spell costs {1} less to cast if you control a Pirate.
 *   Counter target spell unless its controller pays {4}.
 */
class LookoutsDispersalScenarioTest : ScenarioTestBase() {

    init {
        context("Lookout's Dispersal") {

            test("with a Pirate it costs {1}{U} and counters a spell whose controller can't pay {4}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lookout's Dispersal")
                    .withCardOnBattlefield(1, "Storm Fleet Spy") // Human Pirate
                    .withLandsOnBattlefield(1, "Island", 2) // only {1}{U}
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2) // exactly {1}{G}, nothing left to pay
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                val cast = game.castSpellTargetingStackSpell(1, "Lookout's Dispersal", "Grizzly Bears")
                withClue("Discounted cast should succeed with two Islands: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears is countered") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("without a Pirate two lands are not enough to cast it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lookout's Dispersal")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                val cast = game.castSpellTargetingStackSpell(1, "Lookout's Dispersal", "Grizzly Bears")
                withClue("Full {2}{U} cost can't be paid with two Islands") {
                    cast.error.shouldNotBeNull()
                }
            }
        }
    }
}
