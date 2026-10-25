package com.wingedsheep.mtg.sets.definitions.ddj.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Goblin Electromancer
 * {U}{R}
 * Creature — Goblin Wizard
 * 2/2
 * Instant and sorcery spells you cast cost {1} less to cast.
 */
val GoblinElectromancer = card("Goblin Electromancer") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Goblin Wizard"
    power = 2
    toughness = 2
    oracleText = "Instant and sorcery spells you cast cost {1} less to cast."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Svetlin Velinov"
        flavorText = "When asked how much power is required, Izzet mages always answer \"more.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/3/632af23c-eb5c-4725-afca-637e84cecf94.jpg?1783940425"
        ruling("2018-10-05", "Goblin Electromancer's effect reduces only generic mana in the spell's total cost.")
        ruling("2017-03-14", "Two Goblin Electromancers will make instant and sorcery spells you cast cost {2} less to cast, and so on.")
    }
}
