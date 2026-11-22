package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "Each creature your opponents control … has 'When this creature becomes the target of a spell or
 * ability, sacrifice it'" (Dismiss into Dream): a self-sacrifice handed only to the opposing board
 * turns every targeted spell or ability into removal, so the enchantment reads as lasting
 * [IntentTag.REMOVAL] instead of an unreadable permanent.
 */
class OpponentGroupSelfSacrificeGrantReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("granting opponents' creatures a sacrifice-on-target trigger reads as removal") {
            val tags = intents.forName("Dismiss into Dream")!!.tags
            withClue("Dismiss into Dream's tags: $tags") { (IntentTag.REMOVAL in tags) shouldBe true }
        }
    }
}
