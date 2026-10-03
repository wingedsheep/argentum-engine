package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Meltdown (USG #203, reprinted in MH3).
 *
 * "{X}{R} Sorcery — Destroy each artifact with mana value X or less."
 */
class MeltdownScenarioTest : ScenarioTestBase() {

    init {
        test("destroys every artifact with mana value X or less, on both sides, and nothing else") {
            val game = scenario()
                .withPlayers("Caster", "Defender")
                .withCardInHand(1, "Meltdown")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(1, "Ornithopter")      // artifact, MV 0
                .withCardOnBattlefield(2, "Worn Powerstone")  // artifact, MV 3 (boundary)
                .withCardOnBattlefield(2, "Su-Chi")           // artifact, MV 4
                .withCardOnBattlefield(2, "Grizzly Bears")    // non-artifact, MV 2
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castXSpell(1, "Meltdown", xValue = 3)
            withClue("Casting Meltdown (X=3) should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Ornithopter (MV 0) is destroyed") { game.isOnBattlefield("Ornithopter") shouldBe false }
            withClue("Worn Powerstone (MV 3 = X) is destroyed") { game.isOnBattlefield("Worn Powerstone") shouldBe false }
            withClue("Su-Chi (MV 4 > X) survives") { game.isOnBattlefield("Su-Chi") shouldBe true }
            withClue("Grizzly Bears is not an artifact") { game.isOnBattlefield("Grizzly Bears") shouldBe true }
        }

        test("X = 0 destroys only mana value 0 artifacts") {
            val game = scenario()
                .withPlayers("Caster", "Defender")
                .withCardInHand(1, "Meltdown")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Worn Powerstone")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castXSpell(1, "Meltdown", xValue = 0)
            withClue("Casting Meltdown (X=0) should succeed: ${cast.error}") { cast.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.isOnBattlefield("Ornithopter") shouldBe false
            game.isOnBattlefield("Worn Powerstone") shouldBe true
        }
    }
}
