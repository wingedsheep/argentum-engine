package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * "You have hexproof" (Aegis of the Gods, Leyline of Sanctity) and "you have shroud" (True
 * Believer) shield their controller from every burn spell, discard and drain aimed at them. Unread,
 * the static was "abilities not recognised" and the draft heuristic priced Aegis of the Gods as a
 * vanilla 2/1 for two.
 */
class PlayerHexproofReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("a player-level hexproof or shroud static reads as protection") {
            listOf("Aegis of the Gods", "Leyline of Sanctity", "True Believer").forEach { name ->
                withClue(name) {
                    CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.PROTECTION
                    card(name).script.staticAbilities.map { IntentTag.PROTECTION in CardIntentAnalyzer.staticTags(it) } shouldBe
                        listOf(true)
                }
            }
        }

        test("the draft heuristic prices Aegis of the Gods' static above its bare body") {
            val terms = LimitedCardRater.explain(card("Aegis of the Gods")).heuristic
            withClue("terms: $terms") {
                terms.single { it.part.startsWith("Static") }.value shouldBeGreaterThan 0.0
            }
        }
    }
}
