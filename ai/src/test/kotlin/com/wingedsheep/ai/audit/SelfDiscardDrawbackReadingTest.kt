package com.wingedsheep.ai.audit

import com.wingedsheep.ai.audit.AiReadingAudit.Check
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * A discard out of our own hand — Pale Rider of Trostad's "when this creature enters, discard a
 * card" — is the card's drawback or graveyard fuel, as a self-mill is: it needs no intent, and it
 * must never read as the hand disruption ([IntentTag.DISCARD]) that Pick the Brain's "target
 * opponent … exile a card from their hand" is.
 */
class SelfDiscardDrawbackReadingTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("discarding from our own hand is a drawback that needs no intent, not disruption") {
        val rider = card("Pale Rider of Trostad")
        val reading = AiReadingAudit.read(rider)
        reading.readFully shouldBe true
        val intent = reading.abilities.single().findings.single { it.check == Check.INTENT }
        intent.read shouldBe true
        intent.reason shouldContain "discards from its controller's own hand"
        CardIntentAnalyzer.analyze(rider).tags shouldNotContain IntentTag.DISCARD
        withClue("an opponent's hand stays disruption") {
            CardIntentAnalyzer.analyze(card("Pick the Brain")).tags shouldContain IntentTag.DISCARD
            CardIntentAnalyzer.analyze(card("Mind Rot")).tags shouldContain IntentTag.DISCARD
        }
    }
})
