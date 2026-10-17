package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Most Wanted
 * {2}{G}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * Enchanted creature gets +2/+1.
 * When enchanted creature dies, create two Treasure tokens.
 *
 * The Treasures go to Most Wanted's controller, regardless of who controlled the enchanted creature.
 */
val MostWanted = card("Most Wanted") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "Enchanted creature gets +2/+1.\n" +
        "When enchanted creature dies, create two Treasure tokens."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 1, Filters.EnchantedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.CreateTreasure(count = 2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Rémi Jacquot"
        flavorText = "Overnight, Elspeth was branded the most dangerous criminal in New Capenna."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/573c0f68-9ed8-4657-b709-b7f3804aaa68.jpg?1783923100"
    }
}
