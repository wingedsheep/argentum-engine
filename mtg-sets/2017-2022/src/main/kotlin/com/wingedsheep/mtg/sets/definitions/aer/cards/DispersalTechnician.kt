package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dispersal Technician
 * {4}{U}
 * Creature — Vedalken Artificer
 * 3/2
 * When this creature enters, you may return target artifact to its owner's hand.
 */
val DispersalTechnician = card("Dispersal Technician") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Artificer"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you may return target artifact to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val t = target(TargetFilter.Artifact)
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Scott Murphy"
        flavorText = "As renegade forces closed in on the Aether Spire, Consulate blockades failed one by one."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d93a915-ffea-4f50-88ac-2b3253f7dfdf.jpg?1783936775"
    }
}
