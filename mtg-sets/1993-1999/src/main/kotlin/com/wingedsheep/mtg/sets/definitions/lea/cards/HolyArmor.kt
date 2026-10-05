package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

val HolyArmor = card("Holy Armor") {
    manaCost = "{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +0/+2.\n{W}: Enchanted creature gets +0/+1 until end of turn."
    colorIdentity = "W"
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(0, 2)
    }

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = Effects.ModifyStats(0, 1, EffectTarget.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Melissa A. Benson"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b01041d2-687e-4972-81c8-16690809275b.jpg?1783948713"
    }
}
