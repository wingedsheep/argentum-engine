package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "Target creature gets +X/+X until end of turn, where X is the amount of life you gained"
 * (Hurska Sweet-Tooth): X is a trigger-context amount — the life gained, the damage dealt — which is
 * a magnitude and never negative. Read that way, the reflexive pump is a pump of a friendly target
 * instead of an effect the AI can't place.
 */
class TriggerAmountPumpReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a pump by the life just gained helps its target and reads as a pump") {
            val hurska = cardRegistry.requireCard("Hurska Sweet-Tooth")
            val reflexive = hurska.script.triggeredAbilities
                .map { it.effect }.filterIsInstance<ReflexiveTriggerEffect>().single()

            TargetPolarityAnalyzer.forRequirements(reflexive.reflexiveEffect, reflexive.reflexiveTargetRequirements) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
            val tags = intents.forName("Hurska Sweet-Tooth")!!.tags
            withClue("Hurska's tags: $tags") { (IntentTag.PUMP in tags) shouldBe true }
        }
    }
}
