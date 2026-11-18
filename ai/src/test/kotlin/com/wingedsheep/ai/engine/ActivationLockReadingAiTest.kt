package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan

/**
 * Statics that stop or tax activated abilities. Unread, each was "abilities not recognised":
 * - a standing lock — Phyrexian Revoker's and Pithing Needle's named lock, Cursed Totem's
 *   "activated abilities of creatures can't be activated" — reads [IntentTag.ACTIVATION_LOCK];
 * - an Aura's lock or tax on what it enchants — Arrest's "its activated abilities can't be
 *   activated", Oppressive Rays' "cost {3} more to activate" and its "can't attack or block unless
 *   its controller pays {3}" — is the Aura neutralizing its host, [IntentTag.NEUTRALIZE].
 */
class ActivationLockReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("a standing activation lock reads as an activation lock") {
            listOf("Phyrexian Revoker", "Pithing Needle", "Cursed Totem").forEach { name ->
                withClue(name) {
                    val tags = CardIntentAnalyzer.analyze(card(name)).tags
                    tags shouldContain IntentTag.ACTIVATION_LOCK
                    tags shouldNotContain IntentTag.NEUTRALIZE
                }
            }
        }

        test("an Aura that locks or taxes what it enchants reads as neutralizing it") {
            listOf("Oppressive Rays", "Brainwash").forEach { name ->
                withClue(name) {
                    val tags = CardIntentAnalyzer.analyze(card(name)).tags
                    tags shouldContain IntentTag.NEUTRALIZE
                    tags shouldNotContain IntentTag.ACTIVATION_LOCK
                }
            }
        }

        test("a lock permanent is priced above a permanent nothing reads") {
            CardIntentAnalyzer.analyze(card("Pithing Needle")).staticPriorValue shouldBeGreaterThan
                CardIntent.UNKNOWN.staticPriorValue
        }

        test("the draft heuristic prices Phyrexian Revoker's lock above its bare body") {
            val terms = LimitedCardRater.explain(card("Phyrexian Revoker")).heuristic
            val staticTerms = terms.filter { it.part.startsWith("Static") }
            withClue("terms: $terms") {
                staticTerms.sumOf { it.value } shouldBeGreaterThan 0.0
            }
        }
    }
}
