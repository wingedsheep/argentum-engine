package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class CopyTriggeredExceptionsSerializationTest : FunSpec({
    test("retained color exceptions round trip and take precedence over color riders") {
        val retained = CopyExceptions(retainColors = true)
        Json.decodeFromString<CopyExceptions>(Json.encodeToString(retained)) shouldBe retained
        retained.over(CopyExceptions(overrideColors = setOf(com.wingedsheep.sdk.core.Color.RED))) shouldBe retained
        retained.over(CopyExceptions(addedColors = setOf(com.wingedsheep.sdk.core.Color.RED))) shouldBe retained
        CopyExceptions(overrideColors = setOf(com.wingedsheep.sdk.core.Color.RED)).over(retained)
            .retainColors shouldBe false
        retained.clauses() shouldBe listOf("it doesn't copy that creature's color")
        io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
            CopyExceptions(retainColors = true, overrideColors = emptySet())
        }
    }

    test("retained resolving trigger exception round trips and composes") {
        val exception = CopyExceptions(retainResolvingTriggeredAbility = true)
        Json.decodeFromString<CopyExceptions>(Json.encodeToString(exception)) shouldBe exception
        exception.over(CopyExceptions(addedKeywords = setOf(com.wingedsheep.sdk.core.Keyword.FLYING)))
            .retainResolvingTriggeredAbility shouldBe true
        CopyExceptions.None.over(exception) shouldBe exception
        exception.clauses() shouldBe listOf("it has this ability")
    }

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

    test("entry-copy exceptions round trip added activated abilities") {
        val definition = card("Test Copy Activated Data") {
            typeLine = "Creature — Shapeshifter"
            power = 0; toughness = 0
            val ability = grantedActivatedAbility {
                cost = Costs.Mana("{X}")
                effect = Effects.GainLife(1)
            }
            replacementEffect(EntersAsCopy(exceptions = CopyExceptions(addedActivatedAbilities = listOf(ability))))
        }
        val replacement: ReplacementEffect = definition.script.replacementEffects.single()
        val encoded = Json.encodeToString<ReplacementEffect>(replacement)
        Json.decodeFromString<ReplacementEffect>(encoded) shouldBe replacement
        val exceptions = (replacement as EntersAsCopy).exceptions
        exceptions.over(exceptions).addedActivatedAbilities.size shouldBe 2
    }
})
