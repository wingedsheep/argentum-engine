package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Umara Entangler (Oath of the Gatewatch #65)
 * {1}{U}
 * Creature — Merfolk Rogue Ally
 * 2 / 1
 *
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 */
val UmaraEntangler = card("Umara Entangler") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Rogue Ally"
    power = 2
    toughness = 1
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"

    prowess()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "James Ryman"
        flavorText = "\"Sea Gate is gone, the Halimar basin empty. But rebuild the seawall and give these waters time, and they will fill the basin anew.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d4ddc45-cd67-4b2c-84d0-4604783fd8e7.jpg?1783937916"
    }
}
