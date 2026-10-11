package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

/**
 * The SDK contract of [CreateTokenCopyOfTargetEffect.basePower] / [CreateTokenCopyOfTargetEffect.baseToughness]
 * — the game-dependent copy P/T (Saw in Half). Engine behaviour is in `SawInHalfScenarioTest`.
 */
class CreateTokenCopyOfTargetBasePowerTest : FunSpec({

    val half = DynamicAmount.VariableReference("half")

    test("base power and toughness render as an except clause") {
        val effect = CreateTokenCopyOfTargetEffect(
            target = EffectTarget.ContextTarget(0),
            count = DynamicAmount.Fixed(2),
            basePower = half,
            baseToughness = half,
        )
        effect.description shouldContain "except their base power is"
    }

    test("base power needs base toughness") {
        shouldThrow<IllegalArgumentException> {
            CreateTokenCopyOfTargetEffect(target = EffectTarget.ContextTarget(0), basePower = half)
        }
    }

    test("base P/T can't combine with a fixed P/T override") {
        shouldThrow<IllegalArgumentException> {
            CreateTokenCopyOfTargetEffect(
                target = EffectTarget.ContextTarget(0),
                overridePower = 1, overrideToughness = 1,
                basePower = half, baseToughness = half,
            )
        }
        shouldThrow<IllegalArgumentException> {
            CreateTokenCopyOfTargetEffect(
                target = EffectTarget.ContextTarget(0),
                exceptions = CopyExceptions(powerOverride = 1, toughnessOverride = 1),
                basePower = half, baseToughness = half,
            )
        }
    }
})
