package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.shouldBe

/**
 * A token count sized by the targeted players — "create X 1/1 Soldiers, where X is the number of
 * creatures those opponents control" (Call the Coppercoats), "investigate X times, where X is the
 * total number of creatures those players control" (Officious Interrogation). The tokens are the
 * controller's; the targets are only counted. Unread, the player slot had no leaf aimed at it and
 * read [TargetPolarity.UNKNOWN] ("can't tell if this helps or hurts"); it now reads
 * [TargetPolarity.FLEXIBLE], like a draw sized by a target opponent (Knollspine Dragon).
 */
class TokenCountSizedByTargetsReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a token count sized by target opponents keeps the slot open instead of unread") {
            intents.spellTargetPolarities("Call the Coppercoats") shouldBe listOf(TargetPolarity.FLEXIBLE)
        }

        test("an investigate count sized by target players keeps the slot open instead of unread") {
            intents.spellTargetPolarities("Officious Interrogation") shouldBe listOf(TargetPolarity.FLEXIBLE)
        }
    }
}
