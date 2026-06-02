package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Sultai Soothsayer
 * {2}{B}{G}{U}
 * Creature — Snake Shaman
 * 2/5
 * When this creature enters, look at the top four cards of your library.
 * Put one of them into your hand and the rest into your graveyard.
 */
val SultaiSoothsayer = card("Sultai Soothsayer") {
    manaCost = "{2}{B}{G}{U}"
    colorIdentity = "UBG"
    typeLine = "Creature — Snake Shaman"
    power = 2
    toughness = 5
    oracleText = "When this creature enters, look at the top four cards of your library. Put one of them into your hand and the rest into your graveyard."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.lookAtTopAndKeep(
            count = 4,
            keepCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Cynthia Sheppard"
        flavorText = "The naga of the Sultai Brood made deals with dark forces to keep their power."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae0de2c9-957e-41ee-8899-b93e6f1091dc.jpg?1562791937"
    }
}
