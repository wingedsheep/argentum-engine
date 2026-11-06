package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.GameObjectFilter

/** Trusty Companion — Kaladesh #33. */
val TrustyCompanion = card("Trusty Companion") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Hyena"
    power = 3
    toughness = 2
    oracleText = "Vigilance\nThis creature can't attack alone."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = CantAttackUnlessCoAttacker(coAttackerFilter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Jakub Kasper"
        flavorText = "It shows affection an automaton never could."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a79eefb-50a4-41c4-93cf-378fa546f539.jpg?1783937226"
        ruling("2016-09-20", "Trusty Companion can be declared as an attacker only if another creature is declared as an attacker at the same time. Once it's attacking, removing the other creature won't cause Trusty Companion to stop attacking.")
        ruling("2016-09-20", "If you control more than one creature that can't attack alone, they can attack together, even if no other creatures attack.")
        ruling("2016-09-20", "In a Two-Headed Giant game, Trusty Companion can attack along with a creature controlled by your teammate, even if no other creatures you control attack.")
    }
}
