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
 * Agonizing Remorse (THB #83): one choice over the opponent's nonland hand cards and every card
 * in their graveyard (lands included), then exile it and lose 1 life.
 */
class AgonizingRemorseScenarioTest : ScenarioTestBase() {

    private fun TestGame.names(zone: Zone) =
        state.getZone(player2Id, zone).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    private fun TestGame.idIn(zone: Zone, name: String) =
        state.getZone(player2Id, zone).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.cast() {
        val result = castSpellTargetingPlayer(1, "Agonizing Remorse", 2)
        withClue("Casting should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    init {
        test("pool is nonland hand cards plus every graveyard card; a graveyard land can be exiled") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Agonizing Remorse")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInHand(2, "Hill Giant")
                .withCardInHand(2, "Forest")
                .withCardInGraveyard(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cast()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.options shouldContainExactlyInAnyOrder listOf(
                game.idIn(Zone.HAND, "Hill Giant"),
                game.idIn(Zone.GRAVEYARD, "Mountain"),
            )
            game.selectCards(listOf(game.idIn(Zone.GRAVEYARD, "Mountain")))
            game.resolveStack()

            game.names(Zone.EXILE) shouldBe listOf("Mountain")
            game.names(Zone.HAND) shouldContainExactlyInAnyOrder listOf("Hill Giant", "Forest")
            game.getLifeTotal(1) shouldBe 19
            game.getLifeTotal(2) shouldBe 20
        }

        test("exiles a nonland card from hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Agonizing Remorse")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInHand(2, "Hill Giant")
                .withCardInGraveyard(2, "Glory Seeker")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cast()
            game.selectCards(listOf(game.idIn(Zone.HAND, "Hill Giant")))
            game.resolveStack()

            game.names(Zone.EXILE) shouldBe listOf("Hill Giant")
            game.names(Zone.GRAVEYARD) shouldBe listOf("Glory Seeker")
            game.getLifeTotal(1) shouldBe 19
        }

        test("only lands in hand and an empty graveyard: you just lose 1 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Agonizing Remorse")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInHand(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cast()

            game.hasPendingDecision() shouldBe false
            game.names(Zone.EXILE) shouldBe emptyList()
            game.names(Zone.HAND) shouldBe listOf("Forest")
            game.getLifeTotal(1) shouldBe 19
        }
    }
}
