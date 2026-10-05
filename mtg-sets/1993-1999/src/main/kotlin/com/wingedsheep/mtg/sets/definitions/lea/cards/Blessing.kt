package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

val Blessing = card("Blessing") {
    manaCost = "{W}{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n{W}: Enchanted creature gets +1/+1 until end of turn."
    colorIdentity = "W"
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Julie Baroh"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f131fd27-18da-47ca-b59f-135bcac83abd.jpg?1783948717"
    }
}
