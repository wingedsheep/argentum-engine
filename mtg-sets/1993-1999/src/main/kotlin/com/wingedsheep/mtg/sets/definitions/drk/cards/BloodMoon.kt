package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SetLandTypesForGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/** Blood Moon — nonbasic lands are Mountains. */
val BloodMoon = card("Blood Moon") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Nonbasic lands are Mountains."

    staticAbility {
        ability = SetLandTypesForGroup(
            filter = GroupFilter(GameObjectFilter.NonbasicLand),
            landTypes = setOf("Mountain"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Tom Wänerstrand"
        flavorText = "Heavy light flooded across the landscape, cloaking everything in deep crimson."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78373616-e2d6-4ccf-998f-09f02bea45b4.jpg?1783947937"
        ruling("2020-08-07", "Nonbasic lands lose their other land types and abilities, gain the Mountain land type, and gain the ability \"{T}: Add {R}.\"")
        ruling("2020-08-07", "The effect doesn't change a land's name or supertypes, so it doesn't make a land basic or remove the legendary supertype.")
    }
}
