package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignCombatDamageAsUnblocked

/**
 * Lone Wolf
 * {2}{G}
 * Creature — Wolf
 * 2/2
 * You may have this creature assign its combat damage as though it weren't blocked.
 */
val LoneWolf = card("Lone Wolf") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    power = 2
    toughness = 2
    oracleText = "You may have this creature assign its combat damage as though it weren't blocked."

    staticAbility {
        ability = AssignCombatDamageAsUnblocked()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Michael Weaver"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7ff4d831-7388-4321-a636-79cf7bde25bb.jpg?1783946454"
    }
}
