package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val VeteranCavalier = card("Veteran Cavalier") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)"

    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "rk post"
        flavorText = "Spirit is the sword and experience the sharpening stone.\n—Arabian proverb"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67301687-42a5-45b5-aeeb-d57da3ac0ce0.jpg?1783946045"
    }
}
