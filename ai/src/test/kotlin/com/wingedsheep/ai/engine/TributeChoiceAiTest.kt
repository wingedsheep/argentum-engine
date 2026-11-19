package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tribute (CR 702.104): the *opponent* of the creature's controller answers "pay tribute or not" as
 * it enters. The AI answering it weighs the counters it would hand over against the "if tribute
 * wasn't paid" trigger it would let through, rather than taking the first option.
 */
class TributeChoiceAiTest : ScenarioTestBase() {

    private fun TestGame.advanceToTributeChoice(): ChooseOptionDecision {
        var guard = 0
        while (state.pendingDecision == null && guard++ < 20) {
            execute(PassPriority(state.priorityPlayerId!!))
        }
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe player2Id
        return decision
    }

    private fun TestGame.aiAnswer(decision: ChooseOptionDecision): String =
        decision.options[AIPlayer.create(cardRegistry, player2Id, AiProfile.LIVE)
            .respondToDecision(state, decision)
            .shouldBeInstanceOf<OptionChosenResponse>().optionIndex]

    private fun TestGame.castAndAsk(card: String): ChooseOptionDecision {
        castSpell(1, card).error shouldBe null
        return advanceToTributeChoice()
    }

    init {
        // Declining lets the Fanatic swing as a hasty 4/4 trampler into an empty board at 4 life;
        // paying leaves a 4/4 that can't attack this turn.
        test("at 4 life with no blockers, the AI pays tribute rather than take a lethal hasty attack") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Fanatic of Xenagos")
                .withLifeTotal(2, 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withTurnNumber(5)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.aiAnswer(game.castAndAsk("Fanatic of Xenagos")) shouldBe "Pay tribute"
        }

        // Three +1/+1 counters make a 7/7 for good; declining costs the opponent only 4 life.
        test("the AI won't make Snake of the Golden Grove a 7/7 to deny 4 life") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInHand(1, "Snake of the Golden Grove")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withTurnNumber(5)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.aiAnswer(game.castAndAsk("Snake of the Golden Grove")) shouldBe "Don't pay tribute"
        }

        // The counters an opponent may hand over are half of Tribute's bargain; the draft heuristic
        // prices them beside the not-paid trigger, a share of a counter each.
        test("the draft heuristic prices Tribute's counters") {
            val terms = LimitedCardRater.explain(cardRegistry.getCard("Snake of the Golden Grove")!!).heuristic
            withClue("terms: $terms") {
                terms.single { it.part.startsWith("Replacement") }.value shouldBe (0.3 plusOrMinus 1e-9)
            }
        }
    }
}
