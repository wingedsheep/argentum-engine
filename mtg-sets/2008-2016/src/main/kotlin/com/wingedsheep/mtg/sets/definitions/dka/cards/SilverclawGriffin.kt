package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Silverclaw Griffin
 * {3}{W}{W}
 * Creature — Griffin
 * 3/2
 * Flying, first strike
 */
val SilverclawGriffin = card("Silverclaw Griffin") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Griffin"
    power = 3
    toughness = 2
    oracleText = "Flying, first strike"

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Daniel Ljunggren"
        flavorText = "It patrols the grafs for miles around Thraben, ensuring that the unhallowed do not wake for long."
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54528722-a6aa-4567-9cd1-e4a97adec7d0.jpg"
    }
}
