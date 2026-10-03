package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Weight of Conscience
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't attack.
 * Tap two untapped creatures you control that share a creature type: Exile enchanted creature.
 */
val WeightOfConscience = card("Weight of Conscience") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack.\n" +
        "Tap two untapped creatures you control that share a creature type: Exile enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    activatedAbility {
        cost = Costs.TapPermanents(2, sharedCreatureType = true)
        effect = Effects.Exile(EffectTarget.EnchantedCreature)
        description = "Tap two untapped creatures you control that share a creature type: Exile enchanted creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Heather Hudson"
        flavorText = "Sometimes the weight of the world on your shoulders is a literal one."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/396c8c41-70b4-4189-9160-8b7367a817a2.jpg?1783942802"
    }
}
