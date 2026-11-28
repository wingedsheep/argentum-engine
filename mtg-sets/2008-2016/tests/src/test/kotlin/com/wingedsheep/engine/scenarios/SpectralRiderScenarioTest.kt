package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spectral Rider (ISD #35) — {W}{W} 2/2 Spirit Knight.
 *
 *   Intimidate
 */
class SpectralRiderScenarioTest : ScenarioTestBase() {

    init {
        test("intimidate: a green creature can't block it, a white one can") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Spectral Rider")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.state.projectedState.hasKeyword(game.findPermanent("Spectral Rider")!!, Keyword.INTIMIDATE) shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Spectral Rider" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            withClue("green Grizzly Bears shares no color with the white Rider") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Spectral Rider"))).error shouldNotBe null
            }
            withClue("white Savannah Lions shares a color and may block") {
                game.declareBlockers(mapOf("Savannah Lions" to listOf("Spectral Rider"))).error shouldBe null
            }
        }
    }
}
