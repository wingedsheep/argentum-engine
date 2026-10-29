package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Klothys, God of Destiny (THB #220).
 *
 * The first-main-phase trigger exiles the target graveyard card, then branches on that card's
 * type *after* it has moved to exile: a land card adds {R} or {G} (the player's choice) and
 * nothing else; any other card gains 2 life and deals 2 damage to each opponent.
 */
class KlothysGodOfDestinyScenarioTest : ScenarioTestBase() {

    private fun TestGame.pool(playerId: EntityId): ManaPoolComponent =
        state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    private fun ScenarioBuilder.withLibraries(): ScenarioBuilder {
        repeat(5) {
            withCardInLibrary(1, "Island")
            withCardInLibrary(2, "Island")
        }
        return this
    }

    init {
        context("Klothys, God of Destiny") {

            test("exiling a land card adds {R} or {G} and nothing else") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Klothys, God of Destiny")
                    .withCardInGraveyard(2, "Forest")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withLibraries()
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.DRAW)
                    .build()

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                val forest = game.findCardsInGraveyard(2, "Forest").single()
                game.selectTargets(listOf(forest))
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("the player chooses between red and green") { decision shouldNotBe null }
                game.submitDecision(ColorChosenResponse(decision!!.id, Color.GREEN)).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Forest") shouldBe true
                game.pool(game.player1Id).green shouldBe 1
                game.pool(game.player1Id).red shouldBe 0
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }

            test("exiling a nonland card gains 2 life and deals 2 damage to each opponent") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Klothys, God of Destiny")
                    .withCardInGraveyard(2, "Forest")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withLibraries()
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.DRAW)
                    .build()

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
                game.selectTargets(listOf(bears))
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.getLifeTotal(1) shouldBe 22
                game.getLifeTotal(2) shouldBe 18
                game.pool(game.player1Id).green shouldBe 0
                game.pool(game.player1Id).red shouldBe 0
            }
        }
    }
}
