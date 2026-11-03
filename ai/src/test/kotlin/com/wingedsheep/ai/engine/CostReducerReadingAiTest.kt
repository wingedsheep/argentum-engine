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
 * A static that makes its controller's own spells or activated abilities cheaper — Bladegraft
 * Aspirant's "Equipment spells you cast cost {1} less" and its targeted-ability twin, Silver-Fur
 * Master's "Ninjutsu abilities you activate cost {1} less". Unread, Bladegraft's statics were
 * "abilities not recognised" and the draft heuristic priced it as a bare 2/3 menace.
 */
class CostReducerReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("a discount on our own spells and abilities reads as a cost reducer") {
            listOf("Bladegraft Aspirant", "Silver-Fur Master", "Training Grounds").forEach { name ->
                withClue(name) { CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.COST_REDUCER }
            }
            val bladegraft = card("Bladegraft Aspirant").script.staticAbilities
            withClue("both of Bladegraft's statics — the spell discount and the ability discount — read on their own") {
                bladegraft.map { IntentTag.COST_REDUCER in CardIntentAnalyzer.staticTags(it) } shouldBe listOf(true, true)
            }
        }

        test("a discount is not a mana source, and a tax on opponents is not a discount") {
            withClue("Bladegraft Aspirant must not read as ramp") {
                CardIntentAnalyzer.analyze(card("Bladegraft Aspirant")).tags shouldNotContain IntentTag.RAMP
            }
            withClue("Kopala's opponent tax is protection, not a cost reducer") {
                CardIntentAnalyzer.analyze(card("Kopala, Warden of Waves")).tags shouldNotContain IntentTag.COST_REDUCER
            }
        }

        test("the draft heuristic prices Bladegraft's statics above its bare body") {
            val terms = LimitedCardRater.explain(card("Bladegraft Aspirant")).heuristic
            val staticTerms = terms.filter { it.part.startsWith("Static") }
            withClue("terms: $terms") {
                staticTerms.size shouldBe 2
                staticTerms.sumOf { it.value } shouldBeGreaterThan 0.0
            }
        }
    }
}
