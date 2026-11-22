package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Arrows of Justice
 * {2}{R/W}
 * Instant
 * Arrows of Justice deals 4 damage to target attacking or blocking creature.
 */
val ArrowsOfJustice = card("Arrows of Justice") {
    manaCost = "{2}{R/W}"
    colorIdentity = "WR"
    typeLine = "Instant"
    oracleText = "Arrows of Justice deals 4 damage to target attacking or blocking creature."

    spell {
        val t = target(TargetFilter.AttackingOrBlockingCreature)
        effect = Effects.DealDamage(4, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "James Ryman"
        flavorText = "Archers from the Horizon Military Academy hone their skills by targeting pickpockets blocks away."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c64a15f4-6e2f-4479-95da-8805ce2091fa.jpg"
    }
}
