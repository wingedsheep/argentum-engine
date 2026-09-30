package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.TextWordCategory
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class TextWordCategoriesSerializationTest : FunSpec({
    for (categories in listOf(setOf(TextWordCategory.COLOR_WORD), setOf(TextWordCategory.BASIC_LAND_TYPE), TextWordCategory.entries.toSet())) {
        test("word categories $categories survive a polymorphic round trip") {
            val effect = Effects.ChangeWordInText(categories, EffectTarget.ContextTarget(1), Duration.Permanent)
            val encoded = CardSerialization.json.encodeToString<Effect>(effect)
            CardSerialization.json.decodeFromString<Effect>(encoded) shouldBe effect
        }
    }
    test("empty category sets cannot create an unanswerable decision") {
        shouldThrow<IllegalArgumentException> {
            Effects.ChangeWordInText(emptySet(), EffectTarget.Self)
        }
    }
})
