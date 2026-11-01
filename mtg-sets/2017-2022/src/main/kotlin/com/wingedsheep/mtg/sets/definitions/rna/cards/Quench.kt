package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Quench — Ravnica Allegiance #48
 * {1}{U} · Instant
 */
val Quench = card("Quench") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {2}."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{2}")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Jason Rainville"
        flavorText = "\"When properly invoked, the plasma globule encloses the offensive spell with a satisfying slurp.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee0ba01b-de96-4f8f-9405-ff3ad288afac.jpg?1783933706"
    }
}
