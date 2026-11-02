package com.wingedsheep.ai.engine

import com.wingedsheep.ai.ActionResponse
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The live controller learns each opponent's registered decklist from the source the server hands
 * it ([EngineAiPlayerController.setOpponentDeckListSource]) — never its own, and only once — and
 * plays on it: the `7988f9c0` turn-7 Refute Destiny (see [RemovalForUnseenThreatsTest]) is held.
 */
class EngineAiOpponentDecklistTest : ScenarioTestBase() {

    private val theirList = mapOf(
        "Ruric Thar, Magecrusher" to 1, "Titanbones, Towering Heart" to 2, "Hexhaven Invigorator" to 1,
        "Craftwork Crusher" to 1, "Wrecking Gecko" to 1, "Greenhouse Propagator" to 1, "Grizzly Bears" to 10,
        "Forest" to 17,
    )

    init {
        test("the controller reads the opponent's decklist and holds its only removal for the bombs") {
            val game = scenario().withPlayers()
                .withTurnNumber(7)
                .withActivePlayer(1)
                .withLifeTotal(2, 21)
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInHand(1, "Refute Destiny")
                .withCardOnBattlefield(1, "Danitha, Sword of Hope")
                .withLandsOnBattlefield(2, "Forest", 3)
                .withCardOnBattlefield(2, "Greenhouse Propagator")
                .withCardInHand(2, "Grizzly Bears").withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Grizzly Bears").withCardInHand(2, "Forest").withCardInHand(2, "Forest")
                .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val asked = mutableListOf<EntityId>()
            val controller = EngineAiPlayerController(cardRegistry, game.player1Id, { game.state })
            controller.setOpponentDeckListSource { seat -> asked += seat; if (seat == game.player2Id) theirList else null }

            val view = ClientStateTransformer(cardRegistry, predicateEvaluator = services.predicateEvaluator)
                .transform(game.state, game.player1Id)
            val response = controller.chooseAction(view, game.getLegalActions(1), null, emptyList())
            controller.chooseAction(view, game.getLegalActions(1), null, emptyList())

            asked shouldBe listOf(game.player2Id)
            controller.opponentKnowledge!!.knowsDecklistOf(game.player2Id) shouldBe true
            val action = response.shouldBeInstanceOf<ActionResponse.SubmitAction>().action
            (action is CastSpell) shouldBe false
        }
    }
}
