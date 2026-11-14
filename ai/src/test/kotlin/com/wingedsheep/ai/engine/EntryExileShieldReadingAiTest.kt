package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * A granted battlefield-entry exile — "until end of turn, if a creature would enter and it wasn't
 * cast, exile it instead" (Hallowed Moonlight, Mistcaller). Unread, Mistcaller's sacrifice was "no
 * intent" and both carried a timing gap; it now reads as reanimation hate whose turn-long shield
 * needs no hold, like a prevention shield.
 */
class EntryExileShieldReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("Mistcaller's sacrifice and Hallowed Moonlight read as reanimation hate") {
            CardIntentAnalyzer.analyze(card("Mistcaller")).tags shouldContain IntentTag.GRAVEYARD_HATE
            CardIntentAnalyzer.analyze(card("Hallowed Moonlight")).tags shouldContain IntentTag.GRAVEYARD_HATE
        }

        test("an exile-on-death rider over our own creatures is not hate") {
            withClue("Malicious Eclipse redirects deaths, not entries") {
                CardIntentAnalyzer.analyze(card("Malicious Eclipse")).tags shouldNotContain IntentTag.GRAVEYARD_HATE
            }
        }

        test("the audit reads every ability of both cards fully") {
            for (name in listOf("Mistcaller", "Hallowed Moonlight")) {
                val gaps = AiReadingAudit.read(card(name)).abilities.flatMap { it.findings }.filterNot { it.read }
                withClue("$name gaps: $gaps") { gaps.size shouldBe 0 }
            }
        }
    }
}
