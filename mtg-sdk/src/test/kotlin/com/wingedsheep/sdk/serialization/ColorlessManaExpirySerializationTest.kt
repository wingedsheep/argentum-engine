package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ColorlessManaExpirySerializationTest : FunSpec({
    test("colourless expiry composes with dynamic amount restriction and riders") {
        for (expiry in ManaExpiry.entries) {
            val effect = Effects.AddColorlessMana(
                DynamicAmount.XValue, ManaRestriction.CreatureSpellsOnly,
                setOf(ManaSpellRider.MakesSpellUncounterable()), expiry
            )
            val encoded = CardSerialization.json.encodeToString<Effect>(effect)
            CardSerialization.json.decodeFromString<Effect>(encoded) shouldBe effect
        }
    }

    test("old colourless JSON and both facade overloads default to ordinary mana") {
        val effect = CardSerialization.json.decodeFromString<Effect>(
            """{"type":"AddColorlessMana","amount":{"type":"Fixed","amount":2}}"""
        )
        effect shouldBe Effects.AddColorlessMana(2)
        effect shouldBe Effects.AddColorlessMana(DynamicAmount.Fixed(2))
        (effect as AddColorlessManaEffect).expiry shouldBe ManaExpiry.END_OF_TURN
    }

    test("retained colourless mana describes its duration") {
        Effects.AddColorlessMana(2, expiry = ManaExpiry.KEPT_UNTIL_END_OF_TURN).description shouldBe
            "Add {C}{C}. Until end of turn, you don't lose this mana as steps and phases end"
    }
})
