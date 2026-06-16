package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Stone Docent (Secrets of Strixhaven #36).
 *
 * Stone Docent ({1}{W}, 3/1, Spirit Chimera):
 *   {W}, Exile this card from your graveyard: You gain 2 life. Surveil 1.
 *   Activate only as a sorcery.
 *
 * Exercises the graveyard-activated, sorcery-speed ability: pay {W} + exile self, gain 2 life,
 * and surveil 1.
 */
class StoneDocentScenarioTest : ScenarioTestBase() {

    private fun life(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<LifeTotalComponent>()?.life ?: 0

    init {
        context("Stone Docent — graveyard gain-life + surveil") {

            test("paying {W} and exiling from the graveyard gains 2 life and surveils 1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Stone Docent")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val player1 = game.state.activePlayerId!!
                val startLife = life(game, player1)

                withClue("Starts in the graveyard") {
                    game.isInGraveyard(1, "Stone Docent") shouldBe true
                }

                val graveyardCard = game.state.getGraveyard(player1)
                    .first {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Stone Docent"
                    }
                val abilityId = cardRegistry.getCard("Stone Docent")!!
                    .activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = player1,
                        sourceId = graveyardCard,
                        abilityId = abilityId
                    )
                )
                withClue("Activating the graveyard ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                // Surveil 1: if a decision is pending (keep on top vs graveyard), keep it on top.
                if (game.hasPendingDecision()) {
                    game.selectCards(emptyList())
                    game.resolveStack()
                }

                withClue("Gained 2 life") {
                    life(game, player1) shouldBe startLife + 2
                }
                withClue("Stone Docent is exiled (no longer in the graveyard)") {
                    game.isInGraveyard(1, "Stone Docent") shouldBe false
                }
            }
        }
    }
}
