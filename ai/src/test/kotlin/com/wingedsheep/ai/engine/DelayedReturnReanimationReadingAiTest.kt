package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.ths.cards.RescueFromTheUnderworld
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A graveyard target gathered now and returned to our battlefield by a delayed trigger that carries
 * it — "choose target creature card in your graveyard. Return that card … to the battlefield under
 * your control at the beginning of your next upkeep" (Rescue from the Underworld) — is the same
 * reanimation as an immediate return. Unread, the slot was a coin toss and the spell had no purpose
 * the AI could name.
 */
class DelayedReturnReanimationReadingAiTest : FunSpec({

    val script = RescueFromTheUnderworld.script
    val spell = script.spellEffect!!

    test("the delayed return reads the graveyard slot as a gift and the spell as recursion") {
        TargetPolarityAnalyzer.forRequirements(spell, script.targetRequirements) shouldBe
            listOf(TargetPolarity.BENEFICIAL)
        CardIntentAnalyzer.reanimatesChosenGraveyardTargets(spell, script.targetRequirements) shouldBe true
        CardIntentAnalyzer.analyzeEffect(spell, script.targetRequirements).tags shouldContain IntentTag.RECURSION
    }
})
