package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Primal Visitation
 * {3}{R}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +3/+3 and has haste.
 */
val PrimalVisitation = card("Primal Visitation") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +3/+3 and has haste."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(3, 3)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "187"
        artist = "Christopher Moeller"
        flavorText = "Gruul shamans believe all sentient beings have the soul of savagery inside them, an animal form clawing to escape."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2dd518e8-047a-4df0-a0b8-ba116d048fa8.jpg"
    }
}
