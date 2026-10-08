package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Powerstone Fracture
 * {1}{B}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice an artifact or creature.
 * Destroy target creature or planeswalker.
 */
val PowerstoneFracture = card("Powerstone Fracture") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or creature.\n" +
        "Destroy target creature or planeswalker."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Artifact or GameObjectFilter.Creature))

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Campbell White"
        flavorText = "\"Move! When the powerstone blows, that behemoth will be nothing but a hole in the ground.\"\n—Ashnod, to Hajar"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2323301f-565a-4c0f-bffd-18386ccd1f7a.jpg"
    }
}
