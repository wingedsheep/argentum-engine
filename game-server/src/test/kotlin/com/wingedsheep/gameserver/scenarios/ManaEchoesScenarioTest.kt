package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Mana Echoes.
 *
 * Card reference:
 * - Mana Echoes (2RR): Enchantment
 *   Whenever a creature enters, you may add an amount of {C} equal to the number
 *   of creatures you control that share a creature type with it.
 */
class ManaEchoesScenarioTest : ScenarioTestBase() {

    init {
        context("Mana Echoes triggered ability") {

            test("adds colorless mana when creature enters sharing a type") {
                // Player 1 has Mana Echoes + 2 Goblins on battlefield, casts another Goblin
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mana Echoes")
                    .withCardOnBattlefield(1, "Goblin Sledder")    // Goblin
                    .withCardOnBattlefield(1, "Festering Goblin")  // Goblin (also Zombie)
                    .withCardInHand(1, "Goblin Sky Raider")        // Goblin
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Goblin Sky Raider ({2}{R})
                game.castSpell(1, "Goblin Sky Raider")
                game.resolveStack()

                // Trigger fires. 3 Goblins now on battlefield controlled by player 1
                // (Goblin Sledder + Festering Goblin + Goblin Sky Raider).
                // All share the "Goblin" type with the entering creature → 3 colorless mana.
                // Answer "yes" to the may ability
                game.answerYesNo(true)

                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Player 1 should have 3 colorless mana from Mana Echoes") {
                    manaPool?.colorless shouldBe 3
                }
            }

            test("may choose not to add mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mana Echoes")
                    .withCardOnBattlefield(1, "Goblin Sledder")
                    .withCardInHand(1, "Goblin Sky Raider")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Goblin Sky Raider")
                game.resolveStack()

                // Answer "no" to the may ability
                game.answerYesNo(false)

                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Player 1 should have no colorless mana") {
                    manaPool?.colorless shouldBe 0
                }
            }

            test("triggers for opponent's creatures but counts only your creatures") {
                // Player 1 has Mana Echoes + 1 Elf. Opponent casts an Elf.
                // Mana Echoes triggers for player 1 (enchantment's controller).
                // Player 1 controls 1 Elf that shares a type → 1 colorless mana.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mana Echoes")
                    .withCardOnBattlefield(1, "Elvish Warrior")    // Elf Warrior
                    .withCardInHand(2, "Elvish Warrior")           // Elf Warrior
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Elvish Warrior")
                game.resolveStack()

                // Mana Echoes triggers for player 1.
                // Player 1 controls 1 Elf (Elvish Warrior) that shares a type.
                // The entering creature is opponent's, so not counted for "you control".
                game.answerYesNo(true)

                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Player 1 should have 1 colorless mana (1 Elf they control shares type)") {
                    manaPool?.colorless shouldBe 1
                }
            }

            test("entering creature counts itself when sharing a type") {
                // Player 1 has Mana Echoes + a Goblin, casts a Human Soldier (no shared types with Goblin).
                // The entering creature still shares its own types with itself → 1.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mana Echoes")
                    .withCardOnBattlefield(1, "Goblin Sledder")    // Goblin
                    .withCardInHand(1, "Glory Seeker")             // Human Soldier
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Glory Seeker")
                game.resolveStack()

                // Trigger fires. The entering creature is a Human Soldier.
                // Player 1 controls Goblin Sledder (Goblin) and Glory Seeker (Human Soldier).
                // Only Glory Seeker shares a type with itself → 1 colorless mana.
                game.answerYesNo(true)

                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Player 1 should have 1 colorless mana (only the entering creature shares type with itself)") {
                    manaPool?.colorless shouldBe 1
                }
            }
        }
    }
}
