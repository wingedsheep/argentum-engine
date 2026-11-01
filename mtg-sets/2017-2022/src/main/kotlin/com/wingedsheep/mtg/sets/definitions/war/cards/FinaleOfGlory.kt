package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Finale of Glory — War of the Spark #12
 * {X}{W}{W} · Sorcery
 *
 * Create X 2/2 white Soldier creature tokens with vigilance. If X is 10 or more, also create X 4/4
 * white Angel creature tokens with flying and vigilance.
 */
val FinaleOfGlory = card("Finale of Glory") {
    manaCost = "{X}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Create X 2/2 white Soldier creature tokens with vigilance. If X is 10 or more, also " +
        "create X 4/4 white Angel creature tokens with flying and vigilance."

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.xValue(),
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/c/a/ca11c711-93fe-45ca-b393-3851148defc4.jpg?1783933355",
        ) then Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.xValue(),
                ComparisonOperator.GTE,
                10,
            ),
            then = Effects.CreateToken(
                count = DynamicAmounts.xValue(),
                power = 4,
                toughness = 4,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Angel"),
                keywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
                imageUri = "https://cards.scryfall.io/normal/front/b/1/b16b81e0-5525-4be3-96db-807146fdc5b3.jpg?1783933355",
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "12"
        artist = "Stanton Feng"
        flavorText = "The sky boiled over the citadel, dire and dark. Then the angels brought the dawn."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4ccacb84-2b8e-40b8-8d53-d1b97bb2971f.jpg?1783933484"
    }
}
