package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kobold Drill Sergeant
 * {1}{R}
 * Creature — Kobold Soldier
 * 1/2
 *
 * Other Kobold creatures you control get +0/+1 and have trample.
 */
val KoboldDrillSergeant = card("Kobold Drill Sergeant") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Kobold Soldier"
    oracleText = "Other Kobold creatures you control get +0/+1 and have trample."
    power = 1
    toughness = 2

    val otherKobolds = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype("Kobold")).youControl(), excludeSelf = true)

    staticAbility {
        ability = ModifyStats(powerBonus = 0, toughnessBonus = 1, filter = otherKobolds)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, otherKobolds)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "Julie Baroh"
        flavorText = "\"Joining this army is easy, boy. Just survive your first battle.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/4/741b14f8-625d-41be-a734-0efe042a6ee8.jpg?1783948055"
    }
}
