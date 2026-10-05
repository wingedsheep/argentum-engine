package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

val Regeneration = card("Regeneration") {
    manaCost = "{1}{G}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature (Target a creature as you cast this. This card enters attached to that creature.)\n{G}: Regenerate enchanted creature. (The next time that creature would be destroyed this turn, instead tap it, remove it from combat, and heal all damage on it.)"
    colorIdentity = "G"
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    activatedAbility {
        cost = Costs.Mana("{G}")
        effect = Effects.Regenerate(EffectTarget.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "213"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7b7aa34-b4f8-41b4-82ce-ab2e204c3bf4.jpg?1783948673"
    }
}
