package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignUnblockedCombatDamageToDefendingCreature

/**
 * Cunning Giant
 * {5}{R}
 * Creature — Giant
 * 4/4
 * If this creature is unblocked, you may have it assign its combat damage to a creature defending
 * player controls.
 */
val CunningGiant = card("Cunning Giant") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 4
    toughness = 4
    oracleText = "If this creature is unblocked, you may have it assign its combat damage to a creature defending player controls."

    staticAbility {
        ability = AssignUnblockedCombatDamageToDefendingCreature()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Jeffrey R. Busch"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0aa284a7-3aac-4e88-becb-548a28c77401.jpg?1783946468"
    }
}
