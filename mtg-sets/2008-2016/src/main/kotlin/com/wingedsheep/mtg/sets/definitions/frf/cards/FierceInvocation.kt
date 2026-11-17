package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fierce Invocation
 * {4}{R}
 * Sorcery
 * Manifest the top card of your library, then put two +1/+1 counters on it.
 *
 * The manifest publishes the face-down permanent as the `"manifested"` pipeline collection, and both
 * counters go on exactly that permanent. With an empty library nothing is manifested and no counters
 * are placed.
 */
val FierceInvocation = card("Fierce Invocation") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Manifest the top card of your library, then put two +1/+1 counters on it. (To manifest a " +
        "card, put it onto the battlefield face down as a 2/2 creature. Turn it face up any time for its " +
        "mana cost if it's a creature card.)"

    spell {
        effect = Patterns.Library.manifest() then
            Effects.AddCountersToCollection("manifested", CounterType.PLUS_ONE_PLUS_ONE, 2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/0137de04-9fd7-41b0-b497-163e6e93432b.jpg?1783938688"
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
