package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Veteran's Sidearm
 * {2}
 * Artifact — Equipment
 * Equipped creature gets +1/+1.
 * Equip {1}
 */
val VeteransSidearm = card("Veteran's Sidearm") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\nEquip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter.attachedCreature()
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "242"
        artist = "Aaron Miller"
        flavorText = "\"I've broken three swords, eighteen lances, and countless shields, but this little blade has survived every battle, just like I have.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c1b3e7d-0fd8-4324-be7b-382e75ae9c17.jpg?1783938307"
    }
}
