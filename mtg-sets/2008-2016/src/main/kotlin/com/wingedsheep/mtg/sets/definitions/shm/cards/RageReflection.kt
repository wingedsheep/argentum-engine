package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Rage Reflection
 * {4}{R}{R}
 * Enchantment
 *
 * Creatures you control have double strike.
 *
 * - A single [GrantKeyword] over every creature you control. No `excludeSelf` — Rage Reflection is
 *   an enchantment, so it never matches the creature filter anyway.
 * - The filter is evaluated continuously against projected state, so creatures that enter after
 *   the enchantment (or that change controller onto your side) pick double strike up immediately.
 */
val RageReflection = card("Rage Reflection") {
    manaCost = "{4}{R}{R}"
    typeLine = "Enchantment"
    oracleText = "Creatures you control have double strike."

    staticAbility {
        ability = GrantKeyword(
            Keyword.DOUBLE_STRIKE,
            GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Terese Nielsen & Ron Spencer"
        flavorText = "Vengeance is a dish best served twice."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/275e118d-1d50-47ee-a2c9-76169ce372cf.jpg?1783942746"
    }
}
