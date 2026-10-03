package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleCharacteristic
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.CrewSaddleCost

/**
 * Giant Ox — Kaldheim #11 (canonical printing; reprinted in J22)
 * {1}{W} · Creature — Ox · 0/6
 *
 * This creature crews Vehicles using its toughness rather than its power.
 *
 * Crew only — the Ox saddles a Mount with its (zero) power.
 */
val GiantOx = card("Giant Ox") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Ox"
    power = 0
    toughness = 6
    oracleText = "This creature crews Vehicles using its toughness rather than its power."

    staticAbility {
        ability = CrewSaddleContribution(
            characteristic = CrewSaddleCharacteristic.TOUGHNESS,
            costs = setOf(CrewSaddleCost.CREW)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Joe Slucher"
        flavorText = "\"How strong is he? Well, I once lost control of the plow, and he carved a furrow right " +
            "through my house! Didn't even slow down.\"\n—Guldir, Beskir farmer"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc71d8ca-c613-4534-bc9d-bc1e13202a2c.jpg?1783928285"
    }
}
