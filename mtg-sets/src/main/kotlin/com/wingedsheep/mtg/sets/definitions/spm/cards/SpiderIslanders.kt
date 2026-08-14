package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.model.Rarity

/**
 * Spider-Islanders — Marvel's Spider-Man #91
 * {3}{R} · Creature — Spider Horror Citizen · 4/3
 *
 * Mayhem {1}{R}
 */
val SpiderIslanders = card("Spider-Islanders") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Spider Horror Citizen"
    power = 4
    toughness = 3
    oracleText = "Mayhem {1}{R} (You may cast this card from your graveyard for {1}{R} if you " +
        "discarded it this turn. Timing rules still apply.)"

    mayhem("{1}{R}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Helge C. Balzer"
        flavorText = "\"We've done it, Professor Warren. The glorious metamorphosis of Manhattan has begun!\"\n—Spider-Queen, Adriana Soria"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9132e45-4ddb-4565-ac45-86f1ecc6230d.jpg?1783905332"
    }
}
