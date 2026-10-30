package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Inspiring Veteran
 * {R}{W}
 * Creature — Human Knight
 * 2/2
 *
 * Other Knights you control get +1/+1.
 */
val InspiringVeteran = card("Inspiring Veteran") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Knight"
    oracleText = "Other Knights you control get +1/+1."
    power = 2
    toughness = 2

    staticAbility {
        ability = ModifyStats(
            1, 1,
            GroupFilter(GameObjectFilter.Permanent.withSubtype("Knight").youControl(), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Scott Murphy"
        flavorText = "\"I fight for my daughter, who may not set foot on a battlefield for many years. Remember who you fight for.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c3f372d-259d-4a31-9491-2d369b3f3f8b.jpg?1783932596"
    }
}
