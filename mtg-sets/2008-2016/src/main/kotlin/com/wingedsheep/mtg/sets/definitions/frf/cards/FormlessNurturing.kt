package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Formless Nurturing
 * {3}{G}
 * Sorcery
 * Manifest the top card of your library, then put a +1/+1 counter on it.
 *
 * The manifest publishes the face-down permanent as the `"manifested"` pipeline collection, and the
 * counter goes on exactly that permanent. With an empty library nothing is manifested and no
 * counter is placed.
 */
val FormlessNurturing = card("Formless Nurturing") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Manifest the top card of your library, then put a +1/+1 counter on it. (To manifest a " +
        "card, put it onto the battlefield face down as a 2/2 creature. Turn it face up any time for its " +
        "mana cost if it's a creature card.)"

    spell {
        effect = Patterns.Library.manifest() then
            Effects.AddCountersToCollection("manifested", CounterType.PLUS_ONE_PLUS_ONE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f70646e4-936a-4c2a-8d4a-a2c659733093.jpg?1783938681"
        ruling(
            "2014-11-24",
            "A permanent that turns face up or face down changes characteristics but is otherwise the same " +
                "permanent. Spells and abilities that were targeting that permanent, as well as Auras and " +
                "Equipment that were attached to the permanent, aren't affected."
        )
        ruling(
            "2014-11-24",
            "Unlike a face-down creature that was cast using the morph ability, a manifested creature may still be " +
                "turned face up after it loses its abilities if it's a creature card."
        )
    }
}
