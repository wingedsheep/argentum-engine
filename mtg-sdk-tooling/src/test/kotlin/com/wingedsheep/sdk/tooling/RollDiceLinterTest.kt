package com.wingedsheep.sdk.tooling

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RollDiceLinterTest : FunSpec({
    test("named dice result counts are number writers for subsequent effects") {
        val card=card("Test Dice Tally") {
            typeLine="Sorcery"
            spell {
                effect=Effects.RollDice(resultCounts=mapOf("even" to setOf(2,4,6))) then
                    Effects.GainLife(DynamicAmount.VariableReference("even"))
            }
        }
        CardLinter.lint(card).filter { it.severity == LintSeverity.ERROR } shouldBe emptyList()
    }
})
