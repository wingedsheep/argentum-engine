package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Way of the Thief
 * {3}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2.
 * Enchanted creature can't be blocked as long as you control a Gate.
 *
 * The evasion is Bilbo's Ring's conditional host-scoped [CantBeBlocked]; "you" is the Aura's
 * controller, so the Gate check follows whoever controls Way of the Thief.
 */
val WayOfTheThief = card("Way of the Thief") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +2/+2.\n" +
        "Enchanted creature can't be blocked as long as you control a Gate."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = CantBeBlocked(Filters.EnchantedCreature)
        condition = Conditions.YouControl(GameObjectFilter.Land.withSubtype("Gate"))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Igor Kieryluk"
        flavorText = "A true shortcut isn't a way other people don't know; it's a way other people can't go."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b249ca81-bd8d-4d3d-81d6-15e8d669c416.jpg?1783940133"
    }
}
