package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Harmonize {2}{G}{G}
 * Sorcery
 *
 * Draw three cards.
 */
val Harmonize = card("Harmonize") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Draw three cards."

    spell {
        effect = Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Danny Schwartz"
        flavorText = "You don't need to know the words to sing along."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb27bd6f-dec5-4e05-b228-d606a03380ad.jpg?1721428766"
    }
}
