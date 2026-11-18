package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Revel of the Fallen God (JOU #155) — {3}{R}{R}{G}{G} Sorcery.
 *
 *   Create four 2/2 red and green Satyr creature tokens with haste.
 */
class RevelOfTheFallenGodScenarioTest : ScenarioTestBase() {
    init {
        test("creates four 2/2 red-green hasty Satyrs that can attack right away") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Revel of the Fallen God")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Revel of the Fallen God").error shouldBe null
            game.resolveStack()

            val satyrs = game.findPermanents("Satyr Token")
            satyrs shouldHaveSize 4
            val projected = game.state.projectedState
            satyrs.forEach { satyr ->
                projected.getPower(satyr) shouldBe 2
                projected.getToughness(satyr) shouldBe 2
                projected.hasColor(satyr, Color.RED) shouldBe true
                projected.hasColor(satyr, Color.GREEN) shouldBe true
                projected.hasKeyword(satyr, Keyword.HASTE) shouldBe true
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("haste lets the fresh Satyrs attack") {
                game.declareAttackers(mapOf("Satyr Token" to 2)).error shouldBe null
            }
        }
    }
}
