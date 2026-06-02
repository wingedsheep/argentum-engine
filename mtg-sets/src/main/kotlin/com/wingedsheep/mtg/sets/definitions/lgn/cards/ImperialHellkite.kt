package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Imperial Hellkite
 * {5}{R}{R}
 * Creature — Dragon
 * 6/6
 * Flying
 * Morph {6}{R}{R}
 * When this creature is turned face up, you may search your library for a Dragon card,
 * reveal it, put it into your hand, then shuffle.
 */
val ImperialHellkite = card("Imperial Hellkite") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying\nMorph {6}{R}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, you may search your library for a Dragon card, reveal it, put it into your hand, then shuffle."

    keywords(Keyword.FLYING)

    morph = "{6}{R}{R}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = MayEffect(
            LibraryPatterns.searchLibrary(
                filter = GameObjectFilter.Creature.withSubtype("Dragon"),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1fc3c5f3-f71b-4a1e-bd90-365d23889925.jpg?1562901333"
        ruling("2004-10-04", "You do not have to find a Dragon card if you do not want to, even if you have one in your library.")
        ruling("2004-10-04", "The trigger occurs when you use the Morph ability to turn the card face up, or when an effect turns it face up. It will not trigger on being revealed or on leaving the battlefield.")
    }
}
