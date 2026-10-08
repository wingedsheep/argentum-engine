package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fallaji Excavation
 * {3}{G}{G}
 * Sorcery
 * Create three tapped Powerstone tokens. You gain 3 life.
 */
val FallajiExcavation = card("Fallaji Excavation") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create three tapped Powerstone tokens. You gain 3 life. (The tokens are artifacts with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    spell {
        effect = Effects.CreatePowerstone(3, tapped = true) then Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Eilene Cherie"
        flavorText = "Few relics excited Fallaji archaeologists more than Thran powerstones, which they called \"eyes of the Old Ones.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac75c41f-82dc-4b06-a233-9b74ea177a3d.jpg?1783920046"
    }
}
