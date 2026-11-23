package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantProtectionFromToGroup
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Guildscorn Ward
 * {W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has protection from multicolored.
 */
val GuildscornWard = card("Guildscorn Ward") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has protection from multicolored."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantProtectionFromToGroup(ProtectionScope.Multicolored)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Ryan Barger"
        flavorText = "The Gateless have risen. The Ten shall fall.\n—Ninth District graffiti"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89c5c496-0a3e-40e1-84ac-8ad3a9d8352b.jpg"
    }
}
