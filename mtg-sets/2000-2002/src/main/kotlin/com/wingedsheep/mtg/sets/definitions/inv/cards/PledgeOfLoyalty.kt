package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantProtectionFromControlledColors
import com.wingedsheep.sdk.scripting.ProtectionSpares
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Pledge of Loyalty
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has protection from the colors of permanents you control. This effect doesn't
 * remove this Aura.
 *
 * The protection set is board-derived: [GrantProtectionFromControlledColors] reads the projected
 * colors of every permanent the Aura's controller controls (after Layer 5) and grants the enchanted
 * creature protection from each. Because the white Aura is itself a permanent you control, the
 * enchanted creature gains protection from white; [ProtectionSpares.SELF] keeps that protection from
 * removing this Aura, while other Auras of those colors still fall off (CR 704.5m).
 */
val PledgeOfLoyalty = card("Pledge of Loyalty") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has protection from the colors of permanents you control. This effect " +
        "doesn't remove this Aura."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantProtectionFromControlledColors(spares = ProtectionSpares.SELF)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "24"
        artist = "Franz Vohwinkel"
        flavorText = "Urza convinced many Dominarians not only to set aside their differences, but to embrace them."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6f98c26-5b30-400c-8af1-8c6c43065f63.jpg?1562938145"
    }
}
