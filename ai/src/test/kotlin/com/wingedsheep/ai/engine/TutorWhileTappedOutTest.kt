package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.ai.engine.evaluation.EvalWeights
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A draw or tutor resolved while tapped out must not be priced as the mana it "would have" used.
 *
 * 2026-10-11 AI-vs-AI logs (`live`), game 3 turn 9: Goblin Matron's "you may search your library for
 * a Goblin card" was declined with Goblin Glider in the library. `BoardPresence`'s land-sequencing
 * refund counts tapped lands as idle only while nothing in hand becomes castable by untapping them;
 * Glider ({1}{R}) fell into that window the moment the search put it in hand, so saying yes cost the
 * whole refund — four tapped lands' worth, more than the card. The window is now read off the hand
 * the decision started from (`BoardEvaluator.rootedAt`).
 */
class TutorWhileTappedOutTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    /** Pass priority until Matron's enters trigger asks its yes/no. */
    private fun TestGame.advanceToYesNo(): YesNoDecision {
        var guard = 0
        while (state.pendingDecision == null && guard++ < 20) {
            execute(PassPriority(state.priorityPlayerId!!))
        }
        return state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
    }

    /** [AiProfile.LIVE]'s evaluator, wired the way `AIPlayer.create` wires it. */
    private fun liveEvaluator(): BoardEvaluator {
        val profile = AiProfile.LIVE
        return EvalWeights.resolveEvaluator(
            profile.evalWeightsId,
            intents,
            landDropIsNotCardLoss = profile.landDropIsNotCardLoss,
            sequenceLandsByUsableMana = profile.sequenceLandsByUsableMana,
            discountedRaceClock = profile.discountedRaceClock,
            creatureValuation = profile.creatureValuation,
            priceLandsInHandAsMana = profile.priceLandsInHandAsMana,
            evasionAfterAttacking = profile.evasionAfterAttacking,
            pendingLandDropIsMana = profile.pendingLandDropIsMana,
        )
    }

    private fun TestGame.answer(decision: YesNoDecision, evaluator: BoardEvaluator): Boolean {
        val simulator = GameSimulator(cardRegistry)
        val responder = DecisionResponder(simulator, evaluator, intents = intents, informedChoices = true)
        simulator.decisionResolver = { s, d -> responder.respond(s, d, d.playerId) }
        return (responder.respond(state, decision, player1Id) as YesNoResponse).choice
    }

    init {
        test("g3 T9: Goblin Matron searches for a two-drop with four of five lands tapped") {
            val game = scenario().withPlayers()
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(1, "Mountain", tapped = true)
                .withCardOnBattlefield(1, "Goblin Raider")
                .withCardOnBattlefield(1, "Talas Explorer")
                .withCardInHand(1, "Goblin Matron")
                .withCardInHand(1, "Sleight of Hand")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Goblin Glider")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withTurnNumber(9)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            game.castSpell(1, "Goblin Matron").error shouldBe null
            val decision = game.advanceToYesNo()
            withClue("the reconstruction: four lands tapped, one untapped, Glider castable once they untap") {
                val lands = game.state.getBattlefield(game.player1Id)
                lands.count { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe 4
            }

            val live = liveEvaluator()
            // The misplay, pinned: the same evaluator scored leaf-only — no root, so the window is
            // read off the leaf's hand and the fetched Glider closes it.
            val leafOnly = BoardEvaluator { state, projected, player -> live.evaluate(state, projected, player) }
            game.answer(decision, leafOnly) shouldBe false

            game.answer(decision, live) shouldBe true
            AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
                .respondToDecision(game.state, decision)
                .shouldBeInstanceOf<YesNoResponse>().choice shouldBe true
        }
    }
}
