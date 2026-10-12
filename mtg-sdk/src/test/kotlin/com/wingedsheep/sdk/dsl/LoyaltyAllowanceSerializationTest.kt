package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GrantLoyaltyActivationPermissionEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import kotlinx.serialization.json.Json

class LoyaltyAllowanceSerializationTest : FunSpec({
    test("old Oath JSON retains its original allowance") {
        Json.decodeFromString<StaticAbility>("""{"type":"ExtraLoyaltyActivation"}""") shouldBe ExtraLoyaltyActivation()
    }
    test("self and filtered allowances round trip with arbitrary counts") {
        for (filter in listOf(GameObjectFilter.Any.sourceItself(), GameObjectFilter.Planeswalker.youControl())) {
            val ability: StaticAbility = ExtraLoyaltyActivation(filter, times = 3)
            Json.decodeFromString<StaticAbility>(Json.encodeToString(ability)) shouldBe ability
        }
    }
    test("an extra allowance cannot lower the normal limit") {
        shouldThrow<IllegalArgumentException> { ExtraLoyaltyActivation(times = 1) }
    }
    test("player-wide loyalty permissions round trip on both axes") {
        for (effect in listOf(Effects.ExtraLoyaltyActivations(times = 3), Effects.InstantSpeedLoyaltyAbilities())) {
            Json.decodeFromString<Effect>(Json.encodeToString(effect)) shouldBe effect
        }
    }
    test("a player-wide loyalty permission must relax timing or the count") {
        shouldThrow<IllegalArgumentException> { GrantLoyaltyActivationPermissionEffect(times = 1) }
        Effects.ExtraLoyaltyActivations().description shouldEndWith "twice rather than only once"
        Effects.InstantSpeedLoyaltyAbilities().description shouldEndWith "any time you could cast an instant"
    }
})
