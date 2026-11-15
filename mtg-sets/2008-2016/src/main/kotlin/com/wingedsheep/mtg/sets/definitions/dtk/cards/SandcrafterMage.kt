package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sandcrafter Mage
 * {2}{W}
 * Creature — Human Wizard
 * 2/2
 *
 * When this creature enters, bolster 1. (Choose a creature with the least toughness among
 * creatures you control and put a +1/+1 counter on it.)
 */
val SandcrafterMage = card("Sandcrafter Mage") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, bolster 1. " +
        "(Choose a creature with the least toughness among creatures you control and put a +1/+1 counter on it.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Mechanic.bolster(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Willian Murai"
        flavorText = "With heat, sand can form a delicate work of art; with pressure, an impenetrable bulwark."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8998b4a5-d94b-45b3-b619-bd850e240ce0.jpg?1783938613"
    }
}
