package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Return target spell to its owner's hand" (Failure, Reprieve) — `ReturnSpellToOwnersHandEffect`
 * — undoes a spell on the stack like a counter: it reads as [IntentTag.COUNTERSPELL], aimed only at
 * an opposing spell. Unread, the AI held no intent for the card and could aim it at its own spell.
 */
class SpellBounceReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("returning a spell to hand is a counterspell") {
            CardIntentAnalyzer.analyze(card("Reprieve")).tags shouldContain IntentTag.COUNTERSPELL
            CardIntentAnalyzer.analyze(card("Failure // Comply")).tags shouldContain IntentTag.COUNTERSPELL
        }

        test("the bounced spell is an opponent's") {
            TargetPolarityAnalyzer.forSpell(card("Reprieve")) shouldBe listOf(TargetPolarity.HARMFUL)
        }
    }
}
