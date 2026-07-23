package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Into the Fae Court
 * {3}{U}{U}
 * Sorcery
 *
 * Draw three cards. Create a 1/1 blue Faerie creature token with flying and
 * "This token can block only creatures with flying."
 */
val IntoTheFaeCourt = card("Into the Fae Court") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw three cards. Create a 1/1 blue Faerie creature token with flying and " +
        "\"This token can block only creatures with flying.\""

    spell {
        effect = Effects.Composite(
            Effects.DrawCards(3),
            woeFaerieToken()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Anna Steinbauer"
        flavorText = "Kellan stepped from his village into a strange world where trees bore jeweled " +
            "fruit. A regal, inhuman voice rang in his ears: \"Tell me, brave hero, are you true of heart?\""
        imageUri = "https://cards.scryfall.io/normal/front/9/6/969b13bb-6411-41f9-b6b4-af4ffca62e17.jpg?1783915119"
    }
}
