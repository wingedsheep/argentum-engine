package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.TapUntapEffect

/**
 * Inspirit
 * {2}{W}
 * Instant
 * Untap target creature. It gets +2/+4 until end of turn.
 */
val Inspirit = card("Inspirit") {
    manaCost = "{2}{W}"
    typeLine = "Instant"

    spell {
        target = com.wingedsheep.sdk.targeting.TargetCreature()
        effect = TapUntapEffect(EffectTarget.ContextTarget(0), tap = false) then
                ModifyStatsEffect(2, 4, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Pete Venters"
        flavorText = "\"You're not done yet.\"\n—Akroma, to Kamahl"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/228d3985-e498-4e06-9cb1-c3a4b7b13e3a.jpg?1562902598"
    }
}
