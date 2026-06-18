package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Wildfire Wickerfolk (DSK #239) — {R}{G} Artifact Creature — Scarecrow, 3/2,
 * Haste.
 *
 * "Delirium — This creature gets +1/+1 and has trample as long as there are four or more card
 *  types among cards in your graveyard."
 *
 * Modeled as two [ConditionalStaticAbility] (ModifyStats +1/+1 and GrantKeyword TRAMPLE) gated by
 * a graveyard distinct-card-type count ≥ 4 (Aggregation.DISTINCT_TYPES).
 */
class WildfireWickerfolkScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Wildfire Wickerfolk delirium") {

            test("with fewer than four card types in graveyard it is a vanilla 3/2 without trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wildfire Wickerfolk")
                    // 3 card types: creature, instant, sorcery
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wicker = game.findPermanent("Wildfire Wickerfolk")!!

                withClue("base 3/2 with only 3 card types in graveyard") {
                    projector.getProjectedPower(game.state, wicker) shouldBe 3
                    projector.getProjectedToughness(game.state, wicker) shouldBe 2
                }
                withClue("no trample without delirium") {
                    projector.hasProjectedKeyword(game.state, wicker, Keyword.TRAMPLE) shouldBe false
                }
            }

            test("with four or more card types it gets +1/+1 and trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wildfire Wickerfolk")
                    // 4 card types: creature, instant, sorcery, artifact
                    .withCardInGraveyard(1, "Grizzly Bears")        // creature
                    .withCardInGraveyard(1, "Lightning Bolt")       // instant
                    .withCardInGraveyard(1, "Lava Spike")           // sorcery
                    .withCardInGraveyard(1, "Ornithopter")          // artifact
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wicker = game.findPermanent("Wildfire Wickerfolk")!!

                withClue("delirium active: +1/+1 -> 4/3") {
                    projector.getProjectedPower(game.state, wicker) shouldBe 4
                    projector.getProjectedToughness(game.state, wicker) shouldBe 3
                }
                withClue("delirium active: trample granted") {
                    projector.hasProjectedKeyword(game.state, wicker, Keyword.TRAMPLE) shouldBe true
                }
            }
        }
    }
}
