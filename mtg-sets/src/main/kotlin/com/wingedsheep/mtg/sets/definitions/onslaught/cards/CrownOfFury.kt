package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.GrantToEnchantedCreatureTypeGroupEffect
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Crown of Fury
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+0 and has first strike.
 * Sacrifice Crown of Fury: Enchanted creature and other creatures that share
 * a creature type with it get +1/+0 and gain first strike until end of turn.
 */
val CrownOfFury = card("Crown of Fury") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/+0 and has first strike.\nSacrifice Crown of Fury: Enchanted creature and other creatures that share a creature type with it get +1/+0 and gain first strike until end of turn."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(1, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE)
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = GrantToEnchantedCreatureTypeGroupEffect(
            powerModifier = 1,
            toughnessModifier = 0,
            keyword = Keyword.FIRST_STRIKE
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Kev Walker"
        flavorText = "The crown never rests easy on a peaceful brow."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6caae974-f531-469d-8c6a-2077c4f3294a.jpg?1562920593"
    }
}
