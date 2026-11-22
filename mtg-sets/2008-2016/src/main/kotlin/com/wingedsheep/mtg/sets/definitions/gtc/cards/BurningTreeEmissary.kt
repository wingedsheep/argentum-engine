package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Burning-Tree Emissary
 * {R/G}{R/G}
 * Creature — Human Shaman
 * 2/2
 * When this creature enters, add {R}{G}.
 */
val BurningTreeEmissary = card("Burning-Tree Emissary") {
    manaCost = "{R/G}{R/G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Shaman"
    oracleText = "When this creature enters, add {R}{G}."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddMana(Color.RED) then Effects.AddMana(Color.GREEN)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Izzy"
        flavorText = "Those who regard the Gruul as savage simpletons underestimate the subtle power of their shamans."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/899d5f35-3613-4c69-9176-13baf442fb50.jpg"
    }
}
