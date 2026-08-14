package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Practiced Offense. */
class PracticedOffenseScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private fun TestGame.findExileCopy(name: String): com.wingedsheep.sdk.model.EntityId? =
        state.getExile(player1Id).firstOrNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }

    init {
        context("Practiced Offense") {

            test("puts a +1/+1 counter on each creature the target player controls and grants the chosen keyword") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Practiced Offense")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val bears = game.findPermanents("Grizzly Bears")
                bears.size shouldBe 2
                val handCard = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Practiced Offense"
                }

                // Target: player 1 (counters on each creature they control) + first Bears (keyword).
                game.execute(
                    CastSpell(
                        game.player1Id,
                        handCard,
                        targets = listOf(
                            ChosenTarget.Player(game.player1Id),
                            ChosenTarget.Permanent(bears[0]),
                        ),
                    )
                )

                // Resolve, answering the "double strike or lifelink" choice as it surfaces.
                game.resolveStack()
                val pending = game.state.pendingDecision
                if (pending is ChooseOptionDecision) {
                    // Choose double strike (index 0).
                    game.submitDecision(OptionChosenResponse(pending.id, 0))
                    game.resolveStack()
                }

                withClue("Each creature the target player controls gets a +1/+1 counter") {
                    for (b in bears) {
                        val counters = game.state.getEntity(b)?.get<CountersComponent>()
                        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 1
                    }
                }
                withClue("The target creature gains double strike") {
                    projector.project(game.state).hasKeyword(bears[0], Keyword.DOUBLE_STRIKE) shouldBe true
                }

                withClue("Practiced Offense is now in the graveyard and can be flashed back") {
                    game.state.getGraveyard(game.player1Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Practiced Offense"
                    } shouldBe true
                }
            }
        }
    }
}
