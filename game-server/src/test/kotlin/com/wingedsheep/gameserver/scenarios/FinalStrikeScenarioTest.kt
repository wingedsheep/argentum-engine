package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Final Strike.
 *
 * Card reference:
 * - Final Strike (2BB): Sorcery
 *   "As an additional cost to cast this spell, sacrifice a creature.
 *    Final Strike deals damage equal to the sacrificed creature's power
 *    to target opponent or planeswalker."
 */
class FinalStrikeScenarioTest : ScenarioTestBase() {

    init {
        context("Final Strike - sacrifice creature and deal damage") {
            test("sacrificing a 4/1 creature deals 4 damage to opponent") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Final Strike")
                    .withCardOnBattlefield(1, "Elvish Ranger") // 4/1
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.getLifeTotal(2) shouldBe 20

                val castResult = game.castSpellWithSacrifice(
                    playerNumber = 1,
                    spellName = "Final Strike",
                    sacrificeCreatureName = "Elvish Ranger",
                    targetPlayerNumber = 2
                )
                withClue("Final Strike should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Elvish Ranger should be in graveyard (sacrificed as cost)
                withClue("Elvish Ranger should be in graveyard after sacrifice") {
                    game.isInGraveyard(1, "Elvish Ranger") shouldBe true
                }
                withClue("Elvish Ranger should not be on battlefield") {
                    game.isOnBattlefield("Elvish Ranger") shouldBe false
                }

                // Resolve the spell
                game.resolveStack()

                withClue("Opponent should take 4 damage (from 4/1 creature's power)") {
                    game.getLifeTotal(2) shouldBe 16
                }
            }

            test("sacrificing a 1/4 creature deals 1 damage to opponent") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Final Strike")
                    .withCardOnBattlefield(1, "Border Guard") // 1/4
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellWithSacrifice(
                    playerNumber = 1,
                    spellName = "Final Strike",
                    sacrificeCreatureName = "Border Guard",
                    targetPlayerNumber = 2
                )
                withClue("Final Strike should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Opponent should take 1 damage (from 1/4 creature's power)") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("cannot cast Final Strike with no creatures on battlefield") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Final Strike")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Trying to cast without sacrifice payment should fail
                val castResult = game.castSpellTargetingPlayer(1, "Final Strike", 2)
                withClue("Final Strike should fail without sacrifice") {
                    castResult.error shouldBe "You must sacrifice 1 creature card to cast this spell"
                }
            }
        }
    }
}
