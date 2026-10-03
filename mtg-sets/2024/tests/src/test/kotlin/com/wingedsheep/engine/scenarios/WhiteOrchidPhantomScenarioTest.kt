package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * White Orchid Phantom (MH3) — "When this creature enters, destroy up to one target nonbasic land.
 * Its controller may search their library for a basic land card, put it onto the battlefield
 * tapped, then shuffle."
 */
class WhiteOrchidPhantomScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "White Orchid Phantom")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(2, "Evolving Wilds")
        .withCardOnBattlefield(2, "Island")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun landsNamed(game: TestGame, name: String, controller: EntityId): List<EntityId> =
        game.state.getBattlefield().filter { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == name &&
                game.state.projectedState.getController(id) == controller
        }

    /** Cast the Phantom and resolve it so its enters trigger asks for targets. */
    private fun castPhantom(game: TestGame) {
        game.castSpell(1, "White Orchid Phantom").error shouldBe null
        game.resolveStack()
    }

    private fun resolveAnswering(game: TestGame, search: Boolean) {
        game.resolveStack()
        var safety = 0
        while (safety++ < 20) {
            when (val pending = game.state.pendingDecision) {
                is YesNoDecision -> {
                    pending.playerId shouldBe game.player2Id
                    game.answerYesNo(search).error shouldBe null
                }
                is SelectCardsDecision -> {
                    pending.playerId shouldBe game.player2Id
                    game.selectCards(pending.options.take(1)).error shouldBe null
                }
                null -> if (game.state.stack.isNotEmpty()) game.resolveStack() else break
                else -> error("Unexpected decision $pending")
            }
        }
    }

    init {
        test("destroys the nonbasic land; its controller fetches a tapped basic") {
            val game = board()
            castPhantom(game)
            game.selectTargets(listOf(game.findPermanent("Evolving Wilds")!!)).error shouldBe null
            resolveAnswering(game, search = true)

            game.isOnBattlefield("White Orchid Phantom") shouldBe true
            game.isInGraveyard(2, "Evolving Wilds") shouldBe true
            val forests = landsNamed(game, "Forest", game.player2Id)
            withClue("the destroyed land's controller fetched one Forest, tapped") {
                forests.size shouldBe 1
                game.state.getEntity(forests.single())!!.has<TappedComponent>() shouldBe true
            }
            landsNamed(game, "Plains", game.player1Id).size shouldBe 2
        }

        test("the land's controller may decline the search") {
            val game = board()
            castPhantom(game)
            game.selectTargets(listOf(game.findPermanent("Evolving Wilds")!!)).error shouldBe null
            resolveAnswering(game, search = false)

            game.isInGraveyard(2, "Evolving Wilds") shouldBe true
            landsNamed(game, "Forest", game.player2Id).size shouldBe 0
        }

        test("a basic land can't be targeted") {
            val game = board()
            castPhantom(game)
            game.selectTargets(listOf(game.findPermanent("Island")!!)).error shouldNotBe null
        }

        test("choosing no target destroys nothing and nobody searches") {
            val game = board()
            castPhantom(game)
            game.skipTargets().error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.isOnBattlefield("Evolving Wilds") shouldBe true
            landsNamed(game, "Forest", game.player2Id).size shouldBe 0
        }
    }
}
