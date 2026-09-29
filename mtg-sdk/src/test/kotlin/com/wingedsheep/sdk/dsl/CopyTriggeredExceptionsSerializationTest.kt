package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class CopyTriggeredExceptionsSerializationTest : FunSpec({
    test("entry-copy exceptions round trip nested triggered abilities and preserve duplicates") {
        val definition = card("Test Copy Data") {
            typeLine = "Creature — Shapeshifter"
            power = 0; toughness = 0
            val ability = grantedTriggeredAbility {
                trigger = Triggers.self.attacks()
                effect = Effects.Connive()
            }
            replacementEffect(EntersAsCopy(exceptions = CopyExceptions(addedTriggeredAbilities = listOf(ability, ability))))
        }
        val replacement: ReplacementEffect = definition.script.replacementEffects.single()
        val encoded = Json.encodeToString<ReplacementEffect>(replacement)
        Json.decodeFromString<ReplacementEffect>(encoded) shouldBe replacement
        val exceptions = (replacement as EntersAsCopy).exceptions
        exceptions.over(exceptions).addedTriggeredAbilities.size shouldBe 4
        CopyExceptions.None.over(exceptions) shouldBe exceptions
    }
})
