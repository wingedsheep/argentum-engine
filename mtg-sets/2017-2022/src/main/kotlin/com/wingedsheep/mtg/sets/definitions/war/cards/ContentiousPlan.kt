package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Contentious Plan
 * {1}{U}
 * Sorcery
 * Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)
 * Draw a card.
 */
val ContentiousPlan = card("Contentious Plan") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)\nDraw a card."

    spell {
        effect = Effects.Proliferate() then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Eric Deschamps"
        flavorText = "Niv-Mizzet had perfected a plan, but he had no control over the mortal minds who had to agree to implement it."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e30deb6-9e1f-4545-ae30-c30ba6c7b3a0.jpg?1783933466"
    }
}
