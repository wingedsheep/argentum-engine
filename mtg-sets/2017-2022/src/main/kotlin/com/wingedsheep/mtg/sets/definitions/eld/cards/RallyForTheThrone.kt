package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Rally for the Throne
 * {2}{W}
 * Instant
 * Create two 1/1 white Human creature tokens.
 * Adamant — If at least three white mana was spent to cast this spell, you gain 1 life for each
 * creature you control.
 *
 * The tokens are created first, so the Adamant life gain counts them. Adamant reads this spell's own
 * cast payment via [DynamicAmounts.manaOfColorSpent], as Silverflame Ritual does.
 */
val RallyForTheThrone = card("Rally for the Throne") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Create two 1/1 white Human creature tokens.\n" +
        "Adamant — If at least three white mana was spent to cast this spell, you gain 1 life for each creature you control."

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/9/4/94057dc6-e589-4a29-9bda-90f5bece96c4.jpg?1783932485",
        ) then Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.manaOfColorSpent(Color.WHITE),
                ComparisonOperator.GTE,
                3,
            ),
            Effects.GainLife(DynamicAmounts.creaturesYouControl()),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "James Paick"
        flavorText = "\"If you're as loyal as you claim, now is the time to show it.\"\n—Queen Linden of Ardenvale"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/548d7ff0-70b6-4221-b50b-3ee8547e1b0a.jpg?1783932669"
    }
}
