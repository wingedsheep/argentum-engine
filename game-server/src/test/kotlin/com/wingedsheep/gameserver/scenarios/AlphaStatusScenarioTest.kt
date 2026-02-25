package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Alpha Status.
 *
 * Card reference:
 * - Alpha Status ({2}{G}): Enchantment — Aura
 *   Enchant creature
 *   Enchanted creature gets +2/+2 for each other creature on the battlefield
 *   that shares a creature type with it.
 */
class AlphaStatusScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Alpha Status - P/T modification based on shared creature types") {

            test("enchanted creature gets +2/+2 for each creature sharing a type") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Alpha Status")
                    // Enchant target: Goblin Brigand (2/2 Goblin)
                    .withCardOnBattlefield(1, "Goblin Brigand")
                    // Two other Goblins: should give +4/+4
                    .withCardOnBattlefield(1, "Rock Jockey")
                    .withCardOnBattlefield(2, "Goblin Warchief")
                    // Non-Goblin creature: should not count
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goblinBrigand = game.findPermanent("Goblin Brigand")!!

                // Cast Alpha Status targeting Goblin Brigand
                game.castSpell(1, "Alpha Status", goblinBrigand)
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                val power = projected.getPower(goblinBrigand)
                val toughness = projected.getToughness(goblinBrigand)

                withClue("Goblin Brigand should have bonus from 2 shared-type creatures (+4/+4 from Alpha Status)") {
                    // Base 2/2, +4/+4 from Alpha Status (2 other goblins)
                    power shouldBe 6
                    toughness shouldBe 6
                }
            }

            test("enchanted creature gets no bonus when no creatures share a type") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Alpha Status")
                    .withCardOnBattlefield(1, "Goblin Brigand")
                    // Only non-Goblin creatures
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goblinBrigand = game.findPermanent("Goblin Brigand")!!

                game.castSpell(1, "Alpha Status", goblinBrigand)
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                val power = projected.getPower(goblinBrigand)
                val toughness = projected.getToughness(goblinBrigand)

                withClue("Goblin Brigand should have no Alpha Status bonus (no shared types)") {
                    // Base 2/2, no shared type bonus
                    power shouldBe 2
                    toughness shouldBe 2
                }
            }

            test("counts creatures from both players") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Alpha Status")
                    .withCardOnBattlefield(1, "Silver Knight") // Human Knight 2/2
                    .withCardOnBattlefield(2, "Noble Templar") // Human Cleric Soldier (shares Human)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val silverKnight = game.findPermanent("Silver Knight")!!

                game.castSpell(1, "Alpha Status", silverKnight)
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                val power = projected.getPower(silverKnight)
                val toughness = projected.getToughness(silverKnight)

                withClue("Silver Knight should get +2/+2 from Noble Templar (shared Human type)") {
                    // Silver Knight base 2/2, +2/+2 from 1 shared creature = 4/4
                    power shouldBe 4
                    toughness shouldBe 4
                }
            }

            test("each creature counted only once even if sharing multiple types") {
                // Per ruling: "Alpha Status counts each creature once if that creature
                // shares at least one creature type with the enchanted creature."
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Alpha Status")
                    // Daru Warchief: Human Soldier (1/1), gives Soldiers +1/+2
                    .withCardOnBattlefield(1, "Daru Warchief")
                    // Noble Templar: Human Cleric Soldier — shares Human AND Soldier
                    .withCardOnBattlefield(1, "Noble Templar")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val daruWarchief = game.findPermanent("Daru Warchief")!!

                game.castSpell(1, "Alpha Status", daruWarchief)
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                val power = projected.getPower(daruWarchief)
                val toughness = projected.getToughness(daruWarchief)

                withClue("Daru Warchief should get +2/+2 from Noble Templar (counted once even if multiple types match)") {
                    // Daru Warchief base 1/1
                    // +1/+2 from its own Soldier lord effect (Soldiers you control, includes self)
                    // +2/+2 from Alpha Status (Noble Templar shares Human and Soldier, counted once)
                    // = 4/5
                    power shouldBe 4
                    toughness shouldBe 5
                }
            }
        }
    }
}
