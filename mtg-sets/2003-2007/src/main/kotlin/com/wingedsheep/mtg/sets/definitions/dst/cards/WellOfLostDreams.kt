package com.wingedsheep.mtg.sets.definitions.dst.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Well of Lost Dreams
 * {4}
 * Artifact
 * Whenever you gain life, you may pay {X}, where X is less than or equal to the amount of life you
 * gained. If you do, draw X cards.
 *
 * Shanna, Purifying Blade's capped-payment composition with the cap read off the triggering event:
 * X is chosen on resolution from 0 up to `triggerLifeGained()` (the amount of *this* life-gain
 * event), then `MayPay(PayDynamicMana(X))` pays it and the "if you do" draws X. Choosing 0 is the
 * decline.
 */
val WellOfLostDreams = card("Well of Lost Dreams") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "Whenever you gain life, you may pay {X}, where X is less than or equal to the " +
        "amount of life you gained. If you do, draw X cards."

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        val x = DynamicAmounts.xValue()
        effect = Effects.ChooseNumberThen(
            then = Effects.If(
                condition = Conditions.CompareAmounts(x, ComparisonOperator.GT, 0),
                then = Effects.MayPay(
                    cost = Effects.PayDynamicMana(x),
                    then = Effects.DrawCards(x),
                ),
            ),
            maxValue = DynamicAmounts.triggerLifeGained(),
            prompt = "Choose X — pay {X} to draw X cards (0 to decline)",
        )
        description = "Whenever you gain life, you may pay {X}, where X is less than or equal to the " +
            "amount of life you gained. If you do, draw X cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Jeff Miracola"
        flavorText = "Some say the knowledge lost during the Ritual of Rebuking is returned through the well's waters."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bf1b915-7790-43f4-b308-a80a1be6ec29.jpg?1783944414"
    }
}
