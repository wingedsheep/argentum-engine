package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A group reads as Oracle prints it — the noun before the controller, plural for "all", singular for
 * "each" — never the "all other you control permanent…s" a type-word concatenation produced.
 */
class GroupFilterDescriptionTest : FunSpec({

    val othersWithCounter = GroupFilter(
        GameObjectFilter.Permanent.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE)
    ).other()

    test("a group names the plural noun, then the controller, then its trailing clause") {
        othersWithCounter.description shouldBe "all other permanents you control with a +1/+1 counter"
        GroupFilter.OtherCreaturesYouControl.description shouldBe "all other creatures you control"
        GroupFilter.AllCreatures.description shouldBe "all creatures"
    }

    test("a per-member loop reads the group as a singular each-phrase") {
        othersWithCounter.eachDescription shouldBe "each other permanent you control with a +1/+1 counter"
        GroupFilter.OtherCreaturesYouControl.eachDescription shouldBe "each other creature you control"
        GroupFilter.AllCreatures.eachDescription shouldBe "each creature"
    }

    test("a counter move from a group source reads like the printed text") {
        Effects.MoveChosenCountersToTarget(
            source = EffectTarget.GroupRef(othersWithCounter),
            destination = EffectTarget.Self,
            counterType = CounterType.PLUS_ONE_PLUS_ONE
        ).description shouldBe "Move any number of +1/+1 counters from other permanents you control " +
            "with a +1/+1 counter onto this creature"
    }
})
