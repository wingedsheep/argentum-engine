package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A prevention shield whose reaction names the spell's own target
 * ([com.wingedsheep.sdk.scripting.effects.PreventDamageEffect.onPrevented]): Channel Harm's "you
 * may have Channel Harm deal that much damage to target creature". The target slot lives in the
 * reaction, so it is read as the reaction's damage reads anywhere else — aimed at their side.
 */
class PreventionReactionTargetPolarityAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("Channel Harm's target creature is the reaction's damage target, so it reads harmful") {
            intents.spellTargetPolarities("Channel Harm") shouldBe listOf(TargetPolarity.HARMFUL)
        }

        test("it still reads as protection") {
            val tags = intents.forName("Channel Harm")!!.tags
            withClue("$tags") { (IntentTag.PROTECTION in tags) shouldBe true }
        }
    }
}
