package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Holodeck — {7} Legendary Artifact: "Once during each of your turns, you may play a permanent
 * card you own from outside the game or from your graveyard."
 *
 * Outside the game is the owner's sideboard (CR 400.11a). One allowance covers both zones and both
 * kinds of play (a land or a permanent spell); it refreshes on your next turn and never opens on an
 * opponent's.
 */
class TheHolodeckScenarioTest : ScenarioTestBase() {

    private fun TestGame.sideboardCard(name: String): EntityId =
        state.getZone(player1Id, Zone.SIDEBOARD).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.graveyardCard(player: Int, name: String): EntityId =
        findCardsInGraveyard(player, name).single()

    /** The plays player 1 is offered from a graveyard or the sideboard, as "Type:Description". */
    private fun TestGame.holodeckOffers(): List<String> =
        getLegalActions(1)
            .filter { it.sourceZone == "SIDEBOARD" || it.sourceZone == "GRAVEYARD" }
            .map { it.actionType + ":" + it.description }

    private fun holodeckGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "The Holodeck")
        .withLandsOnBattlefield(1, "Forest", 4)
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInGraveyard(1, "Ambush Viper")
        .withCardInGraveyard(1, "Lightning Bolt")
        .withCardInGraveyard(2, "Hill Giant")
        .withCardInSideboard(1, "Island")
        .withCardInSideboard(1, "Divination")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("permanent cards in your graveyard and sideboard are offered; non-permanents and an opponent's cards are not") {
            val game = holodeckGame()
            val offers = game.holodeckOffers()
            withClue("offers: $offers") {
                offers.any { it.startsWith("CastSpell") && it.contains("Grizzly Bears") } shouldBe true
                offers.any { it.startsWith("PlayLand") && it.contains("Island") } shouldBe true
                offers.any { it.contains("Lightning Bolt") } shouldBe false
                offers.any { it.contains("Divination") } shouldBe false
                offers.any { it.contains("Hill Giant") } shouldBe false
            }
            game.execute(CastSpell(game.player1Id, game.graveyardCard(2, "Hill Giant"))).error shouldNotBe null
            game.execute(CastSpell(game.player1Id, game.graveyardCard(1, "Lightning Bolt"))).error shouldNotBe null
        }

        test("a permanent spell is cast from your graveyard") {
            val game = holodeckGame()
            game.execute(CastSpell(game.player1Id, game.graveyardCard(1, "Grizzly Bears"))).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
        }

        test("a land is played from outside the game") {
            val game = holodeckGame()
            game.execute(PlayLand(game.player1Id, game.sideboardCard("Island"))).error shouldBe null
            game.isOnBattlefield("Island") shouldBe true
            game.isInSideboard(1, "Island") shouldBe false
        }

        test("one play a turn across both zones, refreshed on your next turn but not usable on an opponent's") {
            val game = holodeckGame()
            game.execute(CastSpell(game.player1Id, game.graveyardCard(1, "Grizzly Bears"))).error shouldBe null
            game.resolveStack()

            withClue("the allowance is spent for the graveyard and the sideboard alike") {
                game.holodeckOffers() shouldBe emptyList()
                game.execute(PlayLand(game.player1Id, game.sideboardCard("Island"))).error shouldNotBe null
            }

            // The opponent's upkeep, with player 1 holding priority: even a flash creature
            // (Ambush Viper) isn't offered — the grant is "during each of your turns".
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.execute(PassPriority(game.player2Id)).error shouldBe null
            game.state.priorityPlayerId shouldBe game.player1Id
            withClue("nothing on the opponent's turn") {
                game.holodeckOffers() shouldBe emptyList()
                game.execute(CastSpell(game.player1Id, game.graveyardCard(1, "Ambush Viper"))).error shouldNotBe null
            }

            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            withClue("a fresh allowance on your next turn: ${game.holodeckOffers()}") {
                game.holodeckOffers().any { it.startsWith("PlayLand") && it.contains("Island") } shouldBe true
                game.holodeckOffers().any { it.contains("Ambush Viper") } shouldBe true
            }
            game.execute(PlayLand(game.player1Id, game.sideboardCard("Island"))).error shouldBe null
            game.holodeckOffers() shouldBe emptyList()
        }
    }
}
