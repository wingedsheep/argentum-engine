package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Agoraphobia
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets -5/-0.
 * {2}{U}: Return this Aura to its owner's hand.
 */
val Agoraphobia = card("Agoraphobia") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets -5/-0.\n{2}{U}: Return this Aura to its owner's hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(-5, 0, Filters.EnchantedCreature)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Jim Murray"
        flavorText = "\"Everyone in this city is choosing sides. I choose inside.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1a3efab-ee0a-4770-a323-e4bac38e4287.jpg"
    }
}
