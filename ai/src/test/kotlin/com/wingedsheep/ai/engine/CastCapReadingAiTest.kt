package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * A static that caps how many spells opponents cast each turn — Rule of Law's symmetric "each
 * player can't cast more than one spell each turn", Curse of Exhaustion's "enchanted player
 * can't …". Unread, Curse of Exhaustion's enchant slot was UNKNOWN (nothing told the AI to put it
 * on an opponent) and its static was "abilities not recognised".
 */
class CastCapReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("Curse of Exhaustion's enchant-player slot is a curse, aimed across the table") {
            TargetPolarityAnalyzer.forSpell(card("Curse of Exhaustion")) shouldBe listOf(TargetPolarity.HARMFUL)
        }

        test("a symmetric, filtered or enchanted-player cast cap reads as a cast cap") {
            listOf("Curse of Exhaustion", "Rule of Law", "Deafening Silence").forEach { name ->
                withClue(name) { CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.CAST_CAP }
            }
        }

        test("a cap over its controller alone is a price, not a lock") {
            CardIntentAnalyzer.staticTags(RestrictSpellsCastPerTurn(affected = Player.You)) shouldNotContain
                IntentTag.CAST_CAP
        }

        test("the draft heuristic gives the cap a static term") {
            val terms = LimitedCardRater.explain(card("Rule of Law")).heuristic
            withClue("terms: $terms") {
                terms.filter { it.part.startsWith("Static") }.sumOf { it.value } shouldBeGreaterThan 0.0
            }
        }
    }
}
