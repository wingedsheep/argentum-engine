package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Allure of the Unknown (THB #207): the opponent exiles a nonland card from the top six, the
 * caster takes the rest, and that same opponent may cast the exiled card for free.
 */
class AllureOfTheUnknownScenarioTest : ScenarioTestBase() {

    private fun TestGame.names(player: Int, zone: Zone) =
        state.getZone(if (player == 1) player1Id else player2Id, zone).mapNotNull {
            state.getEntity(it)?.get<CardComponent>()?.name
        }

    private fun TestGame.libraryId(name: String) =
        state.getZone(player1Id, Zone.LIBRARY).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.exileId(name: String) =
        state.getZone(player1Id, Zone.EXILE).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.castAllure() {
        val result = castSpell(1, "Allure of the Unknown")
        withClue("Casting should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    private fun setup() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Allure of the Unknown")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Glory Seeker")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("the opponent exiles a nonland card and casts it for free; the caster takes the rest") {
            val game = setup()
            game.castAllure()

            val exileChoice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the opponent makes the exile choice") { exileChoice.playerId shouldBe game.player2Id }
            exileChoice.options shouldContainExactlyInAnyOrder listOf(
                game.libraryId("Hill Giant"),
                game.libraryId("Glory Seeker"),
            )
            game.selectCards(listOf(game.libraryId("Hill Giant")))

            val castChoice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the same opponent is offered the free cast") { castChoice.playerId shouldBe game.player2Id }
            game.selectCards(listOf(game.exileId("Hill Giant")))
            game.resolveStack()

            game.names(1, Zone.HAND) shouldContainExactlyInAnyOrder
                listOf("Glory Seeker", "Mountain", "Mountain", "Swamp", "Swamp")
            val giant = game.findPermanent("Hill Giant")
            withClue("Hill Giant should be on the battlefield") { (giant != null) shouldBe true }
            game.state.projectedState.getController(giant!!) shouldBe game.player2Id
        }

        test("declining the free cast leaves the card in exile") {
            val game = setup()
            game.castAllure()

            game.selectCards(listOf(game.libraryId("Glory Seeker")))
            game.skipSelection()
            game.resolveStack()

            game.names(1, Zone.EXILE) shouldBe listOf("Glory Seeker")
            game.names(1, Zone.HAND) shouldContainExactlyInAnyOrder
                listOf("Hill Giant", "Mountain", "Mountain", "Swamp", "Swamp")
            game.isOnBattlefield("Glory Seeker") shouldBe false
        }
    }
}
