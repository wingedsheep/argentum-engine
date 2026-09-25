package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Yamabushi's Flame (CHK #198) — "Yamabushi's Flame deals 3 damage to any target. If a creature
 * dealt damage this way would die this turn, exile it instead."
 *
 * The exile clause outlives the spell: it covers the rest of the turn, not only the death the
 * damage itself causes.
 */
class YamabushisFlameScenarioTest : ScenarioTestBase() {

    init {
        context("Yamabushi's Flame") {

            test("a creature killed by the damage is exiled instead of dying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Yamabushi's Flame")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Yamabushi's Flame", bears).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            }

            test("a creature that survives the damage but dies later this turn is still exiled") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Horned Turtle") // 1/4
                    .withCardInHand(1, "Yamabushi's Flame")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val turtle = game.findPermanent("Horned Turtle")!!
                game.castSpell(1, "Yamabushi's Flame", turtle).error shouldBe null
                game.resolveStack()
                withClue("3 damage doesn't kill a 1/4") { game.isOnBattlefield("Horned Turtle") shouldBe true }

                game.castSpell(1, "Lightning Bolt", turtle).error shouldBe null
                game.resolveStack()

                withClue("it was dealt damage by the Flame this turn, so it's exiled when it dies") {
                    game.isInExile(2, "Horned Turtle") shouldBe true
                    game.isInGraveyard(2, "Horned Turtle") shouldBe false
                }
            }

            test("it can target a player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Yamabushi's Flame")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Yamabushi's Flame", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
            }
        }
    }
}
