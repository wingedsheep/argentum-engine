package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Deadly Dispute (AFR #94) — {1}{B} Instant.
 *
 *   As an additional cost to cast this spell, sacrifice an artifact or creature.
 *   Draw two cards and create a Treasure token.
 */
class DeadlyDisputeScenarioTest : ScenarioTestBase() {

    private fun castSacrificing(game: TestGame, permanentName: String) {
        val dispute = game.findCardsInHand(1, "Deadly Dispute").single()
        val fodder = game.findPermanent(permanentName)!!
        val result = game.execute(
            CastSpell(
                game.player1Id,
                dispute,
                emptyList(),
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
            )
        )
        withClue("casting Deadly Dispute should succeed: ${result.error}") { result.error shouldBe null }
        game.resolveStack()
    }

    init {
        context("Deadly Dispute") {

            test("sacrificing an artifact draws two cards and creates a Treasure") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Deadly Dispute")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                castSacrificing(game, "Ornithopter")

                withClue("the sacrificed artifact is in the graveyard") {
                    game.isInGraveyard(1, "Ornithopter") shouldBe true
                }
                withClue("cast one, drew two") { game.handSize(1) shouldBe handBefore - 1 + 2 }
                withClue("one Treasure token was created") { game.findPermanents("Treasure").size shouldBe 1 }
            }

            test("a creature also pays the additional cost") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Deadly Dispute")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                castSacrificing(game, "Grizzly Bears")

                withClue("the sacrificed creature is in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                game.handSize(1) shouldBe handBefore - 1 + 2
                game.findPermanents("Treasure").size shouldBe 1
            }
        }
    }
}
