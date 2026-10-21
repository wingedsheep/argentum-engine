package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Ranger's Longbow
 * {1}{G}
 * Artifact — Equipment
 * Equipped creature gets +2/+1 and has reach.
 * Equip {3}
 */
val RangersLongbow = card("Ranger's Longbow") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+1 and has reach.\nEquip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"
    staticAbility {
        ability = ModifyStats(2, 1)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.REACH)
    }
    equipAbility("{3}")
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Marcela Medeiros"
        flavorText = "With the right weapon, a fearsome beholder is nothing more than a big floating bullseye."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34c99b32-353c-4930-a6ee-3fb44cf70253.jpg?1783926457"
    }
}
