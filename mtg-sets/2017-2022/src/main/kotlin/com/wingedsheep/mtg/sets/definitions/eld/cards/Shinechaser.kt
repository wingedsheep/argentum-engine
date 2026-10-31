package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shinechaser
 * {1}{W}{U}
 * Creature — Faerie
 * 1/1
 *
 * Flying, vigilance
 * This creature gets +1/+1 as long as you control an artifact.
 * This creature gets +1/+1 as long as you control an enchantment.
 *
 * Two independent [ConditionalStaticAbility] bonuses over [Filters.Self], each gated by an
 * [Exists] check — an artifact enchantment satisfies both at once.
 */
val Shinechaser = card("Shinechaser") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Faerie"
    oracleText = "Flying, vigilance\n" +
        "This creature gets +1/+1 as long as you control an artifact.\n" +
        "This creature gets +1/+1 as long as you control an enchantment."
    power = 1
    toughness = 1

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, Filters.Self),
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Artifact)
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, Filters.Self),
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Enchantment)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Evan Shipard"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6d5314b-4994-4cd0-a680-12e56be7c1e2.jpg"
    }
}
