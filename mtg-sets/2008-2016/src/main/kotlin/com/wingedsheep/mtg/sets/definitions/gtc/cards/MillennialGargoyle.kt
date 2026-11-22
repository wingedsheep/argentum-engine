package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Millennial Gargoyle
 * {4}
 * Artifact Creature — Gargoyle
 * 2/2
 * Flying
 */
val MillennialGargoyle = card("Millennial Gargoyle") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Gargoyle"
    oracleText = "Flying"
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "232"
        artist = "Seb McKinnon"
        flavorText = "\"You've got to keep your wits about you on the roofs. Not all stones are compliant handholds.\"\n—Iveta, rooftop runner"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98d1bc6e-84aa-4973-924a-6688b742bafa.jpg"
    }
}
