package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.LoyaltyActivationPermission
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
    test("a static allowance JSON decodes with its count") {
        Json.decodeFromString<StaticAbility>("""{"type":"LoyaltyActivationPermission","times":2}""") shouldBe
            LoyaltyActivationPermission(times = 2)
    }
    test("self and filtered allowances round trip with arbitrary counts") {
        for (filter in listOf(GameObjectFilter.Any.sourceItself(), GameObjectFilter.Planeswalker.youControl())) {
            for (ability in listOf<StaticAbility>(
                LoyaltyActivationPermission(filter, times = 3),
                LoyaltyActivationPermission(filter, instantSpeed = true),
            )) {
                Json.decodeFromString<StaticAbility>(Json.encodeToString(ability)) shouldBe ability
            }
        }
    }
    test("a static permission must relax timing or the count") {
        shouldThrow<IllegalArgumentException> { LoyaltyActivationPermission(times = 1) }
        shouldThrow<IllegalArgumentException> { LoyaltyActivationPermission(instantSpeed = true, times = 0) }
        LoyaltyActivationPermission(GameObjectFilter.Any.sourceItself(), instantSpeed = true).description shouldEndWith
            "any time you could cast an instant"
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
