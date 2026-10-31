package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.OneShotMana
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A Treasure is mana — one mana, once — and the evaluator has to see it as that
 * ([AiProfile.oneShotManaIsMana], the `OneShotMana` term).
 *
 * Follow-up to #4116 (choose-N modal modes), where a mode that makes a Treasure scored as nothing
 * worth having: `Tempo` counts lands only, and the board prior read the token as a flat mana rock
 * whatever the hand held.
 */
class TreasureValueAiTest : ScenarioTestBase() {

    private fun TestGame.advanceToOption(): ChooseOptionDecision {
        var guard = 0
        while (state.pendingDecision == null && guard++ < 20) {
            execute(PassPriority(state.priorityPlayerId!!))
        }
        return state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
    }

    private fun TestGame.choice(profile: AiProfile, decision: ChooseOptionDecision): Int =
        AIPlayer.create(cardRegistry, player1Id, profile)
            .respondToDecision(state, decision)
            .shouldBeInstanceOf<OptionChosenResponse>().optionIndex

    init {
        // Three Forests, no land in hand, a four-drop waiting, at 20 life. Bears entering already
        // took Gala Greeters' +1/+1 counter; Llanowar Elves entering asks again, and only "create a
        // tapped Treasure" and "gain 2 life" are left. The Treasure is the fourth mana — and the red —
        // that casts Hill Giant next turn; 2 life at 20 is close to nothing. Without the term the life won,
        // because the Treasure's whole worth was a flat mana-rock prior that the evaluator's
        // threat terms outweighed by a hair.
        test("Gala Greeters makes the Treasure that casts the four-drop in hand a turn early") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardOnBattlefield(1, "Gala Greeters")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Llanowar Elves")
                .withCardInHand(1, "Hill Giant")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withTurnNumber(5)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            val first = game.advanceToOption()
            game.execute(SubmitDecision(game.player1Id, OptionChosenResponse(first.id, 0))).error shouldBe null
            var guard = 0
            while (game.state.stack.isNotEmpty() && guard++ < 10) {
                game.execute(PassPriority(game.state.priorityPlayerId!!))
            }
            game.castSpell(1, "Llanowar Elves").error shouldBe null
            val decision = game.advanceToOption()
            val treasure = decision.options.indexOf("Create a tapped Treasure token")
            val life = decision.options.indexOf("You gain 2 life")
            withClue("the counter was taken this turn: ${decision.options}") {
                (treasure >= 0 && life >= 0) shouldBe true
            }

            // The misplay, pinned: without the term the 2 life wins.
            game.choice(AiProfile.LIVE.copy(oneShotManaIsMana = false), decision) shouldBe life
            game.choice(AiProfile.LIVE, decision) shouldBe treasure
        }

        test("a Treasure is a share of a land drop, never the price of the first land") {
            val land = com.wingedsheep.ai.engine.evaluation.Tempo.landValueAt(4) -
                com.wingedsheep.ai.engine.evaluation.Tempo.landValueAt(3)
            val treasure = OneShotMana.value(lands = 3, sources = 1)
            withClue("one mana once is a share of the mana every turn") {
                treasure shouldBeGreaterThan 0.0
                treasure shouldBeLessThan land
            }
            // Beside no lands it is not priced as the first land, which Tempo charges 7 for.
            OneShotMana.value(lands = 0, sources = 1) shouldBeLessThan 2.0
        }

        test("the term reads Treasure and Gold, and not Clue, Food or Blood") {
            val intents = IntentCatalog.of(cardRegistry)
            fun printed(name: String) =
                intents.definition(name)!!.script.activatedAbilities.any(OneShotMana::isOneShotMana)
            printed("Treasure") shouldBe true
            printed("Gold") shouldBe true
            printed("Clue") shouldBe false
            printed("Food") shouldBe false
            printed("Blood") shouldBe false
        }
    }
}
