package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Hard Cover (M20 #63)
 * {U}  Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +0/+2 and has "{T}: Draw a card, then discard a card."
 */
val HardCover = card("Hard Cover") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +0/+2 and has \"{T}: Draw a card, then discard a card.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(0, 2)
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Patterns.Hand.loot()
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "PINDURSKI"
        flavorText = "A good book is a refuge, but a great book is a fortress."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e16c9f3a-534b-42d4-9765-8d1cb15375aa.jpg"
    }
}
