package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration

/**
 * Deep Space Scan
 * {4}{U}{U}
 * Sorcery
 *
 * Strange new worlds — Draw X cards, where X is the number of differently named lands you control.
 * You have no maximum hand size until your next turn.
 *
 * "Strange new worlds" is an ability word with no rules meaning.
 */
val DeepSpaceScan = card("Deep Space Scan") {
    manaCost = "{4}{U}{U}"
    typeLine = "Sorcery"
    colorIdentity = "U"
    oracleText = "Strange new worlds — Draw X cards, where X is the number of differently named lands you control. " +
        "You have no maximum hand size until your next turn."

    spell {
        effect = Effects.DrawCards(DynamicAmounts.differentlyNamedLandsYouControl()) then
            Effects.RemoveMaximumHandSize(duration = Duration.UntilYourNextTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Andreia Ugrai"
        flavorText = "Neutrino spectrometry and quark resonance scans formed the foundation of Starfleet sensor tech, " +
            "but active subspace flux analysis changed the game."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15ffeda3-28ac-4adf-9844-d7b051aae227.jpg?1792508172"
    }
}
