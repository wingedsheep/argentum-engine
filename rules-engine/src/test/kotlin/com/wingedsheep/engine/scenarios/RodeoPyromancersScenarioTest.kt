package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rodeo Pyromancers (OTJ #143) — {3}{R} Human Mercenary, 3/4.
 *
 *   "Whenever you cast your first spell each turn, add {R}{R}."
 *
 * Verifies the first spell the controller casts each turn triggers the ability and adds {R}{R}
 * to their mana pool, and that a second spell that same turn does not trigger it again.
 */
class RodeoPyromancersScenarioTest : ScenarioTestBase() {

    init {
        context("Rodeo Pyromancers") {

            test("first spell each turn adds {R}{R}; second spell does not trigger again") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rodeo Pyromancers")
                    .withCardInHand(1, "Grizzly Bears") // first spell, {1}{G}
                    .withCardInHand(1, "Goblin Guide") // second spell, {R}
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun pool(): ManaPoolComponent =
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

                // Cast first spell — {1}{G} paid from the two Forests.
                val first = game.castSpell(1, "Grizzly Bears")
                withClue("Casting the first spell should succeed: ${first.error}") {
                    first.error shouldBe null
                }
                // Resolve the Rodeo trigger (and the creature spell).
                game.resolveStack()

                withClue("First spell triggers Rodeo Pyromancers, adding {R}{R}") {
                    pool().red shouldBe 2
                }

                // Cast a second spell this turn — Goblin Guide's {R} is paid from the pooled red.
                // No lands remain, so the cast is only possible because the {R}{R} is in the pool.
                val second = game.castSpell(1, "Goblin Guide")
                withClue("Casting the second spell should succeed (paid from pooled red): ${second.error}") {
                    second.error shouldBe null
                }
                game.resolveStack()

                withClue("Second spell does NOT trigger Rodeo again: only one {R} remains (2 added, 1 spent)") {
                    // If a second trigger had fired it would have added another {R}{R}, leaving 3.
                    pool().red shouldBe 1
                }
            }
        }
    }
}
