package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sea Legs
 * {U}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * Enchanted creature gets +0/+2 as long as it's a Pirate. Otherwise, it gets -2/-0.
 */
val SeaLegs = card("Sea Legs") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature\n" +
        "Enchanted creature gets +0/+2 as long as it's a Pirate. Otherwise, it gets -2/-0."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(0, 2, GroupFilter.attachedCreature())
        condition = EnchantedCreatureHasSubtype(Subtype("Pirate"))
    }

    staticAbility {
        ability = ModifyStats(-2, 0, GroupFilter.attachedCreature())
        condition = Conditions.Not(EnchantedCreatureHasSubtype(Subtype("Pirate")))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Deruchenko Alexander"
        flavorText = "\"When the waves pick up, either you find your feet or you lose your lunch.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae48b747-b9ea-4af1-84c5-3f5835d1ba1f.jpg?1783935324"
    }
}
