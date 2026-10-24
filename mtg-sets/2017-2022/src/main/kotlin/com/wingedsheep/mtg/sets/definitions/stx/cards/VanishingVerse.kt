package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vanishing Verse
 * {W}{B}
 * Instant
 *
 * Exile target monocolored permanent.
 *
 * The monocolored restriction lives in the target filter, read from projected colours, so a
 * permanent that becomes colorless or multicolored in response is no longer a legal target.
 */
val VanishingVerse = card("Vanishing Verse") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "Exile target monocolored permanent."

    spell {
        val permanent = target(TargetFilter.Permanent.monocolored())
        effect = Effects.Exile(permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Chris Seaman"
        flavorText = "\"Whoever summoned that thing had better confess right this moment, or the whole class " +
            "will be cleaning the ink receptacles for a month!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e12573fe-aee3-49db-93e2-d00972cbb3b1.jpg?1783927288"
        ruling("2024-04-12", "A monocolored permanent is exactly one color. Colorless permanents aren't monocolored.")
    }
}
