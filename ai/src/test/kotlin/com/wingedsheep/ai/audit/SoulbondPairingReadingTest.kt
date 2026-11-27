package com.wingedsheep.ai.audit

import com.wingedsheep.ai.audit.AiReadingAudit.Check
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Soulbond's two pairing triggers (CR 702.95a) gather a partner — an unpaired creature of ours, or
 * the creature that just entered — and pair it with the source
 * ([com.wingedsheep.sdk.scripting.effects.PairWithSourceEffect]). Nothing moves and the executor
 * only ever links two of the controller's creatures, so like equip the pairing needs no intent of
 * its own: what it is for is read off the card's soulbond-pair statics.
 */
class SoulbondPairingReadingTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }

    test("both soulbond pairing triggers read as needing no intent") {
        val reading = AiReadingAudit.read(byName["Wingcrafter"] ?: error("Wingcrafter is not in the corpus"))
        withClue(reading.flags) { reading.readFully shouldBe true }
        val triggers = reading.abilities.filter { a -> a.findings.any { it.check == Check.INTENT } }
        triggers.size shouldBe 2
        triggers.forEach { a ->
            a.findings.single { it.check == Check.INTENT }.reason shouldContain "pairs two of its controller's creatures for soulbond"
        }
    }

    test("a paired reach grant reads as more blocks, so the soulbond card is recognised") {
        val trappers = byName["Geist Trappers"] ?: error("Geist Trappers is not in the corpus")
        CardIntentAnalyzer.analyze(trappers).tags shouldContain IntentTag.EXTRA_BLOCKING
        AiCardRating.of(trappers, listOf("AVR")).intent.unrecognised shouldBe false
    }
})
