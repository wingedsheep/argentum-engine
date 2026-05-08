package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Red Tiger Mechan
 * {3}{R}
 * Artifact Creature — Robot Cat
 * Haste
 * Warp {1}{R} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)
 * 3/3
 */
val RedTigerMechan = card("Red Tiger Mechan") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Robot Cat"
    oracleText = "Haste\n" +
        "Warp {1}{R} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"
    power = 3
    toughness = 3

    keywords(Keyword.HASTE)

    warp = "{1}{R}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Simon Dominic"
        flavorText = "While the Kavaron tiger has been extinct for centuries, its renowned ferocity endures."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7b2fa48-cd2d-42ea-afd8-8cbd7a1bcdab.jpg?1752947176"
    }
}
