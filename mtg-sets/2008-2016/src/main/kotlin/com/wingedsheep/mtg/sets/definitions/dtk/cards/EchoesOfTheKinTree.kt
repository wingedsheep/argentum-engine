package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Echoes of the Kin Tree
 * {1}{W}
 * Enchantment
 *
 * {2}{W}: Bolster 1. (Choose a creature with the least toughness among creatures you control
 * and put a +1/+1 counter on it.)
 */
val EchoesOfTheKinTree = card("Echoes of the Kin Tree") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "{2}{W}: Bolster 1. (Choose a creature with the least toughness among creatures you control " +
        "and put a +1/+1 counter on it.)"

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        effect = Patterns.Mechanic.bolster(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Ryan Alexander Lee"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/531414e6-ea72-4934-b05e-e1782249fd1f.jpg?1783938618"
    }
}
