package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Loam Larva
 * {1}{G}
 * Creature — Insect
 * 1/3
 * When this creature enters, you may search your library for a basic land card, reveal it, then
 * shuffle and put that card on top.
 */
val LoamLarva = card("Loam Larva") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, you may search your library for a basic land card, " +
        "reveal it, then shuffle and put that card on top."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.TOP_OF_LIBRARY,
            reveal = true,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Jason Kang"
        flavorText = "The forest floor of Murasa is home to life as diverse and vibrant as that of the canopy above."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/affa9314-0021-445e-9496-349254b16bf8.jpg"
    }
}
