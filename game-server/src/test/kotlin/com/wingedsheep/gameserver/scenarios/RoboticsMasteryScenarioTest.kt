package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Robotics Mastery.
 *
 * Card reference:
 * - Robotics Mastery ({4}{U}): Enchantment — Aura
 *   "Flash"
 *   "Enchant creature"
 *   "Enchanted creature gets +2/+2."
 *   "When Robotics Mastery enters the battlefield, create two 1/1 colorless Robot artifact creature tokens with flying."
 */
class RoboticsMasteryScenarioTest : ScenarioTestBase() {

    init {
        context("Robotics Mastery Aura attach") {

            test("attaches to own creature and grants +2/+2 when cast with Flash during opponent's turn") {
                val game = scenario()
                    .withPlayers("PlayerA", "PlayerB")
                    .withCardInHand(1, "Robotics Mastery")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeekerBefore = game.findPermanent("Glory Seeker")!!

                val castResult = game.castSpell(1, "Robotics Mastery", glorySeekerBefore)
                withClue("Casting Robotics Mastery with Flash should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Robotics Mastery should be on the battlefield") {
                    game.isOnBattlefield("Robotics Mastery") shouldBe true
                }

                val glorySeeker = game.findPermanent("Glory Seeker")!!
                val projected = StateProjector().project(game.state)

                withClue("Enchanted Glory Seeker power should be 4 (2 base + 2 from Robotics Mastery)") {
                    projected.getPower(glorySeeker) shouldBe 4
                }
                withClue("Enchanted Glory Seeker toughness should be 4 (2 base + 2 from Robotics Mastery)") {
                    projected.getToughness(glorySeeker) shouldBe 4
                }
            }
        }

        context("Robotics Mastery ETB token creation") {

            test("creates two 1/1 colorless Robot artifact creature tokens with flying on ETB") {
                val game = scenario()
                    .withPlayers("PlayerA", "PlayerB")
                    .withCardInHand(1, "Robotics Mastery")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeeker = game.findPermanent("Glory Seeker")!!

                val castResult = game.castSpell(1, "Robotics Mastery", glorySeeker)
                withClue("Casting Robotics Mastery should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val robotTokens = game.findAllPermanents("Robot Token")
                withClue("ETB should create exactly two Robot tokens") {
                    robotTokens.size shouldBe 2
                }

                val projected = StateProjector().project(game.state)
                robotTokens.forEach { tokenId ->
                    withClue("Robot token $tokenId should be a 1/1") {
                        projected.getPower(tokenId) shouldBe 1
                        projected.getToughness(tokenId) shouldBe 1
                    }
                    withClue("Robot token $tokenId should have flying") {
                        projected.hasKeyword(tokenId, Keyword.FLYING) shouldBe true
                    }
                    withClue("Robot token $tokenId should be a colorless artifact creature with the Robot subtype") {
                        projected.getColors(tokenId) shouldBe emptySet<String>()
                        projected.isCreature(tokenId) shouldBe true
                        projected.hasType(tokenId, "ARTIFACT") shouldBe true
                        projected.hasSubtype(tokenId, "Robot") shouldBe true
                    }
                }
            }
        }
    }
}
