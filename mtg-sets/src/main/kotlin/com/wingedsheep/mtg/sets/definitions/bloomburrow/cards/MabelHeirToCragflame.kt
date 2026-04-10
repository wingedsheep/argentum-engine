package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Mabel, Heir to Cragflame {1}{R}{W}
 * Legendary Creature — Mouse Soldier
 * 3/3
 *
 * Other Mice you control get +1/+1.
 * When Mabel enters, create Cragflame, a legendary colorless Equipment artifact
 * token with "Equipped creature gets +1/+1 and has vigilance, trample, and haste"
 * and equip {2}.
 */
val MabelHeirToCragflame = card("Mabel, Heir to Cragflame") {
    manaCost = "{1}{R}{W}"
    typeLine = "Legendary Creature — Mouse Soldier"
    oracleText = "Other Mice you control get +1/+1.\nWhen Mabel enters, create Cragflame, a legendary colorless Equipment artifact token with \"Equipped creature gets +1/+1 and has vigilance, trample, and haste\" and equip {2}."
    power = 3
    toughness = 3

    // Other Mice you control get +1/+1
    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype("Mouse").youControl(),
                excludeSelf = true
            )
        )
    }

    // ETB: create Cragflame Equipment token
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreatePredefinedTokenEffect("Cragflame")
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Aurore Folny"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be6627fd-729d-44f2-b6bf-5299f49d1e3d.jpg?1721427144"
    }
}
