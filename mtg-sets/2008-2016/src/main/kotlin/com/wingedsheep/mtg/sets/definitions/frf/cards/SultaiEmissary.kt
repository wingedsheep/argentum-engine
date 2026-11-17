package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sultai Emissary
 * {1}{B}
 * Creature — Zombie Warrior
 * 1/1
 * When this creature dies, manifest the top card of your library.
 */
val SultaiEmissary = card("Sultai Emissary") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Warrior"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, manifest the top card of your library. (Put that card onto " +
        "the battlefield face down as a 2/2 creature. Turn it face up any time for its mana cost if it's " +
        "a creature card.)"

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Patterns.Library.manifest()
        description = "When Sultai Emissary dies, manifest the top card of your library."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Mathias Kollros"
        flavorText = "To the Sultai, Ugin's power was just one more resource to be exploited."
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12421e1c-5144-4391-abf2-a2fb864920e9.jpg?1783938692"
    }
}
