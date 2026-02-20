package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GrantToEnchantedCreatureTypeGroupEffect
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Crown of Suspicion
 * {1}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/-1.
 * Sacrifice Crown of Suspicion: Enchanted creature and other creatures that share
 * a creature type with it get +2/-1 until end of turn.
 */
val CrownOfSuspicion = card("Crown of Suspicion") {
    manaCost = "{1}{B}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +2/-1.\nSacrifice Crown of Suspicion: Enchanted creature and other creatures that share a creature type with it get +2/-1 until end of turn."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(2, -1)
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = GrantToEnchantedCreatureTypeGroupEffect(
            powerModifier = 2,
            toughnessModifier = -1
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Wayne England"
        flavorText = "Darkness, hide my fear."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8953e11b-cc3a-4c8d-9d7e-04bf90c77027.jpg?1562927415"
    }
}
