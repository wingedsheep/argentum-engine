package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Goblin Caves — "As long as enchanted land is a basic Mountain, Goblin
 * creatures get +0/+2."
 *
 * Three things can quietly go wrong and each gets a case: the anthem firing off the *wrong* land
 * (so a Forest-enchanted Caves must give nothing), the subtype filter leaking onto non-Goblins, and
 * the "you control" clause the card doesn't have being added by accident (so an opponent's Goblin
 * must be buffed too).
 */
class GoblinCavesScenarioTest : ScenarioTestBase() {

    init {
        context("Goblin Caves") {

            test("Goblins on both sides gain toughness while it enchants a basic Mountain") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardAttachedTo(1, "Goblin Caves", "Mountain")
                    .withCardOnBattlefield(1, "Goblin Balloon Brigade")
                    .withCardOnBattlefield(2, "Marsh Goblins")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mine = game.findPermanent("Goblin Balloon Brigade")!!
                val theirs = game.findPermanent("Marsh Goblins")!!
                val bear = game.findPermanent("Grizzly Bears")!!

                withClue("Goblin Balloon Brigade is a 1/1; +0/+2 makes it 1/3") {
                    game.state.projectedState.getPower(mine) shouldBe 1
                    game.state.projectedState.getToughness(mine) shouldBe 3
                }
                withClue("the card names no controller, so the opponent's Goblin gains it too") {
                    // Marsh Goblins is a 1/1 as well.
                    game.state.projectedState.getToughness(theirs) shouldBe 3
                }
                withClue("a non-Goblin is untouched") {
                    game.state.projectedState.getToughness(bear) shouldBe 2
                }
            }

            test("enchanting something that isn't a basic Mountain gives nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardAttachedTo(1, "Goblin Caves", "Forest")
                    .withCardOnBattlefield(1, "Goblin Balloon Brigade")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goblin = game.findPermanent("Goblin Balloon Brigade")!!
                withClue("the switch is the enchanted land, not the Aura merely being on the battlefield") {
                    game.state.projectedState.getToughness(goblin) shouldBe 1
                }
            }
        }
    }
}
