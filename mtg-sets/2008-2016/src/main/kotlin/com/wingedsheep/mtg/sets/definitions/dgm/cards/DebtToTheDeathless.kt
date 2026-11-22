package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity

/**
 * Debt to the Deathless
 * {X}{W}{W}{B}{B}
 * Sorcery
 * Each opponent loses two times X life. You gain life equal to the life lost this way.
 *
 * Exsanguinate's drain with the amount doubled: `Multiply(XValue, 2)` is "two times X", and
 * [Effects.DrainLife] gains exactly the life actually lost across all opponents.
 */
val DebtToTheDeathless = card("Debt to the Deathless") {
    manaCost = "{X}{W}{W}{B}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Each opponent loses two times X life. You gain life equal to the life lost this way."

    spell {
        effect = Effects.DrainLife(DynamicAmounts.xValue() * 2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Seb McKinnon"
        flavorText = "Some of Orzhov's cruelest collectors remember their debtors by taste alone."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/610e5a91-857b-4121-8b75-dbbea27aa0aa.jpg?1783940030"
    }
}
