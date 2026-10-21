package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Greataxe
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +4/+0.
 * Equip {5}
 */
val Greataxe = card("Greataxe") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +4/+0.\nEquip {5} ({5}: Attach to target creature you control. Equip only as a sorcery.)"
    staticAbility {
        ability = ModifyStats(4, 0)
    }
    equipAbility("{5}")
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "245"
        artist = "Kari Christensen"
        flavorText = "\"If you're so mighty, scaled one, then why are you dead?\""
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e840aef6-eec4-40dd-bbc4-6fd0b2b57805.jpg?1783926439"
    }
}
