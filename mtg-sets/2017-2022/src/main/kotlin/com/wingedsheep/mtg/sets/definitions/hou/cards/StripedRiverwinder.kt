package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

val StripedRiverwinder = card("Striped Riverwinder") {
    manaCost = "{6}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Serpent"
    oracleText = "Hexproof (This creature can't be the target of spells or abilities your opponents control.)\nCycling {U} ({U}, Discard this card: Draw a card.)"
    power = 5
    toughness = 5

    keywords(Keyword.HEXPROOF)
    keywordAbility(KeywordAbility.cycling("{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Craig J Spearing"
        flavorText = "The denizens of the Luxa River woke angry on the currents of blood."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbeef9ef-487c-400b-bcee-1c0e8ec94b6a.jpg?1783936047"
    }
}
