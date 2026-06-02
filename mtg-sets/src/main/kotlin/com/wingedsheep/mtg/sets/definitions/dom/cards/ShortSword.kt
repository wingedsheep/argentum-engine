package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Short Sword
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +1/+1.
 * Equip {1}
 */
val ShortSword = card("Short Sword") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\nEquip {1}"

    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "229"
        artist = "John Severin Brassell"
        flavorText = "\"Sometimes the only difference between a martyr and a hero is a sword.\" —Captain Sisay, Memoirs"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb79a623-21c3-4310-bc76-310935511d45.jpg?1592322777"
    }
}
