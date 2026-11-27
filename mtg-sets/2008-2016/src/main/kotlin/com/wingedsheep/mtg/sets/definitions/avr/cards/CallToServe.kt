package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Call to Serve
 * {1}{W}
 * Enchantment — Aura
 * Enchant nonblack creature
 * Enchanted creature gets +1/+2, has flying, and is an Angel in addition to its other types.
 */
val CallToServe = card("Call to Serve") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant nonblack creature\n" +
        "Enchanted creature gets +1/+2, has flying, and is an Angel in addition to its other types."

    auraTarget = TargetObject(filter = TargetFilter.Creature.notColor(Color.BLACK))

    staticAbility {
        ability = ModifyStats(1, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    staticAbility {
        // GrantSubtype defaults its filter to the source; an Aura must name the enchanted creature.
        ability = GrantSubtype("Angel", Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Jaime Jones"
        flavorText = "Grace is a gift, never a reward."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce4a3e80-6e95-4346-8ab8-eecc1a09ca24.jpg?1783940740"
    }
}
