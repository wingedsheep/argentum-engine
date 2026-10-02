package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Serpent Skin
 * {2}{G}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * Enchanted creature gets +1/+1.
 * {G}: Regenerate enchanted creature.
 *
 * The regeneration ability belongs to the Aura (Thrull Retainer's shape, paid with {G} instead of a
 * sacrifice), so it can be activated any number of times while Serpent Skin stays attached.
 */
val SerpentSkin = card("Serpent Skin") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature\nEnchanted creature gets +1/+1.\n{G}: Regenerate enchanted creature."
    keywords(Keyword.FLASH)
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    activatedAbility {
        cost = Costs.Mana("{G}")
        effect = Effects.Regenerate(EffectTarget.EnchantedPermanent)
        description = "{G}: Regenerate enchanted creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "240"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c5722d9-d1a4-4ad2-bf85-db666d4a30d9.jpg?1783944283"
    }
}
