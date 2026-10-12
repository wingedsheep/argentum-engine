package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/** Whenever another creature you control dies, create a Treasure token. */
val PitilessPlunderer = card("Pitiless Plunderer") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Pirate"
    power = 1
    toughness = 4
    oracleText = "Whenever another creature you control dies, create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "David Palumbo"
        flavorText = "\"Shame to let good gold go to the grave.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c87ebffe-5907-427b-9f9b-7c36a12b4a03.jpg?1783935307"
        ruling(
            "2018-01-19",
            "If Pitiless Plunderer dies at the same time as one or more other creatures you control, " +
                "its ability will still trigger for each of those other creatures."
        )
    }
}
