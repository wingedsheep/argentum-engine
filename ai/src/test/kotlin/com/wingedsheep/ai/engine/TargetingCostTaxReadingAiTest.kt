package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * "Spells your opponents cast that target a Merfolk you control cost {2} more to cast" and its
 * activated-ability twin (Kopala, Warden of Waves; Sphinx of New Prahv's spell-only half) tax the
 * opponent for answering our permanents — a ward-like protection. Unread, the statics were
 * "abilities not recognised" and the draft heuristic priced Kopala as a vanilla 2/2.
 */
class TargetingCostTaxReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("a tax on opponents targeting our permanents reads as protection") {
            listOf("Kopala, Warden of Waves", "Sphinx of New Prahv").forEach { name ->
                withClue(name) { CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.PROTECTION }
            }
            val kopala = card("Kopala, Warden of Waves").script.staticAbilities
            withClue("both of Kopala's statics — the spell tax and the ability tax — read on their own") {
                kopala.map { IntentTag.PROTECTION in CardIntentAnalyzer.staticTags(it) } shouldBe listOf(true, true)
            }
        }

        test("an untargeted tax on every opponent ability is not protection") {
            withClue("Tithe Taker taxes all abilities, not answers aimed at our permanents") {
                CardIntentAnalyzer.analyze(card("Tithe Taker")).tags shouldNotContain IntentTag.PROTECTION
            }
        }

        test("the draft heuristic prices Kopala's statics above its bare body") {
            val terms = LimitedCardRater.explain(card("Kopala, Warden of Waves")).heuristic
            val staticTerms = terms.filter { it.part.startsWith("Static") }
            withClue("terms: $terms") {
                staticTerms.size shouldBe 2
                staticTerms.sumOf { it.value } shouldBeGreaterThan 0.0
            }
        }
    }
}
