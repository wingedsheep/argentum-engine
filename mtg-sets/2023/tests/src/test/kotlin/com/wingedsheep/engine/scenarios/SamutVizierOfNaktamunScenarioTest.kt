package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Samut, Vizier of Naktamun (MAT #45): "Whenever a creature you control deals combat damage to a
 * player, if that creature entered this turn, draw a card."
 *
 * The intervening-if reads the *damaging* creature (the triggering entity), not Samut — only the
 * creatures that entered this turn draw, one card each.
 */
class SamutVizierOfNaktamunScenarioTest : ScenarioTestBase() {

    private fun attackAndFinishCombat(game: TestGame, attackers: List<String>) {
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(attackers.associateWith { 2 })
        game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
    }

    init {
        test("only creatures that entered this turn draw when they connect") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Samut, Vizier of Naktamun")
                .withCardOnBattlefield(1, "Raging Goblin", enteredThisTurn = true)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)
            attackAndFinishCombat(game, listOf("Samut, Vizier of Naktamun", "Raging Goblin", "Grizzly Bears"))

            withClue("All three connected (2 + 1 + 2 damage)") { game.getLifeTotal(2) shouldBe 15 }
            withClue("Only Raging Goblin entered this turn, so exactly one card is drawn") {
                game.handSize(1) shouldBe handBefore + 1
            }
        }

        test("Samut itself draws on the turn it entered, and each new creature draws separately") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Samut, Vizier of Naktamun", enteredThisTurn = true)
                .withCardOnBattlefield(1, "Raging Goblin", enteredThisTurn = true)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)
            attackAndFinishCombat(game, listOf("Samut, Vizier of Naktamun", "Raging Goblin"))

            withClue("Samut (first-strike step) and Raging Goblin (regular step) each draw") {
                game.handSize(1) shouldBe handBefore + 2
            }
        }
    }
}
