package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Go for the Throat
 * {1}{B}
 * Instant
 * Destroy target nonartifact creature.
 *
 * Canonical printing: Mirrodin Besieged (MBS) — the earliest real expansion printing.
 * The Brothers' War (BRO) reprint contributes only a `Printing` row.
 */
val GoForTheThroat = card("Go for the Throat") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target nonartifact creature."

    spell {
        val t = target(TargetFilter.Creature.nonartifact())
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "David Rapoza"
        flavorText = "Having flesh is increasingly a liability on Mirrodin."
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c665cfc-7e9a-444b-96b5-e8e4ef57a98a.jpg?1783941384"
    }
}
