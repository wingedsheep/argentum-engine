package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.frf.cards.Arcbond
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A delayed trigger that watches its target and fires on that object being dealt damage, firing
 * nothing that helps or harms the watched object itself — "whenever that creature is dealt damage
 * this turn, it deals that much damage to each other creature and each player" (Arcbond). The
 * creature only decides when the relay fires, so the one to name is whichever is about to be dealt
 * the most damage, on either side: [TargetPolarity.FLEXIBLE], compared by simulation. Unread, the
 * slot was UNKNOWN.
 */
class DamageWatchDelayedTriggerPolarityAiTest : FunSpec({

    test("Arcbond's watched creature reads as either side's") {
        TargetPolarityAnalyzer.forRequirements(Arcbond.script.spellEffect, Arcbond.script.targetRequirements) shouldBe
            listOf(TargetPolarity.FLEXIBLE)
    }

    test("the relay it fires still reads as the sweeper it is") {
        CardIntentAnalyzer.analyzeEffect(Arcbond.script.spellEffect!!, Arcbond.script.targetRequirements).tags shouldContain
            IntentTag.REMOVAL
    }
})
