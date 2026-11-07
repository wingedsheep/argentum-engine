package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scour the Laboratory (EMN #73) — {4}{U}{U} Instant.
 *
 *   Delirium — This spell costs {2} less to cast if there are four or more card types among cards
 *   in your graveyard.
 *   Draw three cards.
 *
 * Four Islands can pay for it only once delirium knocks {2} off the cost.
 */
class ScourTheLaboratoryScenarioTest : ScenarioTestBase() {

    init {
        context("Scour the Laboratory delirium cost reduction") {

            test("with three card types in the graveyard, four lands can't cast it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Scour the Laboratory")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Scour the Laboratory").error shouldNotBe null
            }

            test("with four card types in the graveyard, it costs {2}{U}{U} and draws three") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Scour the Laboratory")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Scour the Laboratory").error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 3
                game.isInGraveyard(1, "Scour the Laboratory") shouldBe true
            }
        }
    }
}
