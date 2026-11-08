package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * A trigger's captured collection names no source of its own: Godsend's "whenever equipped
 * creature blocks or becomes blocked by one or more creatures, you may exile one of those
 * creatures" chooses among the combat partners the engine captured when it fired, so the
 * pipeline read as `Pipeline(?→exile)` — no intent, no rating. The trigger's event says what the
 * set holds (opposing creatures it was paired with), and read as that gather the exile is spot
 * removal.
 */
class TriggerCapturedCollectionReadingAiTest : ScenarioTestBase() {
    /** Our Godsend-equipped Grizzly Bears attacks and is blocked by [blockers]; the trigger is resolving. */
    private fun blockedBy(vararg blockers: String): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardAttachedTo(1, "Godsend", "Grizzly Bears")
        blockers.forEach { builder.withCardOnBattlefield(2, it) }
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(blockers.associateWith { listOf("Grizzly Bears") }).error shouldBe null
        game.resolveStack()
        return game
    }

    /** What the engine AI answers Godsend's "you may exile one of those creatures" with. */
    private fun TestGame.aiPick(): List<EntityId> {
        val decision = state.pendingDecision as SelectCardsDecision
        val responder = DecisionResponder(GameSimulator(cardRegistry), AIPlayer.defaultEvaluator())
        return (responder.respond(state, decision, player1Id) as CardsSelectedResponse).selectedCards
    }

    init {
        test("exiling one of the captured combat partners reads as exile removal") {
            CardIntentAnalyzer.analyze(cardRegistry.requireCard("Godsend")).tags shouldContainAll
                listOf(IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL)
        }

        test("the AI exiles the bigger of the two creatures blocking its equipped attacker") {
            val game = blockedBy("Hill Giant", "Craw Wurm")
            val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
            withClue("one of those creatures, the bigger threat") {
                game.aiPick() shouldContainExactly listOf(wurm)
            }
        }

        test("a lone blocker is exiled, not declined") {
            val game = blockedBy("Hill Giant")
            val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
            withClue("\"up to one\" of a single opposing creature is still removal worth taking") {
                game.aiPick() shouldContainExactly listOf(giant)
            }
        }
    }
}
