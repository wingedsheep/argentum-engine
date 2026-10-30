package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Colossification
 * {5}{G}{G}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature gets +20/+20.
 */
val Colossification = card("Colossification") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhen this Aura enters, tap enchanted creature.\nEnchanted creature gets +20/+20."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = ModifyStats(20, 20)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Johan Grenier"
        flavorText = "\"Turns out the case of the flattened outpost and the case of the missing kitten *were* related.\"\n—Endris, Drannith magistrate"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b6e6f2a-5015-44c6-aa8d-85188494d1a6.jpg"
    }
}
