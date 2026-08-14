package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Honorbound Page // Forum's Favor. */
class HonorboundPageScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private fun TestGame.findExileCopy(name: String): com.wingedsheep.sdk.model.EntityId? =
        state.getExile(player1Id).firstOrNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }

    init {
        context("Honorbound Page // Forum's Favor") {

            test("the prepare-spell copy gives target creature +1/+0 and flying until end of turn") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Honorbound Page")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Honorbound Page")
                game.resolveStack()

                val page = game.findPermanent("Honorbound Page")!!
                withClue("Honorbound Page enters with first strike") {
                    projector.project(game.state).hasKeyword(page, Keyword.FIRST_STRIKE) shouldBe true
                }

                val bears = game.findPermanent("Grizzly Bears")!!
                val copyId = game.findExileCopy("Honorbound Page")
                withClue("Becoming prepared creates a Forum's Favor copy in exile") {
                    copyId shouldNotBe null
                }

                // Cast the prepare-spell copy (face 0) targeting Grizzly Bears.
                game.execute(
                    CastSpell(
                        game.player1Id,
                        copyId!!,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        faceIndex = 0,
                    )
                )
                game.resolveStack()

                val projected = projector.project(game.state)
                withClue("Grizzly Bears (base 2/2) gets +1/+0 → power 3") {
                    projected.getPower(bears) shouldBe 3
                }
                withClue("Grizzly Bears gains flying") {
                    projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
                }
            }
        }
    }
}
