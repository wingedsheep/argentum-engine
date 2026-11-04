package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

val RampagingHippo = card("Rampaging Hippo") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hippo"
    oracleText = "Trample\nCycling {2} ({2}, Discard this card: Draw a card.)"
    power = 5
    toughness = 6

    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Aaron Miller"
        flavorText = "\"Respect is earned through deeds—or the capacity to bite someone in half.\"\n—Sana, former Ahn-crop initiate"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c97229f-2fc8-439a-a18b-efa09b366b70.jpg?1783936015"
    }
}
