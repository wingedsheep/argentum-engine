package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * A static that stops permanents entering (or dying) from triggering abilities — Tocatli Honor
 * Guard's and Torpor Orb's "creatures entering the battlefield don't cause abilities to trigger",
 * Hushbringer's enters-or-dies lock, Elesh Norn, Mother of Machines' opponents-only half. Unread,
 * each was "abilities not recognised": Torpor Orb was priced as an unreadable permanent and the
 * draft heuristic priced Tocatli Honor Guard as a vanilla 1/3.
 */
class TriggerLockReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("a zone-change trigger lock reads as a trigger lock") {
            listOf(
                "Tocatli Honor Guard", "Torpor Orb", "Hushbringer",
                "Elesh Norn, Mother of Machines", "Doorkeeper Thrull",
            ).forEach { name ->
                withClue(name) { CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.TRIGGER_LOCK }
            }
        }

        test("a symmetric lock is not read as aimed at the opponent") {
            CardIntentAnalyzer.analyze(card("Torpor Orb")).affectsOpponent shouldBe false
        }

        test("a lock permanent is priced above a permanent nothing reads") {
            CardIntentAnalyzer.analyze(card("Torpor Orb")).staticPriorValue shouldBeGreaterThan
                CardIntent.UNKNOWN.staticPriorValue
        }

        test("the draft heuristic prices Tocatli Honor Guard's lock above its bare body") {
            val terms = LimitedCardRater.explain(card("Tocatli Honor Guard")).heuristic
            val staticTerms = terms.filter { it.part.startsWith("Static") }
            withClue("terms: $terms") {
                staticTerms.size shouldBe 1
                staticTerms.sumOf { it.value } shouldBeGreaterThan 0.0
            }
        }
    }
}
