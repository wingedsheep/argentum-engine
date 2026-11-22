package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Populate — "choose a creature token you control, then create a token that's a copy of it"
 * ([com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfChosenPermanentEffect]) — copies a
 * permanent of our own picked on resolution: another body of ours, [IntentTag.TOKEN_MAKER], as the
 * targeted copy (Electroduplicate) is.
 *
 * Unread, Wake the Reflections had no intent at all, and Muster the Departed's populate trigger was
 * a gap beside its read tokens.
 */
class PopulateTokenCopyIntentAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("a populate spell reads as a token maker") {
            withClue("Wake the Reflections: populate") {
                CardIntentAnalyzer.analyze(card("Wake the Reflections")).tags shouldContain IntentTag.TOKEN_MAKER
                AiReadingAudit.read(card("Wake the Reflections")).readFully shouldBe true
            }
        }

        test("a populate trigger is read as the token it makes") {
            withClue("Muster the Departed: 'whenever another nontoken creature you control dies … populate'") {
                AiReadingAudit.read(card("Muster the Departed")).readFully shouldBe true
            }
        }
    }
}
