package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

val LurchingRotbeast = card("Lurching Rotbeast") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Beast"
    oracleText = "Cycling {B} ({B}, Discard this card: Draw a card.)"
    power = 4
    toughness = 2

    keywordAbility(KeywordAbility.cycling("{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Jason A. Engle"
        flavorText = "The dead that swept across Naktamun came in all shapes and sizes."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f06be97-71c8-46c8-a1c2-5da3af25e6de.jpg?1783936039"
    }
}
