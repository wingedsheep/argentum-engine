package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Spirit Away
 * {5}{U}{U}
 * Enchantment — Aura
 * Enchant creature
 * You control enchanted creature.
 * Enchanted creature gets +2/+2 and has flying.
 */
val SpiritAway = card("Spirit Away") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nYou control enchanted creature.\nEnchanted creature gets +2/+2 and has flying."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ControlEnchantedPermanent
    }

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "76"
        artist = "Greg Staples"
        flavorText = "The fear of slipping from the geist's tenuous grip overwhelmed Tolo's joy at his first flight."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8823bdc-0467-47f1-9bef-a281b4a7071d.jpg?1783940710"
    }
}
