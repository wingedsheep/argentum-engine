package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * Ingest (CR 702.115) and "can't be blocked" as the AI reads them. Ingest's trigger is derived
 * from the keyword, so no ability reader sees it: the board evaluator and the draft heuristic read
 * the keyword itself. Benthic Infiltrator's "this creature can't be blocked" is evasion no blocker
 * answers, so its body is priced at least as a flier's.
 */
class IngestReadingAiTest : ScenarioTestBase() {

    init {
        fun body(power: Int, toughness: Int, vararg keywords: String): Double =
            BoardPresence.creatureBodyValue(power, toughness, keywords.toSet())

        test("an ingest creature is worth more on the board than the same body without it") {
            body(1, 2, Keyword.FLYING.name, Keyword.INGEST.name) shouldBeGreaterThan
                body(1, 2, Keyword.FLYING.name)
        }

        test("an unblockable body is worth at least a flier's, and more than a vanilla one") {
            val unblockable = body(1, 4, AbilityFlag.CANT_BE_BLOCKED.name)
            unblockable shouldBeGreaterThan body(1, 4)
            unblockable shouldBeGreaterThanOrEqual body(1, 4, Keyword.FLYING.name)
        }

        test("the draft heuristic carries an Ingest term") {
            val terms = LimitedCardRater.explain(cardRegistry.getCard("Mist Intruder")!!).heuristic
            terms.any { it.part == Keyword.INGEST.displayName && it.value > 0.0 } shouldBe true
        }
    }
}
