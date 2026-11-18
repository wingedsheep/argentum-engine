package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * An Aura whose static grants the enchanted creature protection — from a color (the Ward cycle,
 * Spectra Ward), from the chosen color (Flickering Ward) or from the colors of permanents you
 * control (Pledge of Loyalty) — is a shield for its host: the enchant slot is aimed at our own
 * creature, and the card's intent is [IntentTag.PROTECTION].
 *
 * Unread, the chosen-color and controlled-colors grants left the slot's polarity unknown, so the
 * AI could not tell which creature to put the Aura on; and no static protection grant tagged an
 * intent, so White Ward read as "abilities not recognised".
 */
class ProtectionAuraReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("protection-granting Auras are aimed at our own creature and read as protection") {
            for (name in listOf("White Ward", "Spectra Ward", "Flickering Ward", "Pledge of Loyalty")) {
                withClue("$name: the enchant slot helps its host") {
                    TargetPolarityAnalyzer.forSpell(card(name)) shouldBe listOf(TargetPolarity.BENEFICIAL)
                }
                withClue("$name: a protection grant is a shield") {
                    CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.PROTECTION
                }
            }
        }
    }
}
