package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.dsl.Effects

/**
 * Eviscerate
 * {3}{B}
 * Sorcery
 * Destroy target creature.
 */
val Eviscerate = card("Eviscerate") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature."

    spell {
        val t = target("target", TargetCreature())
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Min Yum"
        flavorText = "\"Fear the dark if you must, but don't mistake sunlight for safety.\"\n—Josu Vess"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62ba90b8-3a30-4058-b8d3-72900b1f4fe0.jpg?1562736723"
    }
}
