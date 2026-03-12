package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Rescue
 * {U}
 * Instant
 * Return target permanent you control to its owner's hand.
 */
val Rescue = card("Rescue") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "Return target permanent you control to its owner's hand."

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.PermanentYouControl))
        effect = Effects.ReturnToHand(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Joe Slucher"
        flavorText = "\"With just a few seconds to escape, Deryan saved Hurkyl's editions on restoring physical objects from ash.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08050607-b558-4f99-b716-bbbab54e9b68.jpg?1562730962"
    }
}
