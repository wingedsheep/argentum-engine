package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Trinket Mage
 * {2}{U}
 * Creature — Human Wizard
 * 2/2
 *
 * When this creature enters, you may search your library for an artifact card with mana value 1
 * or less, reveal that card, put it into your hand, then shuffle.
 */
val TrinketMage = card("Trinket Mage") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "When this creature enters, you may search your library for an artifact card with " +
        "mana value 1 or less, reveal that card, put it into your hand, then shuffle."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact.manaValueAtMost(1),
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Mark A. Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c5a41ab-1840-4abb-a8bb-f0b1e7d1b450.jpg?1783944402"
        ruling("2021-03-19", "If a card in a player's library has {X} in its mana cost, X is considered to be 0.")
    }
}
