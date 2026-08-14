package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Grave Researcher. */
class GraveResearcherScenarioTest : ScenarioTestBase() {

    private fun stunCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    private fun TestGame.findExileCopy(playerNumber: Int, name: String): EntityId? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).firstOrNull { id ->
            val e = state.getEntity(id)
            e?.get<CardComponent>()?.name == name && e.get<PreparedSpellCopyComponent>() != null
        }
    }

    /** Resolve surveil's keep/bin + top-card ordering decisions (keeping everything on top). */
    private fun resolveSurveil(game: TestGame) {
        var guard = 0
        while (guard++ < 6) {
            when (val pd = game.getPendingDecision()) {
                null -> return
                is ReorderLibraryDecision -> game.submitDecision(OrderedResponse(pd.id, pd.cards))
                else -> game.skipSelection()
            }
            game.resolveStack()
        }
    }

    init {
        context("Grave Researcher — upkeep surveil + become prepared at 3+ creatures in GY") {

            test("becomes prepared when three or more creature cards are in your graveyard") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grave Researcher", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(6) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(6) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val researcher = game.findPermanent("Grave Researcher")!!

                // Advance to player 1's own upkeep so the "your upkeep" trigger fires.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // Surveil 1 pauses for keep/bin (and a top-card ordering) decision — keep on top.
                resolveSurveil(game)

                withClue("With 3 creature cards in the graveyard, it becomes prepared") {
                    game.state.getEntity(researcher)?.get<PreparedComponent>() shouldNotBe null
                }
                withClue("A Reanimate prepare-spell copy now exists in exile") {
                    game.findExileCopy(1, "Grave Researcher") shouldNotBe null
                }
            }

            test("does NOT become prepared with fewer than three creature cards in graveyard") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grave Researcher", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(6) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(6) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val researcher = game.findPermanent("Grave Researcher")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                resolveSurveil(game)

                withClue("Only 2 creature cards in graveyard → stays unprepared") {
                    game.state.getEntity(researcher)?.get<PreparedComponent>() shouldBe null
                }
                withClue("No prepare-spell copy should exist") {
                    game.findExileCopy(1, "Grave Researcher") shouldBe null
                }
            }
        }

        context("Grave Researcher — Reanimate (back face)") {

            test("casting Reanimate puts a creature from any graveyard under your control and loses life = its MV") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grave Researcher", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLifeTotal(1, 20)
                    // Three creature cards in YOUR graveyard so the upkeep trigger prepares it.
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withCardInGraveyard(1, "Goblin Guide")
                    // Centaur Courser is a {2}{G} 3/3 (mana value 3) in the OPPONENT's graveyard.
                    .withCardInGraveyard(2, "Centaur Courser")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(8) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(8) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                // Advance to player 1's upkeep so it becomes prepared.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                resolveSurveil(game)

                // Reach player 1's precombat main (sorcery-speed, empty stack) to cast the copy.
                var guard = 0
                while (!(game.state.activePlayerId == game.player1Id &&
                        game.state.phase == Phase.PRECOMBAT_MAIN) && guard++ < 6
                ) {
                    game.passUntilPhase(Phase.ENDING, Step.END)
                    game.resolveStack()
                    game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    game.resolveStack()
                }

                val copyId = game.findExileCopy(1, "Grave Researcher")!!
                val courser = game.state.getGraveyard(game.player2Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Centaur Courser"
                }
                val lifeBefore = game.getLifeTotal(1)

                game.execute(
                    CastSpell(
                        game.player1Id,
                        copyId,
                        targets = listOf(ChosenTarget.Card(courser, game.player2Id, Zone.GRAVEYARD)),
                        faceIndex = 0,
                    )
                )
                game.resolveStack()

                withClue("Centaur Courser enters the battlefield under player 1's control") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
                withClue("You lose life equal to Centaur Courser's mana value (3)") {
                    game.getLifeTotal(1) shouldBe lifeBefore - 3
                }
            }
        }
    }
}
