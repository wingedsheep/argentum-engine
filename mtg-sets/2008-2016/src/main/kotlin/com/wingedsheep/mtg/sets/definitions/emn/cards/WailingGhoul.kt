package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wailing Ghoul
 * {1}{B}
 * Creature — Zombie
 * 1/3
 * When this creature enters, mill two cards. (Put the top two cards of your library into your graveyard.)
 */
val WailingGhoul = card("Wailing Ghoul") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText = "When this creature enters, mill two cards. (Put the top two cards of your library into your graveyard.)"
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Svetlin Velinov"
        flavorText = "\"Don't worry, sweet brother. Help is on the way.\"\n—Ghoulcaller Gisa"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b1bf0dd-825c-4c8a-a48d-84f35fbe5901.jpg"
    }
}
