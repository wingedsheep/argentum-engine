package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.SpikeTailedCeratops
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class ExtraBlockingAiTest : FunSpec({
    test("extra blocking is recognized and priced beyond the creature's body") {
        CardIntentAnalyzer.analyze(SpikeTailedCeratops).tags shouldContain IntentTag.EXTRA_BLOCKING
        val rating = AiCardRating.of(SpikeTailedCeratops, listOf("XLN"))
        rating.intent.unrecognised shouldBe false
        rating.limited.explanation.heuristic.filter { it.part.startsWith("Static") }
            .sumOf { it.value } shouldBeGreaterThan 0.0
        rating.board.parts.single { it.label == "Additional blocks" }.value shouldBeGreaterThan 0.0
    }

    for ((name, number, expected) in listOf(
        Triple("Spike-Tailed Ceratops", 2, 2),
        Triple("Spike-Tailed Ceratops", 3, 2),
        Triple("Centaur Courser", 2, 1),
    )) {
        test("AI assigns $expected blocks with $name against $number attackers") {
            val game = GameTestDriver().apply {
                registerCards(TestCards.all + SpikeTailedCeratops)
                initMirrorMatch(Deck.of("Forest" to 40))
                passPriorityUntil(Step.PRECOMBAT_MAIN)
            }
            val blocker = game.putCreatureOnBattlefield(game.player2, name)
            val attackers = List(number) { game.putCreatureOnBattlefield(game.player1, "Llanowar Elves") }
            attackers.forEach(game::removeSummoningSickness)
            game.passPriorityUntil(Step.DECLARE_ATTACKERS)
            game.declareAttackers(game.player1, attackers, game.player2).error shouldBe null
            game.passPriorityUntil(Step.DECLARE_BLOCKERS)
            val legal = LegalActionEnumerator.create(game.cardRegistry).enumerate(game.state, game.player2)
                .single { it.actionType == "DeclareBlockers" }
            val action = AIPlayer.create(game.cardRegistry, game.player2)
                .chooseFrom(game.state, listOf(legal)).action as DeclareBlockers
            action.blockers[blocker]!!.size shouldBe expected
            game.submit(action).error shouldBe null
        }
    }
})
