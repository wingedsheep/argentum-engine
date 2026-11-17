package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.frf.cards.AleshaWhoSmilesAtDeath
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.mtg.sets.definitions.m20.cards.ThunderkinAwakener
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A gather → move pipeline that returns its chosen graveyard targets to our battlefield reads as
 * the reanimation it is even behind a gate we decide: "you may pay {W/B}{W/B}. If you do, return
 * target creature card with power 2 or less from your graveyard to the battlefield tapped and
 * attacking" (Alesha, Who Smiles at Death) is Thunderkin Awakener's return with a price on it.
 * Unread, the slot was a coin toss and the trigger had no purpose the AI could name.
 */
class GatedReanimationPipelineReadingAiTest : FunSpec({

    val alesha = AleshaWhoSmilesAtDeath.script.triggeredAbilities.single()
    val awakener = ThunderkinAwakener.script.triggeredAbilities.single()
    fun targetsOf(trigger: TriggeredAbility) = listOfNotNull(trigger.targetRequirement) + trigger.additionalTargetRequirements

    test("the ungated return already reads as a gift and a recursion") {
        TargetPolarityAnalyzer.forRequirements(awakener.effect, targetsOf(awakener)) shouldBe
            listOf(TargetPolarity.BENEFICIAL)
        CardIntentAnalyzer.analyzeEffect(awakener.effect, targetsOf(awakener)).tags shouldContain IntentTag.RECURSION
    }

    test("a may-pay gate in front of the return keeps that reading") {
        TargetPolarityAnalyzer.forRequirements(alesha.effect, targetsOf(alesha)) shouldBe
            listOf(TargetPolarity.BENEFICIAL)
        CardIntentAnalyzer.analyzeEffect(alesha.effect, targetsOf(alesha)).tags shouldContain IntentTag.RECURSION
        CardIntentAnalyzer.reanimatesChosenGraveyardTargets(alesha.effect, targetsOf(alesha)) shouldBe true
    }
})
