package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Epiphany Storm
 * {R}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has "{R}, {T}, Discard a card: Draw a card."
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val EpiphanyStorm = card("Epiphany Storm") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"{R}, {T}, Discard a card: Draw a card.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap, Costs.DiscardCard)
                effect = Effects.DrawCards(1)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Clint Cearley"
        flavorText = "In Meletis, certain oracles are trained to be divine lightning rods, receiving countless visions in an instant."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd3a0128-4499-498d-9721-b1fd70ea84b4.jpg?1783939549"
    }
}
