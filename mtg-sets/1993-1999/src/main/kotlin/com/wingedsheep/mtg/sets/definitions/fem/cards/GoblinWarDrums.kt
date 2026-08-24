package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Goblin War Drums
 * {2}{R}
 * Enchantment
 * Creatures you control have menace.
 *
 * Printed as "All creatures you control gain the ability 'This creature can't be blocked except
 * by two or more creatures'"; the modern Oracle wording is plain menace (CR 702.111).
 */
val GoblinWarDrums = card("Goblin War Drums") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Creatures you control have menace. (They can't be blocked except by two or more creatures.)"

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, GroupFilter(GameObjectFilter.Creature.youControl()))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58a"
        artist = "Dan Frazier"
        flavorText = "\"The Goblins' dreaded War Drums struck terror into the hearts of even their bravest foes.\"\n—*Sarpadian Empires, vol. IV*"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a2c4e4b-e9a7-4180-927b-589514c21876.jpg?1783947893"
    }
}
