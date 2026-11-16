package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sandsteppe Scavenger
 * {4}{G}
 * Creature — Dog Scout
 * 2/2
 *
 * When this creature enters, bolster 2. (Choose a creature with the least toughness among
 * creatures you control and put two +1/+1 counters on it.)
 */
val SandsteppeScavenger = card("Sandsteppe Scavenger") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dog Scout"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, bolster 2. " +
        "(Choose a creature with the least toughness among creatures you control and put two +1/+1 counters on it.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Mechanic.bolster(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "200"
        artist = "Kieran Yanner"
        flavorText = "\"Sad what passes for a dragon among the Silumgar.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/6/861f215b-4f3b-4aca-9664-3ef9a0ddea1a.jpg?1783938577"
    }
}
