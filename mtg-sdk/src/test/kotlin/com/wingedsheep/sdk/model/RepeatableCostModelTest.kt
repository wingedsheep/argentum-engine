package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class RepeatableCostModelTest : FunSpec({
    test("a repeatable composite scales each atom and round-trips") {
        val cost = AdditionalCost.Composite(listOf(Costs.additional.DiscardCards(), Costs.additional.ExileCards(4)))
        val keyword = KeywordAbility.OptionalAdditionalCost(additionalCost = cost, multi = true)
        keyword.additionalCostPaid(2) shouldBe AdditionalCost.Composite(listOf(Costs.additional.DiscardCards(2), Costs.additional.ExileCards(8)))
        Json.decodeFromString<KeywordAbility>(Json.encodeToString<KeywordAbility>(keyword)) shouldBe keyword
    }
    test("independent picks sharing one selection channel cannot be declared repeatable") {
        shouldThrow<IllegalArgumentException> {
            KeywordAbility.OptionalAdditionalCost(additionalCost = AdditionalCost.Composite(
                listOf(Costs.additional.DiscardCards(), Costs.additional.DiscardCards(2))
            ), multi = true)
        }
    }
    test("non-amount repeated costs remain rejected instead of silently going unpaid") {
        shouldThrow<IllegalArgumentException> {
            KeywordAbility.OptionalAdditionalCost(additionalCost = Costs.additional.SacrificePermanent(), multi = true)
        }
    }
    test("nested composites cannot hide duplicate selection channels") {
        listOf(Costs.additional.DiscardCards(), Costs.additional.ExileCards(4)).forEach { selection ->
            shouldThrow<IllegalArgumentException> {
                KeywordAbility.OptionalAdditionalCost(additionalCost = AdditionalCost.Composite(
                    listOf(selection, AdditionalCost.Composite(listOf(selection)))
                ), multi = true)
            }
        }
    }
    test("nested composites with independent channels still scale") {
        val cost = AdditionalCost.Composite(listOf(
            Costs.additional.DiscardCards(),
            AdditionalCost.Composite(listOf(Costs.additional.ExileCards(4)))
        ))
        val keyword = KeywordAbility.OptionalAdditionalCost(additionalCost = cost, multi = true)
        keyword.additionalCostPaid(2) shouldBe AdditionalCost.Composite(listOf(
            Costs.additional.DiscardCards(2),
            AdditionalCost.Composite(listOf(Costs.additional.ExileCards(8)))
        ))
    }
})
