package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Nexus of Fate — Core Set 2019 #306 (Buy-a-Box promo; canonical printing).
 *
 * The graveyard replacement is intrinsic to the card, so it applies from every zone: on
 * resolution, when countered or fizzled, discarded or milled — each time the card is shuffled
 * into its owner's library instead (the Progenitus shape).
 */
val NexusOfFate = card("Nexus of Fate") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Take an extra turn after this one.\n" +
        "If Nexus of Fate would be put into a graveyard from anywhere, reveal Nexus of Fate and " +
        "shuffle it into its owner's library instead."

    spell {
        effect = Effects.TakeExtraTurn()
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.LIBRARY,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
            selfOnly = true,
            shuffleIntoLibrary = true,
            reveal = true,
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "306"
        artist = "Mike Bierek"
        flavorText = "Sarkhan wandered into a tomb and back in time."
        inBooster = false
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f163cfbf-6df6-4af5-9fe4-23b0d511586a.jpg?1783934478"
        ruling(
            "2018-07-13",
            "Nexus of Fate's last ability applies if it would be put into a graveyard in any way, " +
                "including while it's resolving."
        )
    }
}
