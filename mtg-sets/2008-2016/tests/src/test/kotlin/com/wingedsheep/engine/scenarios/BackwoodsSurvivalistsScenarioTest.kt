package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Backwoods Survivalists (Eldritch Moon #150) — {3}{G} Creature — Human Warrior, 4/3.
 *
 * "Delirium — This creature gets +1/+1 and has trample as long as there are four or more card
 *  types among cards in your graveyard."
 */
class BackwoodsSurvivalistsScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Backwoods Survivalists delirium") {

            test("with three card types in the graveyard it is a 4/3 without trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Backwoods Survivalists")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val survivalists = game.findPermanent("Backwoods Survivalists")!!

                withClue("no delirium: base 4/3, no trample") {
                    projector.getProjectedPower(game.state, survivalists) shouldBe 4
                    projector.getProjectedToughness(game.state, survivalists) shouldBe 3
                    projector.hasProjectedKeyword(game.state, survivalists, Keyword.TRAMPLE) shouldBe false
                }
            }

            test("with four card types in the graveyard it is a 5/4 with trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Backwoods Survivalists")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val survivalists = game.findPermanent("Backwoods Survivalists")!!

                withClue("delirium: +1/+1 -> 5/4 and trample") {
                    projector.getProjectedPower(game.state, survivalists) shouldBe 5
                    projector.getProjectedToughness(game.state, survivalists) shouldBe 4
                    projector.hasProjectedKeyword(game.state, survivalists, Keyword.TRAMPLE) shouldBe true
                }
            }
        }
    }
}
