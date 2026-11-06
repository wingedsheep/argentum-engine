package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.definitions.aer.cards.ConsulateCrackdown
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * "Exile all artifacts your opponents control until this enchantment leaves the battlefield" —
 * `Effects.ExileGroupAndLink`, a battlefield gather moved to exile. The destroy-all and bounce-all
 * spellings already read as a sweep; unread, the exile-all left Consulate Crackdown a blank
 * enchantment the AI couldn't place.
 */
class ExileAllPipelineReadingAiTest : FunSpec({

    test("an exile-all pipeline reads as an exiling sweeper") {
        CardIntentAnalyzer.analyze(ConsulateCrackdown).tags shouldContainAll
            listOf(IntentTag.REMOVAL, IntentTag.SWEEPER, IntentTag.EXILE_REMOVAL)
    }

    test("Consulate Crackdown reads fully") {
        val reading = AiReadingAudit.read(ConsulateCrackdown)
        withClue("$reading") { reading.readFully shouldBe true }
    }
})
