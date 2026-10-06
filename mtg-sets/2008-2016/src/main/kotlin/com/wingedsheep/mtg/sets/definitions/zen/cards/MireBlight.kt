package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Mire Blight
 * {B}
 * Enchantment — Aura
 * Enchant creature
 * When enchanted creature is dealt damage, destroy it.
 */
val MireBlight = card("Mire Blight") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhen enchanted creature is dealt damage, destroy it."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.isDealtDamage()
        effect = Effects.Destroy(EffectTarget.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Dave Kendall"
        flavorText = "\"Is there anything in Guul Draz that *doesn't* suck the life out of you?\"\n—Tarsa, Sea Gate sell-sword"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e63238d-a022-4ea1-a83d-0b2260d56a17.jpg?1783942150"
    }
}
