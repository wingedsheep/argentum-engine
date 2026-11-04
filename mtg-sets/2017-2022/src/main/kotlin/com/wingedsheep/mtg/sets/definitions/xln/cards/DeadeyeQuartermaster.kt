package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Deadeye Quartermaster
 * {3}{U}
 * Creature — Human Pirate
 * 2/2
 * When this creature enters, you may search your library for an Equipment or Vehicle card,
 * reveal it, put it into your hand, then shuffle.
 */
val DeadeyeQuartermaster = card("Deadeye Quartermaster") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may search your library for an Equipment or Vehicle card, reveal it, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any.withAnySubtype("Equipment", "Vehicle"),
                destination = SearchDestination.HAND,
                reveal = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Josh Hass"
        flavorText = "\"Welcome aboard, mate! You're going to need this.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5ba70d58-69fd-4675-85d2-90bc2595c01c.jpg?1783935785"
    }
}
