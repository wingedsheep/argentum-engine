package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Wickerfolk Indomitable (DFT #109).
 *
 * Wickerfolk Indomitable {3}{B} — Artifact Creature — Scarecrow 4/3
 * You may cast this card from your graveyard by paying 2 life and sacrificing an artifact or
 * creature in addition to paying its other costs.
 *
 * The load-bearing claims:
 *  - the graveyard permission exists and both extra costs are actually charged (life *and* a
 *    sacrifice), on top of the printed {3}{B};
 *  - the sacrifice filter is the union "artifact **or** creature" — a land can't pay it.
 */
class WickerfolkIndomitableScenarioTest : ScenarioTestBase() {

    private fun graveyardGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInGraveyard(1, "Wickerfolk Indomitable")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withLifeTotal(1, 20)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.wickerfolkInGraveyard(): EntityId =
        state.getGraveyard(player1Id).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "Wickerfolk Indomitable"
        }

    init {
        context("Wickerfolk Indomitable") {

            test("casting from the graveyard charges 2 life and a sacrifice on top of {3}{B}") {
                val game = graveyardGame()
                val bears = game.findPermanent("Grizzly Bears")!!

                val result = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.wickerfolkInGraveyard(),
                        additionalCostPayment = AdditionalCostPayment(
                            lifePaid = 2,
                            sacrificedPermanents = listOf(bears)
                        )
                    )
                )
                withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Wickerfolk Indomitable") shouldBe true
                withClue("2 life paid") {
                    game.state.getEntity(game.player1Id)?.get<LifeTotalComponent>()?.life shouldBe 18
                }
                withClue("the Grizzly Bears was sacrificed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("a land cannot pay the artifact-or-creature sacrifice") {
                val game = graveyardGame()
                val swamp = game.findPermanents("Swamp").first()

                val result = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.wickerfolkInGraveyard(),
                        additionalCostPayment = AdditionalCostPayment(
                            lifePaid = 2,
                            sacrificedPermanents = listOf(swamp)
                        )
                    )
                )
                withClue("the filter is artifact|creature, not any permanent") {
                    (result.error != null) shouldBe true
                }
            }
        }
    }
}
