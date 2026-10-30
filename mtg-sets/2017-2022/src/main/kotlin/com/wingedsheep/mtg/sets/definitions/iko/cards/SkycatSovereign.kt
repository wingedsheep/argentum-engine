package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Skycat Sovereign
 * {W}{U}
 * Creature — Elemental Cat
 * 1/1
 * Flying
 * This creature gets +1/+1 for each other creature you control with flying.
 * {2}{W}{U}: Create a 1/1 white Cat Bird creature token with flying.
 */
val SkycatSovereign = card("Skycat Sovereign") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Elemental Cat"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "This creature gets +1/+1 for each other creature you control with flying.\n" +
        "{2}{W}{U}: Create a 1/1 white Cat Bird creature token with flying."

    keywords(Keyword.FLYING)

    staticAbility {
        val otherFliers = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
            excludeSelf = true
        ).count()
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = otherFliers,
            toughnessBonus = otherFliers
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}{U}")
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Cat", "Bird"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/8/1/816da759-e224-4960-b2b6-453cadc2faf1.jpg?1783930948",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Slawomir Maniak"
        flavorText = "The bounty on its head is as old as Skysail, but no one has ever tried to collect."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/6209dbfd-b765-44c5-b3ad-94f6e0705926.jpg?1783931018"
    }
}
