package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Stitcher's Supplier
 * {B}
 * Creature — Zombie
 * 1/1
 * When this creature enters or dies, mill three cards. (Put the top three cards of your library
 * into your graveyard.)
 *
 * "Enters or dies" is two triggered abilities (Crow of Dark Tidings' shape), each a
 * `Patterns.Library.mill(3)` for the controller.
 */
val StitchersSupplier = card("Stitcher's Supplier") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "When this creature enters or dies, mill three cards. (Put the top three cards of " +
        "your library into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(3)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Patterns.Library.mill(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Chris Seaman"
        flavorText = "No part goes to waste."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b737126-50b5-4678-91bf-197b64086fe4.jpg?1783934562"
    }
}
