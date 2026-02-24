package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Debilitating Injury
 * {1}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets -2/-2.
 */
val DebilitatingInjury = card("Debilitating Injury") {
    manaCost = "{1}{B}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets -2/-2."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(-2, -2, StaticTarget.AttachedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Slawomir Maniak"
        flavorText = "\"If weakness does not exist within the Temur then we shall force it upon them.\" —Sidisi, khan of the Sultai"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09b2b263-34db-4961-bf41-1eaaba3701da.jpg?1562782284"
    }
}
