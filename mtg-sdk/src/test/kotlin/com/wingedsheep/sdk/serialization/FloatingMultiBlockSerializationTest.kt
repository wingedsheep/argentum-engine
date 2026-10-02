package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class FloatingMultiBlockSerializationTest : FunSpec({
    test("floating multi-block composition and defending-controller filter round trip") {
        val effect: Effect = Effects.GrantStaticAbility(
            CompositeStaticAbility(listOf(CanBlockAnyNumber(),MustBlockEachAttacker(
                GroupFilter(GameObjectFilter.Creature.defendingPlayerControls())))),
            EffectTarget.ContextTarget(0),Duration.EndOfTurn)
        val json = CardSerialization.json.encodeToString(effect)
        CardSerialization.json.decodeFromString<Effect>(json) shouldBe effect
    }
})
