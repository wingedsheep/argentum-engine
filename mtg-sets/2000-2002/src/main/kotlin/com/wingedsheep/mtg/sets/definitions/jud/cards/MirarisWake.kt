package com.wingedsheep.mtg.sets.definitions.jud.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Mirari's Wake
 * {3}{G}{W}
 * Enchantment
 * Creatures you control get +1/+1.
 * Whenever you tap a land for mana, add one mana of any type that land produced.
 *
 * The mana line is Mana Flare's mirror form of [AdditionalManaOnSourceTap] (`color = null`
 * adds one mana of a type the land produced), narrowed to lands you control.
 */
val MirarisWake = card("Mirari's Wake") {
    manaCost = "{3}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Enchantment"
    oracleText = "Creatures you control get +1/+1.\n" +
        "Whenever you tap a land for mana, add one mana of any type that land produced."

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter.AllCreaturesYouControl)
    }

    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Land.youControl(),
            color = null // mirror the produced type ("any type that land produced")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "David Martin"
        flavorText = "The land drank power from the Mirari as though it had thirsted forever."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5ddad46-5e2e-43c9-8c91-7aca6ca23562.jpg?1783945107"
        ruling("2021-06-18", "If tapping a land produces more than one type of mana, you choose one type that was produced and add one mana of that type.")
        ruling("2021-06-18", "If the mana produced by the land has any additional restrictions or riders, those restrictions or riders don't apply to the mana added by Mirari's Wake. You just get one mana of the appropriate type.")
    }
}
