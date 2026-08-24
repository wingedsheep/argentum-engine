package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lightning Hounds
 * {2}{R}{R}
 * Creature — Dog
 * 3 / 2
 *
 * First strike
 */
val LightningHounds = card("Lightning Hounds") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dog"
    oracleText = "First strike"
    power = 3
    toughness = 2
    keywords(Keyword.FIRST_STRIKE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "201"
        artist = "Andrew Robinson"
        flavorText = "Quick enough to avoid jhovalls when alone and fierce enough to attack them in packs, the hounds were as at home in the mountains as the Mercadians were atop theirs."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38c82a1d-5db1-4090-b446-cc5bc6dc811d.jpg"
    }
}
