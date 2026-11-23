package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Debtor's Pulpit
 * {4}{W}
 * Enchantment — Aura
 * Enchant land
 * Enchanted land has "{T}: Tap target creature."
 */
val DebtorsPulpit = card("Debtor's Pulpit") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nEnchanted land has \"{T}: Tap target creature.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val creature = target(TargetFilter.Creature)
                effect = Effects.Tap(creature)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "James Paick"
        flavorText = "\"You may not be able to pay. But you can still kneel.\"\n—Milana, Orzhov prelate"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fafb0372-6860-4b0b-b92e-873735489006.jpg?1783940144"
    }
}
