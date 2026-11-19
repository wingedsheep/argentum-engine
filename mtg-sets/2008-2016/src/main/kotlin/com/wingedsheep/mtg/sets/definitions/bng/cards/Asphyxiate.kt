package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Asphyxiate
 * {1}{B}{B}
 * Sorcery
 *
 * Destroy target untapped creature.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val Asphyxiate = card("Asphyxiate") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target untapped creature."

    spell {
        val creature = target(TargetFilter.UntappedCreature)
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Kev Walker"
        flavorText = "\"Some poisons enter through the blood. Some are ingested. Some are inhaled. All ways through which mortals draw strength are paths for poison.\"\n—Agathe, priest of Pharika"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/894f3f5f-586d-45e4-9af7-4de80e44dfae.jpg?1783939560"
    }
}
