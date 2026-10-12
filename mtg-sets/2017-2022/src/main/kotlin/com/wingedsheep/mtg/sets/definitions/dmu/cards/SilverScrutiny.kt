package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

val SilverScrutiny = card("Silver Scrutiny") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "You may cast this spell as though it had flash if X is 3 or less.\nDraw X cards."

    conditionalFlash = Conditions.CompareAmounts(DynamicAmounts.xValue(), ComparisonOperator.LTE, 3)

    spell {
        effect = Effects.DrawCards(DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Donato Giancola"
        flavorText = "Karn's creation allowed him to monitor events across Dominaria—and everything he saw worried him."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b650ab6-9c22-424f-99e5-8ef5235a18a3.jpg?1783921345"
    }
}
