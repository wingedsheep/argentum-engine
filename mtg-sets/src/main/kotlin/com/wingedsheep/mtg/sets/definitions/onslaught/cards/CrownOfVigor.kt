package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GrantToEnchantedCreatureTypeGroupEffect
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Crown of Vigor
 * {1}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1.
 * Sacrifice Crown of Vigor: Enchanted creature and other creatures that share
 * a creature type with it get +1/+1 until end of turn.
 */
val CrownOfVigor = card("Crown of Vigor") {
    manaCost = "{1}{G}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/+1.\nSacrifice Crown of Vigor: Enchanted creature and other creatures that share a creature type with it get +1/+1 until end of turn."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = GrantToEnchantedCreatureTypeGroupEffect(
            powerModifier = 1,
            toughnessModifier = 1
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "253"
        artist = "Greg Hildebrandt"
        flavorText = "The crown never rests easy on a languid brow."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7e320a6-88e2-4be1-97e2-30e0f3c2e450.jpg?1562950225"
    }
}
