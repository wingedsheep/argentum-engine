package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReduceEquipCost

/**
 * Dwarven Mauler — The Hobbit #95
 * {R} · Creature — Dwarf Warrior · Uncommon
 * 2/1
 *
 * Equip abilities you activate that target this creature cost {2} less to activate.
 *
 * The target-restricted form of the controller-scoped [ReduceEquipCost] (Cloud, Planet's Champion):
 * with `onlyIfTargetIsSource` the {2} reduction applies only to equip abilities whose chosen target
 * is this creature, and only to the generic portion of the cost, floored at {0}.
 */
val DwarvenMauler = card("Dwarven Mauler") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Warrior"
    power = 2
    toughness = 1
    oracleText = "Equip abilities you activate that target this creature cost {2} less to activate."

    staticAbility {
        ability = ReduceEquipCost(amount = 2, onlyIfTargetIsSource = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Nathaniel Himawan"
        flavorText = "There rose from across the valley a deep-throated roar. With cries of " +
            "\"Moria!\" and \"Dáin, Dáin!\" the Dwarves of the Iron Hills plunged in."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd0f0415-43af-4f5d-8999-853c5d42780d.jpg?1784895019"
    }
}
