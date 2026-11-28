package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dream Twist (ISD #54) — {U} Instant.
 *
 *   Target player mills three cards.
 *   Flashback {1}{U}
 */
class DreamTwistScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.opponentLibrary(): ScenarioBuilder =
        also { b -> repeat(5) { b.withCardInLibrary(2, "Island") } }

    init {
        test("the target player mills three cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Dream Twist")
                .withLandsOnBattlefield(1, "Island", 1)
                .opponentLibrary()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpellTargetingPlayer(1, "Dream Twist", 2)
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.librarySize(2) shouldBe 2
            game.graveyardSize(2) shouldBe 3
            game.isInGraveyard(1, "Dream Twist") shouldBe true
        }

        test("flashback {1}{U} mills again from the graveyard and exiles it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Dream Twist")
                .withLandsOnBattlefield(1, "Island", 2)
                .opponentLibrary()
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val card = game.findCardsInGraveyard(1, "Dream Twist").single()
            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    card,
                    listOf(ChosenTarget.Player(game.player2Id)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.FLASHBACK,
                )
            )
            withClue("flashback cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.librarySize(2) shouldBe 2
            game.isInExile(1, "Dream Twist") shouldBe true
        }
    }
}
