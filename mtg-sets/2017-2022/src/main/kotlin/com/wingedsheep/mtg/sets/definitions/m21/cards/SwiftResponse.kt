package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Swift Response
 * {1}{W}
 * Instant
 * Destroy target tapped creature.
 */
val SwiftResponse = card("Swift Response") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target tapped creature."

    spell {
        val creature = target(TargetFilter.TappedCreature)
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Deruchenko Alexander"
        flavorText = "\"There's nothing quite as satisfying as delivering a good kick to the face.\"\n" +
            "—Adrasteia, Akroan captain"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a90c1ad0-83bd-471c-8d4c-e65bc2abaa18.jpg?1783930732"
    }
}
