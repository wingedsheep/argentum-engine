package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Whisk Away
 * {2}{U}
 * Instant
 * Put target attacking or blocking creature on top of its owner's library.
 */
val WhiskAway = card("Whisk Away") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Put target attacking or blocking creature on top of its owner's library."

    spell {
        val creature = target(TargetFilter.AttackingOrBlockingCreature)
        effect = Effects.PutOnTopOfLibrary(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Zack Stella"
        flavorText = "The maneuvers of the Abzan formation intrigued Ojutai in both their complexity and their futility."
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b5d96a9-11dd-44cc-baf1-bab298ae03f0.jpg"
    }
}
