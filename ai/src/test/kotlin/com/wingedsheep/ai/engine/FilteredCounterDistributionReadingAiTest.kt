package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "When this creature dies, you may distribute a number of +1/+1 counters equal to the number of
 * +1/+1 counters on it among any number of creatures you control" (Vastwood Hydra): a split chosen
 * at resolution among the creatures a filter matches, with no targets. +1/+1 counters handed to
 * other creatures are a lasting pump, read the way a targeted distribution is.
 */
class FilteredCounterDistributionReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a filtered +1/+1 counter distribution reads as a pump") {
            val tags = intents.forName("Vastwood Hydra")!!.tags
            withClue("Vastwood Hydra's tags: $tags") { (IntentTag.PUMP in tags) shouldBe true }
        }
    }
}
