package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Boots of Speed
 * {R}
 * Artifact — Equipment
 * Equipped creature gets +1/+0 and has haste.
 * Equip {1}
 */
val BootsOfSpeed = card("Boots of Speed") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 and has haste.\nEquip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"
    staticAbility {
        ability = ModifyStats(1, 0)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.HASTE)
    }
    equipAbility("{1}")
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Chris Rallis"
        flavorText = "Click your heels and become the wind."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db6d9913-3d4f-4f47-ba3a-6586a50ddbd7.jpg?1783926483"
    }
}
