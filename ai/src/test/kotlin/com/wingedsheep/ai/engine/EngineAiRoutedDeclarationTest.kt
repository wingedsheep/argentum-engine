package com.wingedsheep.ai.engine

import com.wingedsheep.ai.ActionResponse
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A block declaration the server routes to the engine AI from another seat (Brutal Hordechief's
 * "you choose how those creatures block", Master Warcraft) is still that seat's declaration. The
 * AI must submit it for that seat — one under its own id is rejected every time, and the stall
 * guard concedes the AI's seat after enough rejections.
 */
class EngineAiRoutedDeclarationTest : ScenarioTestBase() {

    private fun TestGame.advanceTo(step: Step) {
        var guard = 0
        while (state.step != step && guard++ < 20) {
            val p = state.priorityPlayerId ?: break
            execute(PassPriority(p))
        }
        state.step shouldBe step
    }

    init {
        test("the AI holding Hordechief's grant declares the defender's forced blocks for the defender") {
            val game = scenario()
                .withPlayers("AI", "Human")
                .withCardOnBattlefield(1, "Brutal Hordechief")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val hordechief = game.findPermanent("Brutal Hordechief")!!
            val ability = cardRegistry.getCard("Brutal Hordechief")!!.script.activatedAbilities.single()
            game.execute(ActivateAbility(game.player1Id, hordechief, ability.id)).error shouldBe null
            game.resolveStack()
            game.advanceTo(Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Brutal Hordechief" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.advanceTo(Step.DECLARE_BLOCKERS)

            val controller = EngineAiPlayerController(cardRegistry, game.player1Id, { game.state })
            val view = ClientStateTransformer(cardRegistry, predicateEvaluator = services.predicateEvaluator).transform(game.state, game.player1Id)
            val response = controller.chooseAction(view, game.getLegalActions(1), null, emptyList())

            val action = response.shouldBeInstanceOf<ActionResponse.SubmitAction>().action
                .shouldBeInstanceOf<DeclareBlockers>()
            action.playerId shouldBe game.player2Id
            withClue("the Giant must block, so the declaration names it") {
                action.blockers.keys.shouldNotBeEmpty()
            }
            game.execute(action).error shouldBe null
        }
    }
}
