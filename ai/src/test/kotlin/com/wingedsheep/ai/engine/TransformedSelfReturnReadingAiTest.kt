package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Return this card to the battlefield transformed" out of the graveyard
 * (`ReturnSelfFromZoneTransformedEffect`: Harvest Hand, the Ojer gods, Skin Invasion) is our own
 * card back onto the battlefield as its other face — [IntentTag.RECURSION], the single-card
 * graveyard-to-battlefield return it is. Unread, the trigger read as "no intent".
 *
 * Also pinned here: an Aura only our own creatures can wear whose abilities never name its host
 * (Journey to Eternity's dies trigger acts on the creature that died) is a gift on our side, the
 * mirror of an "enchant creature an opponent controls" Aura reading as a curse.
 */
class TransformedSelfReturnReadingAiTest : ScenarioTestBase() {

    init {
        test("returning itself transformed from the graveyard reads as recursion") {
            for (name in listOf("Harvest Hand", "Ojer Axonil, Deepest Might")) {
                val card = cardRegistry.getCard(name)!!
                withClue(name) {
                    CardIntentAnalyzer.analyze(card).tags shouldContain IntentTag.RECURSION
                    AiReadingAudit.read(card).readFully shouldBe true
                }
            }
        }

        test("an Aura only our creatures can wear, with nothing on its host to read, is ours to place") {
            val journey = cardRegistry.getCard("Journey to Eternity")!!
            TargetPolarityAnalyzer.forSpell(journey) shouldBe listOf(TargetPolarity.BENEFICIAL)
            AiReadingAudit.read(journey).readFully shouldBe true
        }
    }
}
