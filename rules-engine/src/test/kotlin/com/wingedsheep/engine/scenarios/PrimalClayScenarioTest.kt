package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Primal Clay (ATQ #61).
 *
 * {4} Artifact Creature — Shapeshifter, star/star
 * "As this creature enters, it becomes your choice of a 3/3 artifact creature, a 2/2 artifact
 *  creature with flying, or a 1/6 Wall artifact creature with defender in addition to its other
 *  types."
 *
 * Each test casts Primal Clay, answers the as-it-enters MODE choice with a different option, and
 * verifies the chosen mode's fixed P/T and keyword/type.
 */
class PrimalClayScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        // Mode ids declared on the card, in option order.
        fun playAndChoose(modeIndex: Int): Pair<TestGame, com.wingedsheep.sdk.model.EntityId> {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Primal Clay")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Primal Clay").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            withClue("entering Primal Clay prompts the mode choice") { (decision != null) shouldBe true }
            game.submitDecision(OptionChosenResponse(decision!!.id, modeIndex))
            game.resolveStack()

            return game to game.findPermanent("Primal Clay")!!
        }

        context("Primal Clay") {

            test("choosing the 3/3 mode: a 3/3 with no evasion") {
                val (game, clay) = playAndChoose(0)
                val projected = stateProjector.project(game.state)
                withClue("3/3") {
                    projected.getPower(clay) shouldBe 3
                    projected.getToughness(clay) shouldBe 3
                }
                projected.hasKeyword(clay, Keyword.FLYING) shouldBe false
                projected.hasKeyword(clay, Keyword.DEFENDER) shouldBe false
            }

            test("choosing the 2/2 flying mode: a 2/2 with flying") {
                val (game, clay) = playAndChoose(1)
                val projected = stateProjector.project(game.state)
                withClue("2/2") {
                    projected.getPower(clay) shouldBe 2
                    projected.getToughness(clay) shouldBe 2
                }
                withClue("has flying") { projected.hasKeyword(clay, Keyword.FLYING) shouldBe true }
                projected.hasKeyword(clay, Keyword.DEFENDER) shouldBe false
            }

            test("choosing the 1/6 defender mode: a 1/6 Wall with defender") {
                val (game, clay) = playAndChoose(2)
                val projected = stateProjector.project(game.state)
                withClue("1/6") {
                    projected.getPower(clay) shouldBe 1
                    projected.getToughness(clay) shouldBe 6
                }
                withClue("has defender") { projected.hasKeyword(clay, Keyword.DEFENDER) shouldBe true }
                withClue("is a Wall") { projected.getSubtypes(clay).contains("Wall") shouldBe true }
                projected.hasKeyword(clay, Keyword.FLYING) shouldBe false
            }
        }
    }
}
