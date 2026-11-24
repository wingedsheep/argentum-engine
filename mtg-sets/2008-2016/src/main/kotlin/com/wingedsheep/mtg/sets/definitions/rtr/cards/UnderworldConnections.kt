package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Underworld Connections
 * {1}{B}{B}
 * Enchantment — Aura
 * Enchant land
 * Enchanted land has "{T}, Pay 1 life: Draw a card."
 *
 * The granted ability belongs to the enchanted land (the land taps, not the Aura) — the
 * Squirrel Nest shape with a composite tap + life cost.
 */
val UnderworldConnections = card("Underworld Connections") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land has \"{T}, Pay 1 life: Draw a card.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Composite(Costs.Tap, Costs.PayLife(1)),
                effect = Effects.DrawCards(1)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Yeong-Hao Han"
        flavorText = "If you're looking for it, it's available. The question is how much you're willing to pay."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19c52e3b-b3b8-4243-96fe-fa4c8eea7c59.jpg?1783940358"
    }
}
