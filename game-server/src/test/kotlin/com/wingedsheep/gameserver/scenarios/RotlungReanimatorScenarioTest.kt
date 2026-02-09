package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rotlung Reanimator.
 *
 * Card reference:
 * - Rotlung Reanimator ({2}{B}): Creature — Zombie Cleric, 2/2
 *   "Whenever Rotlung Reanimator or another Cleric dies, create a 2/2 black Zombie creature token."
 */
class RotlungReanimatorScenarioTest : ScenarioTestBase() {

    init {
        context("Rotlung Reanimator self-death trigger") {
            test("creates a Zombie token when Rotlung Reanimator itself dies") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rotlung Reanimator")
                    .withCardInHand(2, "Shock") // 2 damage kills the 2/2
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Shock the Rotlung Reanimator
                val castResult = game.castSpell(2, "Shock", game.findPermanent("Rotlung Reanimator")!!)
                withClue("Shock should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve Shock -> Rotlung dies -> death trigger creates token
                game.resolveStack()

                withClue("Rotlung Reanimator should be in graveyard") {
                    game.isInGraveyard(1, "Rotlung Reanimator") shouldBe true
                }

                val zombieTokens = game.findAllPermanents("Zombie Token")
                withClue("Should create exactly 1 Zombie token") {
                    zombieTokens.size shouldBe 1
                }
            }
        }

        context("Rotlung Reanimator triggers on another Cleric dying") {
            test("creates a Zombie token when another Cleric dies") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rotlung Reanimator")
                    .withCardOnBattlefield(1, "Battlefield Medic") // 1/1 Cleric
                    .withCardInHand(2, "Shock") // kills the 1/1 Medic
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Shock the Battlefield Medic (another Cleric)
                val castResult = game.castSpell(2, "Shock", game.findPermanent("Battlefield Medic")!!)
                withClue("Shock should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve -> Medic dies -> Rotlung trigger creates token
                game.resolveStack()

                withClue("Battlefield Medic should be in graveyard") {
                    game.isInGraveyard(1, "Battlefield Medic") shouldBe true
                }

                withClue("Rotlung Reanimator should still be on the battlefield") {
                    game.isOnBattlefield("Rotlung Reanimator") shouldBe true
                }

                val zombieTokens = game.findAllPermanents("Zombie Token")
                withClue("Should create exactly 1 Zombie token from Cleric death") {
                    zombieTokens.size shouldBe 1
                }
            }
        }

        context("Rotlung Reanimator triggers on opponent's Cleric dying") {
            test("creates a Zombie token when an opponent's Cleric dies") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rotlung Reanimator")
                    .withCardOnBattlefield(2, "Battlefield Medic") // Opponent's Cleric
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Shock the opponent's Battlefield Medic
                val castResult = game.castSpell(1, "Shock", game.findPermanent("Battlefield Medic")!!)
                withClue("Shock should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve -> Opponent's Medic dies -> Rotlung trigger creates token for P1
                game.resolveStack()

                withClue("Battlefield Medic should be in opponent's graveyard") {
                    game.isInGraveyard(2, "Battlefield Medic") shouldBe true
                }

                val zombieTokens = game.findAllPermanents("Zombie Token")
                withClue("Should create exactly 1 Zombie token from opponent's Cleric death") {
                    zombieTokens.size shouldBe 1
                }
            }
        }

        context("Multiple Cleric deaths trigger multiple tokens") {
            test("Infest kills multiple Clerics and creates tokens for each") {
                // Infest: All creatures get -2/-2 until end of turn
                // This kills both the 2/2 Rotlung and the 1/1 Medic
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rotlung Reanimator") // 2/2 Cleric
                    .withCardOnBattlefield(1, "Battlefield Medic")  // 1/1 Cleric
                    .withCardInHand(1, "Infest")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Infest")
                withClue("Infest should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve Infest -> both creatures die -> triggers fire
                game.resolveStack()

                withClue("Rotlung Reanimator should be in graveyard") {
                    game.isInGraveyard(1, "Rotlung Reanimator") shouldBe true
                }
                withClue("Battlefield Medic should be in graveyard") {
                    game.isInGraveyard(1, "Battlefield Medic") shouldBe true
                }

                // Rotlung dying = 1 token (self-death trigger)
                // Medic dying = 1 token (other Cleric trigger, but Rotlung is also dying so
                // whether it sees the Medic die depends on last-known-info)
                // This is a subtle rules interaction - both die simultaneously,
                // so Rotlung still sees the Medic die (Rule 603.10)
                val zombieTokens = game.findAllPermanents("Zombie Token")
                withClue("Should create 2 Zombie tokens (one for each Cleric death)") {
                    zombieTokens.size shouldBe 2
                }
            }
        }
    }
}
