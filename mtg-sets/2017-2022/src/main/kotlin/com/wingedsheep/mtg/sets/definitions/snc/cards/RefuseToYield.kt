package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Refuse to Yield
 * {1}{W}
 * Instant
 * Target creature gets +2/+7 until end of turn. Untap it.
 */
val RefuseToYield = card("Refuse to Yield") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+7 until end of turn. Untap it."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 7, creature) then Effects.Untap(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Josh Hass"
        flavorText = "Her opponent threw her through the window, so she retaliated by throwing the window through her opponent."
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7b597b3-5792-49e4-9bcb-74012e4e9cb6.jpg"
    }
}
