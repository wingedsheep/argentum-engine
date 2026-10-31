package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Reckless Air Strike (M20 #154) — {R} Sorcery.
 *
 *   Choose one —
 *   • Reckless Air Strike deals 3 damage to target creature with flying.
 *   • Destroy target artifact.
 */
class RecklessAirStrikeScenarioTest : ScenarioTestBase() {

    init {
        context("Reckless Air Strike") {

            test("mode 1 deals 3 damage to a creature with flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reckless Air Strike")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Wind Drake") // 2/2 flier
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val drake = game.findPermanent("Wind Drake")!!
                game.castSpellWithMode(1, "Reckless Air Strike", 0, drake).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Wind Drake") shouldBe false
                game.isInGraveyard(2, "Wind Drake") shouldBe true
            }

            test("mode 1 can't target a creature without flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reckless Air Strike")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpellWithMode(1, "Reckless Air Strike", 0, bears).error.shouldNotBeNull()
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("mode 2 destroys target artifact") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reckless Air Strike")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ornithopter = game.findPermanent("Ornithopter")!!
                game.castSpellWithMode(1, "Reckless Air Strike", 1, ornithopter).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Ornithopter") shouldBe false
                game.isInGraveyard(2, "Ornithopter") shouldBe true
            }
        }
    }
}
