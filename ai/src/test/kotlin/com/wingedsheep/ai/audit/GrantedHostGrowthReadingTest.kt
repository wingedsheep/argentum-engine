package com.wingedsheep.ai.audit

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * "Enchanted creature has 'Whenever you gain life, put that many +1/+1 counters on this creature'"
 * (Sunbond): "this creature" in a trigger handed to the host is the host, so the Aura grows the
 * creature it enchants — a lasting pump, not an unrecognised static.
 */
class GrantedHostGrowthReadingTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }

    test("an Aura granting its host a +1/+1-counter trigger reads as a pump") {
        val sunbond = byName["Sunbond"] ?: error("Sunbond is not in the corpus")
        CardIntentAnalyzer.analyze(sunbond).tags shouldContain IntentTag.PUMP
    }
})
