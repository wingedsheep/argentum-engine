package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Goblin Chieftain
 * {1}{R}{R}
 * Creature — Goblin
 * 2/2
 *
 * Haste
 * Other Goblin creatures you control get +1/+1 and have haste.
 */
val GoblinChieftain = card("Goblin Chieftain") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "Haste (This creature can attack and {T} as soon as it comes under your control.)\n" +
        "Other Goblin creatures you control get +1/+1 and have haste."
    power = 2
    toughness = 2

    keywords(Keyword.HASTE)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN).youControl(),
                excludeSelf = true,
            ),
        )
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.HASTE,
            GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN).youControl(),
                excludeSelf = true,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "Sam Wood"
        flavorText = "\"We are goblinkind, heirs to the mountain empires of chieftains past. Rest is death " +
            "to us, and arson is our call to war.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5c8a4a4-1611-4188-9c59-8aefb016b5ad.jpg?1783942372"
    }
}
