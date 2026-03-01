package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mystic of the Hidden Way
 * {4}{U}
 * Creature — Human Monk
 * 3/2
 * Mystic of the Hidden Way can't be blocked.
 * Morph {2}{U}
 */
val MysticOfTheHiddenWay = card("Mystic of the Hidden Way") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Human Monk"
    power = 3
    toughness = 2
    oracleText = "Mystic of the Hidden Way can't be blocked.\nMorph {2}{U}"

    flags(AbilityFlag.CANT_BE_BLOCKED)

    morph = "{2}{U}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Ryan Alexander Lee"
        flavorText = "\"There are no obstacles, only different paths.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b5de843-e05d-43b5-a5d0-a737484fbd71.jpg?1562789034"
    }
}
