package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Peer Pressure
 * {3}{U}
 * Sorcery
 * Choose a creature type. If you control more creatures of that type than each
 * other player, you gain control of all creatures of that type.
 * (This effect lasts indefinitely.)
 */
val PeerPressure = card("Peer Pressure") {
    manaCost = "{3}{U}"
    typeLine = "Sorcery"
    oracleText = "Choose a creature type. If you control more creatures of that type than each other player, you gain control of all creatures of that type. (This effect lasts indefinitely.)"

    spell {
        effect = Effects.ChooseCreatureTypeGainControl()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Kev Walker"
        flavorText = "\"The first step to getting what you want is convincing everyone they want you to have it.\"\n—Phage the Untouchable"
        imageUri = "https://cards.scryfall.io/large/front/b/e/be0110ba-49e4-4729-8a84-4d408b20df53.jpg?1562939869"
    }
}
