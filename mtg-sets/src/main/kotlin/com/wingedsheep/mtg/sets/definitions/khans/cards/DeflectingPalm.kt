package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Deflecting Palm
 * {R}{W}
 * Instant
 * The next time a source of your choice would deal damage to you this turn,
 * prevent that damage. If damage is prevented this way, Deflecting Palm deals
 * that much damage to that source's controller.
 *
 * Note: This spell does not target. The source is chosen on resolution.
 */
val DeflectingPalm = card("Deflecting Palm") {
    manaCost = "{R}{W}"
    typeLine = "Instant"
    oracleText = "The next time a source of your choice would deal damage to you this turn, prevent that damage. If damage is prevented this way, Deflecting Palm deals that much damage to that source's controller."

    spell {
        effect = Effects.DeflectNextDamageFromChosenSource()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32374918-1bcb-4516-96af-f27da752517e.jpg?1562784565"
    }
}
