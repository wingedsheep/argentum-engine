package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Ivory Tower (ATQ #53).
 *
 * {1} Artifact
 * "At the beginning of your upkeep, you gain X life, where X is the number of cards in your
 *  hand minus 4."
 *
 * The trigger only fires on a real step transition into UPKEEP, so each scenario builds at
 * the start of the turn (UNTAP) and passes into UPKEEP, then resolves the trigger.
 */
class IvoryTowerScenarioTest : ScenarioTestBase() {

    /** Build a game with [handCount] generic cards in hand and advance into upkeep. */
    private fun runUpkeep(handCount: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Ivory Tower")
            .withLifeTotal(1, 20)
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UNTAP)
        repeat(handCount) { builder.withCardInHand(1, "Mountain") }
        val game = builder.build()

        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        context("Ivory Tower") {

            test("hand of 7 gains 3 life (7 - 4)") {
                val game = runUpkeep(7)
                withClue("Hand of 7 → gain 3 → 20 + 3 = 23") {
                    game.handSize(1) shouldBe 7
                    game.getLifeTotal(1) shouldBe 23
                }
            }

            test("hand of 4 gains 0 life (4 - 4)") {
                val game = runUpkeep(4)
                withClue("Hand of 4 → gain 0 → life unchanged at 20") {
                    game.handSize(1) shouldBe 4
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("hand of 2 gains 0 life, never negative (floored at 0)") {
                val game = runUpkeep(2)
                withClue("Hand of 2 → 2 - 4 = -2 floored to 0 → life unchanged at 20") {
                    game.handSize(1) shouldBe 2
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
