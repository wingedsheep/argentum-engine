package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAdditionalTypesToGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Yavimaya, Cradle of Growth — Modern Horizons 2 #261 (canonical printing)
 * Legendary Land
 *
 * Each land is a Forest in addition to its other land types.
 *
 * A layer-4 type-adding static over every land on the battlefield (both players', Yavimaya itself
 * included) — the same spelling as Urborg, Tomb of Yawgmoth. The engine derives each basic land type's intrinsic mana ability from
 * the projected subtypes, so every land gains "{T}: Add {G}." (CR 305.6) — which is also how
 * Yavimaya itself taps for mana. Land cards off the battlefield are unaffected.
 */
val YavimayaCradleOfGrowth = card("Yavimaya, Cradle of Growth") {
    typeLine = "Legendary Land"
    oracleText = "Each land is a Forest in addition to its other land types."

    staticAbility {
        ability = GrantAdditionalTypesToGroup(
            filter = GroupFilter.AllLands,
            addSubtypes = listOf("Forest")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "261"
        artist = "Sarah Finnigan"
        flavorText = "\"Multani's heart is a seed, and all of Yavimaya is its flower. There is as much life here " +
            "as in the rest of Dominaria together.\"\n—Karn"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e4b6e22-93b2-4896-bba5-0ceaa5d8ea3c.jpg?1783926791"
        ruling("2021-06-18", "Yavimaya, Cradle of Growth isn't a Forest while it's not on the battlefield.")
        ruling("2021-06-18", "Land cards not on the battlefield aren't Forests while Yavimaya is on the battlefield.")
        ruling("2021-06-18", "Yavimaya's ability causes each land on the battlefield to have the land type Forest. Any land that's a Forest has the ability \"{T}: Add {G}.\" Nothing else changes about those lands, including their names, other subtypes, and whether they're legendary, basic, or snow.")
    }
}
