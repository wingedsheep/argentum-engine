package com.wingedsheep.sdk.scripting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ModifySpellCostDescriptionTest : DescribeSpec({

    describe("optional life payment") {
        it("round trips the payment axis without changing an ordinary modifier") {
            val json = com.wingedsheep.sdk.serialization.CardSerialization.json
            val ordinary = ModifySpellCost(SpellCostTarget.YouCast(GameObjectFilter.Permanent), CostModification.ReduceColored("{U}"))
            val optional = ordinary.copy(optionalLifePayment = 2)
            val encoded = json.encodeToString(StaticAbility.serializer(), optional)
            json.decodeFromString(StaticAbility.serializer(), encoded) shouldBe optional
            json.decodeFromString(StaticAbility.serializer(), json.encodeToString(StaticAbility.serializer(), ordinary)) shouldBe ordinary
        }
    }

    describe("ModifySpellCost.description with NthOfTypePerTurn gating") {

        it("renders the unconstrained Any filter without a leaked 'card' adjective (Uthros Psionicist)") {
            val ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGeneric(2),
                gating = CostGating.NthOfTypePerTurn(2),
            )
            ability.description shouldBe "The second spell you cast costs {2} less to cast each turn"
        }

        it("keeps the filter adjective for a typed filter (Eluge, the Shoreless Sea)") {
            val ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery),
                modification = CostModification.ReduceColored("{U}"),
                gating = CostGating.NthOfTypePerTurn(1),
            )
            ability.description shouldBe
                "The first instant or sorcery spell you cast costs {U} less to cast each turn"
        }

        it("keeps the plural form when ungated") {
            val ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGeneric(1),
            )
            ability.description shouldBe "spells you cast cost {1} less to cast"
        }
    }
})
