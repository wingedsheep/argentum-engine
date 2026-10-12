package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Desynchronize
 * {4}{U}
 * Instant
 * Target nonland permanent's owner puts it on their choice of the top or bottom of their library.
 * Scry 2.
 *
 * The owner makes the top/bottom choice on resolution; the scry is the spell's controller's.
 */
val Desynchronize = card("Desynchronize") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target nonland permanent's owner puts it on their choice of the top or bottom of " +
        "their library. Scry 2."

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.PutOnTopOrBottomOfLibrary(permanent) then Effects.Scry(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Rovina Cai"
        flavorText = "Before Kaya could pull Teferi's spirit back, the overloaded temporal anchor " +
            "failed, and he was set adrift in the currents of time."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46e101e5-2b20-48dd-aac4-15a3c32a7441.jpg?1783920113"
    }
}
