package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Dub
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2, has first strike, and is a Knight in addition to its other types.
 */
val Dub = card("Dub") {
    manaCost = "{2}{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +2/+2, has first strike, and is a Knight in addition to its other types."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE)
    }

    staticAbility {
        ability = GrantSubtype("Knight")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Bastien L. Deharme"
        flavorText = "\"Rise, knight of New Benalia.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dca49646-970a-4b91-9cae-5dbdef9f7727.jpg?1562744048"
    }
}
