package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dusk Feaster (EMN #88) — {5}{B}{B} Creature — Vampire, 4/5.
 *
 *   Delirium — This spell costs {2} less to cast if there are four or more card types among cards
 *   in your graveyard.
 *   Flying
 *
 * Five Swamps can pay for it only once delirium knocks {2} off the cost.
 */
class DuskFeasterScenarioTest : ScenarioTestBase() {

    init {
        context("Dusk Feaster delirium cost reduction") {

            test("with three card types in the graveyard, five lands can't cast it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dusk Feaster")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Dusk Feaster").error shouldNotBe null
                game.isOnBattlefield("Dusk Feaster") shouldBe false
            }

            test("with four card types in the graveyard, it costs {3}{B}{B}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dusk Feaster")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Dusk Feaster").error shouldBe null
                game.resolveStack()

                val feaster = game.findPermanent("Dusk Feaster")!!
                game.state.projectedState.hasKeyword(feaster, Keyword.FLYING) shouldBe true
            }
        }
    }
}
