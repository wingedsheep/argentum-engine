package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Onulet
 * {3}
 * Artifact Creature — Construct
 * 2/2
 * When this creature dies, you gain 2 life.
 */
val Onulet = card("Onulet") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 2
    oracleText = "When this creature dies, you gain 2 life."

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Anson Maddocks"
        flavorText = "An early inspiration for Urza, Tocasia's Onulets contained magical essences that could be cannibalized after they stopped functioning."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d77fe8e2-8438-473e-ace5-01baddd2c4ed.jpg?1562940572"
    }
}
