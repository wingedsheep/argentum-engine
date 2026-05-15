package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Blasphemous Act.
 *
 * Card reference:
 * - Blasphemous Act ({8}{R}): Sorcery
 *   This spell costs {1} less to cast for each creature on the battlefield.
 *   Blasphemous Act deals 13 damage to each creature.
 */
class BlasphemousActScenarioTest : ScenarioTestBase() {

    init {
        context("Blasphemous Act cost reduction") {

            test("each creature on the battlefield discounts the spell, including opponents' creatures") {
                // 4 creatures on the battlefield (2 yours + 2 opponent's) → reduction 4
                // → cost {4}{R} = 5 mana. Provide exactly 5 Mountains.
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Blasphemous Act")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(1, "Festering Goblin")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardOnBattlefield(2, "Festering Goblin")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Blasphemous Act")
                withClue("Four creatures should reduce cost to {4}{R}: ${castResult.error}") {
                    castResult.error shouldBe null
                }
            }

            test("cannot cast without enough discount or mana") {
                // No creatures on the battlefield → no reduction → cost {8}{R} = 9 mana.
                // Only 5 Mountains available → cannot pay.
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Blasphemous Act")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Blasphemous Act")
                withClue("Cast with no creatures and 5 Mountains should fail") {
                    castResult.error shouldNotBe null
                }
            }
        }

        context("Blasphemous Act damage") {

            test("deals 13 damage to each creature, wiping low-toughness boards") {
                // 4 creatures → cost {4}{R}, but use {8}{R} budget for clarity.
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Blasphemous Act")
                    .withLandsOnBattlefield(1, "Mountain", 9)
                    .withCardOnBattlefield(1, "Glory Seeker")        // 2/2
                    .withCardOnBattlefield(1, "Towering Baloth")     // 7/6
                    .withCardOnBattlefield(2, "Festering Goblin")    // 1/1
                    .withCardOnBattlefield(2, "Glory Seeker")        // 2/2
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Blasphemous Act")
                game.resolveStack()

                withClue("Glory Seeker (2/2) should die from 13 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
                withClue("Festering Goblin (1/1) should die from 13 damage") {
                    game.isOnBattlefield("Festering Goblin") shouldBe false
                }
                withClue("Towering Baloth (7/6) should die from 13 damage") {
                    game.isOnBattlefield("Towering Baloth") shouldBe false
                }
            }
        }
    }
}
