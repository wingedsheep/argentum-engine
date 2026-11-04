package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

val GraniticTitan = card("Granitic Titan") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    oracleText = "Menace\nCycling {2} ({2}, Discard this card: Draw a card.)"
    power = 5
    toughness = 4

    keywords(Keyword.MENACE)
    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Yohann Schepacz"
        flavorText = "\"That one's not mine. Run!\"\n—Nissa Revane"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/607bdb54-a956-4a9c-89cf-af6f1163b856.jpg?1783936028"
    }
}
