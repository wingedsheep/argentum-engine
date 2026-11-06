package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Can't be blocked by artifact creatures" — Audacious Infiltrator and Fen Hauler's
 * [com.wingedsheep.sdk.scripting.CantBeBlockedBy] static. Unread, both cards were "abilities not
 * recognised" and the draft heuristic priced them as vanilla bodies.
 */
class CantBeBlockedByReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("partial unblockability reads as evasion") {
            listOf("Audacious Infiltrator", "Fen Hauler").forEach { name ->
                withClue(name) { CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.EVASION_GRANT }
            }
        }

        test("the static itself carries the evasion tag") {
            val static = card("Audacious Infiltrator").script.staticAbilities.single()
            (IntentTag.EVASION_GRANT in CardIntentAnalyzer.staticTags(static)) shouldBe true
        }
    }
}
