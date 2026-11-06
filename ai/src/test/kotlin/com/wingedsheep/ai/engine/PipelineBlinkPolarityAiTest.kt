package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.aer.cards.IllusionistsStratagem
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Exile up to two target creatures you control, then return those cards to the battlefield" as a
 * gather → exile (linked) → gather the linked pile → battlefield pipeline: the blink the single-move
 * spelling already reads. On our own creatures it is a save and an ETB re-buy, so the slot is a gift.
 */
class PipelineBlinkPolarityAiTest : FunSpec({

    test("a pipeline blink of our own creatures is aimed at our side") {
        TargetPolarityAnalyzer.forSpell(IllusionistsStratagem) shouldBe listOf(TargetPolarity.BENEFICIAL)
    }

    test("Illusionist's Stratagem reads fully") {
        val reading = AiReadingAudit.read(IllusionistsStratagem)
        withClue("$reading") { reading.readFully shouldBe true }
    }
})
