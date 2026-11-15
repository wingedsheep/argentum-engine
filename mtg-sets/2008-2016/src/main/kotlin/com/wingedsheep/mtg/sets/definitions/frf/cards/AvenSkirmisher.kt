package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aven Skirmisher
 * {W}
 * Creature — Bird Warrior
 * 1/1
 * Flying
 */
val AvenSkirmisher = card("Aven Skirmisher") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Warrior"
    oracleText = "Flying"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Jason Rainville"
        flavorText = "\"We do not hide from the dragons that pretend to rule the skies. If we did, the dragons would become our rulers, and our way of life would be lost.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05f7a3b1-4d92-4d32-a823-2e774c6e7e73.jpg"
    }
}
