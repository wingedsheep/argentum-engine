package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Prideful Feastling
 * {2}{W/B}
 * Creature — Shapeshifter
 * 2/3
 *
 * Changeling (This card is every creature type.)
 * Lifelink
 */
val PridefulFeastling = card("Prideful Feastling") {
    manaCost = "{2}{W/B}"
    typeLine = "Creature — Shapeshifter"
    power = 2
    toughness = 3
    oracleText = "Changeling (This card is every creature type.)\nLifelink"

    keywords(Keyword.CHANGELING, Keyword.LIFELINK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "238"
        artist = "Raph Lomotan"
        flavorText = "Changelings are friendly by nature, but some forms carry ancestral memories of carnage and hunger."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/7578cb61-f606-4559-a10a-343a583228ab.jpg?1767776681"
    }
}
