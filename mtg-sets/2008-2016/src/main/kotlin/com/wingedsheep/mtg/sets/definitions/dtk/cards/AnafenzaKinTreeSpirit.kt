package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Anafenza, Kin-Tree Spirit
 * {W}{W}
 * Legendary Creature — Spirit Soldier
 * 2/2
 *
 * Whenever another nontoken creature you control enters, bolster 1. (Choose a creature with
 * the least toughness among creatures you control and put a +1/+1 counter on it.)
 */
val AnafenzaKinTreeSpirit = card("Anafenza, Kin-Tree Spirit") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Spirit Soldier"
    power = 2
    toughness = 2
    oracleText = "Whenever another nontoken creature you control enters, bolster 1. " +
        "(Choose a creature with the least toughness among creatures you control and put a +1/+1 counter on it.)"

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().nontoken()).enters()
        effect = Patterns.Mechanic.bolster(1)
        description = "Whenever another nontoken creature you control enters, bolster 1."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Ryan Yee"
        flavorText = "Martyred for worshipping her ancestors, she now walks among them."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55611f05-10cf-42d0-b93e-7f0c0c852a06.jpg?1783938622"
    }
}
