package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "Target creature loses all abilities" aimed at anyone's creature (Turn to Frog) shuts it off:
 * [IntentTag.NEUTRALIZE]. Aimed at a slot only our own creatures fill ("target creature you
 * control … becomes a 4/4 … loses all abilities, and gains flying", Dance of the Skywise) it is the
 * price of a bigger flying body, a combat trick, never an answer.
 */
class OwnSideLoseAllAbilitiesReadingAiTest : ScenarioTestBase() {

    private fun spellOf(name: String) =
        (cardRegistry.getCard(name) ?: error("'$name' is not in the corpus")).script.spellEffect!!

    private fun requirementsOf(name: String) =
        (cardRegistry.getCard(name) ?: error("'$name' is not in the corpus")).script.targetRequirements

    init {
        test("losing all abilities neutralizes anyone's creature, but not our own") {
            withClue("Turn to Frog shuts any creature off") {
                CardIntentAnalyzer.analyzeEffect(spellOf("Turn to Frog"), requirementsOf("Turn to Frog"))
                    .tags shouldContain IntentTag.NEUTRALIZE
            }
            withClue("Dance of the Skywise only reaches our own creature: a trick, not an answer") {
                val tags = CardIntentAnalyzer.analyzeEffect(
                    spellOf("Dance of the Skywise"), requirementsOf("Dance of the Skywise")
                ).tags
                tags shouldNotContain IntentTag.NEUTRALIZE
                tags shouldContain IntentTag.EVASION_GRANT
                AiReadingAudit.audit(cardRegistry.getCard("Dance of the Skywise")!!).shouldBeEmpty()
            }
        }
    }
}
