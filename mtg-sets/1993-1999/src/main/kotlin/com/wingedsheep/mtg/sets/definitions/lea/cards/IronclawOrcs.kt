package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ironclaw Orcs
 * {1}{R}
 * Creature — Orc
 * 2/2
 * This creature can't block creatures with power 2 or greater.
 *
 * Expressed as its complement (it can block only creatures with power 1 or less), mirroring
 * Brassclaw Orcs. The comparison reads the attacker's projected power.
 */
val IronclawOrcs = card("Ironclaw Orcs") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc"
    oracleText = "This creature can't block creatures with power 2 or greater."
    power = 2
    toughness = 2

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(GameObjectFilter.Creature.powerAtMost(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "159"
        artist = "Anson Maddocks"
        flavorText = "Generations of genetic weeding have given rise to the deviously cowardly Ironclaw clan. To say that Orcs in general are vicious, depraved, and ignoble does not do justice to the Ironclaw."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d56421a8-34ae-4033-943f-c59a7bf2b6f9.jpg?1783948684"
    }
}
