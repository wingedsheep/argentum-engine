package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.forMirrodin
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ReduceEquipCost

/**
 * Bladehold War-Whip
 * {1}{R}{W}
 * Artifact — Equipment
 * For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)
 * Equip abilities you activate of other Equipment cost {1} less to activate.
 * Equipped creature has double strike.
 * Equip {3}{R}{W}
 */
val BladeholdWarWhip = card("Bladehold War-Whip") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Artifact — Equipment"
    oracleText = "For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)\n" +
        "Equip abilities you activate of other Equipment cost {1} less to activate.\n" +
        "Equipped creature has double strike.\n" +
        "Equip {3}{R}{W}"

    forMirrodin()

    staticAbility {
        ability = ReduceEquipCost(amount = 1, onlyOtherEquip = true)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.DOUBLE_STRIKE, Filters.EquippedCreature)
    }

    equipAbility("{3}{R}{W}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Tony Foti"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/563e9ba5-e0ec-4fb3-8301-689aa145cc19.jpg?1783918004"
    }
}
