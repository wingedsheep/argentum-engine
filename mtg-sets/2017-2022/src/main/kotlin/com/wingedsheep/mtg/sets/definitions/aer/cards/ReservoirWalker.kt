package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Reservoir Walker
 * {5}
 * Artifact Creature — Construct
 * 3/3
 * When this creature enters, you gain 3 life and get {E}{E}{E} (three energy counters).
 */
val ReservoirWalker = card("Reservoir Walker") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you gain 3 life and get {E}{E}{E} (three energy counters)."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3) then Effects.GetEnergy(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Johann Bodin"
        flavorText = "Renegade supply lines are, by necessity, mobile and difficult to disrupt."
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8afe3be3-b0bc-4617-b4de-f60d14f1b91d.jpg?1783936720"
    }
}
