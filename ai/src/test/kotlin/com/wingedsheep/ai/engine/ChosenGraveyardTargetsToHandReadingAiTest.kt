package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A pipeline that gathers its own graveyard targets and returns the lot to our hand — "Return X
 * target cards from your graveyard to your hand" (Wildest Dreams) — is the single-card return to
 * hand it adds up to: each slot is [TargetPolarity.BENEFICIAL] (the best card is the one to name)
 * and the spell reads as [IntentTag.RECURSION]. Unread, the slot fell back to the removal ranking,
 * which orders our own cards worst first, and the spell read as doing nothing.
 *
 * Also pinned here: "distribute four +1/+1 counters among target creatures you control"
 * (Verdurous Gearhulk) feeds the +1/+1 counters axis, as a single AddCounters does.
 */
class ChosenGraveyardTargetsToHandReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("returning X graveyard targets to hand reads as recursion with a gift slot") {
            withClue("Wildest Dreams: our best graveyard cards are the ones to name") {
                intents.spellTargetPolarities("Wildest Dreams") shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
            withClue("Wildest Dreams: the cards come back to hand") {
                intents.forName("Wildest Dreams")!!.tags shouldContain IntentTag.RECURSION
            }
            AiReadingAudit.read(cardRegistry.getCard("Wildest Dreams")!!).readFully shouldBe true
        }

        test("distributing +1/+1 counters feeds the counters axis") {
            SynergyAxes.of(cardRegistry.getCard("Verdurous Gearhulk")!!).feeds shouldContain SynergyAxis.PLUS_COUNTERS
        }
    }
}
