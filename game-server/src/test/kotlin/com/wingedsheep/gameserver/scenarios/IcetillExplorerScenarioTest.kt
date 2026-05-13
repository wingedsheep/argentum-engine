package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Icetill Explorer.
 *
 * Card reference:
 * - Icetill Explorer ({2}{G}{G}): Creature — Insect Scout 2/4
 *   You may play an additional land on each of your turns.
 *   You may play lands from your graveyard.
 *   Landfall — Whenever a land you control enters, mill a card.
 */
class IcetillExplorerScenarioTest : ScenarioTestBase() {

    /** Find a land in a player's graveyard by name, returning its entity id. */
    private fun findInGraveyard(game: TestGame, playerNumber: Int, cardName: String): EntityId {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getGraveyard(playerId).find { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == cardName
        } ?: error("'$cardName' not found in player $playerNumber's graveyard")
    }

    init {
        context("MayPlayLandsFromGraveyard") {

            test("can play a land from graveyard with Icetill Explorer on battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Icetill Explorer")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInLibrary(1, "Forest")  // prevent draw-from-empty loss
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forestId = findInGraveyard(game, 1, "Forest")
                val result = game.execute(PlayLand(game.player1Id, forestId))

                withClue("Playing a land from graveyard should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                withClue("Forest should now be on the battlefield") {
                    game.isOnBattlefield("Forest") shouldBe true
                }
                withClue("Forest should no longer be in graveyard") {
                    game.isInGraveyard(1, "Forest") shouldBe false
                }
            }

            test("playing a graveyard land consumes a land drop") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Icetill Explorer")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val landDropsBefore = game.state.getEntity(game.player1Id)?.get<LandDropsComponent>()!!.remaining
                val forestId = findInGraveyard(game, 1, "Forest")
                game.execute(PlayLand(game.player1Id, forestId))

                // Playing a land from the graveyard must consume the same `remaining` counter
                // a hand play would: the new Crucible-style permission shortcuts the Muldrotha
                // tracker but still routes through `landDrops.use()`.
                val landDropsAfter = game.state.getEntity(game.player1Id)?.get<LandDropsComponent>()
                withClue("Graveyard land play should decrement LandDropsComponent.remaining by 1") {
                    landDropsAfter shouldNotBe null
                    landDropsAfter!!.remaining shouldBe landDropsBefore - 1
                }
            }

            test("cannot play land from graveyard without Icetill Explorer") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forestId = findInGraveyard(game, 1, "Forest")
                val result = game.execute(PlayLand(game.player1Id, forestId))

                withClue("Playing land from graveyard should fail without permission") {
                    result.error shouldNotBe null
                }
            }
        }

        context("Landfall — mill a card when land enters") {

            test("playing a land from graveyard triggers landfall and mills one card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Icetill Explorer")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInLibrary(1, "Forest")  // the card that will be milled
                    .withCardInLibrary(1, "Forest")  // prevent draw-from-empty loss
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)
                val graveyardBefore = game.graveyardSize(1)

                val forestId = findInGraveyard(game, 1, "Forest")
                game.execute(PlayLand(game.player1Id, forestId))
                game.resolveStack()

                // Landfall trigger fires — one card should be milled
                withClue("One card should have been milled from library to graveyard") {
                    game.librarySize(1) shouldBe libraryBefore - 1
                    // graveyard: lost the land (played to battlefield) but gained 1 milled card
                    game.graveyardSize(1) shouldBe graveyardBefore - 1 + 1
                }
            }
        }

        context("GrantAdditionalLandDrop — play two lands per turn") {

            test("can play two lands per turn with Icetill Explorer") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Icetill Explorer")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")  // third land — should fail to play
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // First land: from graveyard
                val graveyardForest = findInGraveyard(game, 1, "Forest")
                val result1 = game.execute(PlayLand(game.player1Id, graveyardForest))
                game.resolveStack()  // resolve landfall trigger before playing the next land
                withClue("First land (from graveyard) should succeed: ${result1.error}") {
                    result1.error shouldBe null
                }

                // Second land: from hand (using the bonus land drop)
                val handForestId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Forest"
                }
                val result2 = game.execute(PlayLand(game.player1Id, handForestId))
                game.resolveStack()  // resolve second landfall trigger
                withClue("Second land (from hand via bonus drop) should succeed: ${result2.error}") {
                    result2.error shouldBe null
                }

                // Third land should fail — base drop + Icetill bonus is 2 per turn, both spent.
                val remainingForest = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Forest"
                }
                val result3 = game.execute(PlayLand(game.player1Id, remainingForest))
                withClue("Third land should fail — Icetill grants exactly one extra drop") {
                    result3.error shouldNotBe null
                }
            }
        }
    }
}
