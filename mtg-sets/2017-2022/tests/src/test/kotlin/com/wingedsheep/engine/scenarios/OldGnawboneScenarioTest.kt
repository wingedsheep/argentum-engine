package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Old Gnawbone (AFR #197) — "Flying. Whenever a creature you control deals combat damage to a player,
 * create that many Treasure tokens."
 *
 * Each creature that connects is its own trigger, carrying its own damage amount.
 */
class OldGnawboneScenarioTest : ScenarioTestBase() {

    init {
        context("Old Gnawbone") {

            test("each creature that connects creates Treasures equal to the damage it dealt") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Old Gnawbone", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Old Gnawbone" to 2, "Grizzly Bears" to 2))
                advanceThroughCombatDamage(game)

                game.getLifeTotal(2) shouldBe 11
                withClue("7 from Old Gnawbone plus 2 from Grizzly Bears") {
                    game.findAllPermanents("Treasure").size shouldBe 9
                }
            }

            test("combat damage dealt to a blocking creature creates nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Old Gnawbone")
                    .withCardOnBattlefield(1, "Centaur Courser", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Centaur Courser" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Centaur Courser")))
                advanceThroughCombatDamage(game)

                game.getLifeTotal(2) shouldBe 20
                game.findAllPermanents("Treasure").size shouldBe 0
            }

            test("an opponent's creature dealing combat damage to you creates nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Old Gnawbone", tapped = true)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 1))
                advanceThroughCombatDamage(game)

                game.getLifeTotal(1) shouldBe 18
                game.findAllPermanents("Treasure").size shouldBe 0
            }
        }
    }

    private fun advanceThroughCombatDamage(game: TestGame) {
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var iterations = 0
        while (game.state.pendingDecision == null &&
            game.state.stack.isNotEmpty() &&
            iterations++ < 20
        ) {
            game.passPriority()
        }
    }
}
