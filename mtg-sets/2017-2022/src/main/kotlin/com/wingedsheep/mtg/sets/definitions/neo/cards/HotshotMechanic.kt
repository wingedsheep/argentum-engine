package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.CrewSaddleCost

/**
 * Hotshot Mechanic — Kamigawa: Neon Dynasty #16 (canonical printing; reprinted in J22)
 * {W} · Artifact Creature — Fox Pilot · 2/1
 *
 * This creature crews Vehicles as though its power were 2 greater.
 *
 * Crew only — the Mechanic saddles a Mount with its printed power.
 */
val HotshotMechanic = card("Hotshot Mechanic") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Fox Pilot"
    power = 2
    toughness = 1
    oracleText = "This creature crews Vehicles as though its power were 2 greater."

    staticAbility {
        ability = CrewSaddleContribution(modifier = 2, costs = setOf(CrewSaddleCost.CREW))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Julio Reyna"
        flavorText = "\"If your mech is factory standard, you might as well be walking.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a70e8fa-b71d-441e-b049-dacb09a9a7af.jpg?1783923922"
    }
}
