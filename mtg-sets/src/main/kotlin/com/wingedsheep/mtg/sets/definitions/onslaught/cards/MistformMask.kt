package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mistform Mask
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * {1}: Enchanted creature becomes the creature type of your choice until end of turn.
 */
val MistformMask = card("Mistform Mask") {
    manaCost = "{1}{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n{1}: Enchanted creature becomes the creature type of your choice until end of turn."

    auraTarget = Targets.Creature

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = BecomeCreatureTypeEffect(
            target = EffectTarget.EnchantedCreature
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Monte Michael Moore"
        flavorText = "\"Trust, the fifth myth of reality: Every truth holds the seed of betrayal.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fbbb075-5795-425f-9e33-70cb922eea16.jpg?1562920869"
    }
}
