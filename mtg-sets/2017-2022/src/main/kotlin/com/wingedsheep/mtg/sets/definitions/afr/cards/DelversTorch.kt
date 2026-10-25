package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Delver's Torch
 * {1}{W}
 * Artifact — Equipment
 * Equipped creature gets +1/+1.
 * Whenever equipped creature attacks, venture into the dungeon. (Enter the first room or advance to the next room.)
 * Equip {3}
 */
val DelversTorch = card("Delver's Torch") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "Whenever equipped creature attacks, venture into the dungeon. (Enter the first room or advance to the next room.)\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.VentureIntoTheDungeon()
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Bram Sels"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53df7b80-71bf-4013-ad44-f76eae8ac81b.jpg?1783926535"
    }
}
