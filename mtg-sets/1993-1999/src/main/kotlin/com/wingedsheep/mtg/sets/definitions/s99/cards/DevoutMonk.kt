package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val DevoutMonk = card("Devout Monk") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Monk Cleric"
    oracleText = "When this creature enters, you gain 1 life."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Daniel Gelon"
        flavorText = "Discipline wears many robes."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd1101f5-0bc1-47fa-891b-206b9c1c7f79.jpg?1783946049"
    }
}
