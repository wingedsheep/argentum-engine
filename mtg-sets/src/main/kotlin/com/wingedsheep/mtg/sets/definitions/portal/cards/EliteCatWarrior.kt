package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Elite Cat Warrior
 * {2}{G}
 * Creature — Cat Warrior
 * 2/3
 * Forestwalk
 */
val EliteCatWarrior = card("Elite Cat Warrior") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Cat Warrior"
    power = 2
    toughness = 3

    keywords(Keyword.FORESTWALK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Mark Zug"
        flavorText = "She walks where she wills, and her claws find their marks."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01a04e26-6103-4fd3-a6d5-3c12a38dd8e8.jpg"
    }
}
