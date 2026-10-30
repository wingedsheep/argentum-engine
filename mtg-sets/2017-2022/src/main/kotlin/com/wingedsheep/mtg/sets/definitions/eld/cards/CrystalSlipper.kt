package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Crystal Slipper
 * {1}{R}
 * Artifact — Equipment
 *
 * Equipped creature gets +1/+0 and has haste.
 * Equip {1}
 */
val CrystalSlipper = card("Crystal Slipper") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 and has haste.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(+1, 0, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, Filters.EquippedCreature)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Howard Lyon"
        flavorText = "\"After the death of her mother, Cassia found that things that appear fragile can be very strong indeed.\"\n—*Beyond the Great Henge*"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f738ca6-5254-4dbc-9f59-854e81c8dac2.jpg?1783932626"
    }
}
