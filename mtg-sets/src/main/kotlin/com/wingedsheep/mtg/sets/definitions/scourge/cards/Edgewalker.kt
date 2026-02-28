package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReduceSpellColoredCostBySubtype

/**
 * Edgewalker
 * {1}{W}{B}
 * Creature — Human Cleric
 * 2/2
 * Cleric spells you cast cost {W}{B} less to cast.
 * This effect reduces only the amount of colored mana you pay.
 */
val Edgewalker = card("Edgewalker") {
    manaCost = "{1}{W}{B}"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Cleric spells you cast cost {W}{B} less to cast. This effect reduces only the amount of colored mana you pay. (For example, if you cast a Cleric spell with mana cost {1}{W}, it costs {1} to cast.)"

    staticAbility {
        ability = ReduceSpellColoredCostBySubtype(
            subtype = "Cleric",
            manaReduction = "{W}{B}"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Ben Thompson"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8b477c2-2cd5-41f2-8754-d4d5000df58d.jpg?1562534693"
    }
}
