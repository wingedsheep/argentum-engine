package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Riot Gear
 * {2}
 * Artifact — Equipment
 * Equipped creature gets +1/+2.
 * Equip {2}
 */
val RiotGear = card("Riot Gear") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+2.\nEquip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 2, Filters.EquippedCreature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "236"
        artist = "Jack Wang"
        flavorText = "Any meeting between guilds requires proper equipment."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3be1289-76f9-40b3-9387-b76a8b8d8797.jpg"
    }
}
