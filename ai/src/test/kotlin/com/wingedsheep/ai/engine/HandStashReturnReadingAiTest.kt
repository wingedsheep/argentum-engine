package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "{T}: Draw a card, then exile a card from your hand face down. … {U}{B}, {T}: Return a card exiled
 * with this creature to its owner's hand" (Bane Alley Broker). The pile is filled by a pipeline exile
 * of cards gathered from our own hand, so it only ever holds our own cards and handing one back to
 * "its owner's hand" returns it to us: recursion the AI can cash in.
 */
class HandStashReturnReadingAiTest : FunSpec({

    val broker by lazy {
        MtgSetCatalog.all.flatMap { it.cards }.first { it.name == "Bane Alley Broker" }
    }

    test("returning a card we stashed from our own hand is recursion") {
        val intent = CardIntentAnalyzer.analyze(broker)
        intent.tags shouldContain IntentTag.RECURSION
        withClue("the return is an activation, so the AI can use it") {
            intent.cashableTags shouldContain IntentTag.RECURSION
        }
        withClue("every ability of the Broker now reads fully") {
            AiReadingAudit.read(broker).abilities.filter { it.checked }.all { it.readFully } shouldBe true
        }
    }
})
