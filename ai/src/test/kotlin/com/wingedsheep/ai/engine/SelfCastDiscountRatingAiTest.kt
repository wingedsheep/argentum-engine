package com.wingedsheep.ai.engine

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan

/**
 * "This spell costs {X} less to cast, where X is your devotion to black" (Marshmist Titan): the
 * printed mana value overstates the body's price, so the discounted card must rate above the same
 * body without the discount.
 */
class SelfCastDiscountRatingAiTest : FunSpec({
    fun titan(name: String, discounted: Boolean) = card(name) {
        manaCost = "{6}{B}"
        typeLine = "Creature — Giant"
        power = 4
        toughness = 5
        if (discounted) {
            staticAbility {
                ability = ModifySpellCost(
                    target = SpellCostTarget.SelfCast,
                    modification = CostModification.ReduceGenericBy(
                        CostReductionSource.Dynamic(DynamicAmounts.devotionTo(Color.BLACK)),
                    ),
                )
            }
        }
    }

    test("a devotion self-discount rates the body above its printed mana value") {
        LimitedCardRater.explain(titan("Discounted titan probe", true)).heuristicRating shouldBeGreaterThan
            LimitedCardRater.explain(titan("Full-price titan probe", false)).heuristicRating
    }
})
