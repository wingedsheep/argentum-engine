package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sea God's Scorn
 * {4}{U}{U}
 * Sorcery
 * Return up to three target creatures and/or enchantments to their owners' hands.
 *
 * Into the Void's "up to N target" bounce (one optional multi-target requirement, the body run
 * per chosen target via [Effects.ForEachTarget]) widened to three and to a creature-or-enchantment
 * filter, as Force of Vigor does for artifacts and/or enchantments.
 */
val SeaGodsScorn = card("Sea God's Scorn") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return up to three target creatures and/or enchantments to their owners' hands."

    spell {
        targets(
            TargetFilter(GameObjectFilter.Creature or GameObjectFilter.Enchantment),
            count = 3,
            optional = true,
        )
        effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Zoltan Boros"
        flavorText = "Instead of performing their daily devotions, Heliod's acolytes spent the morning " +
            "picking strands of kelp off the altar."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9504dc26-f5d8-4b9d-9eb1-51a12b893beb.jpg?1783931579"
    }
}
