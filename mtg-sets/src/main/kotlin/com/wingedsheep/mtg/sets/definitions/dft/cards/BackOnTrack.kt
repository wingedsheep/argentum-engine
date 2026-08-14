package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Back on Track — Aetherdrift #76
 * {4}{B} · Sorcery
 */
val BackOnTrack = card("Back on Track") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target creature or Vehicle card from your graveyard to the battlefield. " +
        "Create a 1/1 colorless Pilot creature token with \"This token saddles Mounts and crews " +
        "Vehicles as though its power were 2 greater.\""

    spell {
        val returned = target(
            "target creature or Vehicle card in your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.CreatureOrVehicle.ownedByYou(),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.Move(returned, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD).then(
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                creatureTypes = setOf("Pilot"),
                imageUri = "https://cards.scryfall.io/normal/front/8/6/8672d795-04f9-4089-9c92-6d6ff628da12.jpg?1783907682",
                staticAbilities = listOf(CrewSaddleContribution(modifier = 2))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Raoul Vitale"
        flavorText = "At the Tombs Before Time, the living of Amonkhet made vital alliances with the honored dead."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/884c0032-9c62-4028-a55f-6a3da2545654.jpg?1783907899"
    }
}
