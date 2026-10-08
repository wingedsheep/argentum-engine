package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Powerstone Engineer
 * {1}{W}
 * Creature — Human Artificer
 * 2/1
 * When this creature dies, create a tapped Powerstone token.
 */
val PowerstoneEngineer = card("Powerstone Engineer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 1
    oracleText = "When this creature dies, create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreatePowerstone(tapped = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Scott Murphy"
        flavorText = "\"Well there's your problem—resonance leak lowering output by thirty percent. I can patch that up.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2901c5f6-5d83-437d-b434-d9beb115dd82.jpg?1783920128"
    }
}
