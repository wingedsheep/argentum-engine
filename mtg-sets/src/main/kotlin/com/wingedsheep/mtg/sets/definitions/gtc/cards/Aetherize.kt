package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Aetherize
 * {3}{U}
 * Instant
 *
 * Return all attacking creatures to their owner's hand.
 *
 * A non-targeted mass bounce of every attacking creature (regardless of who controls
 * them). Modeled with the [Patterns.Group.returnAllToHand] pipeline over the
 * [GroupFilter.AttackingCreatures] group — each creature goes to its owner's hand.
 */
val Aetherize = card("Aetherize") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return all attacking creatures to their owner's hand."

    spell {
        effect = Patterns.Group.returnAllToHand(GroupFilter.AttackingCreatures)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Ryan Barger"
        flavorText = "\"You can come back once you've learned some manners—and figured out how to reconstitute your physical forms.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33303859-c6e0-4ebd-bb5f-44be7f5d7459.jpg?1782714145"
    }
}
