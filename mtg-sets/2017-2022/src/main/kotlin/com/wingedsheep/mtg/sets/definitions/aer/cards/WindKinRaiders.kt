package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wind-Kin Raiders
 * {4}{U}{U}
 * Creature — Human Artificer
 * 4/3
 * Improvise
 * Flying
 */
val WindKinRaiders = card("Wind-Kin Raiders") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Artificer"
    power = 4
    toughness = 3
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after " +
        "you're done activating mana abilities pays for {1}.)\nFlying"

    keywords(Keyword.IMPROVISE, Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Shreya Shetty"
        flavorText = "Small aeronaut societies like the Wind-Kin had little to lose by siding with the " +
            "renegades, and much to gain."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbfaa040-61fd-4705-a7ee-c67b49f740e3.jpg"
    }
}
