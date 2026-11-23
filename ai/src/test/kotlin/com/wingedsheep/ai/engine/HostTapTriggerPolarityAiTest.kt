package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.lea.cards.PsychicVenom
import com.wingedsheep.mtg.sets.definitions.roe.cards.ContaminatedGround
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Whenever enchanted land becomes tapped, its controller loses 2 life" (Contaminated Ground) and
 * "… deals 2 damage to that land's controller" (Psychic Venom): a tax on the host's controller each
 * time they use it, so the Aura is a curse to cast on an opponent's land.
 */
class HostTapTriggerPolarityAiTest : FunSpec({

    test("an Aura draining its host's controller on tap reads harmful") {
        TargetPolarityAnalyzer.forSpell(ContaminatedGround) shouldBe listOf(TargetPolarity.HARMFUL)
        TargetPolarityAnalyzer.forSpell(PsychicVenom) shouldBe listOf(TargetPolarity.HARMFUL)
    }

    test("its controller's life loss is a clock on that player") {
        CardIntentAnalyzer.analyze(ContaminatedGround).tags shouldContain IntentTag.DAMAGE_TO_PLAYERS
    }

    test("Contaminated Ground reads fully") {
        val reading = AiReadingAudit.read(ContaminatedGround)
        withClue("$reading") { reading.readFully shouldBe true }
    }
})
