package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByFewerThan

/**
 * Rampaging Ceratops — {4}{R}
 * Creature — Dinosaur
 * 5/4
 * This creature can't be blocked except by three or more creatures.
 */
val RampagingCeratops = card("Rampaging Ceratops") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur"
    oracleText = "This creature can't be blocked except by three or more creatures."
    power = 5
    toughness = 4

    staticAbility {
        ability = CantBeBlockedByFewerThan(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Nicholas Gregory"
        flavorText = "Anticipating tight spaces and trivial fauna, the first Dusk Legion expeditions were small, mobile, and lightly armored. Hundreds of casualties later, they started sending plate-clad platoons."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bab3f8f-cb06-466d-a35d-0b5e1a2b524c.jpg?1782694479"
    }
}
