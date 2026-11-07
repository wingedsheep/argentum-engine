package com.wingedsheep.mtg.sets.definitions.gs1.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Purple-Crystal Crab
 * {1}{U}
 * Creature — Crab
 * 1/1
 * When this creature dies, draw a card.
 */
val PurpleCrystalCrab = card("Purple-Crystal Crab") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Crab"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, draw a card."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(1)
        description = "When this creature dies, draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Tan Yan Yao"
        flavorText = "A precious shell without, a delicious taste within."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e26c576d-94c8-4f63-9f54-732fb1eade12.jpg?1783934637"
    }
}
