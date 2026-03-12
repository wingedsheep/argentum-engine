package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Frenzied Rage
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+1 and has menace.
 */
val FrenziedRage = card("Frenzied Rage") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +2/+1 and has menace."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(2, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Simon Dominic"
        flavorText = "\"The heat in our hearts sometimes rises to the surface.\" —Valduk, keeper of the Flame"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9ca9c16-5720-4531-88ea-48b48d189479.jpg?1591104402"
    }
}
