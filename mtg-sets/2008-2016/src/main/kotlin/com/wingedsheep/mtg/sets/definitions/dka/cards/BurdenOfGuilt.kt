package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Burden of Guilt
 * {W}
 * Enchantment — Aura
 * Enchant creature
 * {1}: Tap enchanted creature.
 */
val BurdenOfGuilt = card("Burden of Guilt") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n{1}: Tap enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
        description = "{1}: Tap enchanted creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "John Stanko"
        flavorText = "\"Grab an axe and defend the gate! Your despair is an extravagance we can ill afford.\"\n—Thalia, Knight-Cathar"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7440288-6c55-4502-bf20-3c5b50a2de5a.jpg"
    }
}
