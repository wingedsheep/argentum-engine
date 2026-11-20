package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Ragemonger
 * {1}{B}{R}
 * Creature — Minotaur Shaman
 * 2/3
 * Minotaur spells you cast cost {B}{R} less to cast.
 * This effect reduces only the amount of colored mana you pay.
 */
val Ragemonger = card("Ragemonger") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Minotaur Shaman"
    power = 2
    toughness = 3
    oracleText = "Minotaur spells you cast cost {B}{R} less to cast. This effect reduces only the amount of colored mana you pay. (For example, if you cast a Minotaur spell with mana cost {2}{R}, it costs {2} to cast.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype("Minotaur")),
            modification = CostModification.ReduceColored("{B}{R}"),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1eb6eda2-e3df-440a-8900-8310f11db6e9.jpg?1783939523"
    }
}
