package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Brightstone Ritual.
 *
 * Card reference:
 * - Brightstone Ritual (R): Instant
 *   "Add {R} for each Goblin on the battlefield."
 */
class BrightstoneRitualScenarioTest : ScenarioTestBase() {

    init {
        context("Brightstone Ritual mana generation") {
            test("adds red mana equal to goblin count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Brightstone Ritual")
                    .withCardOnBattlefield(1, "Goblin Sky Raider") // Goblin
                    .withCardOnBattlefield(1, "Goblin Sledder") // Goblin
                    .withCardOnBattlefield(2, "Festering Goblin") // Goblin (opponent's)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Brightstone Ritual")
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // 3 Goblins on the battlefield -> add 3 red mana
                // The Mountain was tapped for casting, so mana pool should have 3 red
                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Player should have 3 red mana (from 3 Goblins)") {
                    manaPool?.red shouldBe 3
                }
            }

            test("adds zero mana when no goblins on battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Brightstone Ritual")
                    .withCardOnBattlefield(1, "Hill Giant") // Not a Goblin
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Brightstone Ritual")
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // 0 Goblins -> 0 red mana added
                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Player should have 0 red mana (no Goblins)") {
                    manaPool?.red shouldBe 0
                }
            }
        }
    }
}
