package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Guise of Fire
 * {R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/-1 and attacks each combat if able.
 */
val GuiseOfFire = card("Guise of Fire") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/-1 and attacks each combat if able."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, -1)
    }

    staticAbility {
        ability = MustAttack()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Dave Kendall"
        flavorText = "\"Fire will eventually destroy a zombie, but a fiery zombie destroys a lot of other " +
            "things first.\"\n—Rem Karolus, Blade of the Inquisitors"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/beb10d42-fa19-400c-bad8-ec3827f077bc.jpg?1783940684"
        ruling(
            "2012-05-01",
            "The controller of the enchanted creature still decides which player or planeswalker the creature attacks.",
        )
    }
}
