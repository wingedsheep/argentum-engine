package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Persistent Specimen (VOW #125) — {B} Creature — Skeleton, 1/1.
 *
 *   {2}{B}: Return this card from your graveyard to the battlefield tapped.
 *
 * Exercises the graveyard-activated recursion (same shape as Teacher's Pest): the ability is
 * activated from the graveyard and the card re-enters the battlefield tapped.
 */
class PersistentSpecimenScenarioTest : ScenarioTestBase() {

    init {
        context("Persistent Specimen graveyard recursion") {

            test("the {2}{B} ability returns the card from the graveyard to the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Persistent Specimen")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Starts in the graveyard, not on the battlefield") {
                    game.isInGraveyard(1, "Persistent Specimen") shouldBe true
                    game.isOnBattlefield("Persistent Specimen") shouldBe false
                }

                val graveyardCard = game.state.getGraveyard(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Persistent Specimen"
                }
                val abilityId = cardRegistry.getCard("Persistent Specimen")!!
                    .activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = graveyardCard,
                        abilityId = abilityId
                    )
                )
                withClue("Activating the {2}{B} graveyard ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                val returned = game.findPermanent("Persistent Specimen")
                withClue("Persistent Specimen is back on the battlefield") {
                    (returned != null) shouldBe true
                }
                withClue("It returns tapped") {
                    game.state.getEntity(returned!!)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
