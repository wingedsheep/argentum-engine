package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * A static that stops counters going on players or permanents — Solemnity's "Players can't get
 * counters. Counters can't be put on artifacts, creatures, enchantments, or lands", Blightbeetle's
 * opponents-only "+1/+1 counters" half. Unread, Solemnity was "abilities not recognised" and priced
 * as an unreadable permanent.
 */
class CounterLockReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("a player or battlefield-wide counter lock reads as a counter lock") {
            listOf("Solemnity", "Blightbeetle").forEach { name ->
                withClue(name) { CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.COUNTER_LOCK }
            }
        }

        test("an Aura's lock on the creature it enchants is not a board-wide counter lock") {
            CardIntentAnalyzer.analyze(card("Blossombind")).tags shouldNotContain IntentTag.COUNTER_LOCK
        }

        test("a symmetric lock is not read as aimed at the opponent") {
            CardIntentAnalyzer.analyze(card("Solemnity")).affectsOpponent shouldBe false
        }

        test("a counter-lock permanent is priced above a permanent nothing reads") {
            CardIntentAnalyzer.analyze(card("Solemnity")).staticPriorValue shouldBeGreaterThan
                CardIntent.UNKNOWN.staticPriorValue
        }
    }
}
