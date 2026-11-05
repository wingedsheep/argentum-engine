package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "At the beginning of your upkeep, exile the top card of your library. If it's a nonland card,
 * you may cast that card this turn" (Vance's Blasting Cannons): a "this turn" impulse made at the
 * start of our own turn has both main phases ahead to spend the card in — a card drawn each turn,
 * read as [IntentTag.DRAW] and priced as repeatable card draw on the battlefield. A "this turn"
 * impulse we time ourselves (Act on Impulse) still does not read as one.
 */
class TurnStartImpulseReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("'$name' is not in the corpus")

    init {
        test("an upkeep impulse is card draw, and the permanent is priced as repeatable draw") {
            val cannons = card("Vance's Blasting Cannons")
            val intent = CardIntentAnalyzer.analyzeSelf(cannons)
            withClue("the upkeep exile-and-cast-this-turn reads as a draw") {
                intent.tags shouldContain IntentTag.DRAW
                AiReadingAudit.audit(cannons).shouldBeEmpty()
            }
            withClue("on the battlefield it is worth repeatable card draw, not a generic repeatable ability") {
                CardIntentAnalyzer.priorLadder(intent).map { it.claim } shouldContain "repeatable card draw"
            }
        }

        test("a this-turn impulse we time ourselves still is not read as a draw") {
            CardIntentAnalyzer.analyze(card("Act on Impulse")).tags shouldNotContain IntentTag.DRAW
        }
    }
}
