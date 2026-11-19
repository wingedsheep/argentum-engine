package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Oracle's Insight
 * {3}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has "{T}: Scry 1, then draw a card." (To scry 1, look at the top card of
 * your library, then you may put that card on the bottom.)
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val OraclesInsight = card("Oracle's Insight") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"{T}: Scry 1, then draw a card.\" " +
        "(To scry 1, look at the top card of your library, then you may put that card on the bottom.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                effect = Patterns.Library.scry(1) then Effects.DrawCards(1)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/546dae51-070c-4f3f-b0b4-6b88ff314af1.jpg?1783939567"
    }
}
