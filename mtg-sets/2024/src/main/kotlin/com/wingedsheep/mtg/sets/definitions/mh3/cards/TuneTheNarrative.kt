package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tune the Narrative
 * {U}
 * Instant
 * Draw a card. You get {E}{E} (two energy counters).
 */
val TuneTheNarrative = card("Tune the Narrative") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw a card. You get {E}{E} (two energy counters)."

    spell {
        effect = Effects.DrawCards(1) then Effects.GetEnergy(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Nereida"
        flavorText = "\"The kami whisper of other worlds, brimming with stories untold. " +
            "It would take a lifetime to learn them. A lifetime well spent.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40b13321-98f1-4e8c-802d-65498e43ec24.jpg?1783911287"
    }
}
