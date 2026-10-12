package com.wingedsheep.engine.event

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * Which text the trigger targeting banner shows: the authored ability text (trigger clause cut)
 * over a step-composed pipeline's generated text, the runtime text where it substitutes a live
 * value into a single effect's sentence.
 */
class TriggerTargetHintTest : DescribeSpec({

    describe("choose") {

        val pipeline = Effects.Pipeline {
            val exiled = gather(com.wingedsheep.sdk.scripting.effects.CardSource.ChosenTargets)
            exile(exiled)
        }

        it("an authored pipeline ability shows its authored text, without the trigger clause") {
            TriggerTargetHint.choose(
                authored = "When this creature enters, exile up to one target card from a graveyard.",
                effect = pipeline,
                runtime = pipeline.description
            ) shouldBe "Exile up to one target card from a graveyard."
        }

        it("a step-composed sequence keeps the authored text even when runtime values differ") {
            TriggerTargetHint.choose(
                authored = "Whenever you attack, target player mills X cards.",
                effect = pipeline,
                runtime = pipeline.description + " (3)"
            ) shouldBe "Target player mills X cards."
        }

        it("a single effect with a live value keeps the resolved runtime text") {
            val pump = Effects.ModifyStats(
                com.wingedsheep.sdk.scripting.values.DynamicAmount.XValue,
                com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(0),
                EffectTarget.ContextTarget(0)
            )
            TriggerTargetHint.choose(
                authored = "When this creature enters, target creature gets +X/+0 until end of turn, where X is …",
                effect = pump,
                runtime = "Target creature gets +3/+0 until end of turn"
            ) shouldBe "Target creature gets +3/+0 until end of turn"
        }

        it("no authored text falls back to the runtime text") {
            TriggerTargetHint.choose(authored = null, effect = pipeline, runtime = "generated") shouldBe "generated"
        }
    }

    describe("withoutTriggerClause") {

        it("cuts a leading When/Whenever/At clause and capitalises the rest") {
            TriggerTargetHint.withoutTriggerClause("At the beginning of your upkeep, draw a card.") shouldBe
                "Draw a card."
            TriggerTargetHint.withoutTriggerClause("Whenever this creature attacks, tap target creature.") shouldBe
                "Tap target creature."
        }

        it("leaves text without a trigger clause alone") {
            TriggerTargetHint.withoutTriggerClause("Exile target card.") shouldBe "Exile target card."
        }
    }
})
