package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignCombatDamageAsUnblocked

/**
 * Deathcoil Wurm
 * {6}{G}{G}
 * Creature — Wurm
 * 7/6
 * You may have this creature assign its combat damage as though it weren't blocked.
 */
val DeathcoilWurm = card("Deathcoil Wurm") {
    manaCost = "{6}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 7
    toughness = 6
    oracleText = "You may have this creature assign its combat damage as though it weren't blocked."

    staticAbility {
        ability = AssignCombatDamageAsUnblocked()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "Rebecca Guay"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c17e3b5-d609-4289-9b74-5ac96ab4ccfa.jpg?1783946457"
    }
}
