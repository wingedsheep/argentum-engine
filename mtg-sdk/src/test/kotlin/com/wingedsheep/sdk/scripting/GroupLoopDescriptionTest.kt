package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A `ForEachInGroup` reads its group one member at a time — "each other permanent you control with
 * a +1/+1 counter" — so the stack text names a singular noun with the controller after it,
 * never a pluralised "each other you control permanents".
 */
class GroupLoopDescriptionTest : FunSpec({

    test("a loop over other permanents you control with a counter reads as a singular each-phrase") {
        val group = GroupFilter(
            GameObjectFilter.Permanent.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE)
        ).other()
        val effect = Effects.ForEachInGroup(
            group,
            Effects.MoveChosenCountersToTarget(
                EffectTarget.IterationEntity, EffectTarget.Self, counterType = CounterType.PLUS_ONE_PLUS_ONE
            )
        )
        group.eachDescription shouldBe "each other permanent you control with a +1/+1 counter"
        effect.description shouldBe "Move any number of +1/+1 counters from each other permanent you control " +
            "with a +1/+1 counter onto this creature"
    }

    test("creatures you control read singular after each") {
        GroupFilter.OtherCreaturesYouControl.eachDescription shouldBe "each other creature you control"
        GroupFilter.AllCreatures.eachDescription shouldBe "each creature"
    }
})
