package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Spectral Reserves (EMN #43) — {3}{W} Sorcery.
 *
 *   Create two 1/1 white Spirit creature tokens with flying. You gain 2 life.
 */
class SpectralReservesScenarioTest : ScenarioTestBase() {

    init {
        test("creates two 1/1 flying Spirits and gains 2 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Spectral Reserves")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Spectral Reserves")
            withClue("${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val spirits = game.findPermanents("Spirit Token")
            spirits.size shouldBe 2
            spirits.forEach { spirit ->
                game.state.projectedState.getPower(spirit) shouldBe 1
                game.state.projectedState.getToughness(spirit) shouldBe 1
                game.state.projectedState.hasKeyword(spirit, Keyword.FLYING) shouldBe true
            }
            game.getLifeTotal(1) shouldBe 22
        }
    }
}
