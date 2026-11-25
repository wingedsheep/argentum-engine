package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "As this enters, choose a player" (ChoiceType.PLAYER): the choice only records a name, so every
 * option simulates to the same board. The AI seated first used to take the first option — itself —
 * and point Stuffy Doll's damage at its own face. Ties now lean to an opponent.
 */
class EnteringPlayerChoiceAiTest : ScenarioTestBase() {
    init {
        test("the AI names its opponent, not itself, as Stuffy Doll enters") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardInHand(1, "Stuffy Doll")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withTurnNumber(5)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.castSpell(1, "Stuffy Doll").error shouldBe null
            var guard = 0
            while (game.state.pendingDecision == null && guard++ < 20) {
                game.execute(PassPriority(game.state.priorityPlayerId!!))
            }
            val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            decision.playerId shouldBe game.player1Id
            game.state.turnOrder.first() shouldBe game.player1Id // the old tie-break's victim

            for (profile in listOf(AiProfile.LIVE, AiProfile.CURRENT)) {
                val answer = AIPlayer.create(cardRegistry, game.player1Id, profile)
                    .respondToDecision(game.state, decision)
                    .shouldBeInstanceOf<OptionChosenResponse>()
                game.state.turnOrder[answer.optionIndex] shouldBe game.player2Id
            }
        }
    }
}
