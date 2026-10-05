package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Drownyard Explorers
 * {3}{U}
 * Creature — Human Wizard
 * 2/4
 * When this creature enters, investigate. (Create a Clue token. It's an artifact with "{2}, Sacrifice this token: Draw a card.")
 */
val DrownyardExplorers = card("Drownyard Explorers") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Human Wizard"
    oracleText = "When this creature enters, investigate. (Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Investigate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Anthony Palumbo"
        flavorText = "\"Angels and inquisitors terrorize villages, but no one seems to notice the stirring out at sea.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3714713f-deb4-4b1c-8764-35287293ca18.jpg?1783937802"
    }
}
