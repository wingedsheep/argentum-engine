package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Beamsaw Prospector
 * {1}{B}
 * Creature — Human Artificer
 * When this creature dies, create a Lander token. (It's an artifact with "{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.")
 * 2/1
 */
val BeamsawProspector = card("Beamsaw Prospector") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 1
    oracleText = "When this creature dies, create a Lander token. (It's an artifact with \"{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\")"

    // Death trigger: create a Lander token
    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.CreateLander()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Aurore Folny"
        flavorText = "The Kav had learned their lesson about mining too deep. Zern Miffles had not."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f717a3f-c6db-4e8d-8b62-6361ab33d000.jpg?1752946915"
    }
}
