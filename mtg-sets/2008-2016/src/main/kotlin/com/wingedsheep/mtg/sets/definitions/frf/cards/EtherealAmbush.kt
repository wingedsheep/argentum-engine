package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ethereal Ambush
 * {3}{G}{U}
 * Instant
 * Manifest the top two cards of your library.
 *
 * The cards are manifested one at a time (the move executor loops per card), so each becomes its own
 * face-down 2/2. With fewer than two cards in library, whatever is there is manifested.
 */
val EtherealAmbush = card("Ethereal Ambush") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Manifest the top two cards of your library. (To manifest a card, put it onto the battlefield " +
        "face down as a 2/2 creature. Turn it face up any time for its mana cost if it's a creature card.)"

    spell {
        effect = Patterns.Library.manifest(DynamicAmounts.fixed(2))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Lius Lasahido"
        flavorText = "The energies of the Temur lands are readily unleashed upon intruders."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/825d3f4d-eba3-4c45-8846-03b246e93f6c.jpg?1783938674"
        ruling(
            "2014-11-24",
            "The cards are manifested one at a time. It must remain clear which face-down creature was the top " +
                "card of your library and which one was the second card of your library."
        )
        ruling(
            "2014-11-24",
            "If you're playing with the top card of your library revealed as Ethereal Ambush resolves (perhaps " +
                "because you control a card such as Courser of Kruphix), you'll manifest the top card, reveal the " +
                "next card (now the top card), and then manifest that card."
        )
    }
}
