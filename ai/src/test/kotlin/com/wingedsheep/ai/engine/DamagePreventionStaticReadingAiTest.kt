package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.Speed
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * A standing damage-prevention shield over its controller's side — Protection of the Hekma's "if a
 * source an opponent controls would deal damage to you, prevent 1 of that damage", Gideon's
 * Intervention's "prevent all damage that would be dealt to you and permanents you control by
 * sources with the chosen name" — and a standing opponents-only cast ban (Gideon's Intervention's
 * "your opponents can't cast spells with the chosen name"). Unread, both enchantments were
 * "abilities not recognised" and priced as unreadable permanents.
 */
class DamagePreventionStaticReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("a prevention shield over you and your permanents reads as protection, standing in play") {
            listOf("Protection of the Hekma", "Gideon's Intervention").forEach { name ->
                val intent = CardIntentAnalyzer.analyze(card(name))
                intent.tags shouldContain IntentTag.PROTECTION
                intent.speed shouldBe Speed.STATIC
            }
        }

        test("prevention that shields the opponent is not read as protection for us") {
            CardIntentAnalyzer.analyze(card("Hostility")).tags shouldNotContain IntentTag.PROTECTION
        }

        test("an opponents-only standing cast ban reads as a cast ban aimed at the opponent") {
            val intent = CardIntentAnalyzer.analyze(card("Gideon's Intervention"))
            intent.tags shouldContain IntentTag.CAST_BAN
            intent.affectsOpponent shouldBe true
        }

        test("a cast ban that binds every player is not read as a cast ban") {
            CardIntentAnalyzer.analyze(card("Gaddock Teeg")).tags shouldNotContain IntentTag.CAST_BAN
        }

        test("both shields are priced above a permanent nothing reads") {
            listOf("Protection of the Hekma", "Gideon's Intervention").forEach { name ->
                CardIntentAnalyzer.analyze(card(name)).staticPriorValue shouldBeGreaterThan
                    CardIntent.UNKNOWN.staticPriorValue
            }
        }
    }
}
