package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Thopter Engineer (ORI #165) — {2}{R} Creature — Human Artificer, 1/3.
 *
 *   When this creature enters, create a 1/1 colorless Thopter artifact creature token with flying.
 *   Artifact creatures you control have haste.
 */
class ThopterEngineerScenarioTest : ScenarioTestBase() {

    init {
        context("Thopter Engineer") {

            test("creates a flying Thopter; artifact creatures you control have haste, others don't") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Thopter Engineer")
                    .withCardOnBattlefield(1, "Ornithopter", summoningSickness = true)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Thopter Engineer")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val thopters = game.findPermanents("Thopter Token")
                withClue("exactly one Thopter token was created") { thopters.size shouldBe 1 }
                val thopter = thopters.single()
                val projected = game.state.projectedState
                withClue("the Thopter flies and, as an artifact creature, has haste") {
                    projected.hasKeyword(thopter, Keyword.FLYING) shouldBe true
                    projected.hasKeyword(thopter, Keyword.HASTE) shouldBe true
                }

                val myOrnithopter = game.findPermanents("Ornithopter")
                    .single { game.state.projectedState.getController(it) == game.player1Id }
                val theirOrnithopter = game.findPermanents("Ornithopter")
                    .single { game.state.projectedState.getController(it) == game.player2Id }
                withClue("your other artifact creature has haste") {
                    projected.hasKeyword(myOrnithopter, Keyword.HASTE) shouldBe true
                }
                withClue("non-artifact creatures and opponents' artifact creatures don't") {
                    projected.hasKeyword(game.findPermanent("Thopter Engineer")!!, Keyword.HASTE) shouldBe false
                    projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.HASTE) shouldBe false
                    projected.hasKeyword(theirOrnithopter, Keyword.HASTE) shouldBe false
                }
            }
        }
    }
}
