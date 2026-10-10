package com.wingedsheep.mtg.sets.definitions.plc.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAdditionalTypesToGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Urborg, Tomb of Yawgmoth — Planar Chaos #165 (canonical printing)
 * Legendary Land · Rare
 *
 * Each land is a Swamp in addition to its other land types.
 *
 * A Layer 4 type-adding static over every land on the battlefield (Urborg included). The
 * "{T}: Add {B}" each land gains is not scripted: it is the Swamp type's intrinsic mana ability,
 * derived from the projected subtype by the engine's `IntrinsicManaAbilities`. The
 * group is battlefield-only, so land cards in other zones are not Swamps (ruling 2021-03-19).
 */
val UrborgTombOfYawgmoth = card("Urborg, Tomb of Yawgmoth") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Legendary Land"
    oracleText = "Each land is a Swamp in addition to its other land types."

    staticAbility {
        ability = GrantAdditionalTypesToGroup(
            filter = GroupFilter.AllLands,
            addSubtypes = listOf("Swamp")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "John Avon"
        flavorText = "\"Yawgmoth's corpse is a wound in the universe. His foul blood seeps out, " +
            "infecting the land with his final curse.\"\n—Lord Windgrace"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19e1224f-82cb-4f41-8739-f880cba61bbb.jpg?1783943133"
        ruling("2021-03-19", "Urborg's ability causes each land on the battlefield to have the land type Swamp. Any land that's a Swamp has the ability \"{T}: Add {B}.\" Nothing else changes about those lands, including their names, other subtypes, other abilities, and whether they're legendary, basic, or snow.")
        ruling("2021-03-19", "Land cards not on the battlefield aren't Swamps while Urborg is on the battlefield.")
        ruling("2021-03-19", "If an effect such as that of Magus of the Moon causes Urborg to lose its abilities by setting it to a basic land type not in addition to its other types, it won't turn lands into Swamps, no matter in what order those effects started to apply.")
    }
}
