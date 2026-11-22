package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Whenever Mirko Vosk deals combat damage to a player, that player reveals cards from the top of
 * their library until they reveal four land cards, then puts those cards into their graveyard."
 * The player the combat-damage trigger names is the one hit — an opponent — so the reveal-until
 * bin is a mill ([IntentTag.MILL]), as a target player's is (Balustrade Spy).
 */
class TriggeringPlayerRevealUntilMillIntentAiTest : ScenarioTestBase() {
    init {
        test("Mirko Vosk's combat-damage trigger reads as a mill of the damaged player") {
            val mirko = cardRegistry.getCard("Mirko Vosk, Mind Drinker")!!
            val trigger = mirko.script.triggeredAbilities.single()
            CardIntentAnalyzer.analyzeEffect(trigger.effect).tags shouldContain IntentTag.MILL
            CardIntentAnalyzer.analyze(mirko).tags shouldContain IntentTag.MILL
            val reading = AiReadingAudit.read(mirko)
            withClue("$reading") { reading.readFully shouldBe true }
        }
    }
}
