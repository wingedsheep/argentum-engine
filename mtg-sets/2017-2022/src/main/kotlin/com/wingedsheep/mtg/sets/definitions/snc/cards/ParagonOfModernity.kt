package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaPayment

/**
 * Paragon of Modernity
 * {4}
 * Artifact Creature — Angel Warrior
 * 2/2
 * Flying
 * {3}: This creature gets +1/+1 until end of turn. If exactly three colors of mana were spent to
 * activate this ability, put a +1/+1 counter on it instead.
 *
 * The colour count reads the ability's own saved payment (`colorsOfManaSpent(ACTIVATION)`), never
 * the creature's cast record; colorless mana is not a color.
 */
val ParagonOfModernity = card("Paragon of Modernity") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Angel Warrior"
    oracleText = "Flying\n{3}: This creature gets +1/+1 until end of turn. If exactly three colors of mana were spent to activate this ability, put a +1/+1 counter on it instead."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.colorsOfManaSpent(ManaPayment.ACTIVATION),
                ComparisonOperator.EQ,
                3,
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            otherwise = Effects.ModifyStats(1, 1, EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "242"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/939450d7-1107-4645-ad23-68749b1ec4c2.jpg?1783923061"
        ruling("2022-04-29", "Colorless is not a color. Similarly, colorless mana isn't counted when determining the number of colors that were spent on an ability.")
    }
}
