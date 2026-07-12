package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Rending Flame (VOW #175) — {2}{R} Instant.
 *
 *   Rending Flame deals 5 damage to target creature or planeswalker. If that permanent is a
 *   Spirit, Rending Flame also deals 2 damage to that permanent's controller.
 *
 * Exercises the base 5 damage to a non-Spirit creature (no rider damage) and the Spirit rider:
 * targeting a Spirit deals the same 5 damage plus 2 damage to that Spirit's controller.
 */
class RendingFlameScenarioTest : ScenarioTestBase() {

    init {
        context("Rending Flame") {

            test("deals 5 damage to a non-Spirit creature with no rider damage to its controller") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Rending Flame")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false) // 3/3, not a Spirit
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val lifeBefore = game.getLifeTotal(2)

                game.castSpell(1, "Rending Flame", giant).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant takes 5 damage and dies (3 toughness)") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
                withClue("no rider damage since Hill Giant is not a Spirit") {
                    game.getLifeTotal(2) shouldBe lifeBefore
                }
            }

            test("targeting a Spirit also deals 2 damage to that Spirit's controller") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Rending Flame")
                    .withCardOnBattlefield(2, "Dawnhart Geist", summoningSickness = false) // 1/3 Spirit Warlock
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val geist = game.findPermanent("Dawnhart Geist")!!
                val lifeBefore = game.getLifeTotal(2)

                game.castSpell(1, "Rending Flame", geist).error shouldBe null
                game.resolveStack()

                withClue("Dawnhart Geist takes 5 damage and dies (3 toughness)") {
                    game.isOnBattlefield("Dawnhart Geist") shouldBe false
                    game.isInGraveyard(2, "Dawnhart Geist") shouldBe true
                }
                withClue("its controller also takes 2 rider damage since it's a Spirit") {
                    game.getLifeTotal(2) shouldBe lifeBefore - 2
                }
            }
        }
    }
}
