package com.wingedsheep.ai.audit

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A scry whose count is only known at resolution — Brain in a Jar's "{3}, {T}, Remove X charge
 * counters: Scry X.", Cascade Seer's "scry X, where X is the number of artifacts you control" — has
 * no compact `ScryEffect` macro: it is the Gather → Select → Move pipeline whose tail emits the
 * ScriedEvent. That tail is the same card selection the macro reads as, so it must not leave the
 * ability flagged as one the AI doesn't know the purpose of.
 */
class DynamicScryReadingTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("a dynamic scry X reads fully, as card selection") {
        for (name in listOf("Brain in a Jar", "Cascade Seer", "Spotter Thopter")) {
            withClue(name) {
                val reading = AiReadingAudit.read(card(name))
                reading.readFully shouldBe true
                CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.CARD_SELECTION
            }
        }
    }
})
