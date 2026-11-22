package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "You may cast creature spells from the top of your library" (Garruk's Horde): every card cast off
 * the top is one its controller never had to draw, so the standing permission reads as card draw
 * rather than as an ability the AI can't place.
 */
class CastFromLibraryTopReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("casting creatures off the library top reads as card draw") {
            val tags = intents.forName("Garruk's Horde")!!.tags
            withClue("Garruk's Horde's tags: $tags") { (IntentTag.DRAW in tags) shouldBe true }
        }
    }
}
