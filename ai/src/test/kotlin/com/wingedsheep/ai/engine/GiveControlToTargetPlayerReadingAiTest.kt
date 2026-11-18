package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "Target opponent gains control of this Equipment" (Avarice Amulet's dies trigger, Jinxed Idol's
 * activation) and "target opponent gains control of target permanent you control" (Harmless
 * Offering) — [com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect]. What the
 * receiver gains is worth what the permanent is (a gift, or Jinxed Idol's curse), so the player slot
 * reads [TargetPolarity.FLEXIBLE]; the permanent handed over is the price, flexible as an exchange's
 * own side is. A permanent handing itself away is its drawback, which needs no intent.
 */
class GiveControlToTargetPlayerReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a hand-off of control reads its permanent and player slots as flexible") {
            val amulet = cardRegistry.requireCard("Avarice Amulet").script.triggeredAbilities.single()
            intents.effectTargetPolarities(amulet.effect, amulet.allTargetRequirements) shouldBe
                listOf(TargetPolarity.FLEXIBLE)
            val offering = cardRegistry.requireCard("Harmless Offering").script.spellEffect!!
            val offeringSlots = cardRegistry.requireCard("Harmless Offering").script.targetRequirements
            intents.effectTargetPolarities(offering, offeringSlots) shouldBe
                listOf(TargetPolarity.FLEXIBLE, TargetPolarity.FLEXIBLE)
        }

        test("a permanent giving itself to an opponent is read fully, as its drawback") {
            for (name in listOf("Avarice Amulet", "Jinxed Idol")) {
                withClue(name) {
                    AiReadingAudit.read(cardRegistry.requireCard(name)).readFully shouldBe true
                }
            }
        }
    }
}
