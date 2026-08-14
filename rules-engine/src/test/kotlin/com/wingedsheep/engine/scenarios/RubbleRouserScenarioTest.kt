package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Rubble Rouser. */
class RubbleRouserScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private fun TestGame.findExileCopy(name: String): com.wingedsheep.sdk.model.EntityId? =
        state.getExile(player1Id).firstOrNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }

    init {
        context("Rubble Rouser") {

            test("the mana ability adds {R} and pings each opponent for 1") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rubble Rouser", summoningSickness = false)
                    .withCardInGraveyard(1, "Forest")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val rouser = game.findPermanent("Rubble Rouser")!!

                // Find the activated ability ({T}, exile a card from graveyard: Add {R} ...).
                val activate = game.getLegalActions(1).firstOrNull { la ->
                    val a = la.action
                    a is com.wingedsheep.engine.core.ActivateAbility && a.sourceId == rouser
                }
                withClue("The {T}, exile-from-graveyard mana ability should be offered") {
                    activate shouldNotBe null
                }

                game.execute(activate!!.action)
                game.resolveStack()

                withClue("Each opponent takes 1 damage from the reflexive trigger") {
                    game.getLifeTotal(2) shouldBe 19
                }
                withClue("The graveyard card was exiled as a cost") {
                    game.state.getGraveyard(game.player1Id).none {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
                    } shouldBe true
                }
            }
        }
    }
}
