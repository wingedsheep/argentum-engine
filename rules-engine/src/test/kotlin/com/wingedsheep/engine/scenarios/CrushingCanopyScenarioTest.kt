package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Crushing Canopy (XLN #183, reprinted VOW #194).
 *
 * {2}{G} Instant. Choose one —
 *   • Destroy target creature with flying.
 *   • Destroy target enchantment.
 */
class CrushingCanopyScenarioTest : ScenarioTestBase() {

    init {
        context("Crushing Canopy — choose one") {

            test("mode 1: destroy target creature with flying") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Crushing Canopy")
                    .withCardOnBattlefield(2, "Storm Crow") // 1/2 flier
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crow = game.findPermanent("Storm Crow")!!
                game.castSpellWithMode(1, "Crushing Canopy", modeIndex = 0, targetId = crow).error shouldBe null
                game.resolveStack()

                withClue("Storm Crow (flying) was destroyed") {
                    game.findPermanent("Storm Crow") shouldBe null
                    game.isInGraveyard(2, "Storm Crow") shouldBe true
                }
            }

            test("mode 1: a non-flying creature is not a legal target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Crushing Canopy")
                    .withCardOnBattlefield(2, "Grizzly Bears") // no flying — illegal
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val result = game.castSpellWithMode(1, "Crushing Canopy", modeIndex = 0, targetId = bears)

                withClue("Grizzly Bears (no flying) is not a legal target for the flying-destroy mode") {
                    (result.error != null) shouldBe true
                }
            }

            test("mode 2: destroy target enchantment") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Crushing Canopy")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(2, "Pacifism", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pacifism = game.findPermanent("Pacifism")!!
                game.castSpellWithMode(1, "Crushing Canopy", modeIndex = 1, targetId = pacifism).error shouldBe null
                game.resolveStack()

                withClue("Pacifism was destroyed") {
                    game.findPermanent("Pacifism") shouldBe null
                    game.isInGraveyard(2, "Pacifism") shouldBe true
                }
            }

            test("mode 2: a creature (non-enchantment) is not a legal target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Crushing Canopy")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val result = game.castSpellWithMode(1, "Crushing Canopy", modeIndex = 1, targetId = bears)

                withClue("Grizzly Bears is not an enchantment, so mode 2 rejects it") {
                    (result.error != null) shouldBe true
                }
            }
        }
    }
}
