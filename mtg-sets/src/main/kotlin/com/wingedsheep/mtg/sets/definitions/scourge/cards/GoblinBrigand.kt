package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Goblin Brigand
 * {1}{R}
 * Creature — Goblin Warrior
 * 2/2
 * Goblin Brigand attacks each combat if able.
 */
val GoblinBrigand = card("Goblin Brigand") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin Warrior"
    power = 2
    toughness = 2
    oracleText = "Goblin Brigand attacks each combat if able."

    staticAbility {
        ability = MustAttack()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Arnie Swekel"
        flavorText = "After the Skirk Ridge collapsed, the goblins erected a system of ropes and pulleys to hold up what was left."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b024afe-7a28-4e3b-afbd-b42f1c45f338.jpg?1562528541"
    }
}
