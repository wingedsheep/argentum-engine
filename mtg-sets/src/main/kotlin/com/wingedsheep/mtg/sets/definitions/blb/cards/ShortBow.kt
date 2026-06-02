package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Short Bow
 * {2}
 * Artifact — Equipment
 *
 * Equipped creature gets +1/+1 and has vigilance and reach.
 * Equip {1}
 */
val ShortBow = card("Short Bow") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 and has vigilance and reach.\nEquip {1}"

    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH, Filters.EquippedCreature)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "248"
        artist = "Zara Alfonso"
        flavorText = "Take heart. Take aim. Take them down."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51d8b72b-fa8f-48d3-bddc-d3ce9b8ba2ea.jpg?1721427294"
    }
}
