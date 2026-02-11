package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Thunder of Hooves.
 *
 * Card reference:
 * - Thunder of Hooves ({3}{R}): Sorcery
 *   "Thunder of Hooves deals X damage to each creature without flying and each player,
 *   where X is the number of Beasts on the battlefield."
 *
 * Test scenarios:
 * 1. With 2 Beasts, deals 2 damage to each non-flying creature and each player
 * 2. Flying creatures are unaffected
 * 3. With 0 Beasts, deals 0 damage
 * 4. Beasts themselves take damage (they are creatures without flying)
 */
class ThunderOfHoovesScenarioTest : ScenarioTestBase() {

    init {
        context("Thunder of Hooves") {
            test("deals damage equal to number of Beasts on the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Thunder of Hooves")
                    .withLandsOnBattlefield(1, "Mountain", 4) // {3}{R}
                    .withCardOnBattlefield(1, "Ravenous Baloth")  // 4/4 Beast
                    .withCardOnBattlefield(2, "Towering Baloth")  // 7/6 Beast
                    .withCardOnBattlefield(2, "Elvish Warrior")   // 2/3 non-Beast, no flying
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Thunder of Hooves")
                game.resolveStack()

                // 2 Beasts on battlefield → X = 2
                withClue("Ravenous Baloth (4/4 Beast) should survive 2 damage") {
                    game.isOnBattlefield("Ravenous Baloth") shouldBe true
                }
                withClue("Towering Baloth (7/6 Beast) should survive 2 damage") {
                    game.isOnBattlefield("Towering Baloth") shouldBe true
                }
                withClue("Elvish Warrior (2/3) should survive 2 damage") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }
                withClue("Player1 should take 2 damage (20 - 2 = 18)") {
                    game.getLifeTotal(1) shouldBe 18
                }
                withClue("Opponent should take 2 damage (20 - 2 = 18)") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("does not damage creatures with flying") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Thunder of Hooves")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Ravenous Baloth")  // 4/4 Beast (no flying)
                    .withCardOnBattlefield(2, "Ascending Aven")   // 3/2 flying
                    .withCardOnBattlefield(2, "Glory Seeker")     // 2/2 no flying
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Thunder of Hooves")
                game.resolveStack()

                // 1 Beast on battlefield → X = 1
                withClue("Ascending Aven (3/2 flying) should NOT be damaged") {
                    game.isOnBattlefield("Ascending Aven") shouldBe true
                }
                withClue("Glory Seeker (2/2 no flying) should survive 1 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
                withClue("Each player takes 1 damage") {
                    game.getLifeTotal(1) shouldBe 19
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("deals 0 damage with no Beasts on the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Thunder of Hooves")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Glory Seeker")    // 2/2 Soldier, not a Beast
                    .withCardOnBattlefield(2, "Elvish Warrior")  // 2/3 Elf, not a Beast
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Thunder of Hooves")
                game.resolveStack()

                // 0 Beasts → X = 0, no damage
                withClue("Glory Seeker should survive 0 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
                withClue("Elvish Warrior should survive 0 damage") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }
                withClue("Player1 should take 0 damage") {
                    game.getLifeTotal(1) shouldBe 20
                }
                withClue("Opponent should take 0 damage") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("kills small creatures with enough Beasts") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Thunder of Hooves")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Ravenous Baloth")   // 4/4 Beast
                    .withCardOnBattlefield(1, "Towering Baloth")   // 7/6 Beast
                    .withCardOnBattlefield(2, "Snarling Undorak")  // 3/3 Beast
                    .withCardOnBattlefield(2, "Glory Seeker")      // 2/2 no flying
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Thunder of Hooves")
                game.resolveStack()

                // 3 Beasts on battlefield → X = 3
                withClue("Ravenous Baloth (4/4) should survive 3 damage") {
                    game.isOnBattlefield("Ravenous Baloth") shouldBe true
                }
                withClue("Towering Baloth (7/6) should survive 3 damage") {
                    game.isOnBattlefield("Towering Baloth") shouldBe true
                }
                withClue("Snarling Undorak (3/3) should die from 3 damage") {
                    game.isOnBattlefield("Snarling Undorak") shouldBe false
                }
                withClue("Glory Seeker (2/2) should die from 3 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
                withClue("Each player takes 3 damage") {
                    game.getLifeTotal(1) shouldBe 17
                    game.getLifeTotal(2) shouldBe 17
                }
            }
        }
    }
}
