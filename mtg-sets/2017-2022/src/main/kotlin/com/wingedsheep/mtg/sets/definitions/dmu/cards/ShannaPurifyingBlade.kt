package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Shanna, Purifying Blade
 * {G}{W}{U}
 * Legendary Creature — Human Warrior
 * 3/3
 * Lifelink
 * At the beginning of your end step, you may pay {X}. If you do, draw X cards. X can't be greater
 * than the amount of life you gained this turn.
 *
 * `Gate.MayPayX` has no cap, so the capped payment is composed: X is chosen on resolution (per the
 * ruling) from 0 up to the life gained this turn, then `MayPay(PayDynamicMana(X))` pays it and the
 * "if you do" draws X. Choosing 0 is the decline. The trigger is not an intervening "if" — it
 * triggers every end step; with no life gained, X can only be 0, so the inner choice is skipped.
 */
val ShannaPurifyingBlade = card("Shanna, Purifying Blade") {
    manaCost = "{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "Lifelink\n" +
        "At the beginning of your end step, you may pay {X}. If you do, draw X cards. " +
        "X can't be greater than the amount of life you gained this turn."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val x = DynamicAmounts.xValue()
        effect = Effects.If(
            condition = Conditions.YouGainedLifeThisTurn,
            then = Effects.ChooseNumberThen(
                then = Effects.If(
                    condition = Conditions.CompareAmounts(x, ComparisonOperator.GT, 0),
                    then = Effects.MayPay(
                        cost = Effects.PayDynamicMana(x),
                        then = Effects.DrawCards(x),
                    ),
                ),
                maxValue = DynamicAmounts.lifeGainedThisTurn(),
                prompt = "Choose X — pay {X} to draw X cards (0 to decline)",
            ),
        )
        description = "At the beginning of your end step, you may pay {X}. If you do, draw X cards. " +
            "X can't be greater than the amount of life you gained this turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "218"
        artist = "Magali Villeneuve"
        flavorText = "Shanna had heard Sisay's legend all her life. Now it was time to forge her own."
        imageUri = "https://cards.scryfall.io/normal/front/7/2/7261c443-8d27-4d85-8027-2a0271b85e07.jpg?1783921275"
        ruling("2022-09-09", "You choose the value for X as the ability resolves.")
    }
}
