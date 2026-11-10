package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Structural Distortion (SOI #185) — {3}{R} Sorcery.
 *
 *   Exile target artifact or land. Structural Distortion deals 2 damage to that permanent's controller.
 *
 * The permanent is gone by the time the damage is dealt, so "that permanent's controller" is the
 * player who controlled it as it left the battlefield — whoever that was, the caster included.
 */
class StructuralDistortionScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Structural Distortion")
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withCardOnBattlefield(1, "Ornithopter")
        .withLandsOnBattlefield(2, "Island", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("exiles an opponent's land and deals 2 damage to that opponent") {
            val game = game()
            val island = game.findPermanent("Island")!!

            val result = game.castSpell(1, "Structural Distortion", island)
            withClue("cast: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInExile(2, "Island") shouldBe true
            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 20
        }

        test("exiling its caster's own artifact deals the 2 damage to the caster") {
            val game = game()
            val ornithopter = game.findPermanent("Ornithopter")!!

            game.castSpell(1, "Structural Distortion", ornithopter).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Ornithopter") shouldBe true
            game.getLifeTotal(1) shouldBe 18
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
