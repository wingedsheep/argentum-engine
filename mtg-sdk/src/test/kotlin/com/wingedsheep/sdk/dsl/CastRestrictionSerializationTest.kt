package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class CastRestrictionSerializationTest : FunSpec({
    test("filtered and blanket casting bans round trip for both supported durations") {
        for (duration in listOf(Duration.EndOfTurn, Duration.Permanent)) {
            for (filter in listOf(GameObjectFilter.Any, GameObjectFilter.Noncreature)) {
                val effect: Effect = Effects.CantCastSpells(EffectTarget.PlayerRef(Player.Each), duration, filter)
                Json.decodeFromString<Effect>(Json.encodeToString(effect)) shouldBe effect
            }
        }
    }
    test("omitting the filter preserves the blanket ban and its description") {
        val effect = Effects.CantCastSpells(EffectTarget.PlayerRef(Player.You)) as CantCastSpellsEffect
        effect.spellFilter shouldBe GameObjectFilter.Any
        effect.description shouldBe "You can't cast spells until end of turn"
    }
})
