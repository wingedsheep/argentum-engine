package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Bounce Off
 * {U}
 * Instant
 *
 * Return target creature or Vehicle to its owner's hand.
 */
val BounceOff = card("Bounce Off") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "Return target creature or Vehicle to its owner's hand."

    spell {
        val t = target(
            "target creature or Vehicle",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.Creature or GameObjectFilter.Any.withSubtype("Vehicle")
                )
            )
        )
        effect = Effects.ReturnToHand(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Deruchenko Alexander"
        flavorText = "Control lost is adventure gained.\n—Keelhauler proverb"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b3c8dda-2405-4879-8dd1-e790a833c42d.jpg?1738356198"
    }
}
