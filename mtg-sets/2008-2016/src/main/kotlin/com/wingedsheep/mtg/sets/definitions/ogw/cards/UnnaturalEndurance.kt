package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unnatural Endurance
 * {B}
 * Instant
 * Devoid (This card has no color.)
 * Target creature gets +2/+0 until end of turn. Regenerate it.
 */
val UnnaturalEndurance = card("Unnatural Endurance") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Target creature gets +2/+0 until end of turn. Regenerate it."

    keywords(Keyword.DEVOID)

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, creature) then
            Effects.Regenerate(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Mathias Kollros"
        flavorText = "\"Eldrazi don't die easy. Sometimes they don't die at all.\"\n—Yon Basrel, Oran-Rief survivalist"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac497133-a44a-4acc-ba7e-8fc1fae7e226.jpg"
    }
}
