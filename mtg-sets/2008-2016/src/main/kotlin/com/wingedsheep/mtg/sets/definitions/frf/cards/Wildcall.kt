package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wildcall
 * {X}{G}{G}
 * Sorcery
 * Manifest the top card of your library, then put X +1/+1 counters on it.
 *
 * The manifest publishes the face-down permanent as the `"manifested"` pipeline collection, and the
 * X counters go on exactly that permanent. X = 0 just manifests; with an empty library nothing is
 * manifested and no counters are placed.
 */
val Wildcall = card("Wildcall") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Manifest the top card of your library, then put X +1/+1 counters on it. (To manifest a " +
        "card, put it onto the battlefield face down as a 2/2 creature. Turn it face up any time for its " +
        "mana cost if it's a creature card.)"

    spell {
        effect = Patterns.Library.manifest() then
            Effects.AddCountersToCollection("manifested", CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Adam Paquette"
        flavorText = "A howl on the wind hides many dangers."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/065fc4d8-c097-4d0e-81cf-22a0b694be9a.jpg?1783938675"
        ruling("2014-11-24", "If you choose 0 for X, you'll just manifest the top card of your library.")
        ruling(
            "2014-11-24",
            "Unlike a face-down creature that was cast using the morph ability, a manifested creature may still be " +
                "turned face up after it loses its abilities if it's a creature card."
        )
    }
}
