package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Capture of Jingzhou
 * {3}{U}{U}
 * Sorcery
 * Take an extra turn after this one.
 */
val CaptureOfJingzhou = card("Capture of Jingzhou") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Take an extra turn after this one."

    spell {
        effect = Effects.TakeExtraTurn()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Jack Wei"
        flavorText = "The province of Jingzhou was the fulcrum of the three kingdoms. At separate times it was coveted and controlled by the Wu, the Shu, and the Wei."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2df84f2-08e8-43e4-825f-dccfe096d92b.jpg?1783946125"
    }
}
