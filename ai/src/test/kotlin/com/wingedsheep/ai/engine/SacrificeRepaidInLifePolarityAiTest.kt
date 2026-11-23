package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.gtc.cards.DevourFlesh
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Target player sacrifices a creature, then gains life equal to that creature's toughness"
 * (Devour Flesh): an edict on the opponent or a creature cashed in for life on ourselves, so the
 * player slot stays open for the simulation instead of reading as two disagreeing steps.
 */
class SacrificeRepaidInLifePolarityAiTest : FunSpec({

    test("a sacrifice repaid in life to the same player reads flexible") {
        TargetPolarityAnalyzer.forSpell(DevourFlesh) shouldBe listOf(TargetPolarity.FLEXIBLE)
    }

    test("Devour Flesh reads fully") {
        val reading = AiReadingAudit.read(DevourFlesh)
        withClue("$reading") { reading.readFully shouldBe true }
    }
})
