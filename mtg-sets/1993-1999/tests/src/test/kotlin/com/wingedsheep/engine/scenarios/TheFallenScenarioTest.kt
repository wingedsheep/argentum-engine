package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for The Fallen (DRK #53).
 *
 * {1}{B}{B}{B} Creature — Zombie 2/3
 * "At the beginning of your upkeep, this creature deals 1 damage to each opponent and planeswalker
 *  it has dealt damage to this game."
 *
 * The grudge is per-source and per-game, so the tests care about three things: that it starts
 * empty, that combat damage puts an opponent on the list, and that the list survives the turn it
 * was written on.
 */
class TheFallenScenarioTest : ScenarioTestBase() {

    init {
        context("The Fallen") {

            test("does nothing on an upkeep before it has damaged anyone") {
                val game = scenario()
                    .withPlayers("Necromancer", "Victim")
                    .withCardOnBattlefield(1, "The Fallen")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()

                withClue("the grudge list is empty, so the upkeep trigger hits nobody") {
                    game.getLifeTotal(2) shouldBe 20
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("after connecting in combat, every later upkeep pings that opponent") {
                val game = scenario()
                    .withPlayers("Necromancer", "Victim")
                    .withCardOnBattlefield(1, "The Fallen")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("The Fallen" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("2 combat damage from the 2/3") {
                    game.getLifeTotal(2) shouldBe 18
                }

                // Round the table to the Necromancer's next upkeep.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()

                withClue("the grudge outlived the turn it was written on: 18 - 1 = 17") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("and it never turns on its own controller") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
