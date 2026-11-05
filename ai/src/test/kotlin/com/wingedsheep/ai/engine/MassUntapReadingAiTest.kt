package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "Untap all creatures you control" — a group `ForEach` untapping each of our creatures — stands
 * our attackers back up to block or attack again: the vigilance a granted keyword gives, so it
 * reads as [IntentTag.COMBAT_ENHANCEMENT]. A symmetric "untap all creatures" helps the opponent
 * as much as us and stays unread.
 */
class MassUntapReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("untapping our own creatures is a combat enhancement; untapping everyone's is not") {
            withClue("Jace, Ingenious Mind-Mage: '+1: Untap all creatures you control'") {
                val untap = card("Jace, Ingenious Mind-Mage").script.activatedAbilities[1]
                CardIntentAnalyzer.analyzeEffect(untap.effect, untap.targetRequirements).tags shouldContain
                    IntentTag.COMBAT_ENHANCEMENT
            }
            withClue("Intruder Alarm: 'untap all creatures' untaps the opponent's team too") {
                CardIntentAnalyzer.analyze(card("Intruder Alarm")).tags shouldNotContain IntentTag.COMBAT_ENHANCEMENT
            }
        }
    }
}
