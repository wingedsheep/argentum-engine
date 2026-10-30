package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Garrison Cat
 * {W}
 * Creature — Cat
 * 1/1
 * When this creature dies, create a 1/1 white Human Soldier creature token.
 */
val GarrisonCat = card("Garrison Cat") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, create a 1/1 white Human Soldier creature token."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/9/0/90de6d1e-e654-4f4a-8014-061ce69e540f.jpg?1783930948",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Sidharth Chaturvedi"
        flavorText = "After much arguing, the soldiers decided to keep it, on the grounds that it killed enough mice to count as an honorary monster slayer."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7ff8345-227c-43b4-bed5-af3a34c0a990.jpg"
    }
}
