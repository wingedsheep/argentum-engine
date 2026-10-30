package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class MoveUntilSourceLeavesSerializationTest : FunSpec({
    test("zone-return effect round trips with a named target") {
        val effect = Effects.MoveUntilSourceLeaves(EffectTarget.BoundVariable("victim"), Zone.EXILE)
        val encoded = CardSerialization.json.encodeToString<Effect>(effect)
        CardSerialization.json.decodeFromString<Effect>(encoded) shouldBe effect
    }

    test("zone-return effect round trips with another anchor") {
        val effect = Effects.MoveUntilLeaves(EffectTarget.ContextTarget(0), Zone.EXILE, anchor = EffectTarget.ContextTarget(1))
        val encoded = CardSerialization.json.encodeToString<Effect>(effect)
        CardSerialization.json.decodeFromString<Effect>(encoded) shouldBe effect
    }
})
