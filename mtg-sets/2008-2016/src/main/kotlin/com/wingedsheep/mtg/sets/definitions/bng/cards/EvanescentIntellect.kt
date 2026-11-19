package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Evanescent Intellect
 * {U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has "{1}{U}, {T}: Target player mills three cards."
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val EvanescentIntellect = card("Evanescent Intellect") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"{1}{U}, {T}: Target player mills three cards.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
                val player = target(Targets.Player)
                effect = Patterns.Library.mill(3, player)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Dan Murayama Scott"
        flavorText = "The mind is a vessel that may be emptied like any other."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d38cfe97-77b3-4423-95ac-88eb850cf854.jpg?1783939571"
    }
}
