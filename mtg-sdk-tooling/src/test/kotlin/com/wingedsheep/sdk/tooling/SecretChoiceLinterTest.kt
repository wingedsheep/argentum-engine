package com.wingedsheep.sdk.tooling

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.SecretChoice
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SecretChoiceLinterTest : FunSpec({
    test("entity ballot inputs are reads and winner collections are available in the follow-up") {
        val definition = card("Test Secret Entity Ballot") {
            typeLine = "Sorcery"
            spell {
                effect = Effects.SecretChoice(
                    choice = SecretChoice.Entity("missingPool", storeMostChosen = "winners"),
                    then = Effects.GainLife(DynamicAmount.DistinctEntitiesInCollections(listOf("winners"))),
                )
            }
        }
        val errors = CardLinter.lint(definition).filter { it.severity == LintSeverity.ERROR }
        errors.size shouldBe 1
        errors.single().message.contains("missingPool") shouldBe true
    }

    test("numeric outcome branches are traversed in the owning pipeline scope") {
        val definition = card("Test Secret Numeric Scope") {
            typeLine = "Sorcery"
            spell {
                effect = Effects.SecretNumbers(
                    matchedEffect = Effects.GainLife(DynamicAmount.DistinctEntitiesInCollections(listOf("missingMatched"))),
                    unmatchedEffect = Effects.GainLife(DynamicAmount.DistinctEntitiesInCollections(listOf("missingUnmatched"))),
                )
            }
        }
        val errors = CardLinter.lint(definition).filter { it.severity == LintSeverity.ERROR }
        errors.size shouldBe 2
        errors.any { it.message.contains("missingMatched") } shouldBe true
        errors.any { it.message.contains("missingUnmatched") } shouldBe true
    }
})
