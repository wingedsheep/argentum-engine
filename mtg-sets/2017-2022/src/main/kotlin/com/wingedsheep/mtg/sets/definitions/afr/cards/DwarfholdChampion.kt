package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dwarfhold Champion
 * {1}{W}
 * Creature — Dwarf Warrior
 * 3/1
 * As long as this creature is equipped, it gets +0/+2.
 *
 * A self-scoped stat bump behind the same "is equipped" gate as Skyhunter Cub
 * ([Conditions.SourceMatches] over `GameObjectFilter.Any.equipped()`).
 */
val DwarfholdChampion = card("Dwarfhold Champion") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Warrior"
    power = 3
    toughness = 1
    oracleText = "As long as this creature is equipped, it gets +0/+2."

    staticAbility {
        ability = ModifyStats(powerBonus = 0, toughnessBonus = 2, filter = GroupFilter.source())
        condition = Conditions.SourceMatches(GameObjectFilter.Any.equipped())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Miguel Mercado"
        flavorText = "Courage is neither a shield nor an axe, but the will to wield both in defense of the innocent."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2404a028-05f5-469b-80e0-e820721ff266.jpg?1783926533"
    }
}
