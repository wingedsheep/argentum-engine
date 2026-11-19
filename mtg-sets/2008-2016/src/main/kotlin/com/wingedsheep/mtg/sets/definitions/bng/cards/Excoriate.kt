package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Excoriate
 * {3}{W}
 * Sorcery
 *
 * Exile target tapped creature.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val Excoriate = card("Excoriate") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile target tapped creature."

    spell {
        val t = target(TargetFilter.TappedCreature)
        effect = Effects.Exile(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Johann Bodin"
        flavorText = "The demon had flown past the reach of Erebos's whip but not the point of the sun god's spear."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d5023a5-b77c-48c6-9e84-ef525300ecdf.jpg?1783939582"
    }
}
