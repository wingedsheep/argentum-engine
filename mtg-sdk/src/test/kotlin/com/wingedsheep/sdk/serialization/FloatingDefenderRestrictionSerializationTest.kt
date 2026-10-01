package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.CantBeAttackedBy
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class FloatingDefenderRestrictionSerializationTest : FunSpec({
    test("optional draw replacement composes a step gate and duration-bounded player restriction") {
        val replacement: ReplacementEffect = ReplaceDrawWith(
            Effects.GrantStaticAbility(CantBeAttackedBy(GameObjectFilter.Creature
                .withoutKeyword(Keyword.FLYING).withoutKeyword(Keyword.ISLANDWALK)),
                EffectTarget.Controller, Duration.UntilYourNextTurn),
            optional = true,
            restrictions = listOf(Conditions.IsInStep(Step.DRAW)),
        )
        val json = CardSerialization.json.encodeToString(replacement)
        CardSerialization.json.decodeFromString<ReplacementEffect>(json) shouldBe replacement
    }
})
