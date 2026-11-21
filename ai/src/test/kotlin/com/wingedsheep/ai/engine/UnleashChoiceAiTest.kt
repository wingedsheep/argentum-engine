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
 * Unleash (CR 702.98): the controller chooses, as the creature enters, whether it gets a +1/+1
 * counter — and with it can't block while the counter stays. The AI answers by simulating both
 * boards rather than taking the first option.
 */
class UnleashChoiceAiTest : ScenarioTestBase() {

    private fun TestGame.castAndAiAnswer(card: String): String {
        castSpell(1, card).error shouldBe null
        var guard = 0
        while (state.pendingDecision == null && guard++ < 20) execute(PassPriority(state.priorityPlayerId!!))
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe player1Id
        return decision.options[AIPlayer.create(cardRegistry, player1Id, AiProfile.LIVE)
            .respondToDecision(state, decision)
            .shouldBeInstanceOf<OptionChosenResponse>().optionIndex]
    }

    init {
        // Nothing on the other side to block: the counter is a free point of evasive power.
        test("facing an empty board, the AI unleashes Rakdos Drake") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Rakdos Drake")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withTurnNumber(5)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.castAndAiAnswer("Rakdos Drake") shouldBe "Unleash"
        }

        // At 2 life against two 1/1 fliers, blocking one with the Drake is what keeps the AI alive.
        test("at 2 life facing a lethal flier, the AI keeps Rakdos Drake able to block") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Rakdos Drake")
                .withCardOnBattlefield(2, "Suntail Hawk")
                .withCardOnBattlefield(2, "Suntail Hawk")
                .withLifeTotal(1, 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withTurnNumber(5)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.castAndAiAnswer("Rakdos Drake") shouldBe "Don't unleash"
        }
    }
}
