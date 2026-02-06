package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RevealAndOpponentChoosesEffect

/**
 * Animal Magnetism
 * {4}{G}
 * Sorcery
 * Reveal the top five cards of your library. An opponent chooses a creature card
 * from among them. Put that card onto the battlefield and the rest into your graveyard.
 */
val AnimalMagnetism = card("Animal Magnetism") {
    manaCost = "{4}{G}"
    typeLine = "Sorcery"

    spell {
        effect = RevealAndOpponentChoosesEffect(
            count = 5,
            filter = GameObjectFilter.Creature
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "245"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c33db646-b30d-4a15-9f8a-63bda74e2d81.jpg?1562941108"
    }
}
