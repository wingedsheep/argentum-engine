package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignCombatDamageAsUnblocked

/**
 * Pride of Lions
 * {3}{G}{G}
 * Creature — Cat
 * 4/4
 * You may have this creature assign its combat damage as though it weren't blocked.
 */
val PrideOfLions = card("Pride of Lions") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 4
    toughness = 4
    oracleText = "You may have this creature assign its combat damage as though it weren't blocked."

    staticAbility {
        ability = AssignCombatDamageAsUnblocked()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5006984-8e3d-4f13-b12e-1fbecd134bb3.jpg?1783946021"
    }
}
