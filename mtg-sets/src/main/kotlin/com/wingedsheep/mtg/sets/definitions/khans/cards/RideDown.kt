package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ride Down
 * {R}{W}
 * Instant
 * Destroy target blocking creature. Creatures that were blocked by that creature
 * this combat gain trample until end of turn.
 */
val RideDown = card("Ride Down") {
    manaCost = "{R}{W}"
    typeLine = "Instant"
    oracleText = "Destroy target blocking creature. Creatures that were blocked by that creature this combat gain trample until end of turn."

    spell {
        val blocker = target("blocking creature", Targets.BlockingCreature)
        // Grant trample before destroy so combat relationships are still intact
        effect = Effects.GrantKeywordToAttackersBlockedBy(Keyword.TRAMPLE, blocker)
            .then(Effects.Destroy(blocker))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Daarken"
        flavorText = "\"I will wash you from my hooves!\" —Mardu taunt"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bc9a434-9617-4a20-88f0-355b20f2c538.jpg?1562785134"
    }
}
