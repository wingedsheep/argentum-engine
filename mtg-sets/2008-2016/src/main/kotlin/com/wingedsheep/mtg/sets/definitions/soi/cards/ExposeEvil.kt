package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Expose Evil (Shadows over Innistrad #19)
 * {1}{W}
 * Instant
 *
 * Tap up to two target creatures.
 * Investigate. (Create a Clue token. It's an artifact with "{2}, Sacrifice this token: Draw a card.")
 *
 * "Up to two" is one optional requirement taking zero to two creatures, so the spell can be cast with
 * no targets just to investigate; with at least one target, it fizzles (and doesn't investigate) if
 * every target became illegal (rulings).
 */
val ExposeEvil = card("Expose Evil") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Tap up to two target creatures.\nInvestigate. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.TapEachTarget() then Effects.Investigate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Jama Jurabaev"
        flavorText = "\"Only the pure stand tall in the light.\"\n—Sigarda"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9de0341a-cd45-439c-adc2-8d24c47c4bd5.jpg?1783937818"
        ruling("2016-04-08", "You can cast Expose Evil with no targets if you only want to investigate.")
        ruling("2016-04-08", "If Expose Evil has at least one target and all of those targets become illegal, the spell doesn't resolve and you won't investigate.")
    }
}
