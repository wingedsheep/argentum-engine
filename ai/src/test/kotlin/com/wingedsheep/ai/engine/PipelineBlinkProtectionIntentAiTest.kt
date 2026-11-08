package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.definitions.avr.cards.GhostlyFlicker
import com.wingedsheep.mtg.sets.definitions.emn.cards.Displace
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Exile up to two target creatures you control, then return those cards to the battlefield under
 * their owner's control" as a gather → exile (linked) → gather the linked pile → battlefield
 * pipeline is the blink Cloudshift spells as two single moves: a save from removal and an ETB
 * re-buy, so it reads as [IntentTag.PROTECTION] as the single-move blink does.
 *
 * Unread, Displace and Ghostly Flicker carried no intent tag at all ("abilities not recognised").
 */
class PipelineBlinkProtectionIntentAiTest : FunSpec({

    test("a pipeline blink of chosen targets reads as protection") {
        for (card in listOf(Displace, GhostlyFlicker)) {
            withClue(card.name) { CardIntentAnalyzer.analyze(card).tags shouldContain IntentTag.PROTECTION }
        }
    }

    test("Displace reads fully") {
        val reading = AiReadingAudit.read(Displace)
        withClue("$reading") { reading.readFully shouldBe true }
    }
})
