package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GlobalEffect
import com.wingedsheep.sdk.scripting.GlobalEffectType

/**
 * Grand Melee
 * {3}{R}
 * Enchantment
 * All creatures attack each combat if able.
 * All creatures block each combat if able.
 */
val GrandMelee = card("Grand Melee") {
    manaCost = "{3}{R}"
    typeLine = "Enchantment"
    oracleText = "All creatures attack each combat if able.\nAll creatures block each combat if able."

    staticAbility {
        ability = GlobalEffect(GlobalEffectType.ALL_CREATURES_MUST_ATTACK)
    }

    staticAbility {
        ability = GlobalEffect(GlobalEffectType.ALL_CREATURES_MUST_BLOCK)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Pete Venters"
        flavorText = "In the end, it's not about the prey. It's about the hunt."
        imageUri = "https://cards.scryfall.io/large/front/9/a/9a0d3142-4224-4b51-885d-33c8938418c1.jpg?1562927541"
    }
}
