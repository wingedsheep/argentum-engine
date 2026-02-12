package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.core.ManaCost

/**
 * Barkhide Mauler
 * {4}{G}
 * Creature — Beast
 * 4/4
 * Cycling {2}
 */
val BarkhideMauler = card("Barkhide Mauler") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Cycling {2}"

    keywordAbility(KeywordAbility.Cycling(ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "246"
        artist = "Iain McCaig"
        flavorText = "Anywhere else they would be hunted for their skins, but in Wirewood, they are safe."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9196ce7-3ff4-4dda-a628-559ada11c9ba.jpg"
    }
}
