package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PeerPressureEffect

/**
 * Peer Pressure
 * {3}{U}
 * Sorcery
 * Choose a creature type. If you control more creatures of that type than each other player,
 * you gain control of all creatures of that type.
 */
val PeerPressure = card("Peer Pressure") {
    manaCost = "{3}{U}"
    typeLine = "Sorcery"
    oracleText = "Choose a creature type. If you control more creatures of that type than each other player, you gain control of all creatures of that type."

    spell {
        effect = PeerPressureEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Edward P. Beard, Jr."
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/b/e/be0110ba-49e4-4729-8a84-4d408b20df53.jpg?1562937932"
    }
}
