package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.gtc.cards.PredatorsRapport
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Choose target creature you control. You gain life equal to that creature's power plus its
 * toughness" (Predator's Rapport): the gain is the controller's, and the slot is only measured by
 * it. Named from our own side it is our biggest creature to pick, so it reads beneficial.
 */
class MeasuredTargetGainPolarityAiTest : FunSpec({

    test("a creature you control measured by your own life gain reads beneficial") {
        TargetPolarityAnalyzer.forSpell(PredatorsRapport) shouldBe listOf(TargetPolarity.BENEFICIAL)
    }

    test("Predator's Rapport reads fully") {
        val reading = AiReadingAudit.read(PredatorsRapport)
        withClue("$reading") { reading.readFully shouldBe true }
    }
})
