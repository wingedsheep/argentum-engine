package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class SpellCopyExceptionsSerializationTest : FunSpec({
    val json = Json { encodeDefaults = true }
    test("spell-copy exceptions round-trip as a polymorphic effect") {
        val effect = Effects.CopyTargetSpell(EffectTarget.ContextTarget(0),
            exceptions = CopyExceptions(overrideColors = setOf(Color.RED)))
        json.decodeFromString(Effect.serializer(), json.encodeToString(Effect.serializer(), effect)) shouldBe effect
    }
    test("ordinary copy effects omit the new empty field even with defaults enabled") {
        val effect = CopyTargetSpellEffect()
        val tree = json.encodeToJsonElement(Effect.serializer(), effect).jsonObject
        tree.containsKey("exceptions") shouldBe false
        json.decodeFromJsonElement(Effect.serializer(), tree) shouldBe effect
    }
    test("text replacement reaches color exceptions without changing the copied mana cost") {
        val replace = object : com.wingedsheep.sdk.scripting.text.TextReplacer {
            override fun replaceCreatureType(subtype: String) = subtype
            override fun replaceSubtype(subtype: com.wingedsheep.sdk.core.Subtype) = subtype
            override fun replaceColor(color: Color) = if (color == Color.RED) Color.GREEN else color
        }
        val effect = CopyTargetSpellEffect(exceptions = CopyExceptions(overrideColors = setOf(Color.RED)))
        effect.applyTextReplacement(replace) shouldBe effect.copy(
            exceptions = CopyExceptions(overrideColors = setOf(Color.GREEN)))
        CopyExceptions(addedColors = setOf(Color.RED, Color.GREEN)).applyTextReplacement(replace) shouldBe
            CopyExceptions(addedColors = setOf(Color.GREEN))
    }

})
