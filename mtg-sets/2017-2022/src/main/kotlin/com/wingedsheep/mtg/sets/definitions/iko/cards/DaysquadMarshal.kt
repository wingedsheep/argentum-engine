package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Daysquad Marshal
 * {3}{W}
 * Creature — Human Soldier
 * 3/3
 * When this creature enters, create a 1/1 white Human Soldier creature token.
 */
val DaysquadMarshal = card("Daysquad Marshal") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, create a 1/1 white Human Soldier creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
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
        collectorNumber = "8"
        artist = "Micah Epstein"
        flavorText = "\"The night emboldens many monsters, but the mightiest will attack in broad daylight.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17d566bd-f272-49d1-bffb-588f2a42046a.jpg"
    }
}
