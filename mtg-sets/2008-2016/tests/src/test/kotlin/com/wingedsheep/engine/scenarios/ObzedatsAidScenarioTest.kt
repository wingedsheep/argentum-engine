package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Obzedat's Aid (DGM #89) — {3}{W}{B} Sorcery.
 *
 *   Return target permanent card from your graveyard to the battlefield.
 */
class ObzedatsAidScenarioTest : ScenarioTestBase() {

    private fun buildGame(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Obzedat's Aid")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInGraveyard(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("returns a permanent card from your graveyard to the battlefield") {
            val game = buildGame()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

            game.castSpellTargetingGraveyardCard(1, "Obzedat's Aid", listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears is back on the battlefield") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }
        }

        test("can't target a card in an opponent's graveyard") {
            val game = buildGame()
            val giant = game.findCardsInGraveyard(2, "Hill Giant").single()

            withClue("only your own graveyard is a legal zone") {
                game.castSpellTargetingGraveyardCard(1, "Obzedat's Aid", listOf(giant)).error.shouldNotBeNull()
            }
        }
    }
}
