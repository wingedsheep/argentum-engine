package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Streetbreaker Wurm
 * {3}{R}{G}
 * Creature — Wurm
 * 6/4
 *
 * Vanilla creature (no abilities).
 */
val StreetbreakerWurm = card("Streetbreaker Wurm") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Wurm"
    power = 6
    toughness = 4

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Greg Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5313054-91a5-401c-84d1-03a2cd265060.jpg?1593272809"
    }
}
