package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A damage multiplier aimed only at an Aura's host — Curse of Bloodletting's "if a source would
 * deal damage to enchanted player, it deals double that damage to that player instead". Unread,
 * its enchant slot was UNKNOWN (nothing told the AI to curse an opponent rather than itself) and
 * the card read "abilities not recognised".
 */
class HostDamageCurseReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("Curse of Bloodletting's enchant-player slot is a curse, aimed across the table") {
            TargetPolarityAnalyzer.forSpell(card("Curse of Bloodletting")) shouldBe listOf(TargetPolarity.HARMFUL)
        }

        test("Curse of Bloodletting reads as a damage amplifier") {
            CardIntentAnalyzer.analyze(card("Curse of Bloodletting")).tags shouldContain IntentTag.DAMAGE_AMPLIFIER
        }
    }
}
