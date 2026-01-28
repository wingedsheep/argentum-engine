package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Path of Peace.
 *
 * Card reference:
 * - Path of Peace (3W): Sorcery
 *   "Destroy target creature. Its owner gains 4 life."
 *
 * Test scenarios:
 * 1. Owner of destroyed creature gains 4 life (owner is the opponent)
 * 2. Owner of destroyed creature gains 4 life (owner is the caster - self-target)
 */
class PathOfPeaceScenarioTest : ScenarioTestBase() {

    init {
        context("Path of Peace life gain effect") {
            test("owner of destroyed creature gains 4 life when targeting opponent's creature") {
                // Setup:
                // - Player 1 has Path of Peace
                // - Player 2 controls a creature
                // - Player 1 casts Path of Peace targeting Player 2's creature
                // - After resolution, Player 2 (owner) should gain 4 life
                val game = scenario()
                    .withPlayers("Caster", "CreatureOwner")
                    .withCardInHand(1, "Path of Peace")
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 creature
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 casts Path of Peace targeting Player 2's Grizzly Bears
                val bearsId = game.findPermanent("Grizzly Bears")
                    ?: error("Grizzly Bears should be on battlefield")

                val castResult = game.castSpell(1, "Path of Peace", bearsId)
                withClue("Path of Peace should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the stack
                game.resolveStack()

                // Verify the creature is destroyed
                withClue("Grizzly Bears should be in graveyard") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }

                withClue("Grizzly Bears should not be on battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }

                // Verify the owner (Player 2) gained 4 life
                withClue("Creature owner (Player 2) should have gained 4 life") {
                    game.getLifeTotal(2) shouldBe 24
                }

                // Verify the caster (Player 1) did NOT gain life
                withClue("Caster (Player 1) should not have gained life") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("owner gains 4 life when destroying own creature") {
                // Setup:
                // - Player 1 has Path of Peace and a creature
                // - Player 1 casts Path of Peace targeting their own creature
                // - After resolution, Player 1 (owner) should gain 4 life
                val game = scenario()
                    .withPlayers("CasterAndOwner", "Opponent")
                    .withCardInHand(1, "Path of Peace")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLifeTotal(1, 15) // Starting at 15 to make the gain more obvious
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 casts Path of Peace targeting their own Grizzly Bears
                val bearsId = game.findPermanent("Grizzly Bears")
                    ?: error("Grizzly Bears should be on battlefield")

                val castResult = game.castSpell(1, "Path of Peace", bearsId)
                withClue("Path of Peace should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the stack
                game.resolveStack()

                // Verify the creature is destroyed
                withClue("Grizzly Bears should be in graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                // Verify the owner/caster (Player 1) gained 4 life
                withClue("Creature owner (Player 1) should have gained 4 life") {
                    game.getLifeTotal(1) shouldBe 19
                }

                // Verify Player 2 did NOT gain life
                withClue("Opponent (Player 2) should not have gained life") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
