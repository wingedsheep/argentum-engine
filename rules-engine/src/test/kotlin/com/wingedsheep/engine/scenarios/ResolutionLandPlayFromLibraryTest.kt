package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A resolving "you may play that card" reaching a land in the library (Djinn of Wishes' revealed
 * card). `PlayFromCollectionWithoutPayingCostExecutor` grants a per-card may-play permission for the
 * span of the play; `PlayLandHandler` honours a *library* permission only mid-resolution, so the
 * same grant can never surface as a land play at priority. The play is still a land play: it needs
 * the active player's turn (CR 305.3) and an unused land play (CR 305.2b).
 */
class ResolutionLandPlayFromLibraryTest : ScenarioTestBase() {

    private fun gameWithForestUnderTop() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.grantLibraryPermission(): EntityId {
        val forest = findCardsInLibrary(1, "Forest").single()
        val (permId, withId) = state.newEntity()
        state = withId.addMayPlayPermission(
            MayPlayPermission(
                id = permId, cardIds = setOf(forest), controllerId = player1Id, timestamp = withId.timestamp
            )
        )
        return forest
    }

    init {
        test("a library may-play permission is not a land play at priority") {
            val game = gameWithForestUnderTop()
            val forest = game.grantLibraryPermission()

            withClue("outside resolution the permission doesn't reach into the library") {
                game.execute(PlayLand(game.player1Id, forest)).error shouldNotBe null
            }
            game.state.getLibrary(game.player1Id).size shouldBe 2
        }

        test("during resolution the permitted land is played from anywhere in the library") {
            val game = gameWithForestUnderTop()
            val forest = game.grantLibraryPermission()

            val result = services.playLandHandler.executeDuringResolution(game.state, PlayLand(game.player1Id, forest))
            result.error shouldBe null
            withClue("the Forest left the library for the battlefield, and the grant is spent") {
                (forest in result.state.getBattlefield(game.player1Id)) shouldBe true
                result.state.getLibrary(game.player1Id).size shouldBe 1
                result.state.mayPlayPermissions shouldBe emptyList()
            }
        }

        test("during resolution a library land still needs an unused land play (CR 305.2b)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Plains")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.execute(PlayLand(game.player1Id, game.findCardsInHand(1, "Plains").single())).error shouldBe null
            val forest = game.grantLibraryPermission()

            services.playLandHandler.executeDuringResolution(game.state, PlayLand(game.player1Id, forest))
                .error shouldNotBe null
        }

        test("during resolution a library land can't be played on another player's turn (CR 305.3)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val forest = game.grantLibraryPermission()

            services.playLandHandler.executeDuringResolution(game.state, PlayLand(game.player1Id, forest))
                .error shouldNotBe null
        }
    }
}
