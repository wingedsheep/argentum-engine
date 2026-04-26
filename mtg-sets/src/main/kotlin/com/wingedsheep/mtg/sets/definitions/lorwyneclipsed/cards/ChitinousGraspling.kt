package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Chitinous Graspling
 * {3}{G/U}
 * Creature — Shapeshifter
 * 3/4
 *
 * Changeling (This card is every creature type.)
 * Reach
 */
val ChitinousGraspling = card("Chitinous Graspling") {
    manaCost = "{3}{G/U}"
    typeLine = "Creature — Shapeshifter"
    power = 3
    toughness = 4
    oracleText = "Changeling (This card is every creature type.)\n" +
        "Reach"

    keywords(Keyword.CHANGELING, Keyword.REACH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "211"
        artist = "Richard Kane Ferguson"
        flavorText = "It had observed the twinkling object from afar, then found a form to sate its curiosity."
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9767360-d536-4902-9d2d-1f3474ce89d6.jpg?1767872195"
    }
}
