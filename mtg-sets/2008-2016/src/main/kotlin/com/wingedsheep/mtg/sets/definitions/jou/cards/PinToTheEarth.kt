package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Pin to the Earth
 * {1}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets -6/-0.
 *
 * A flat -6/-0 over [GroupFilter.attachedCreature] — the same shape as Torpor Dust. Toughness is
 * untouched, so the creature survives; it just stops dealing meaningful damage.
 */
val PinToTheEarth = card("Pin to the Earth") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets -6/-0."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(-6, 0, GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Johann Bodin"
        flavorText = "\"Mortal strength, like mortal hope, is so easily crushed.\"\n—Thassa, god of the sea"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c04e6915-39b0-4fbc-9114-2ecf371b6f72.jpg?1783939446"
    }
}
