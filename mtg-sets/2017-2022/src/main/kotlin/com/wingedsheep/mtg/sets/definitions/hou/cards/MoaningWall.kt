package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

val MoaningWall = card("Moaning Wall") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Wall"
    oracleText = "Defender\nCycling {2} ({2}, Discard this card: Draw a card.)"
    power = 0
    toughness = 5

    keywords(Keyword.DEFENDER)
    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Piotr Jabłoński"
        flavorText = "Before the monuments, before the first trials, before Naktamun itself, Bolas culled the plane of its adults in order to rebuild it for his own designs."
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef494f81-638a-4e5d-b987-0dde7f2a135c.jpg?1783936037"
    }
}
