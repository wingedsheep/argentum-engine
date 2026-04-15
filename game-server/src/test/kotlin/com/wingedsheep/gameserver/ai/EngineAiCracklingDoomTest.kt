package com.wingedsheep.gameserver.ai

import com.wingedsheep.engine.ai.AIPlayer
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Verifies that the in-process engine AI (used by EngineAiController) actually
 * produces a valid CardsSelectedResponse when asked to sacrifice one of several
 * creatures tied for greatest power — the scenario the user reported as freezing.
 *
 * If the engine mode AI ever stops responding to this decision (silent hang),
 * this test will fail at the SubmitDecision step.
 */
class EngineAiCracklingDoomTest : ScenarioTestBase() {

    init {
        context("EngineAi sacrifice decision for Crackling Doom") {
            test("AI picks one of multiple creatures tied for greatest power") {
                val game = scenario()
                    .withPlayers("Human", "AI")
                    .withCardInHand(1, "Crackling Doom")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(2, "Hill Giant")       // 3/3
                    .withCardOnBattlefield(2, "Raging Minotaur")  // 3/3
                    .withCardOnBattlefield(2, "Grizzly Bears")    // 2/2
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Crackling Doom")
                game.resolveStack()

                // Engine has paused on a SelectCardsDecision addressed to the AI
                val decision = game.state.pendingDecision
                decision.shouldNotBeNull()
                (decision is SelectCardsDecision) shouldBe true
                decision.playerId shouldBe game.player2Id
                (decision as SelectCardsDecision).options.size shouldBe 2

                // Instantiate the real engine AI (same class EngineAiController uses)
                // and ask it to respond to the pending decision.
                val ai = AIPlayer.create(cardRegistry, game.player2Id)
                val response = ai.respondToDecision(game.state, decision)

                // Sanity check: AI returned a valid response for this decision
                (response is CardsSelectedResponse) shouldBe true
                val cardsSelected = response as CardsSelectedResponse
                cardsSelected.decisionId shouldBe decision.id
                cardsSelected.selectedCards.size shouldBe 1
                (cardsSelected.selectedCards.single() in decision.options) shouldBe true

                // Submitting the AI's response must advance the game: the chosen
                // creature leaves the battlefield, pending decision clears.
                val chosen = cardsSelected.selectedCards.single()
                val chosenName = game.state.getEntity(chosen)?.let { container ->
                    container.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name
                }
                chosenName.shouldNotBeNull()

                val result = actionProcessor.process(
                    game.state,
                    SubmitDecision(game.player2Id, response)
                ).result
                result.error shouldBe null
                game.state = result.state

                game.state.pendingDecision shouldBe null
                game.findPermanent(chosenName) shouldBe null

                // The non-chosen tied creature (and the 2/2) must still be there.
                val survivor = if (chosenName == "Hill Giant") "Raging Minotaur" else "Hill Giant"
                game.findPermanent(survivor) shouldNotBe null
                game.findPermanent("Grizzly Bears") shouldNotBe null
            }
        }
    }
}
