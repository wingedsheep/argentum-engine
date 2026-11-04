package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.player.PlayerCitysBlessingComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rix.cards.TimestreamNavigator
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Timestream Navigator (RIX #59) — "Ascend. {2}{U}{U}, {T}, Put this creature on the bottom of its
 * owner's library: Take an extra turn after this one. Activate only if you have the city's blessing."
 *
 * The first battlefield-sourced use of the put-self-on-the-bottom cost (Daniels pays it from the
 * graveyard): the Navigator leaves for the bottom of the library as the ability goes on the stack,
 * and the ability still grants the extra turn. Gated on the blessing from its own ascend.
 */
class TimestreamNavigatorScenarioTest : ScenarioTestBase() {

    private val extraTurnAbility = TimestreamNavigator.activatedAbilities.single().id

    init {
        test("needs the city's blessing; the cost bottoms the Navigator and its controller takes an extra turn") {
            // Navigator + 8 Islands = 9 permanents; the Island in hand makes ten.
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Timestream Navigator")
                .withLandsOnBattlefield(1, "Island", 8)
                .withCardInHand(1, "Island")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val navigator = game.findPermanent("Timestream Navigator").shouldNotBeNull()

            withClue("nine permanents: no blessing, so the ability can't be activated") {
                game.execute(ActivateAbility(game.player1Id, navigator, extraTurnAbility)).error.shouldNotBeNull()
            }

            val island = game.findCardsInHand(1, "Island").single()
            game.execute(PlayLand(game.player1Id, island)).error shouldBe null
            game.state.getEntity(game.player1Id)?.has<PlayerCitysBlessingComponent>() shouldBe true

            game.execute(ActivateAbility(game.player1Id, navigator, extraTurnAbility)).error shouldBe null

            withClue("the cost put the Navigator on the bottom of its owner's library before resolution") {
                game.findPermanent("Timestream Navigator") shouldBe null
                game.state.getLibrary(game.player1Id).last() shouldBe navigator
                game.librarySize(1) shouldBe 3
            }

            game.resolveStack()

            withClue("Player 1 takes the extra turn, so Player 2 skips their next turn") {
                game.state.getEntity(game.player2Id)?.has<SkipNextTurnComponent>() shouldBe true
                game.state.getEntity(game.player1Id)?.has<SkipNextTurnComponent>() shouldBe false
            }
        }
    }
}
